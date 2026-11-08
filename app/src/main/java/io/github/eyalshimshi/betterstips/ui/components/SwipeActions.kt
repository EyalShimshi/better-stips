package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** What a swipe runs (#18): its words (shown behind the row, and for TalkBack), its icon, and whether it removes something. */
data class SwipeSpec(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onSwipe: () -> Unit)

private val ROW_SWIPE_THRESHOLD = 96.dp
private val ROW_SWIPE_MAX = 180.dp

/**
 * A row that runs an action when swiped far enough, as in Gmail (#18): [right] when dragged to the right (left to
 * right, in Hebrew too), [left] when dragged to the left. Behind the row the action's color, icon and words show as
 * it's dragged; past the line the phone ticks, and letting go there runs it. The row springs back either way.
 */
@Composable
fun SwipeActionsBox(right: SwipeSpec?, left: SwipeSpec?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val threshold = with(density) { ROW_SWIPE_THRESHOLD.toPx() }
    val maxDrag = with(density) { ROW_SWIPE_MAX.toPx() }
    var dragX by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    val currentRight by rememberUpdatedState(right)
    val currentLeft by rememberUpdatedState(left)
    val dragState = rememberDraggableState { delta ->
        val resisted = if (abs(dragX) >= threshold) delta * 0.3f else delta
        dragX = (dragX + resisted).coerceIn(if (currentLeft != null) -maxDrag else 0f, if (currentRight != null) maxDrag else 0f)
        val nowArmed = abs(dragX) >= threshold
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = right != null || left != null,
                onDragStopped = {
                    when {
                        dragX >= threshold -> currentRight?.onSwipe?.invoke()
                        dragX <= -threshold -> currentLeft?.onSwipe?.invoke()
                    }
                    armed = false
                    animate(dragX, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> dragX = value }
                },
            )
            .semantics {
                customActions = listOfNotNull(right, left).map { spec ->
                    CustomAccessibilityAction(spec.label) {
                        spec.onSwipe()
                        true
                    }
                }
            },
    ) {
        val shown = when {
            dragX > 0f -> right
            dragX < 0f -> left
            else -> null
        }
        if (shown != null) {
            val toRight = dragX > 0f
            val progress = (abs(dragX) / threshold).coerceIn(0f, 1f)
            val background = if (shown.destructive) colors.errorContainer else colors.secondaryContainer
            val tint = if (shown.destructive) colors.onErrorContainer else colors.onSecondaryContainer
            Box(
                Modifier
                    .matchParentSize()
                    .background(background.copy(alpha = if (armed) 1f else 0.35f + 0.5f * progress)),
            )
            Row(
                Modifier
                    .align(if (toRight) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                    .absolutePadding(left = if (toRight) 20.dp else 0.dp, right = if (toRight) 0.dp else 20.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.7f + 0.3f * progress
                        scaleY = 0.7f + 0.3f * progress
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(shown.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(shown.label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
            }
        }
        Box(
            Modifier
                .absoluteOffset { IntOffset(dragX.roundToInt(), 0) }
                .background(colors.background),
        ) { content() }
    }
}
