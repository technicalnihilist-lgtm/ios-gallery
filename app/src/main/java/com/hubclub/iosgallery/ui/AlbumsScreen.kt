package com.hubclub.iosgallery.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hubclub.iosgallery.model.DeviceAlbum
import com.hubclub.iosgallery.model.ManagedAlbum
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.formatCount
import com.hubclub.iosgallery.util.groupByDate

@Composable
fun AlbumsScreen(
    items: List<MediaItem>,
    favorites: Set<Long>,
    deviceAlbums: List<DeviceAlbum>,
    managedAlbums: List<ManagedAlbum>,
    trashCount: Int,
    bottomBarHeight: Dp,
    vm: GalleryViewModel,
    onOpenDeviceAlbum: (DeviceAlbum) -> Unit,
    onOpenManagedAlbum: (ManagedAlbum) -> Unit,
    onOpenTrash: () -> Unit,
    onMessage: (String) -> Unit
) {
    val palette = GalleryTheme.palette

    var showNewAlbum by remember { mutableStateOf(false) }
    var menuAlbum by remember { mutableStateOf<ManagedAlbum?>(null) }
    var renameAlbum by remember { mutableStateOf<ManagedAlbum?>(null) }
    var confirmDelete by remember { mutableStateOf<ManagedAlbum?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        LargeTitleBar(
            title = "Albums",
            subtitle = "${formatCount(managedAlbums.size, "album")} · ${formatCount(deviceAlbums.size, "folder")}"
        ) {
            CircleIconButton(
                icon = GalleryIcons.Plus,
                contentDescription = "New album",
                onClick = { showNewAlbum = true },
                tint = palette.accent,
                background = palette.groupedBackground,
                size = 32.dp,
                iconSize = 17.dp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomBarHeight + 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RecentlyDeletedRow(count = trashCount, onClick = onOpenTrash)
            }

            if (managedAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "My Albums",
                        color = palette.primaryText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(count = managedAlbums.size, key = { managedAlbums[it].id }) { index ->
                    val album = managedAlbums[index]
                    val albumItems = remember(album.itemIds, items) {
                        val byId = items.associateBy { it.id }
                        album.itemIds.mapNotNull { byId[it] }
                    }
                    AlbumCard(
                        name = album.name,
                        count = albumItems.size,
                        cover = albumItems.firstOrNull(),
                        onClick = { onOpenManagedAlbum(album) },
                        onLongClick = { menuAlbum = album }
                    )
                }
            }

            if (deviceAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "From Device",
                        color = palette.primaryText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(count = deviceAlbums.size, key = { "d-${deviceAlbums[it].bucketId}" }) { index ->
                    val album = deviceAlbums[index]
                    AlbumCard(
                        name = album.name,
                        count = album.count,
                        cover = album.cover,
                        onClick = { onOpenDeviceAlbum(album) },
                        onLongClick = { onOpenDeviceAlbum(album) }
                    )
                }
            }

            if (managedAlbums.isEmpty() && deviceAlbums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = GalleryIcons.Albums,
                        title = "No albums yet",
                        message = "Folders on your device appear here automatically. You can also create your own albums.",
                        actionLabel = "Create Album",
                        onAction = { showNewAlbum = true },
                        modifier = Modifier.height(360.dp)
                    )
                }
            }
        }
    }

    if (showNewAlbum) {
        TextInputDialog(
            title = "New Album",
            initial = "",
            placeholder = "Album name",
            confirmLabel = "Create",
            onDismiss = { showNewAlbum = false },
            onConfirm = { name ->
                vm.createAlbum(name) { onMessage("Created “${it.name}”") }
                showNewAlbum = false
            }
        )
    }

    val menuTarget = menuAlbum
    GalleryActionSheet(
        visible = menuTarget != null,
        title = menuTarget?.name,
        actions = listOf(
            SheetAction("Rename Album", GalleryIcons.Pencil) { renameAlbum = menuTarget },
            SheetAction("Delete Album", GalleryIcons.Trash, destructive = true) {
                confirmDelete = menuTarget
            }
        ),
        onDismiss = { menuAlbum = null }
    )

    val renameTarget = renameAlbum
    if (renameTarget != null) {
        TextInputDialog(
            title = "Rename Album",
            initial = renameTarget.name,
            placeholder = "Album name",
            confirmLabel = "Save",
            onDismiss = { renameAlbum = null },
            onConfirm = { name ->
                vm.renameAlbum(renameTarget.id, name)
                renameAlbum = null
            }
        )
    }

    val deleteTarget = confirmDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            title = "Delete “${deleteTarget.name}”?",
            message = "The album is removed from iOS Gallery. Your photos and videos stay on the device.",
            confirmLabel = "Delete Album",
            destructive = true,
            onDismiss = { confirmDelete = null },
            onConfirm = {
                vm.deleteAlbum(deleteTarget.id)
                onMessage("Album deleted")
                confirmDelete = null
            }
        )
    }
}

@Composable
private fun RecentlyDeletedRow(count: Int, onClick: () -> Unit) {
    val palette = GalleryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.groupedBackground)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(palette.card),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GalleryIcons.Trash,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Recently Deleted",
                color = palette.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (count == 0) "Empty" else formatCount(count, "item"),
                color = palette.secondaryText,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = GalleryIcons.ChevronRight,
            contentDescription = null,
            tint = palette.tertiaryText,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AlbumCard(
    name: String,
    count: Int,
    cover: MediaItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFEFEFF2))
        ) {
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover.uri).crossfade(true).build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GalleryIcons.Albums,
                        contentDescription = null,
                        tint = palette.tertiaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            if (cover != null && cover.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0x73000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GalleryIcons.VideoBadge,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = name,
            color = palette.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatCount(count, "item"),
            color = palette.secondaryText,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------- album detail

@Composable
fun AlbumDetailScreen(
    title: String,
    items: List<MediaItem>,
    allItems: List<MediaItem>,
    favorites: Set<Long>,
    managedAlbum: ManagedAlbum?,
    bottomBarHeight: Dp,
    vm: GalleryViewModel,
    onBack: () -> Unit,
    onOpenViewer: (List<MediaItem>, Int) -> Unit,
    onMessage: (String) -> Unit
) {
    val palette = GalleryTheme.palette
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var pickerSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var removeTarget by remember { mutableStateOf<MediaItem?>(null) }

    val albumId = managedAlbum?.id

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(
            title = title,
            onBack = onBack
        ) {
            if (albumId != null) {
                CircleIconButton(
                    icon = GalleryIcons.More,
                    contentDescription = "Album options",
                    onClick = { showMenu = true },
                    tint = palette.primaryText,
                    size = 36.dp
                )
            } else {
                Text(
                    text = formatCount(items.size, "item"),
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = GalleryIcons.Albums,
                    title = "Nothing here yet",
                    message = if (albumId != null)
                        "Add photos from your library to fill this album."
                    else "This folder is empty.",
                    actionLabel = if (albumId != null) "Add Photos" else null,
                    onAction = if (albumId != null) {
                        { picking = true; pickerSelection = emptySet() }
                    } else null
                )
            } else {
                MediaGrid(
                    groups = remember(items) { listOf(com.hubclub.iosgallery.util.DateGroup("", items)) },
                    favorites = favorites,
                    selectionMode = false,
                    selectedIds = emptySet(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    bottomPadding = bottomBarHeight + 16.dp,
                    showHeaders = false,
                    onItemClick = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        onOpenViewer(items, if (index >= 0) index else 0)
                    },
                    onItemLongClick = { item ->
                        if (albumId != null) removeTarget = item
                    }
                )
            }
        }
    }

    GalleryActionSheet(
        visible = showMenu,
        title = managedAlbum?.name,
        actions = listOf(
            SheetAction("Add Photos", GalleryIcons.Plus) {
                picking = true
                pickerSelection = emptySet()
            },
            SheetAction("Rename Album", GalleryIcons.Pencil) { showRename = true },
            SheetAction("Delete Album", GalleryIcons.Trash, destructive = true) { showDelete = true }
        ),
        onDismiss = { showMenu = false }
    )

    if (showRename && managedAlbum != null) {
        TextInputDialog(
            title = "Rename Album",
            initial = managedAlbum.name,
            placeholder = "Album name",
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { name ->
                vm.renameAlbum(managedAlbum.id, name)
                showRename = false
            }
        )
    }

    if (showDelete && managedAlbum != null) {
        ConfirmDialog(
            title = "Delete “${managedAlbum.name}”?",
            message = "The album is removed. Your photos and videos stay on the device.",
            confirmLabel = "Delete Album",
            destructive = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                vm.deleteAlbum(managedAlbum.id)
                showDelete = false
                onMessage("Album deleted")
                onBack()
            }
        )
    }

    val target = removeTarget
    if (target != null && albumId != null) {
        ConfirmDialog(
            title = "Remove from Album?",
            message = "“${target.name}” is removed from this album. The original file stays on your device.",
            confirmLabel = "Remove",
            destructive = true,
            onDismiss = { removeTarget = null },
            onConfirm = {
                vm.removeFromAlbum(albumId, listOf(target.id))
                removeTarget = null
            }
        )
    }

    // ---- photo picker for managed albums -------------------------------------
    if (picking && albumId != null) {
        BackHandler { picking = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = palette.accent,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable { picking = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "Add Photos",
                        color = palette.primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = if (pickerSelection.isEmpty()) "Add" else "Add (${pickerSelection.size})",
                        color = if (pickerSelection.isEmpty()) palette.tertiaryText else palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(enabled = pickerSelection.isNotEmpty()) {
                                vm.addToAlbum(albumId, pickerSelection)
                                onMessage("Added to album")
                                picking = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Hairline()
                MediaGrid(
                    groups = remember(allItems) { listOf(com.hubclub.iosgallery.util.DateGroup("", allItems)) },
                    favorites = favorites,
                    selectionMode = true,
                    selectedIds = pickerSelection,
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    bottomPadding = 24.dp,
                    showHeaders = false,
                    onItemClick = { item ->
                        pickerSelection = if (item.id in pickerSelection) pickerSelection - item.id
                        else pickerSelection + item.id
                    },
                    onItemLongClick = { item ->
                        pickerSelection = if (item.id in pickerSelection) pickerSelection - item.id
                        else pickerSelection + item.id
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------- dialogs

@Composable
fun AlbumPickerSheet(
    visible: Boolean,
    albums: List<ManagedAlbum>,
    selectionCount: Int,
    onDismiss: () -> Unit,
    onPick: (ManagedAlbum) -> Unit,
    onCreateNew: () -> Unit
) {
    val palette = GalleryTheme.palette
    if (!visible) return
    GalleryActionSheet(
        visible = true,
        title = if (selectionCount == 1) "Add 1 item to" else "Add $selectionCount items to",
        actions = albums.map { album ->
            SheetAction(album.name, GalleryIcons.Folder) { onPick(album) }
        } + SheetAction("New Album…", GalleryIcons.Plus) { onCreateNew() },
        onDismiss = onDismiss
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val palette = GalleryTheme.palette
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = title, color = palette.primaryText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder, color = palette.tertiaryText, fontSize = 15.sp) },
                singleLine = true,
                shape = RoundedCornerShape(11.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = palette.groupedBackground,
                    unfocusedContainerColor = palette.groupedBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = palette.accent,
                    focusedTextColor = palette.primaryText,
                    unfocusedTextColor = palette.primaryText
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text(confirmLabel, color = if (text.isNotBlank()) palette.accent else palette.tertiaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.accent) }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val palette = GalleryTheme.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = title, color = palette.primaryText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(text = message, color = palette.secondaryText, fontSize = 14.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) Color(0xFFFF3B30) else palette.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.accent) }
        }
    )
}
