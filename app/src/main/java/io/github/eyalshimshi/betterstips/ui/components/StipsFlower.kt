package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stips' flower colors (#27, from its own flower): coral petals around a deeper red one, on a soft green stem. */
object FlowerColors {
    val Petal = Color(0xFFEF5350)
    val PetalLight = Color(0xFFF47A6E)
    val PetalDeep = Color(0xFFD32F2F)
    val Stem = Color(0xFF7CC47F)

    /** The soft circle behind a flower in lists, on light and dark themes alike. */
    val Tint = Petal.copy(alpha = 0.16f)
}

/**
 * Stips' flower (#27, v0.18): a tulip in Stips' colors, in place of Material's monochrome flower wherever flowers show
 * (answers, profiles, notifications, the flowers limit), so it looks like the flower Stips users know.
 */
val StipsFlower: ImageVector by lazy {
    ImageVector.Builder(name = "StipsFlower", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        // The stem, then a leaf to each side.
        .path(fill = SolidColor(FlowerColors.Stem)) {
            moveTo(11.35f, 10.4f)
            lineTo(12.65f, 10.4f)
            lineTo(12.65f, 21.6f)
            curveTo(12.65f, 22.0f, 12.35f, 22.3f, 12.0f, 22.3f)
            curveTo(11.65f, 22.3f, 11.35f, 22.0f, 11.35f, 21.6f)
            close()
        }
        .path(fill = SolidColor(FlowerColors.Stem)) {
            moveTo(11.6f, 19.6f)
            curveTo(9.2f, 19.8f, 6.9f, 18.4f, 6.0f, 15.6f)
            curveTo(8.7f, 15.4f, 10.9f, 16.9f, 11.6f, 19.6f)
            close()
        }
        .path(fill = SolidColor(FlowerColors.Stem)) {
            moveTo(12.4f, 17.6f)
            curveTo(13.1f, 15.0f, 15.2f, 13.6f, 17.9f, 13.7f)
            curveTo(17.1f, 16.4f, 14.9f, 17.8f, 12.4f, 17.6f)
            close()
        }
        // The two outer petals, then the deeper one between them.
        .path(fill = SolidColor(FlowerColors.Petal)) {
            moveTo(12.0f, 11.0f)
            curveTo(8.6f, 11.0f, 7.0f, 8.6f, 7.0f, 5.6f)
            lineTo(7.0f, 3.6f)
            curveTo(8.7f, 4.4f, 10.2f, 5.6f, 11.1f, 7.2f)
            close()
        }
        .path(fill = SolidColor(FlowerColors.PetalLight)) {
            moveTo(12.0f, 11.0f)
            curveTo(15.4f, 11.0f, 17.0f, 8.6f, 17.0f, 5.6f)
            lineTo(17.0f, 3.6f)
            curveTo(15.3f, 4.4f, 13.8f, 5.6f, 12.9f, 7.2f)
            close()
        }
        .path(fill = SolidColor(FlowerColors.PetalDeep)) {
            moveTo(12.0f, 2.4f)
            curveTo(13.9f, 4.0f, 14.6f, 6.4f, 14.4f, 8.4f)
            curveTo(14.2f, 10.0f, 13.2f, 11.0f, 12.0f, 11.0f)
            curveTo(10.8f, 11.0f, 9.8f, 10.0f, 9.6f, 8.4f)
            curveTo(9.4f, 6.4f, 10.1f, 4.0f, 12.0f, 2.4f)
            close()
        }
        .build()
}

/** Stips' flower in its own colors (v0.18). [contentDescription]: null when words beside it already say "flower". */
@Composable
fun FlowerIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, contentDescription: String? = null) {
    Icon(StipsFlower, contentDescription = contentDescription, tint = Color.Unspecified, modifier = modifier.size(size))
}
