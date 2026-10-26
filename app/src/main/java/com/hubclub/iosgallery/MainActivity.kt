package com.hubclub.iosgallery

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hubclub.iosgallery.ui.AlbumDetailScreen
import com.hubclub.iosgallery.ui.AlbumsScreen
import com.hubclub.iosgallery.ui.EmptyState
import com.hubclub.iosgallery.ui.FavoritesScreen
import com.hubclub.iosgallery.ui.GalleryBottomBar
import com.hubclub.iosgallery.ui.GalleryIcons
import com.hubclub.iosgallery.ui.GalleryTab
import com.hubclub.iosgallery.ui.GalleryViewModel
import com.hubclub.iosgallery.ui.GlassPanel
import com.hubclub.iosgallery.ui.LaunchAnimation
import com.hubclub.iosgallery.ui.LibraryScreen
import com.hubclub.iosgallery.ui.PrimaryButton
import com.hubclub.iosgallery.ui.SearchScreen
import com.hubclub.iosgallery.ui.TrashScreen
import com.hubclub.iosgallery.ui.ViewerRequest
import com.hubclub.iosgallery.ui.ViewerScreen
import com.hubclub.iosgallery.ui.glassClickable
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import com.hubclub.iosgallery.ui.theme.IOSGalleryTheme
import com.hubclub.iosgallery.util.hasMediaPermission
import com.hubclub.iosgallery.util.openAppSettings
import com.hubclub.iosgallery.util.requiredMediaPermissions
import com.hubclub.iosgallery.util.shouldExplainAgain
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // iOS-style zoom: the window grows out of the icon and shrinks back into it.
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.app_open_enter,
                R.anim.app_open_exit
            )
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.app_close_enter,
                R.anim.app_close_exit
            )
        }
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

/** Height reserved for the floating glass tab bar (scrim + pill + system inset). */
private val BottomBarHeight = 108.dp

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
    var showLaunch by rememberSaveable { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }

    // The content starts small and transparent, then grows out of the launch mark.
    val contentScale = remember { Animatable(0.90f) }
    val contentAlpha = remember { Animatable(0f) }

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

    BackHandler(enabled = viewer == null && screen == Screen.Tabs && !closing) {
        closing = true
    }

    // Opening: once the icon has bloomed, the gallery zooms and fades in.
    LaunchedEffect(showLaunch) {
        if (!showLaunch) {
            coroutineScope {
                launch {
                    contentScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    contentAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    // Closing: shrink back towards the icon, then hand over to the system transition.
    LaunchedEffect(closing) {
        if (closing) {
            coroutineScope {
                launch {
                    contentScale.animateTo(
                        targetValue = 0.90f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    contentAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 200)
                    )
                }
            }
            activity?.finish()
        }
    }

    val deviceAlbums = remember(items) { vm.deviceAlbums(items) }

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                    alpha = contentAlpha.value
                }
        ) {
        if (!granted) {
            PermissionGate(
                asked = asked,
                blocked = blocked,
                onRequest = { permissionLauncher.launch(requiredMediaPermissions()) },
                onOpenSettings = { openAppSettings(context) }
            )
        } else {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (targetState !is Screen.Tabs) {
                        // push: the new screen settles forward, the old one drifts back
                        (fadeIn(tween(240)) + scaleIn(
                            initialScale = 0.965f,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInHorizontally { it / 16 }) togetherWith
                            (fadeOut(tween(150)) + scaleOut(
                                targetScale = 1.015f,
                                animationSpec = tween(200)
                            ))
                    } else {
                        // pop: mirrored, so going back feels like the same movement reversed
                        (fadeIn(tween(220)) + scaleIn(
                            initialScale = 1.02f,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )) togetherWith
                            (fadeOut(tween(180)) + scaleOut(
                                targetScale = 0.97f,
                                animationSpec = tween(220)
                            ))
                    }
                },
                label = "screen"
            ) { current ->
                when (current) {
                    Screen.Tabs -> AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(
                                initialScale = 0.99f,
                                animationSpec = tween(
                                    durationMillis = 260,
                                    easing = FastOutSlowInEasing
                                )
                            )) togetherWith
                                (fadeOut(tween(140)) + scaleOut(
                                    targetScale = 1.005f,
                                    animationSpec = tween(140)
                                ))
                        },
                        label = "tab"
                    ) { currentTab ->
                        when (currentTab) {
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
                                bottomBarHeight = BottomBarHeight,
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
                                bottomBarHeight = BottomBarHeight,
                                vm = vm,
                                onBack = { screen = Screen.Tabs },
                                onOpenViewer = { list, index -> viewer = ViewerRequest(list, index) },
                                onMessage = message
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = screen == Screen.Tabs && !librarySelection && viewer == null,
                enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 4 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(170)) { it / 4 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                GalleryBottomBar(
                    current = tab,
                    onSelect = { tab = it }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 108.dp, start = 12.dp, end = 12.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (palette.isDark) Color(0xFF2C2C33) else Color(0xFF1B1D24),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // ---- full-screen viewer, with a soft glass zoom transition -------------
        val request = viewer
        var lastRequest by remember { mutableStateOf<ViewerRequest?>(null) }
        if (request != null) lastRequest = request
        AnimatedVisibility(
            visible = request != null,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.96f, animationSpec = tween(160))
        ) {
            lastRequest?.let { safe ->
                ViewerScreen(
                    request = safe,
                    favorites = meta.favorites,
                    vm = vm,
                    onClose = { viewer = null },
                    onMessage = message
                )
            }
        }

        }

        // ---- animated app icon on launch --------------------------------------
        AnimatedVisibility(
            visible = showLaunch,
            enter = fadeIn(tween(1)),
            exit = fadeOut(tween(280))
        ) {
            LaunchAnimation(onFinished = { showLaunch = false })
        }
    }
}

@Composable
private fun MissingAlbum(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(12.dp)) {
            com.hubclub.iosgallery.ui.GlassIconButton(
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            shape = RoundedCornerShape(32.dp),
            elevation = 26.dp,
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 30.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.hubclub.iosgallery.ui.FlowerMark(size = 76.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = if (asked) "Photo access is off" else "Welcome to iOS Gallery",
                    color = palette.primaryText,
                    fontSize = 24.sp,
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
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "No account · No internet · No tracking",
                    color = palette.tertiaryText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                PrimaryButton(
                    label = if (asked) "Allow Access" else "Continue",
                    onClick = onRequest
                )
                if (blocked) {
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        label = "Open Settings",
                        onClick = onOpenSettings,
                        filled = false
                    )
                }
                Spacer(Modifier.height(26.dp))
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
    }
}
