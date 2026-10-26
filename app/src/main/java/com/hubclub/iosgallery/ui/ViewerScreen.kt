package com.hubclub.iosgallery.ui

import androidx.compose.foundation.layout.PaddingValues
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hubclub.iosgallery.data.FileOps
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.util.formatDateTime
import com.hubclub.iosgallery.util.formatDuration
import com.hubclub.iosgallery.util.formatResolution
import com.hubclub.iosgallery.util.formatSize
import kotlinx.coroutines.launch

data class ViewerRequest(val items: List<MediaItem>, val index: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    request: ViewerRequest,
    favorites: Set<Long>,
    vm: GalleryViewModel,
    onClose: () -> Unit,
    onMessage: (String) -> Unit
) {
    val palette = GalleryTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (request.items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = request.index.coerceIn(0, request.items.lastIndex)
    ) { request.items.size }

    var zoomed by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var pendingCopy by remember { mutableStateOf<Pair<MediaItem, Boolean>?>(null) }
    var moveUri by remember { mutableStateOf<Uri?>(null) }

    val current = request.items.getOrNull(pagerState.currentPage) ?: request.items.first()
    val isFavorite = current.id in favorites
    val trash = rememberTrashFlow(vm, onMessage)

    BackHandler { onClose() }

    // ---- copy / move into a folder the user picks ----------------------------
    val moveConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = moveUri
        moveUri = null
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            scope.launch {
                FileOps.deleteNow(context, listOf(uri))
                vm.refresh()
                onMessage("Moved")
            }
        } else {
            onMessage("Copied — the original was kept")
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val job = pendingCopy
        pendingCopy = null
        if (treeUri == null || job == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val dir = DocumentFile.fromTreeUri(context, treeUri)
            if (dir == null || !dir.canWrite()) {
                onMessage("That folder can't be written to")
                return@launch
            }
            val copied = FileOps.copyInto(context, job.first, dir)
            if (copied == null) {
                onMessage("Couldn't copy that item")
                return@launch
            }
            if (job.second) {
                val deleted = FileOps.deleteNow(context, listOf(job.first.uri))
                if (deleted > 0) {
                    vm.refresh()
                    onMessage("Moved")
                } else {
                    val sender = FileOps.deleteConsent(context, listOf(job.first.uri))
                    if (sender != null) {
                        moveUri = job.first.uri
                        runCatching {
                            moveConsent.launch(IntentSenderRequest.Builder(sender).build())
                        }.onFailure { onMessage("Copied — the original was kept") }
                    } else {
                        onMessage("Copied — the original was kept")
                    }
                }
            } else {
                onMessage("Copied to the chosen folder")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = request.items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(onTap = { chromeVisible = !chromeVisible })
                    },
                contentAlignment = Alignment.Center
            ) {
                if (item.isVideo) {
                    VideoPage(item = item, active = page == pagerState.currentPage)
                } else {
                    ZoomableImage(
                        item = item,
                        onZoomChanged = { zoomed = it }
                    )
                }
            }
        }

        // ---- top chrome ------------------------------------------------------
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.background.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 18.dp,
                    contentPadding = PaddingValues(
                        start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp
                    )
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        icon = GalleryIcons.Back,
                        contentDescription = "Close",
                        onClick = onClose,
                        tint = palette.primaryText,
                        size = 38.dp
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = current.name,
                            color = palette.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (request.items.size > 1)
                                "${pagerState.currentPage + 1} of ${request.items.size}"
                            else if (current.isVideo) "Video" else "Photo",
                            color = palette.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                    CircleIconButton(
                        icon = if (isFavorite) GalleryIcons.HeartFilled else GalleryIcons.Heart,
                        contentDescription = "Favorite",
                        onClick = {
                            vm.toggleFavorite(current.id)
                            onMessage(if (isFavorite) "Removed from Favorites" else "Added to Favorites")
                        },
                        tint = if (isFavorite) palette.favorite else palette.primaryText,
                        size = 38.dp
                    )
                    CircleIconButton(
                        icon = GalleryIcons.More,
                        contentDescription = "More",
                        onClick = { showMenu = true },
                        tint = palette.primaryText,
                        size = 38.dp
                    )
                }
                }
            }
        }

        // ---- bottom chrome ---------------------------------------------------
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, palette.background.copy(alpha = 0.7f))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 24.dp
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerAction(GalleryIcons.Share, "Share") {
                        context.startActivity(FileOps.shareIntent(listOf(current)))
                    }
                    ViewerAction(
                        if (isFavorite) GalleryIcons.HeartFilled else GalleryIcons.Heart,
                        if (isFavorite) "Favorited" else "Favorite",
                        tint = if (isFavorite) palette.favorite else null
                    ) {
                        vm.toggleFavorite(current.id)
                    }
                    ViewerAction(GalleryIcons.Info, "Details") { showInfo = true }
                    ViewerAction(GalleryIcons.Trash, "Delete", destructive = true) {
                        trash(listOf(current))
                        onClose()
                    }
                }
                }
            }
        }
    }

    GalleryActionSheet(
        visible = showMenu,
        title = current.name,
        actions = listOf(
            SheetAction("Share", GalleryIcons.Share) {
                context.startActivity(FileOps.shareIntent(listOf(current)))
            },
            SheetAction("Copy to Folder…", GalleryIcons.Copy) {
                pendingCopy = current to false
                folderPicker.launch(null)
            },
            SheetAction("Move to Folder…", GalleryIcons.Move) {
                pendingCopy = current to true
                folderPicker.launch(null)
            },
            SheetAction("Rename", GalleryIcons.Pencil) { showRename = true },
            SheetAction("Details", GalleryIcons.Info) { showInfo = true },
            SheetAction("Delete", GalleryIcons.Trash, destructive = true) {
                trash(listOf(current))
                onClose()
            }
        ),
        onDismiss = { showMenu = false }
    )

    if (showRename) {
        TextInputDialog(
            title = "Rename",
            initial = current.name,
            placeholder = "File name",
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { name ->
                vm.renameMedia(current, name) { ok ->
                    onMessage(if (ok) "Renamed" else "Android didn't allow renaming this file")
                }
                showRename = false
            }
        )
    }

    if (showInfo) {
        InfoSheet(item = current, onDismiss = { showInfo = false })
    }
}

@Composable
private fun ViewerAction(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    val color = when {
        tint != null -> tint
        destructive -> Color(0xFFFF3B30)
        else -> palette.primaryText
    }
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var container by remember(item.id) { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onZoomChanged(scale > 1.02f) }

    fun clampOffset(candidate: Offset, currentScale: Float): Offset {
        val maxX = (container.width * (currentScale - 1f)) / 2f
        val maxY = (container.height * (currentScale - 1f)) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
            y = candidate.y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { container = it }
            .pointerInput(item.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    scale = newScale
                    offset = if (newScale <= 1.001f) Offset.Zero
                    else clampOffset(offset + pan, newScale)
                }
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.02f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.6f
                        }
                    }
                )
            }
    ) {
        CircularProgressIndicator(
            color = GalleryTheme.palette.tertiaryText,
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
private fun VideoPage(item: MediaItem, active: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember(item.id) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(item.id, active) {
        if (active) {
            val exo = ExoPlayer.Builder(context).build()
            exo.setMediaItem(Media3Item.fromUri(item.uri))
            exo.prepare()
            exo.playWhenReady = true
            player = exo
        }
        onDispose {
            player?.release()
            player = null
        }
    }

    DisposableEffect(lifecycleOwner, item.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    controllerAutoShow = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize()
        )
        if (player == null) {
            CircularProgressIndicator(
                color = GalleryTheme.palette.tertiaryText,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val palette = GalleryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card.copy(alpha = 0.94f),
        scrimColor = palette.scrim,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = item.name,
                color = palette.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(10.dp))
            InfoRow("File name", item.name)
            InfoRow("Type", if (item.isVideo) "Video · ${item.mimeType}" else "Photo · ${item.mimeType}")
            InfoRow("Date", formatDateTime(item.dateMillis))
            InfoRow("Resolution", formatResolution(item.width, item.height))
            InfoRow("Size", formatSize(item.sizeBytes))
            if (item.isVideo) {
                InfoRow("Duration", formatDuration(item.durationMs))
            }
            InfoRow("Folder", item.bucketName)
            Spacer(Modifier.height(10.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = GalleryIcons.Shield,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Stored on this device only",
                    color = palette.secondaryText,
                    fontSize = 11.sp
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
