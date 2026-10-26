package com.hubclub.iosgallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Press feedback shared by every glass control: a small spring scale-down that
 * makes the surface feel like it has weight under the finger.
 */
fun Modifier.glassClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "glassPress"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/**
 * The liquid-glass surface: translucent fill, bright top border, diagonal specular
 * sheen and a soft shadow. Works on every API level; the translucency keeps the
 * content behind it visible, exactly like frosted glass.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 16.dp,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = GalleryTheme.glass
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = glass.shadow.copy(alpha = 0.5f),
                spotColor = glass.shadow.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glass.fillTop, glass.fillBottom)))
            .then(if (tint != null) Modifier.background(tint) else Modifier)
            .border(
                width = 0.9.dp,
                brush = Brush.verticalGradient(listOf(glass.borderTop, glass.borderBottom)),
                shape = shape
            )
    ) {
        // specular sheen across the top-left corner
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        0f to glass.sheen,
                        0.55f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
        )
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Circular glass button used for every toolbar action. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp
) {
    val palette = GalleryTheme.palette
    GlassPanel(
        modifier = modifier
            .size(size)
            .glassClickable(pressedScale = 0.9f, onClick = onClick),
        shape = CircleShape,
        elevation = 10.dp
    ) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: palette.primaryText,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/** Small translucent pill, e.g. the video duration badge on a thumbnail. */
@Composable
fun GlassChip(
    text: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val glass = GalleryTheme.glass
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(0.7.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(9.dp)
                )
                if (text != null) Spacer(Modifier.size(3.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Soft ambient light behind the content, so the glass has something to refract. */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val palette = GalleryTheme.palette
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.background, palette.backgroundDeep)
                )
            )
    ) {
        Box(
            Modifier
                .size(320.dp)
                .offset(x = (-90).dp, y = (-60).dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.accent.copy(alpha = if (palette.isDark) 0.22f else 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .offset(x = 220.dp, y = 120.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFAF52DE).copy(alpha = if (palette.isDark) 0.20f else 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// --------------------------------------------------------------------- the mark

/**
 * The app's multicoloured flower / aperture mark, drawn with Compose.
 * When [animated] the petals bloom out one after another with a springy stagger.
 */
@Composable
fun FlowerMark(
    size: Dp,
    animated: Boolean = false
) {
    val colors = remember {
        listOf(
            Color(0xFFFF3B30),
            Color(0xFFFF9500),
            Color(0xFFFFCC00),
            Color(0xFF34C759),
            Color(0xFF0A84FF),
            Color(0xFFAF52DE)
        )
    }
    var started by remember { mutableStateOf(!animated) }

    val petalScales = colors.indices.map { index ->
        val scale by animateFloatAsState(
            targetValue = if (started) 1f else 0.2f,
            animationSpec = tween(
                durationMillis = 520,
                delayMillis = index * 65,
                easing = FastOutSlowInEasing
            ),
            label = "petal$index"
        )
        scale
    }
    val petalAlpha = colors.indices.map { index ->
        val alpha by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(durationMillis = 380, delayMillis = index * 65),
            label = "petalAlpha$index"
        )
        alpha
    }
    val coreScale by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "core"
    )

    LaunchedEffect(Unit) {
        if (animated) {
            delay(60)
            started = true
        }
    }

    Canvas(modifier = Modifier.size(size)) {
        val diameter = this.size.minDimension
        val center = Offset(diameter / 2f, diameter / 2f)
        val petalRadius = diameter * 0.136f
        val distance = diameter * 0.198f
        colors.forEachIndexed { index, color ->
            val angle = Math.toRadians(index * 60.0)
            val cx = center.x + distance * sin(angle).toFloat()
            val cy = center.y - distance * cos(angle).toFloat()
            drawCircle(
                color = color,
                radius = petalRadius * petalScales[index],
                center = Offset(cx, cy),
                alpha = petalAlpha[index]
            )
            // glassy highlight on the upper-left of each petal
            drawCircle(
                color = Color.White,
                radius = petalRadius * 0.34f * petalScales[index],
                center = Offset(
                    cx - petalRadius * 0.30f,
                    cy - petalRadius * 0.34f
                ),
                alpha = 0.32f * petalAlpha[index]
            )
        }
        drawCircle(
            color = Color.White,
            radius = diameter * 0.10f * coreScale,
            center = center,
            alpha = coreScale
        )
    }
}

// ------------------------------------------------------------ launch animation

/**
 * The animated app icon: the flower blooms petal by petal, a specular highlight
 * sweeps across it, the wordmark rises in, then everything fades into the gallery.
 */
@Composable
fun LaunchAnimation(onFinished: () -> Unit) {
    val palette = GalleryTheme.palette
    val fade = remember { Animatable(1f) }
    val markScale = remember { Animatable(0.86f) }
    val sweep = remember { Animatable(-1f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        markScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        sweep.animateTo(1.6f, tween(620, easing = FastOutSlowInEasing))
        wordAlpha.animateTo(1f, tween(360))
        wordOffset.animateTo(0f, tween(460, easing = FastOutSlowInEasing))
        delay(420)
        fade.animateTo(0f, tween(340))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .graphicsLayer { alpha = fade.value },
        contentAlignment = Alignment.Center
    ) {
        // soft light behind the mark
        Box(
            Modifier
                .size(260.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.30f),
                            Color(0xFFAF52DE).copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    scaleX = markScale.value
                    scaleY = markScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            FlowerMark(size = 104.dp, animated = true)
            // specular highlight sweeping across the glass
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(170.dp)
                    .graphicsLayer {
                        translationX = sweep.value * 110f
                        rotationZ = 16f
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 118.dp)
                .graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordOffset.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "iOS Gallery",
                color = palette.primaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Photos, privately",
                color = palette.secondaryText,
                fontSize = 12.sp
            )
        }
    }
}
