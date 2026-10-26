package com.hubclub.iosgallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubclub.iosgallery.data.FileOps
import com.hubclub.iosgallery.model.ManagedAlbum
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.formatNumber
import com.hubclub.iosgallery.util.groupByDate

@Composable
fun LibraryScreen(
    items: List<MediaItem>,
    favorites: Set<Long>,
    loading: Boolean,
    albums: List<ManagedAlbum>,
    bottomBarHeight: Dp,
    vm: GalleryViewModel,
    onOpenViewer: (List<MediaItem>, Int) -> Unit,
    onSelectionActive: (Boolean) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenCustomize: () -> Unit,
    onMessage: (String) -> Unit
) {
    val palette = GalleryTheme.palette
    val context = LocalContext.current

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMenu by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showNewAlbum by remember { mutableStateOf(false) }
    var pendingAlbumName by remember { mutableStateOf("") }
    var showAbout by remember { mutableStateOf(false) }

    val trash = rememberTrashFlow(vm, onMessage)

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
        onSelectionActive(false)
    }

    fun enterSelection(item: MediaItem) {
        selectionMode = true
        selectedIds = setOf(item.id)
        onSelectionActive(true)
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    val groups = remember(items) { groupByDate(items) }
    val videoCount = remember(items) { items.count { it.isVideo } }
    val selectedItems = remember(selectedIds, items) { items.filter { it.id in selectedIds } }

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (selectionMode) {
            SelectionTopBar(
                count = selectedIds.size,
                onClose = { exitSelection() },
                onSelectAll = { selectedIds = items.map { it.id }.toSet() }
            )
        } else {
            LargeTitleBar(
                title = "Library",
                subtitle = if (items.isEmpty()) "No items yet" else SubtitleLine(items.size, videoCount),
                eyebrow = "iOS Gallery"
            ) {
                CircleIconButton(
                    icon = GalleryIcons.More,
                    contentDescription = "More",
                    onClick = { showMenu = true },
                    tint = palette.primaryText,
                    background = palette.groupedBackground,
                    size = 32.dp
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading && items.isEmpty() -> LoadingState()
                items.isEmpty() -> EmptyState(
                    icon = GalleryIcons.Camera,
                    title = "No photos or videos",
                    message = "Anything you capture or copy to this device will appear here automatically."
                )
                else -> MediaGrid(
                    groups = groups,
                    favorites = favorites,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    bottomPadding = bottomBarHeight + 16.dp,
                    showHeaders = true,
                    onItemClick = { item ->
                        if (selectionMode) {
                            selectedIds = if (item.id in selectedIds) selectedIds - item.id
                            else selectedIds + item.id
                        } else {
                            val index = items.indexOfFirst { it.id == item.id }
                            onOpenViewer(items, if (index >= 0) index else 0)
                        }
                    },
                    onItemLongClick = { item ->
                        if (selectionMode) {
                            selectedIds = if (item.id in selectedIds) selectedIds - item.id
                            else selectedIds + item.id
                        } else {
                            enterSelection(item)
                        }
                    }
                )
            }
        }
    }

    // ---- selection action bar (replaces the tab bar while selecting) ----------
    AnimatedVisibility(
        visible = selectionMode,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                elevation = 26.dp
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionAction(
                    icon = GalleryIcons.Share,
                    label = "Share",
                    enabled = selectedIds.isNotEmpty()
                ) {
                    context.startActivity(FileOps.shareIntent(selectedItems))
                    exitSelection()
                }
                SelectionAction(
                    icon = GalleryIcons.Heart,
                    label = "Favorite",
                    enabled = selectedIds.isNotEmpty()
                ) {
                    vm.setFavorite(selectedIds, true)
                    onMessage(if (selectedIds.size == 1) "Added to Favorites" else "Added to Favorites")
                    exitSelection()
                }
                SelectionAction(
                    icon = GalleryIcons.Plus,
                    label = "Add to Album",
                    enabled = selectedIds.isNotEmpty()
                ) { showAlbumPicker = true }
                SelectionAction(
                    icon = GalleryIcons.Trash,
                    label = "Delete",
                    enabled = selectedIds.isNotEmpty(),
                    destructive = true
                ) {
                    trash(selectedItems)
                    exitSelection()
                }
            }
            }
        }
    }

    }

    // ---- overflow menu -------------------------------------------------------
    GalleryActionSheet(
        visible = showMenu,
        title = "iOS Gallery",
        actions = listOf(
            SheetAction("Customize appearance", GalleryIcons.Grid) { onOpenCustomize() },
            SheetAction("Recently Deleted", GalleryIcons.Trash) { onOpenTrash() },
            SheetAction("Refresh library", GalleryIcons.Restore) {
                vm.refresh()
                onMessage("Library refreshed")
            },
            SheetAction("About & privacy", GalleryIcons.Shield) { showAbout = true }
        ),
        onDismiss = { showMenu = false }
    )

    GalleryActionSheet(
        visible = showAbout,
        title = "iOS Gallery",
        actions = listOf(
            SheetAction("Fully offline · no internet permission", GalleryIcons.Shield) {},
            SheetAction("No accounts, ads, trackers or cloud sync", GalleryIcons.Shield) {},
            SheetAction("Your media never leaves this device", GalleryIcons.Shield) {}
        ),
        onDismiss = { showAbout = false }
    )

    // ---- add-to-album picker -------------------------------------------------
    AlbumPickerSheet(
        visible = showAlbumPicker,
        albums = albums,
        selectionCount = selectedIds.size,
        onDismiss = { showAlbumPicker = false },
        onPick = { album ->
            vm.addToAlbum(album.id, selectedIds)
            onMessage("Added to “${album.name}”")
            showAlbumPicker = false
            exitSelection()
        },
        onCreateNew = {
            showAlbumPicker = false
            pendingAlbumName = ""
            showNewAlbum = true
        }
    )

    if (showNewAlbum) {
        TextInputDialog(
            title = "New Album",
            initial = "",
            placeholder = "Album name",
            confirmLabel = "Create",
            onDismiss = { showNewAlbum = false },
            onConfirm = { name ->
                vm.createAlbum(name) { album ->
                    vm.addToAlbum(album.id, selectedIds)
                }
                onMessage("Created “$name”")
                showNewAlbum = false
                exitSelection()
            }
        )
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit
) {
    val palette = GalleryTheme.palette
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = 16.dp,
            contentPadding = PaddingValues(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = GalleryIcons.Close,
                contentDescription = "Cancel selection",
                onClick = onClose,
                tint = palette.primaryText,
                size = 38.dp,
                iconSize = 17.dp
            )
            Text(
                text = if (count == 1) "1 selected" else "${formatNumber(count)} selected",
                color = palette.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            Text(
                text = "Select All",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .glassClickable(pressedScale = 0.92f, onClick = onSelectAll)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    val tint = when {
        !enabled -> palette.tertiaryText
        destructive -> Color(0xFFFF3B30)
        else -> palette.accent
    }
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
