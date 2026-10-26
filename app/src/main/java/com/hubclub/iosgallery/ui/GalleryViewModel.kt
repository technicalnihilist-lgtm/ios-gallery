package com.hubclub.iosgallery.ui

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hubclub.iosgallery.data.FileOps
import com.hubclub.iosgallery.data.MediaRepository
import com.hubclub.iosgallery.data.MetaStore
import com.hubclub.iosgallery.data.AppSettings
import com.hubclub.iosgallery.data.SettingsStore
import com.hubclub.iosgallery.model.DeviceAlbum
import com.hubclub.iosgallery.model.ManagedAlbum
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.model.MetaState
import com.hubclub.iosgallery.model.TrashEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrashOutcome(
    val entries: List<TrashEntry>,
    val consent: IntentSender?
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val metaStore = MetaStore(app)
    private val settingsStore = SettingsStore(app)

    val settings: StateFlow<AppSettings> = settingsStore.state

    val meta: StateFlow<MetaState> = metaStore.state

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            purgeExpiredTrash()
            load(showSpinner = true)
        }
    }

    fun refresh() {
        viewModelScope.launch { load(showSpinner = false) }
    }

    private suspend fun load(showSpinner: Boolean) {
        if (showSpinner) _loading.value = true
        val loaded = repository.loadLibrary()
        _items.value = loaded
        _loading.value = false
    }

    private suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
        val expired = metaStore.state.value.trash.filter { it.deletedAt < cutoff }.map { it.id }
        if (expired.isNotEmpty()) metaStore.purgeTrash(expired)
    }

    // ------------------------------------------------------------ appearance

    fun setDockScale(value: Float) = settingsStore.setDockScale(value)

    fun setGlassOpacity(value: Float) = settingsStore.setGlassOpacity(value)

    fun setThemeMode(value: String) = settingsStore.setThemeMode(value)

    fun setGridColumns(value: Int) = settingsStore.setGridColumns(value)

    fun resetSettings() = settingsStore.reset()

    /** Device folders, derived from the media index. */
    fun deviceAlbums(items: List<MediaItem>): List<DeviceAlbum> = repository.buildDeviceAlbums(items)

    // ------------------------------------------------------------- favourites

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { metaStore.toggleFavorite(id) }
    }

    fun setFavorite(ids: Collection<Long>, favorite: Boolean) {
        viewModelScope.launch { metaStore.setFavorites(ids, favorite) }
    }

    // ----------------------------------------------------------------- albums

    fun createAlbum(name: String, onCreated: (ManagedAlbum) -> Unit) {
        viewModelScope.launch { onCreated(metaStore.createAlbum(name)) }
    }

    fun renameAlbum(albumId: String, name: String) {
        viewModelScope.launch { metaStore.renameAlbum(albumId, name) }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch { metaStore.deleteAlbum(albumId) }
    }

    fun addToAlbum(albumId: String, ids: Collection<Long>) {
        viewModelScope.launch { metaStore.addToAlbum(albumId, ids) }
    }

    fun removeFromAlbum(albumId: String, ids: Collection<Long>) {
        viewModelScope.launch { metaStore.removeFromAlbum(albumId, ids) }
    }

    // ------------------------------------------------------------------ trash

    /**
     * Soft delete: copies the bytes into the app's private trash first, then asks
     * Android to remove the original. If the system needs confirmation, the
     * returned IntentSender must be launched by the UI.
     */
    suspend fun moveToTrash(targets: List<MediaItem>): TrashOutcome {
        val context = getApplication<Application>()
        val trashDir = metaStore.trashDirectory()
        val entries = mutableListOf<TrashEntry>()
        for (item in targets) {
            val entry = FileOps.copyIntoTrash(context, item, trashDir) ?: continue
            entries.add(entry)
            metaStore.addTrashEntry(entry)
        }
        val uris = targets.map { it.uri }
        val deleted = FileOps.deleteNow(context, uris)
        val consent = if (deleted < uris.size) FileOps.deleteConsent(context, uris) else null
        return TrashOutcome(entries, consent)
    }

    fun discardTrash(entries: List<TrashEntry>) {
        viewModelScope.launch {
            val dir = metaStore.trashDirectory()
            entries.forEach { FileOps.deleteTrashFile(dir, it) }
            metaStore.purgeTrash(entries.map { it.id })
        }
    }

    fun restoreFromTrash(entry: TrashEntry, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val ok = FileOps.restore(context, entry, metaStore.trashDirectory())
            if (ok) metaStore.removeTrashEntry(entry.id)
            onDone(ok)
            if (ok) load(showSpinner = false)
        }
    }

    fun deletePermanently(entries: List<TrashEntry>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val dir = metaStore.trashDirectory()
            entries.forEach { FileOps.deleteTrashFile(dir, it) }
            metaStore.purgeTrash(entries.map { it.id })
            onDone()
        }
    }

    // ------------------------------------------------------------- file ops

    fun renameMedia(item: MediaItem, newName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = FileOps.rename(getApplication<Application>(), item, newName)
            if (ok) load(showSpinner = false)
            onDone(ok)
        }
    }
}
