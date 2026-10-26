package com.hubclub.iosgallery.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubclub.iosgallery.data.AppSettings
import com.hubclub.iosgallery.data.SettingsStore
import com.hubclub.iosgallery.ui.theme.GalleryTheme
import kotlin.math.roundToInt

/**
 * Live appearance controls: dock size, glass transparency and theme.
 * Every change is applied immediately and remembered on the device.
 */
@Composable
fun CustomizeScreen(
    settings: AppSettings,
    onDockScale: (Float) -> Unit,
    onGlassOpacity: (Float) -> Unit,
    onThemeMode: (String) -> Unit,
    onGridColumns: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val palette = GalleryTheme.palette

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(title = "Customize", onBack = onBack) {
            // .glassEffect(.regular.tint(.blue.opacity(0.4)).interactive(), in: .capsule)
            GlassPanel(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .glassClickable(pressedScale = 0.92f, onClick = onReset),
                shape = RoundedCornerShape(50),
                elevation = 8.dp,
                tint = palette.accent.copy(alpha = 0.30f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Reset",
                    color = palette.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 28.dp)
        ) {
            // ---- live dock preview -------------------------------------------
            Text(
                text = "Preview",
                color = palette.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
            )
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                elevation = 14.dp,
                contentPadding = PaddingValues(vertical = 22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DockPill(
                        current = GalleryTab.Library,
                        onSelect = {},
                        scale = settings.dockScale
                    )
                }
            }

            // ---- dock size ---------------------------------------------------
            SettingsCard(
                title = "Dock size",
                value = "${(settings.dockScale * 100).roundToInt()}%",
                hint = "Make the bottom dock smaller or larger."
            ) {
                Slider(
                    value = settings.dockScale,
                    onValueChange = onDockScale,
                    valueRange = SettingsStore.MIN_DOCK..SettingsStore.MAX_DOCK,
                    colors = sliderColors()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", color = palette.tertiaryText, fontSize = 11.sp)
                    Text("Large", color = palette.tertiaryText, fontSize = 11.sp)
                }
            }

            // ---- glass transparency ------------------------------------------
            SettingsCard(
                title = "Glass transparency",
                value = "${(settings.glassOpacity * 100).roundToInt()}%",
                hint = "Lower is more transparent — your photos show through the panels."
            ) {
                Slider(
                    value = settings.glassOpacity,
                    onValueChange = onGlassOpacity,
                    valueRange = SettingsStore.MIN_GLASS..1f,
                    colors = sliderColors()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Clear", color = palette.tertiaryText, fontSize = 11.sp)
                    Text("Solid", color = palette.tertiaryText, fontSize = 11.sp)
                }
            }

            // ---- grid columns -------------------------------------------------
            SettingsCard(
                title = "Grid columns",
                value = "${settings.gridColumns} per row",
                hint = "How many thumbnails fit across the library."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (2..5).forEach { count ->
                        ThemeChip(
                            label = "$count",
                            selected = settings.gridColumns == count,
                            modifier = Modifier.weight(1f),
                            onClick = { onGridColumns(count) }
                        )
                    }
                }
            }

            // ---- theme --------------------------------------------------------
            SettingsCard(
                title = "Appearance",
                value = settings.themeMode.replaceFirstChar { it.uppercase() },
                hint = "Follow the system, or stay light or dark."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        .forEach { (key, label) ->
                            ThemeChip(
                                label = label,
                                selected = settings.themeMode == key,
                                modifier = Modifier.weight(1f),
                                onClick = { onThemeMode(key) }
                            )
                        }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GalleryIcons.Shield,
                    contentDescription = null,
                    tint = palette.tertiaryText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Saved on this device only — no account, no sync.",
                    color = palette.tertiaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = GalleryTheme.palette.accent,
    activeTrackColor = GalleryTheme.palette.accent,
    inactiveTrackColor = GalleryTheme.palette.accent.copy(alpha = 0.18f)
)

@Composable
private fun SettingsCard(
    title: String,
    value: String,
    hint: String,
    content: @Composable () -> Unit
) {
    val palette = GalleryTheme.palette
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 14.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                color = palette.secondaryText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = GalleryTheme.palette
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.26f),
                            palette.accent.copy(alpha = 0.14f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                }
            )
            .glassClickable(pressedScale = 0.94f, onClick = onClick),
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
