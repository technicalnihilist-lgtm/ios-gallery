package com.hubclub.iosgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubclub.iosgallery.model.DeviceAlbum
import com.hubclub.iosgallery.model.ManagedAlbum
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.DateGroup
import com.hubclub.iosgallery.util.formatCount

@Composable
fun SearchScreen(
    items: List<MediaItem>,
    favorites: Set<Long>,
    deviceAlbums: List<DeviceAlbum>,
    managedAlbums: List<ManagedAlbum>,
    bottomBarHeight: Dp,
    onOpenViewer: (List<MediaItem>, Int) -> Unit,
    onOpenDeviceAlbum: (DeviceAlbum) -> Unit,
    onOpenManagedAlbum: (ManagedAlbum) -> Unit
) {
    val palette = GalleryTheme.palette
    var query by remember { mutableStateOf("") }

    val trimmed = query.trim()
    val matchingItems = remember(trimmed, items) {
        if (trimmed.isEmpty()) emptyList()
        else items.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.bucketName.contains(trimmed, ignoreCase = true)
        }
    }
    val matchingDeviceAlbums = remember(trimmed, deviceAlbums) {
        if (trimmed.isEmpty()) emptyList()
        else deviceAlbums.filter { it.name.contains(trimmed, ignoreCase = true) }
    }
    val matchingManagedAlbums = remember(trimmed, managedAlbums) {
        if (trimmed.isEmpty()) emptyList()
        else managedAlbums.filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    val hasAlbumHits = matchingDeviceAlbums.isNotEmpty() || matchingManagedAlbums.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Search",
                color = palette.primaryText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp
            )
            Spacer(Modifier.height(12.dp))
            GallerySearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Photos, videos, albums"
            )
            Spacer(Modifier.height(12.dp))
        }

        when {
            trimmed.isEmpty() -> SearchHint()
            !hasAlbumHits && matchingItems.isEmpty() -> EmptyState(
                icon = GalleryIcons.Search,
                title = "No Results",
                message = "Nothing on this device matches “$trimmed”.\nSearch works on file names and album names, fully offline."
            )

            else -> {
                Column(Modifier.weight(1f)) {
                    if (hasAlbumHits) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                SectionTitle("Albums", formatCount(matchingDeviceAlbums.size + matchingManagedAlbums.size, "result"))
                            }
                            items(count = matchingManagedAlbums.size) { index ->
                                val album = matchingManagedAlbums[index]
                                AlbumResultRow(
                                    name = album.name,
                                    subtitle = "My Album · ${formatCount(album.itemIds.size, "item")}",
                                    onClick = { onOpenManagedAlbum(album) }
                                )
                            }
                            items(count = matchingDeviceAlbums.size) { index ->
                                val album = matchingDeviceAlbums[index]
                                AlbumResultRow(
                                    name = album.name,
                                    subtitle = "Folder · ${formatCount(album.count, "item")}",
                                    onClick = { onOpenDeviceAlbum(album) }
                                )
                            }
                        }
                    }
                    if (matchingItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photos & Videos",
                                color = palette.primaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatCount(matchingItems.size, "item"),
                                color = palette.secondaryText,
                                fontSize = 13.sp
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            MediaGrid(
                                groups = remember(matchingItems) { listOf(DateGroup("", matchingItems)) },
                                favorites = favorites,
                                selectionMode = false,
                                selectedIds = emptySet(),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                bottomPadding = bottomBarHeight + 16.dp,
                                onItemClick = { item ->
                                    val index = matchingItems.indexOfFirst { it.id == item.id }
                                    onOpenViewer(matchingItems, if (index >= 0) index else 0)
                                },
                                onItemLongClick = { item ->
                                    val index = matchingItems.indexOfFirst { it.id == item.id }
                                    onOpenViewer(matchingItems, if (index >= 0) index else 0)
                                }
                            )
                        }
                    } else {
                        Spacer(Modifier.height(bottomBarHeight))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHint() {
    val palette = GalleryTheme.palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(palette.groupedBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GalleryIcons.Search,
                contentDescription = null,
                tint = palette.tertiaryText,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Find anything, offline",
            color = palette.primaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Type a file name or an album name. iOS Gallery searches the media already on this device — nothing is sent anywhere.",
            color = palette.secondaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AlbumResultRow(name: String, subtitle: String, onClick: () -> Unit) {
    val palette = GalleryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(palette.groupedBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GalleryIcons.Albums,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = palette.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, color = palette.secondaryText, fontSize = 12.sp)
        }
        Icon(
            imageVector = GalleryIcons.ChevronRight,
            contentDescription = null,
            tint = palette.tertiaryText,
            modifier = Modifier.size(15.dp)
        )
    }
}
