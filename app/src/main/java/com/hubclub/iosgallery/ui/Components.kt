package com.hubclub.iosgallery.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.DateGroup
import com.hubclub.iosgallery.util.formatDuration
import com.hubclub.iosgallery.util.formatNumber

enum class GalleryTab(val label: String, val icon: ImageVector) {
    Library("Library", GalleryIcons.Library),
    Albums("Albums", GalleryIcons.Albums),
    Favorites("Favorites", GalleryIcons.Heart),
    Search("Search", GalleryIcons.Search)
}

// --------------------------------------------------------------------- pieces

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val palette = GalleryTheme.palette
    Box(modifier.fillMaxWidth().height(0.6.dp).background(palette.separator))
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    background: Color? = null,
    size: Dp = 34.dp,
    iconSize: Dp = 19.dp
) {
    val palette = GalleryTheme.palette
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: Color.Transparent)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: palette.accent,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Big iOS-style screen title with an optional subtitle and trailing actions. */
@Composable
fun LargeTitleBar(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val palette = GalleryTheme.palette
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                color = palette.tertiaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = if (eyebrow != null) 0.dp else 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = palette.secondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

/** Compact bar for pushed screens: back chevron, centred title, actions. */
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = GalleryTheme.palette
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = GalleryIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                tint = palette.primaryText,
                size = 38.dp,
                iconSize = 20.dp
            )
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        Hairline()
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    val palette = GalleryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = palette.primaryText,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(text = trailing, color = palette.secondaryText, fontSize = 13.sp)
        }
    }
}

// -------------------------------------------------------------- bottom bar

@Composable
fun GalleryBottomBar(
    current: GalleryTab,
    onSelect: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = GalleryTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.barBackground.copy(alpha = 0.86f),
                        palette.barBackground
                    )
                )
            )
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryTab.entries.forEach { tab ->
                val selected = tab == current
                val tint = if (selected) palette.accent else palette.secondaryText
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.94f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
                    label = "tabScale"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .combinedClickable(onClick = { onSelect(tab) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier
                            .size(25.dp)
                            .scale(scale)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ thumbnails

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    cornerRadius: Dp = 5.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    val context = LocalContext.current
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFEFEFF2))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
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
                Spacer(Modifier.width(3.dp))
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isFavorite && !selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GalleryIcons.HeartFilled,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (selected) Color(0x330A84FF) else Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.accent else Color(0x4D000000))
                    .border(
                        width = 1.dp,
                        color = if (selected) palette.accent else Color(0xCCFFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = GalleryIcons.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ the grid

@Composable
fun MediaGrid(
    groups: List<DateGroup>,
    favorites: Set<Long>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    showHeaders: Boolean = false,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit
) {
    val palette = GalleryTheme.palette
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        groups.forEach { group ->
            if (showHeaders) {
                item(
                    key = "header-${group.label}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = group.label,
                        color = palette.primaryText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 10.dp)
                    )
                }
            }
            items(
                count = group.items.size,
                key = { index -> "${group.label}-${group.items[index].id}-$index" }
            ) { index ->
                val item = group.items[index]
                MediaThumbnail(
                    item = item,
                    isFavorite = item.id in favorites,
                    selectionMode = selectionMode,
                    selected = item.id in selectedIds,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(bottomPadding))
        }
    }
}

// --------------------------------------------------------------- empty states

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = GalleryTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(palette.groupedBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.tertiaryText,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            color = palette.primaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = palette.secondaryText,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            PrimaryButton(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val palette = GalleryTheme.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (filled) palette.accent else palette.groupedBackground)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (filled) Color.White else palette.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LoadingState(message: String = "Loading your library…") {
    val palette = GalleryTheme.palette
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = palette.accent,
            strokeWidth = 2.4.dp,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(text = message, color = palette.secondaryText, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------- search

@Composable
fun GallerySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}
) {
    val palette = GalleryTheme.palette
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        placeholder = {
            Text(placeholder, color = palette.tertiaryText, fontSize = 15.sp)
        },
        leadingIcon = {
            Icon(
                imageVector = GalleryIcons.Search,
                contentDescription = null,
                tint = palette.tertiaryText,
                modifier = Modifier.size(17.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    imageVector = GalleryIcons.Close,
                    contentDescription = "Clear",
                    tint = palette.tertiaryText,
                    modifier = Modifier
                        .size(15.dp)
                        .combinedClickable(onClick = { onValueChange("") })
                )
            }
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(11.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFEFEFF2),
            unfocusedContainerColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFEFEFF2),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = palette.accent,
            focusedTextColor = palette.primaryText,
            unfocusedTextColor = palette.primaryText
        )
    )
}

// ---------------------------------------------------------------- action sheet

data class SheetAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryActionSheet(
    visible: Boolean,
    title: String? = null,
    actions: List<SheetAction>,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val palette = GalleryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF7F7FA),
        scrimColor = palette.scrim,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.tertiaryText.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 10.dp)
                )
            }
            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable {
                            onDismiss()
                            action.onClick()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = if (action.destructive) Color(0xFFFF3B30) else palette.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = action.label,
                        color = if (action.destructive) Color(0xFFFF3B30) else palette.primaryText,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                color = palette.card,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .combinedClickable(onClick = onDismiss)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(
                Modifier
                    .height(8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, mono: Boolean = false) {
    val palette = GalleryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = palette.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.width(104.dp)
        )
        Text(
            text = value,
            color = palette.primaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else null
        )
    }
}

@Composable
fun SubtitleLine(items: Int, videos: Int): String {
    val photos = items - videos
    val photoText = if (photos == 1) "1 photo" else "${formatNumber(photos)} photos"
    val videoText = if (videos == 1) "1 video" else "${formatNumber(videos)} videos"
    return if (videos == 0) photoText else "$photoText · $videoText"
}
