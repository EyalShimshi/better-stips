package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.R

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * A photo, full screen on black: pinch or double-tap to zoom, drag to look around (#4). [previewUrl] (already in
 * the cache) shows at once while [fullUrl] loads on top of it.
 */
@Composable
fun ImageViewerDialog(fullUrl: String, previewUrl: String?, caption: String?, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val loader = LocalAppContainer.current.imageLoader
        val haptics = LocalHapticFeedback.current
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val density = LocalDensity.current
            val width = with(density) { maxWidth.toPx() }
            val height = with(density) { maxHeight.toPx() }
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            fun clamp(value: Offset, zoom: Float): Offset {
                val maxX = (zoom - 1f) * width / 2f
                val maxY = (zoom - 1f) * height / 2f
                return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
            }

            val state = rememberTransformableState { centroid, zoomChange, panChange, _ ->
                val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                val actualZoom = newScale / scale
                // Keep the point under the fingers in place while zooming.
                val fromCenter = centroid - Offset(width / 2f, height / 2f)
                val zoomed = fromCenter - (fromCenter - offset) * actualZoom
                scale = newScale
                offset = clamp(zoomed + panChange, newScale)
            }
            val zoomModifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (scale <= 1f) onDismiss() },
                        onDoubleTap = { tap ->
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val fromCenter = tap - Offset(width / 2f, height / 2f)
                                scale = DOUBLE_TAP_ZOOM
                                offset = clamp(fromCenter - fromCenter * DOUBLE_TAP_ZOOM, DOUBLE_TAP_ZOOM)
                            }
                        },
                    )
                }
                .transformable(state, canPan = { scale > 1f })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
            Box(zoomModifier) {
                if (previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        imageLoader = loader,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AsyncImage(
                    model = fullUrl,
                    contentDescription = caption,
                    imageLoader = loader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}
