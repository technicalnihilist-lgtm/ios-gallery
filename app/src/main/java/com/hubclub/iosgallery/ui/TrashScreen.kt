package com.hubclub.iosgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.hubclub.iosgallery.model.TrashEntry
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.formatCount
import com.hubclub.iosgallery.util.formatDate
import com.hubclub.iosgallery.util.formatSize
import java.io.File

@Composable
fun TrashScreen(
    entries: List<TrashEntry>,
    trashDir: File,
    vm: GalleryViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val palette = GalleryTheme.palette
    var selected by remember { mutableStateOf<TrashEntry?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TrashEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(title = "Recently Deleted", onBack = onBack) {
            if (entries.isNotEmpty()) {
                Text(
                    text = "Empty",
                    color = Color(0xFFFF3B30),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable { confirmEmpty = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            EmptyState(
                icon = GalleryIcons.Trash,
                title = "Nothing Deleted",
                message = "Photos you delete move here first, so you can always put them back. Items are removed automatically after 30 days."
            )
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Items here stay on this device for 30 days. Tap one to restore it or delete it permanently.",
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = entries.size, key = { entries[it].id }) { index ->
                        val entry = entries[index]
                        TrashThumbnail(entry = entry, file = File(trashDir, entry.fileName)) {
                            selected = entry
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    val current = selected
    GalleryActionSheet(
        visible = current != null,
        title = current?.let { "${it.name} · ${formatSize(it.sizeBytes)} · deleted ${formatDate(it.deletedAt)}" },
        actions = listOf(
            SheetAction("Restore", GalleryIcons.Restore) {
                current?.let { entry ->
                    vm.restoreFromTrash(entry) { ok ->
                        onMessage(if (ok) "Restored to your library" else "Couldn't restore that item")
                    }
                }
            },
            SheetAction("Delete Permanently", GalleryIcons.Trash, destructive = true) {
                confirmDelete = current
            }
        ),
        onDismiss = { selected = null }
    )

    val deleteTarget = confirmDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            title = "Delete Permanently?",
            message = "“${deleteTarget.name}” will be removed from this device for good. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { confirmDelete = null },
            onConfirm = {
                vm.deletePermanently(listOf(deleteTarget)) { onMessage("Deleted permanently") }
                confirmDelete = null
            }
        )
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Empty Recently Deleted?",
            message = "All ${formatCount(entries.size, "item")} here will be removed from this device for good. This can't be undone.",
            confirmLabel = "Delete All",
            destructive = true,
            onDismiss = { confirmEmpty = false },
            onConfirm = {
                vm.deletePermanently(entries) { onMessage("Recently Deleted is empty") }
                confirmEmpty = false
            }
        )
    }
}

@Composable
private fun TrashThumbnail(entry: TrashEntry, file: File, onClick: () -> Unit) {
    val palette = GalleryTheme.palette
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(5.dp))
            .background(if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFEFEFF2))
            .combinedClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
            contentDescription = entry.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (entry.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x73000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GalleryIcons.VideoBadge,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}
