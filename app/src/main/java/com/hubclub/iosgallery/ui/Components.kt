package com.hubclub.iosgallery.ui

import androidx.compose.ui.draw.shadow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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

/** Segmented glass capsule, like the iOS Photos Years / Months / All switch. */
@Composable
fun GlassSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = GalleryTheme.palette
    val glass = GalleryTheme.glass
    val width = 240.dp
    val height = 40.dp
    val segment = width / options.size
    val glide by animateDpAsState(
        targetValue = segment * selectedIndex,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 560f),
        label = "segGlide"
    )
    GlassPanel(
        modifier = modifier.width(width).height(height),
        shape = RoundedCornerShape(height / 2),
        elevation = 16.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = glide)
                    .width(segment)
                    .height(height)
                    .padding(4.dp)
                    .clip(RoundedCornerShape((height - 8.dp) / 2))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.accent.copy(alpha = 0.30f),
                                palette.accent.copy(alpha = 0.16f)
                            )
                        )
                    )
                    .border(
                        0.8.dp,
                        Color.White.copy(alpha = 0.45f),
                        RoundedCornerShape((height - 8.dp) / 2)
                    )
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .glassClickable(pressedScale = 0.94f, onClick = { onSelect(index) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) palette.accent else palette.secondaryText,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** How many thumbnails per row; provided from the saved appearance settings. */
val LocalGridColumns = androidx.compose.runtime.compositionLocalOf { 3 }

enum class GalleryTab(val label: String, val icon: ImageVector) {
    Library("Library", GalleryIcons.Library),
    Albums("Albums", GalleryIcons.Albums),
    Favorites("Favorites", GalleryIcons.Heart),
    Search("Search", GalleryIcons.Search)
}

// --------------------------------------------------------------------- pieces

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val glass = GalleryTheme.glass
    Box(
        modifier
            .fillMaxWidth()
            .height(0.8.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, glass.borderBottom, Color.Transparent)
                )
            )
    )
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    background: Color? = null,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp
) {
    GlassIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        tint = tint,
        size = size,
        iconSize = iconSize
    )
}

/** Floating glass header with the large screen title. */
@Composable
fun LargeTitleBar(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val palette = GalleryTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(palette.background.copy(alpha = 0.55f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = 18.dp,
            contentPadding = PaddingValues(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow,
                            color = palette.secondaryText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = title,
                        color = palette.primaryText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = palette.secondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
            }
        }
    }
}

/** Floating glass bar for pushed screens: back, title, actions. */
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = GalleryTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = 16.dp,
            contentPadding = PaddingValues(start = 6.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = GalleryIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                    tint = palette.primaryText,
                    size = 36.dp,
                    iconSize = 18.dp
                )
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    val palette = GalleryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = palette.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(text = trailing, color = palette.secondaryText, fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------- bottom dock

/**
 * iOS-style dock: a floating glass bar with rounded-square tiles, one per
 * section. No labels (like the iPhone dock) — the icons carry the meaning and
 * the active tile is lifted, brightened and marked by a gliding highlight.
 * [scale] lets the user resize it from the Customize screen.
 */
@Composable
fun GalleryBottomBar(
    current: GalleryTab,
    onSelect: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val glass = GalleryTheme.glass
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(glass.scrimStart, glass.scrimEnd)))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            DockPill(current = current, onSelect = onSelect, scale = scale)
        }
    }
}

/** The glass pill itself, reusable so the Customize screen can preview it live. */
@Composable
fun DockPill(
    current: GalleryTab,
    onSelect: (GalleryTab) -> Unit,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val glass = GalleryTheme.glass
    val tabs = GalleryTab.entries
    val selectedIndex = tabs.indexOf(current)

    val tile = 56.dp * scale
    val gap = 10.dp * scale
    val inset = 12.dp * scale
    val dockWidth = tile * tabs.size + gap * (tabs.size - 1) + inset * 2
    val step = tile + gap
    val pillRadius = 30.dp * scale
    val tileRadius = 17.dp * scale

    Box(
        modifier = modifier
            .width(dockWidth)
            .height(tile + inset * 2)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(pillRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(pillRadius))
            .background(Brush.linearGradient(listOf(glass.fillTop, glass.fillBottom)))
            .border(
                width = 1.dp,
                color = glass.borderTop,
                shape = RoundedCornerShape(pillRadius)
            )
    ) {
        // bright lip along the top edge of the pill
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to glass.highlight,
                        0.05f to Color.Transparent
                    )
                )
        )
        // the highlight glides between tiles on a fluid spring
        val glide by animateDpAsState(
            targetValue = inset + step * selectedIndex,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f),
            label = "dockGlide"
        )
        Box(
            modifier = Modifier
                .offset(x = glide, y = inset)
                .size(tile)
                .clip(RoundedCornerShape(tileRadius))
                .background(Color.White.copy(alpha = 0.16f))
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = inset, vertical = inset),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                DockTile(
                    tab = tab,
                    selected = tab == current,
                    size = tile,
                    cornerRadius = tileRadius,
                    iconSize = 25.dp * scale,
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

/** Two-stop gradient per section, in the spirit of iOS app icons. */
private fun dockGradient(tab: GalleryTab): Pair<Color, Color> = when (tab) {
    GalleryTab.Library -> Color(0xFF63B4FF) to Color(0xFF0A6BFF)
    GalleryTab.Albums -> Color(0xFFC08CFF) to Color(0xFF7A46F0)
    GalleryTab.Favorites -> Color(0xFFFF8BAE) to Color(0xFFFF2D55)
    GalleryTab.Search -> Color(0xFFAEB6C6) to Color(0xFF5E6779)
}

@Composable
private fun DockTile(
    tab: GalleryTab,
    selected: Boolean,
    size: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val (top, bottom) = dockGradient(tab)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "dockScale"
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0.86f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "dockLift"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = lift
            }
            .shadow(
                elevation = if (selected) 12.dp else 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = bottom.copy(alpha = 0.45f),
                spotColor = bottom.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .glassClickable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.26f),
                        0.52f to Color.Transparent
                    )
                )
        )
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ------------------------------------------------------------------ thumbnails

@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    cornerRadius: Dp = 10.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    val glass = GalleryTheme.glass
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (palette.isDark) Color(0xFF16161A) else Color(0xFFE4E8F2))
            .glassClickable(pressedScale = 0.94f, onClick = onClick, onLongClick = onLongClick)
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

        // a whisper of glass over every tile, so the grid reads as one material
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.06f),
                        0.5f to Color.Transparent
                    )
                )
        )

        if (item.isVideo) {
            GlassChip(
                text = formatDuration(item.durationMs),
                icon = GalleryIcons.VideoBadge,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }

        if (isFavorite && !selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.35f), CircleShape),
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
                    .background(if (selected) palette.accent.copy(alpha = 0.18f) else Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) palette.accent else Color.Black.copy(alpha = 0.28f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.White.copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.7f),
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
        // hairline highlight along the top edge of the glass tile
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(glass.borderTop.copy(alpha = 0.35f))
        )
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
    val columns = LocalGridColumns.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.padding(start = 9.dp, top = 20.dp, bottom = 10.dp)
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
                    modifier = Modifier.animateItem(),
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            shape = RoundedCornerShape(28.dp),
            elevation = 20.dp,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(palette.accentSoft)
                        .border(
                            0.9.dp,
                            Color.White.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(label = actionLabel, onClick = onAction)
                }
            }
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
    GlassPanel(
        modifier = modifier.glassClickable(pressedScale = 0.95f, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        elevation = if (filled) 14.dp else 8.dp,
        tint = if (filled) palette.accent.copy(alpha = 0.92f) else null,
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 11.dp)
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
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 12.dp,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
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
                            .glassClickable(pressedScale = 0.85f, onClick = { onValueChange("") })
                    )
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = palette.accent,
                focusedTextColor = palette.primaryText,
                unfocusedTextColor = palette.primaryText
            )
        )
    }
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
    val glass = GalleryTheme.glass
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = palette.scrim.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(glass.highlight.copy(alpha = 0.45f))
                )
                if (title != null) {
                    Text(
                        text = title,
                        color = palette.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            start = 24.dp, end = 24.dp, top = 4.dp, bottom = 10.dp
                        )
                    )
                }
                actions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassClickable(pressedScale = 0.98f, onClick = {
                                onDismiss()
                                action.onClick()
                            })
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (action.destructive) Color(0xFFFF3B30) else palette.primaryText,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = action.label,
                            color = if (action.destructive) Color(0xFFFF3B30) else palette.primaryText,
                            fontSize = 15.5.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.accentSoft)
                        .border(
                            0.8.dp,
                            Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(18.dp)
                        )
                        .glassClickable(pressedScale = 0.97f, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = palette.accent,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(
                    Modifier
                        .height(10.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
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
            fontSize = 12.5.sp,
            modifier = Modifier.width(104.dp)
        )
        Text(
            text = value,
            color = palette.primaryText,
            fontSize = 12.5.sp,
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
