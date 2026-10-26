package com.hubclub.iosgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.DateGroup
import com.hubclub.iosgallery.util.formatCount

@Composable
fun FavoritesScreen(
    items: List<MediaItem>,
    favorites: Set<Long>,
    bottomBarHeight: Dp,
    onOpenViewer: (List<MediaItem>, Int) -> Unit
) {
    val palette = GalleryTheme.palette
    val favoriteItems = remember(items, favorites) {
        items.filter { it.id in favorites }
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        LargeTitleBar(
            title = "Favorites",
            subtitle = if (favoriteItems.isEmpty()) "Nothing yet"
            else formatCount(favoriteItems.size, "item")
        )
        Box(modifier = Modifier.weight(1f)) {
            if (favoriteItems.isEmpty()) {
                EmptyState(
                    icon = GalleryIcons.Heart,
                    title = "No Favorites",
                    message = "Tap the heart on any photo or video and it will show up here."
                )
            } else {
                MediaGrid(
                    groups = remember(favoriteItems) { listOf(DateGroup("", favoriteItems)) },
                    favorites = favorites,
                    selectionMode = false,
                    selectedIds = emptySet(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    bottomPadding = bottomBarHeight + 16.dp,
                    onItemClick = { item ->
                        val index = favoriteItems.indexOfFirst { it.id == item.id }
                        onOpenViewer(favoriteItems, if (index >= 0) index else 0)
                    },
                    onItemLongClick = { item ->
                        val index = favoriteItems.indexOfFirst { it.id == item.id }
                        onOpenViewer(favoriteItems, if (index >= 0) index else 0)
                    }
                )
            }
        }
    }
}
