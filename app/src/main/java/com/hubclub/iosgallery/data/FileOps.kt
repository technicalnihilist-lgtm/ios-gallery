package com.hubclub.iosgallery.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.model.TrashEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * File operations that go through Android's own media APIs so that the system
 * keeps ownership of the user's originals. Nothing is deleted or changed
 * without an explicit user action, and deletions the app is not allowed to make
 * are handed to the system's own confirmation dialog.
 */
object FileOps {

    // ------------------------------------------------------------------ share

    fun shareIntent(items: List<MediaItem>): Intent {
        val intent = if (items.size == 1) {
            val item = items.first()
            Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { if (item.isVideo) "video/*" else "image/*" }
                putExtra(Intent.EXTRA_STREAM, item.uri)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(intent, "Share")
    }

    // ------------------------------------------------------------ copy / move

    suspend fun copyInto(context: Context, item: MediaItem, dir: DocumentFile): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val mime = item.mimeType.ifBlank { "application/octet-stream" }
                val target = dir.createFile(mime, availableName(dir, item.name)) ?: return@runCatching null
                val input = context.contentResolver.openInputStream(item.uri)
                    ?: return@runCatching null
                input.use { source ->
                    context.contentResolver.openOutputStream(target.uri)?.use { sink ->
                        source.copyTo(sink, DEFAULT_BUFFER_SIZE * 4)
                    }
                }
                target.uri
            }.getOrNull()
        }

    private fun availableName(dir: DocumentFile, name: String): String {
        if (dir.findFile(name) == null) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (index < 500) {
            val candidate = "$base ($index)$ext"
            if (dir.findFile(candidate) == null) return candidate
            index++
        }
        return "$base-${System.currentTimeMillis()}$ext"
    }

    // ----------------------------------------------------------------- rename

    suspend fun rename(context: Context, item: MediaItem, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                }
                context.contentResolver.update(item.uri, values, null, null) > 0
            }.getOrDefault(false)
        }

    /** Android 11+ asks the user to allow modifying media the app does not own. */
    fun writeConsent(context: Context, uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaStore.createWriteRequest(context.contentResolver, uris).intentSender
        }.getOrNull()
    }

    // ----------------------------------------------------------------- delete

    /** The system dialog that confirms deleting other apps' media (Android 11+). */
    fun deleteConsent(context: Context, uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        }.getOrNull()
    }

    suspend fun deleteNow(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        uris.forEach { uri ->
            runCatching {
                deleted += context.contentResolver.delete(uri, null, null)
            }
        }
        deleted
    }

    // ------------------------------------------------------ trash (soft delete)

    /**
     * Copies the item's bytes into the app's private trash folder and returns
     * the index entry. The original is untouched at this point.
     */
    suspend fun copyIntoTrash(context: Context, item: MediaItem, trashDir: File): TrashEntry? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!trashDir.exists()) trashDir.mkdirs()
                val extension = item.name.substringAfterLast('.', "")
                val fileName = "${UUID.randomUUID()}${if (extension.isNotBlank()) ".$extension" else ""}"
                val destination = File(trashDir, fileName)
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
                    }
                } ?: return@runCatching null
                TrashEntry(
                    id = UUID.randomUUID().toString(),
                    name = item.name,
                    mimeType = item.mimeType,
                    isVideo = item.isVideo,
                    sizeBytes = item.sizeBytes,
                    width = item.width,
                    height = item.height,
                    dateMillis = item.dateMillis,
                    deletedAt = System.currentTimeMillis(),
                    fileName = fileName
                )
            }.getOrNull()
        }

    /** Writes a trashed item back into the device's media collection. */
    suspend fun restore(context: Context, entry: TrashEntry, trashDir: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = File(trashDir, entry.fileName)
                if (!source.exists()) return@runCatching false
                val collection =
                    if (entry.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val folder =
                    if (entry.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, entry.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, entry.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/iOS Gallery")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val uri = context.contentResolver.insert(collection, values)
                    ?: return@runCatching false
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { it.copyTo(output, DEFAULT_BUFFER_SIZE * 4) }
                } ?: return@runCatching false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                source.delete()
                true
            }.getOrDefault(false)
        }

    fun deleteTrashFile(trashDir: File, entry: TrashEntry) {
        runCatching { File(trashDir, entry.fileName).delete() }
    }
}
