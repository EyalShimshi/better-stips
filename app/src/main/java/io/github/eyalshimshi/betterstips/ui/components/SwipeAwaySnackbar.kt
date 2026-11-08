package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.SnackbarExit
import io.github.eyalshimshi.betterstips.util.SwipeRules
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** How long a swiped snackbar takes to leave. */
private const val EXIT_MS = 160

/**
 * The app's snackbars (v0.16): as before, and a swipe down or to either side sends one away at once, as Android's own
 * do. TalkBack offers "dismiss" instead.
 */
@Composable
fun SwipeAwaySnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> SwipeAwaySnackbar(data) }
}

@Composable
private fun SwipeAwaySnackbar(data: SnackbarData) {
    val elements = LocalAppSettings.current.motion.elements
    val fling = with(LocalDensity.current) { SWIPE_FLING.toPx() }
    val scope = rememberCoroutineScope()
    val currentData by rememberUpdatedState(data)
    var x by remember(data) { mutableFloatStateOf(0f) }
    var y by remember(data) { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun springBack() {
        scope.launch {
            coroutineScope {
                launch { animate(x, 0f, animationSpec = spring()) { value, _ -> x = value } }
                launch { animate(y, 0f, animationSpec = spring()) { value, _ -> y = value } }
            }
        }
    }

    // The gesture is read here, where the snackbar doesn't move; the snackbar itself is drawn moved.
    Box(
        Modifier
            .onSizeChanged { boxSize = it }
            .semantics {
                dismiss {
                    currentData.dismiss()
                    true
                }
            }
            .pointerInput(data) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity()
                        val width = boxSize.width.toFloat()
                        val height = boxSize.height.toFloat()
                        val exit = SwipeRules.snackbarExit(x, y, velocity.x, velocity.y, width, height, fling)
                        if (exit == null) {
                            springBack()
                        } else {
                            scope.launch {
                                if (elements) {
                                    val spec = tween<Float>(EXIT_MS, easing = FastOutLinearInEasing)
                                    when (exit) {
                                        SnackbarExit.Right -> animate(x, width * 1.2f, animationSpec = spec) { value, _ -> x = value }
                                        SnackbarExit.Left -> animate(x, -width * 1.2f, animationSpec = spec) { value, _ -> x = value }
                                        SnackbarExit.Down -> animate(y, height * 1.5f, animationSpec = spec) { value, _ -> y = value }
                                        null -> Unit
                                    }
                                }
                                currentData.dismiss()
                            }
                        }
                    },
                    onDragCancel = { springBack() },
                    onDrag = { change, amount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        x += amount.x
                        // It goes down or sideways, not up.
                        y = max(0f, y + amount.y)
                    },
                )
            },
    ) {
        Snackbar(
            data,
            modifier = Modifier.graphicsLayer {
                translationX = x
                translationY = y
                val across = if (boxSize.width > 0) abs(x) / boxSize.width else 0f
                val down = if (boxSize.height > 0) y / boxSize.height else 0f
                alpha = 1f - (max(across, down) * 0.8f).coerceIn(0f, 1f)
            },
        )
    }
}
