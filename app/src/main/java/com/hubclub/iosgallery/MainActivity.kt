package com.hubclub.iosgallery

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hubclub.iosgallery.ui.AlbumsScreen
import com.hubclub.iosgallery.ui.AlbumDetailScreen
import com.hubclub.iosgallery.ui.EmptyState
import com.hubclub.iosgallery.ui.FavoritesScreen
import com.hubclub.iosgallery.ui.GalleryBottomBar
import com.hubclub.iosgallery.ui.GalleryIcons
import com.hubclub.iosgallery.ui.GalleryTab
import com.hubclub.iosgallery.ui.GalleryViewModel
import com.hubclub.iosgallery.ui.LibraryScreen
import com.hubclub.iosgallery.ui.PrimaryButton
import com.hubclub.iosgallery.ui.SearchScreen
import com.hubclub.iosgallery.ui.TrashScreen
import com.hubclub.iosgallery.ui.ViewerRequest
import com.hubclub.iosgallery.ui.ViewerScreen
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.ui.theme.IOSGalleryTheme
import com.hubclub.iosgallery.util.hasMediaPermission
import com.hubclub.iosgallery.util.openAppSettings
import com.hubclub.iosgallery.util.requiredMediaPermissions
import com.hubclub.iosgallery.util.shouldExplainAgain
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IOSGalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GalleryTheme.palette.background
                ) {
                    GalleryRoot()
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Tabs : Screen
    data object Trash : Screen
    data class DeviceAlbumDetail(val bucketId: Long) : Screen
    data class ManagedAlbumDetail(val albumId: String) : Screen
}

private val BottomBarHeight = 58.dp

@Composable
fun GalleryRoot(vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val palette = GalleryTheme.palette

    val items by vm.items.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    var granted by remember { mutableStateOf(hasMediaPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(GalleryTab.Library) }
    var screen by remember { mutableStateOf<Screen>(Screen.Tabs) }
    var viewer by remember { mutableStateOf<ViewerRequest?>(null) }
    var librarySelection by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message: (String) -> Unit = { text ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        asked = true
        val now = hasMediaPermission(context)
        granted = now
        blocked = !now && result.values.isNotEmpty() && result.values.none { it } &&
            !shouldExplainAgain(activity)
        if (now) vm.refresh()
    }

    // Pick up changes made in system settings, and newly captured media.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = hasMediaPermission(context)
                if (now != granted) granted = now
                if (now) vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = viewer == null && screen != Screen.Tabs) {
        screen = Screen.Tabs
    }

    val deviceAlbums = remember(items) { vm.deviceAlbums(items) }

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        if (!granted) {
            PermissionGate(
                asked = asked,
                blocked = blocked,
                onRequest = { permissionLauncher.launch(requiredMediaPermissions()) },
                onOpenSettings = { openAppSettings(context) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val current = screen) {
                    Screen.Tabs -> when (tab) {
                        GalleryTab.Library -> LibraryScreen(
                            items = items,
                            favorites = meta.favorites,
                            loading = loading,
                            albums = meta.albums,
                            bottomBarHeight = BottomBarHeight,
                            vm = vm,
                            onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) },
                            onSelectionActive = { librarySelection = it },
                            onOpenTrash = { screen = Screen.Trash },
                            onMessage = message
                        )

                        GalleryTab.Albums -> AlbumsScreen(
                            items = items,
                            favorites = meta.favorites,
                            deviceAlbums = deviceAlbums,
                            managedAlbums = meta.albums,
                            trashCount = meta.trash.size,
                            bottomBarHeight = BottomBarHeight,
                            vm = vm,
                            onOpenDeviceAlbum = { album ->
                                screen = Screen.DeviceAlbumDetail(album.bucketId)
                            },
                            onOpenManagedAlbum = { album ->
                                screen = Screen.ManagedAlbumDetail(album.id)
                            },
                            onOpenTrash = { screen = Screen.Trash },
                            onMessage = message
                        )

                        GalleryTab.Favorites -> FavoritesScreen(
                            items = items,
                            favorites = meta.favorites,
                            bottomBarHeight = BottomBarHeight,
                            onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) }
                        )

                        GalleryTab.Search -> SearchScreen(
                            items = items,
                            favorites = meta.favorites,
                            deviceAlbums = deviceAlbums,
                            managedAlbums = meta.albums,
                            bottomBarHeight = BottomBarHeight,
                            onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) },
                            onOpenDeviceAlbum = { album ->
                                screen = Screen.DeviceAlbumDetail(album.bucketId)
                            },
                            onOpenManagedAlbum = { album ->
                                screen = Screen.ManagedAlbumDetail(album.id)
                            }
                        )
                    }

                    Screen.Trash -> TrashScreen(
                        entries = meta.trash,
                        trashDir = File(context.filesDir, "trash"),
                        vm = vm,
                        onBack = { screen = Screen.Tabs },
                        onMessage = message
                    )

                    is Screen.DeviceAlbumDetail -> {
                        val album = deviceAlbums.firstOrNull { it.bucketId == current.bucketId }
                        if (album == null) {
                            MissingAlbum(onBack = { screen = Screen.Tabs })
                        } else {
                            AlbumDetailScreen(
                                title = album.name,
                                items = album.items,
                                allItems = items,
                                favorites = meta.favorites,
                                managedAlbum = null,
                                bottomBarHeight = 0.dp,
                                vm = vm,
                                onBack = { screen = Screen.Tabs },
                                onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) },
                                onMessage = message
                            )
                        }
                    }

                    is Screen.ManagedAlbumDetail -> {
                        val album = meta.albums.firstOrNull { it.id == current.albumId }
                        if (album == null) {
                            MissingAlbum(onBack = { screen = Screen.Tabs })
                        } else {
                            val albumItems = remember(album.itemIds, items) {
                                val byId = items.associateBy { it.id }
                                album.itemIds.mapNotNull { byId[it] }
                            }
                            AlbumDetailScreen(
                                title = album.name,
                                items = albumItems,
                                allItems = items,
                                favorites = meta.favorites,
                                managedAlbum = album,
                                bottomBarHeight = 0.dp,
                                vm = vm,
                                onBack = { screen = Screen.Tabs },
                                onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) },
                                onMessage = message
                            )
                        }
                    }
                }
            }

            if (screen == Screen.Tabs && !librarySelection && viewer == null) {
                GalleryBottomBar(
                    current = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 78.dp, start = 12.dp, end = 12.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (palette.isDark) Color(0xFF2C2C2E) else Color(0xFF1C1C1E),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        val request = viewer
        if (request != null) {
            ViewerScreen(
                request = request,
                favorites = meta.favorites,
                vm = vm,
                onClose = { viewer = null },
                onMessage = message
            )
        }
    }
}

@Composable
private fun MissingAlbum(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(8.dp)) {
            com.hubclub.iosgallery.ui.CircleIconButton(
                icon = GalleryIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                tint = GalleryTheme.palette.primaryText,
                size = 38.dp
            )
        }
        EmptyState(
            icon = GalleryIcons.Albums,
            title = "Album unavailable",
            message = "This album no longer exists."
        )
    }
}

// ---------------------------------------------------------------- permissions

@Composable
private fun PermissionGate(
    asked: Boolean,
    blocked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = GalleryTheme.palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FlowerMark(size = 74.dp)
        Spacer(Modifier.height(26.dp))
        Text(
            text = if (asked) "Photo access is off" else "Welcome to iOS Gallery",
            color = palette.primaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (asked)
                "iOS Gallery needs access to the photos and videos on this device to show them. You can grant access again below, or allow it in system settings."
            else
                "A simple, private gallery for the photos and videos already on your device. Grant access and everything appears in one place.",
            color = palette.secondaryText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No account · No internet · No tracking",
            color = palette.tertiaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(label = if (asked) "Allow Access" else "Continue", onClick = onRequest)
        if (blocked) {
            Spacer(Modifier.height(10.dp))
            PrimaryButton(label = "Open Settings", onClick = onOpenSettings, filled = false)
        }
        Spacer(Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = GalleryIcons.Shield,
                contentDescription = null,
                tint = palette.tertiaryText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "This app has no internet permission at all",
                color = palette.tertiaryText,
                fontSize = 11.sp
            )
        }
    }
}

/** The app's multicoloured flower mark, drawn with Compose (no bitmaps). */
@Composable
fun FlowerMark(size: Dp) {
    val colors = listOf(
        Color(0xFFFF3B30),
        Color(0xFFFF9500),
        Color(0xFFFFCC00),
        Color(0xFF34C759),
        Color(0xFF0A84FF),
        Color(0xFFAF52DE)
    )
    Canvas(modifier = Modifier.size(size)) {
        val diameter = this.size.minDimension
        val center = Offset(diameter / 2f, diameter / 2f)
        val petalRadius = diameter * 0.136f
        val distance = diameter * 0.198f
        colors.forEachIndexed { index, color ->
            val angle = Math.toRadians(index * 60.0)
            val cx = center.x + distance * sin(angle).toFloat()
            val cy = center.y - distance * cos(angle).toFloat()
            drawCircle(color = color, radius = petalRadius, center = Offset(cx, cy))
        }
        drawCircle(color = Color.White, radius = diameter * 0.10f, center = center)
    }
}
