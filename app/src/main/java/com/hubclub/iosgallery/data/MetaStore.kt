package com.hubclub.iosgallery.data

import android.content.Context
import com.hubclub.iosgallery.model.ManagedAlbum
import com.hubclub.iosgallery.model.MetaState
import com.hubclub.iosgallery.model.TrashEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Favourites, app-managed albums and the Recently Deleted index.
 * Stored as a single small JSON file in the app's private storage — never
 * uploaded, never backed up.
 */
class MetaStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "gallery_meta.json")
    private val trashDir = File(context.applicationContext.filesDir, "trash")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<MetaState> = _state.asStateFlow()

    fun trashDirectory(): File {
        if (!trashDir.exists()) trashDir.mkdirs()
        return trashDir
    }

    // ---------------------------------------------------------------- loading

    private fun load(): MetaState {
        if (!file.exists()) return MetaState()
        return try {
            val root = JSONObject(file.readText())
            val favorites = mutableSetOf<Long>()
            root.optJSONArray("favorites")?.let { arr ->
                for (i in 0 until arr.length()) favorites.add(arr.optLong(i))
            }
            val albums = mutableListOf<ManagedAlbum>()
            root.optJSONArray("albums")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val ids = mutableListOf<Long>()
                    o.optJSONArray("itemIds")?.let { idArr ->
                        for (j in 0 until idArr.length()) ids.add(idArr.optLong(j))
                    }
                    albums.add(
                        ManagedAlbum(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Album"),
                            itemIds = ids,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            val trash = mutableListOf<TrashEntry>()
            root.optJSONArray("trash")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    trash.add(
                        TrashEntry(
                            id = o.optString("id"),
                            name = o.optString("name", "Photo"),
                            mimeType = o.optString("mimeType", "image/jpeg"),
                            isVideo = o.optBoolean("isVideo", false),
                            sizeBytes = o.optLong("sizeBytes"),
                            width = o.optInt("width"),
                            height = o.optInt("height"),
                            dateMillis = o.optLong("dateMillis"),
                            deletedAt = o.optLong("deletedAt"),
                            fileName = o.optString("fileName")
                        )
                    )
                }
            }
            MetaState(favorites, albums, trash)
        } catch (t: Throwable) {
            MetaState()
        }
    }

    private fun persist(state: MetaState) {
        try {
            val root = JSONObject()
            root.put("favorites", JSONArray(state.favorites.toList()))
            val albums = JSONArray()
            state.albums.forEach { album ->
                albums.put(
                    JSONObject().apply {
                        put("id", album.id)
                        put("name", album.name)
                        put("createdAt", album.createdAt)
                        put("itemIds", JSONArray(album.itemIds))
                    }
                )
            }
            root.put("albums", albums)
            val trash = JSONArray()
            state.trash.forEach { entry ->
                trash.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("name", entry.name)
                        put("mimeType", entry.mimeType)
                        put("isVideo", entry.isVideo)
                        put("sizeBytes", entry.sizeBytes)
                        put("width", entry.width)
                        put("height", entry.height)
                        put("dateMillis", entry.dateMillis)
                        put("deletedAt", entry.deletedAt)
                        put("fileName", entry.fileName)
                    }
                )
            }
            root.put("trash", trash)
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(root.toString())
                tmp.delete()
            }
        } catch (t: Throwable) {
            // A failed write must never crash the gallery.
        }
    }

    private suspend fun update(block: (MetaState) -> MetaState) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = block(_state.value)
            _state.value = next
            persist(next)
        }
    }

    // -------------------------------------------------------------- favourites

    suspend fun toggleFavorite(id: Long) = update { current ->
        val favorites = current.favorites.toMutableSet()
        if (!favorites.add(id)) favorites.remove(id)
        current.copy(favorites = favorites)
    }

    suspend fun setFavorites(ids: Collection<Long>, favorite: Boolean) = update { current ->
        val favorites = current.favorites.toMutableSet()
        if (favorite) favorites.addAll(ids) else favorites.removeAll(ids.toSet())
        current.copy(favorites = favorites)
    }

    // ------------------------------------------------------------------ albums

    suspend fun createAlbum(name: String): ManagedAlbum {
        val album = ManagedAlbum(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "New Album" },
            itemIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        update { it.copy(albums = it.albums + album) }
        return album
    }

    suspend fun renameAlbum(albumId: String, name: String) = update { current ->
        current.copy(
            albums = current.albums.map {
                if (it.id == albumId) it.copy(name = name.trim().ifBlank { it.name }) else it
            }
        )
    }

    suspend fun deleteAlbum(albumId: String) = update { current ->
        current.copy(albums = current.albums.filterNot { it.id == albumId })
    }

    suspend fun addToAlbum(albumId: String, ids: Collection<Long>) = update { current ->
        current.copy(
            albums = current.albums.map { album ->
                if (album.id == albumId) album.copy(itemIds = (album.itemIds + ids).distinct())
                else album
            }
        )
    }

    suspend fun removeFromAlbum(albumId: String, ids: Collection<Long>) = update { current ->
        current.copy(
            albums = current.albums.map { album ->
                if (album.id == albumId) album.copy(itemIds = album.itemIds - ids.toSet())
                else album
            }
        )
    }

    // ------------------------------------------------------------------- trash

    suspend fun addTrashEntry(entry: TrashEntry) = update { current ->
        current.copy(trash = current.trash + entry)
    }

    suspend fun removeTrashEntry(entryId: String) = update { current ->
        current.copy(trash = current.trash.filterNot { it.id == entryId })
    }

    /** Removes trash entries older than [days] together with their cached bytes. */
    suspend fun purgeTrash(entryIds: Collection<String>) {
        val entries = _state.value.trash.filter { it.id in entryIds }
        withContext(Dispatchers.IO) {
            entries.forEach { runCatching { File(trashDir, it.fileName).delete() } }
        }
        update { current -> current.copy(trash = current.trash.filterNot { it.id in entryIds }) }
    }

    /** Drops references to media that no longer exists on the device. */
    suspend fun pruneMissing(existingIds: Set<Long>) = update { current ->
        val albums = current.albums.map { album ->
            album.copy(itemIds = album.itemIds.filter { it in existingIds })
        }
        current.copy(albums = albums)
    }
}
