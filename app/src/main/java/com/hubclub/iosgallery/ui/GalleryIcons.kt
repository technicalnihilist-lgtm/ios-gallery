package com.hubclub.iosgallery.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Hand-built line icons (no icon-font dependency) so the app stays small and
 * the shapes match the thin, rounded iOS/macOS look.
 */
object GalleryIcons {

    private fun stroke(name: String, width: Float = 1.7f, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun filled(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path),
                fill = SolidColor(Color.Black)
            )
        }
        return builder.build()
    }

    val Library: ImageVector = stroke(
        "library",
        1.7f,
        "M4.6 8.2A2.7 2.7 0 0 1 7.3 5.5h9.4a2.7 2.7 0 0 1 2.7 2.7v9.6a2.7 2.7 0 0 1-2.7 2.7H7.3a2.7 2.7 0 0 1-2.7-2.7z",
        "M9.9 9.5a1.6 1.6 0 1 0 3.2 0 1.6 1.6 0 1 0-3.2 0",
        "M4.9 16.8l4.1-3.8a1.6 1.6 0 0 1 2.1 0l1.4 1.3 2.3-2.2a1.6 1.6 0 0 1 2.1 0l2.1 2"
    )

    val Albums: ImageVector = stroke(
        "albums",
        1.7f,
        "M4 9.3A2.3 2.3 0 0 1 6.3 7h7.9a2.3 2.3 0 0 1 2.3 2.3v7.4a2.3 2.3 0 0 1-2.3 2.3H6.3A2.3 2.3 0 0 1 4 16.7z",
        "M7.7 4.2h9a3.3 3.3 0 0 1 3.3 3.3v9"
    )

    val Heart: ImageVector = stroke(
        "heart",
        1.7f,
        "M12 20.1s-7.2-4.4-7.2-9.5A4.05 4.05 0 0 1 12 7.4a4.05 4.05 0 0 1 7.2 3.2c0 5.1-7.2 9.5-7.2 9.5z"
    )

    val HeartFilled: ImageVector = filled(
        "heart_filled",
        "M12 20.4s-7.5-4.6-7.5-9.9A4.3 4.3 0 0 1 12 7.1a4.3 4.3 0 0 1 7.5 3.4c0 5.3-7.5 9.9-7.5 9.9z"
    )

    val Search: ImageVector = stroke(
        "search",
        1.7f,
        "M4.2 10.6a6.4 6.4 0 1 0 12.8 0 6.4 6.4 0 1 0-12.8 0",
        "M15.4 15.6l4.4 4.4"
    )

    val Back: ImageVector = stroke("back", 2.0f, "M14.6 5.4L8 12l6.6 6.6")
    val ChevronRight: ImageVector = stroke("chevron_right", 1.8f, "M9.4 5.4L16 12l-6.6 6.6")

    val More: ImageVector = stroke(
        "more",
        2.6f,
        "M5.4 12h.02",
        "M12 12h.02",
        "M18.6 12h.02"
    )

    val Share: ImageVector = stroke(
        "share",
        1.7f,
        "M12 3.6v11.2",
        "M8.2 7.4L12 3.6l3.8 3.8",
        "M5.9 11.5v7.3a1.6 1.6 0 0 0 1.6 1.6h9a1.6 1.6 0 0 0 1.6-1.6v-7.3"
    )

    val Trash: ImageVector = stroke(
        "trash",
        1.7f,
        "M4.5 7h15",
        "M9.5 7V5.4A1.4 1.4 0 0 1 10.9 4h2.2a1.4 1.4 0 0 1 1.4 1.4V7",
        "M6.7 7l.8 12a1.6 1.6 0 0 0 1.6 1.5h5.8a1.6 1.6 0 0 0 1.6-1.5l.8-12"
    )

    val Info: ImageVector = stroke(
        "info",
        1.7f,
        "M4.2 12a7.8 7.8 0 1 0 15.6 0 7.8 7.8 0 1 0-15.6 0",
        "M12 11.2v5.4",
        "M12 8h.02"
    )

    val Play: ImageVector = filled("play", "M8.4 5.5a.9.9 0 0 1 1.4-.75l8.6 6.5a.9.9 0 0 1 0 1.5l-8.6 6.5a.9.9 0 0 1-1.4-.75z")

    val Plus: ImageVector = stroke("plus", 1.9f, "M12 5.4v13.2", "M5.4 12h13.2")
    val Check: ImageVector = stroke("check", 2.0f, "M5.4 12.7l4.5 4.5L18.7 7.3")
    val Close: ImageVector = stroke("close", 1.9f, "M6.6 6.6l10.8 10.8", "M17.4 6.6L6.6 17.4")

    val Pencil: ImageVector = stroke(
        "pencil",
        1.7f,
        "M4.6 19.4l.9-3.9L16.1 4.9a1.6 1.6 0 0 1 2.26 0l.74.74a1.6 1.6 0 0 1 0 2.26L8.5 18.5l-3.9.9z",
        "M14.7 6.3l3 3"
    )

    val Folder: ImageVector = stroke(
        "folder",
        1.7f,
        "M4.2 8.1A2.4 2.4 0 0 1 6.6 5.7h2.7l1.9 2.4h6.2a2.4 2.4 0 0 1 2.4 2.4v6.1a2.4 2.4 0 0 1-2.4 2.4H6.6a2.4 2.4 0 0 1-2.4-2.4z"
    )

    val Restore: ImageVector = filled(
        "restore",
        "M12 5.2V2.4L7.8 6.2 12 10V7.2a5 5 0 1 1-5 5H4.2A7.8 7.8 0 1 0 12 5.2z"
    )

    val Copy: ImageVector = stroke(
        "copy",
        1.7f,
        "M9.2 9.7A2.3 2.3 0 0 1 11.5 7.4h6a2.3 2.3 0 0 1 2.3 2.3v6a2.3 2.3 0 0 1-2.3 2.3h-6a2.3 2.3 0 0 1-2.3-2.3z",
        "M4.2 15.2V6.6a2.4 2.4 0 0 1 2.4-2.4h8.6"
    )

    val Move: ImageVector = stroke(
        "move",
        1.7f,
        "M13.6 3.6v6.2",
        "M10.9 7.1l2.7 2.7 2.7-2.7",
        "M4.2 13.1A2.4 2.4 0 0 1 6.6 10.7h2.7l1.9 2.4h6.2a2.4 2.4 0 0 1 2.4 2.4v3.1a2.4 2.4 0 0 1-2.4 2.4H6.6a2.4 2.4 0 0 1-2.4-2.4z"
    )

    val VideoBadge: ImageVector = filled("video", "M9.4 7.6a.7.7 0 0 1 1.06-.6l5.2 4.4a.7.7 0 0 1 0 1.2l-5.2 4.4a.7.7 0 0 1-1.06-.6z")

    val Camera: ImageVector = stroke(
        "camera",
        1.7f,
        "M4.2 9.4A2.4 2.4 0 0 1 6.6 7h1.6l1.2-2h5.2l1.2 2h1.6a2.4 2.4 0 0 1 2.4 2.4v6a2.4 2.4 0 0 1-2.4 2.4H6.6a2.4 2.4 0 0 1-2.4-2.4z",
        "M9.4 12.4a2.6 2.6 0 1 0 5.2 0 2.6 2.6 0 1 0-5.2 0"
    )

    val Shield: ImageVector = stroke(
        "shield",
        1.7f,
        "M12 3.6l6.6 2.6v5.4c0 4-2.8 7.1-6.6 8.8-3.8-1.7-6.6-4.8-6.6-8.8V6.2z",
        "M9.2 12l2.1 2.1 3.6-3.9"
    )

    val Grid: ImageVector = stroke(
        "grid",
        1.7f,
        "M4.4 7.4A3 3 0 0 1 7.4 4.4h9.2a3 3 0 0 1 3 3v9.2a3 3 0 0 1-3 3H7.4a3 3 0 0 1-3-3z",
        "M9.7 4.6v14.8",
        "M4.6 9.7h14.8"
    )
}
