package com.hubclub.iosgallery.model

import android.net.Uri

/** A single photo or video that lives on the device. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isVideo: Boolean,
    val dateMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val bucketId: Long,
    val bucketName: String,
    val durationMs: Long
) {
    val isImage: Boolean get() = !isVideo
}

/** An album that simply mirrors a folder on the device (read only). */
data class DeviceAlbum(
    val bucketId: Long,
    val name: String,
    val cover: MediaItem?,
    val count: Int,
    val items: List<MediaItem>
)

/** An album created inside iOS Gallery. Originals are never moved. */
data class ManagedAlbum(
    val id: String,
    val name: String,
    val itemIds: List<Long>,
    val createdAt: Long
)

/** An item parked in the app-managed "Recently Deleted" area. */
data class TrashEntry(
    val id: String,
    val name: String,
    val mimeType: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateMillis: Long,
    val deletedAt: Long,
    val fileName: String
)

/** Everything the app remembers between launches. */
data class MetaState(
    val favorites: Set<Long> = emptySet(),
    val albums: List<ManagedAlbum> = emptyList(),
    val trash: List<TrashEntry> = emptyList()
)
