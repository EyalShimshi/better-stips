package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.SwipeRules
import io.github.eyalshimshi.betterstips.util.SwipeSide
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.delay

/** What a swipe runs (#18): its words (shown behind the row, and for TalkBack), its icon, and whether it removes something. */
data class SwipeSpec(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onSwipe: () -> Unit)

/**
 * How far a swipe goes before letting go runs it (v0.16): the answers' distance, which the owner found right (#18).
 * Material's swipe-to-dismiss uses 56 dp; questions in lists needed 96 dp before.
 */
val SWIPE_TRIGGER: Dp = 64.dp

/** How far an answer or a chat message can be pulled. */
val SWIPE_MAX: Dp = 104.dp

/** Questions in lists go further, so the action's words can show behind them. */
private val ROW_SWIPE_MAX = 160.dp

/** A flick faster than this (per second) runs a swipe short of the line: Material's speed (Android's lists use 120). */
val SWIPE_FLING: Dp = 125.dp

/** ...once it has moved this far, so a twitch doesn't. */
private val SWIPE_FLING_MIN = 16.dp

/** Past the line the item follows the finger less, so the line can be felt; a question's row less so. */
private const val SWIPE_RESISTANCE = 0.3f
private const val ROW_SWIPE_RESISTANCE = 0.5f

/** A tip's hint (#28) slides the item this share of the way to the line, so it doesn't run, and holds it there. */
private const val PEEK_SHARE = 0.85f
private const val PEEK_OUT_MS = 450
private const val PEEK_HOLD_MS = 1_300L

/** About how long a hint takes, out and back, for tips that follow one another. */
const val PEEK_TOTAL_MS = 2_400L

/**
 * One sideways swipe (v0.16), shared by questions in lists, answers and chat messages: the item follows the finger,
 * the phone ticks at the line, and letting go past it, or a flick toward a side, runs that side's action. A flick
 * back cancels. It springs back either way.
 */
@Stable
class SwipeDrag internal constructor(
    private val trigger: Float,
    private val max: Float,
    private val resistance: Float,
    private val fling: Float,
    private val flingMin: Float,
    private val onArmed: () -> Unit,
) {
    /** How far it's pulled now, in px: positive to the right. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** Past the line: letting go now runs it. */
    var armed by mutableStateOf(false)
        private set

    internal var canRight = false
    internal var canLeft = false

    /** 0 at rest to 1 at the line, to the right and to the left: how much of what's behind shows. */
    val rightProgress: Float get() = (offset / trigger).coerceIn(0f, 1f)
    val leftProgress: Float get() = (-offset / trigger).coerceIn(0f, 1f)

    internal val state = DraggableState { delta ->
        val outward = offset != 0f && (delta > 0f) == (offset > 0f)
        val step = if (outward && abs(offset) >= trigger) delta * resistance else delta
        offset = (offset + step).coerceIn(if (canLeft) -max else 0f, if (canRight) max else 0f)
        val nowArmed = abs(offset) >= trigger
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) onArmed()
        }
    }

    internal fun release(velocity: Float): SwipeSide? = SwipeRules.release(offset, velocity, trigger, fling, flingMin)

    internal suspend fun settle() {
        armed = false
        animate(offset, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> offset = value }
    }

    /**
     * A tip's hint (#28): the item slides toward [side] most of the way to the line, so what's behind it shows, stays a
     * moment and springs back; without [animated] it jumps there and back. It moves as a drag, so a finger on the item
     * takes over at once (and this ends).
     */
    suspend fun peek(side: SwipeSide, animated: Boolean) {
        if (if (side == SwipeSide.Right) !canRight else !canLeft) return
        val target = (if (side == SwipeSide.Right) 1f else -1f) * trigger * PEEK_SHARE
        state.drag(MutatePriority.Default) {
            var at = offset
            val moveTo: (Float) -> Unit = { value ->
                dragBy(value - at)
                at = value
            }
            if (animated) {
                animate(at, target, animationSpec = tween(PEEK_OUT_MS, easing = FastOutSlowInEasing)) { value, _ -> moveTo(value) }
                delay(PEEK_HOLD_MS)
                animate(at, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> moveTo(value) }
            } else {
                moveTo(target)
                delay(PEEK_OUT_MS + PEEK_HOLD_MS)
                moveTo(0f)
            }
        }
    }
}

/** A [SwipeDrag] for an answer or a chat message; [trigger] and [max] are where it runs and how far it goes. */
@Composable
fun rememberSwipeDrag(trigger: Dp = SWIPE_TRIGGER, max: Dp = SWIPE_MAX, resistance: Float = SWIPE_RESISTANCE): SwipeDrag {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    return remember(density, trigger, max, resistance) {
        with(density) {
            SwipeDrag(trigger.toPx(), max.toPx(), resistance, SWIPE_FLING.toPx(), SWIPE_FLING_MIN.toPx()) {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
        }
    }
}

/**
 * Makes this the handle of [drag]: [onRight] runs on a swipe to the right (left to right, in Hebrew too) and [onLeft]
 * on one to the left. A side without an action doesn't move.
 */
fun Modifier.swipeToAct(drag: SwipeDrag, onRight: (() -> Unit)?, onLeft: (() -> Unit)?): Modifier {
    drag.canRight = onRight != null
    drag.canLeft = onLeft != null
    return draggable(
        state = drag.state,
        orientation = Orientation.Horizontal,
        enabled = onRight != null || onLeft != null,
        onDragStopped = { velocity ->
            when (drag.release(velocity)) {
                SwipeSide.Right -> onRight?.invoke()
                SwipeSide.Left -> onLeft?.invoke()
                null -> Unit
            }
            drag.settle()
        },
    )
}

/**
 * A row that runs an action when swiped, as in Gmail (#18): [right] when dragged to the right (left to right, in
 * Hebrew too), [left] when dragged to the left. Behind the row the action's color, icon and words show as it's
 * dragged; at the line the phone ticks and the icon pops, and letting go there, or flicking, runs it (v0.16).
 */
@Composable
fun SwipeActionsBox(
    right: SwipeSpec?,
    left: SwipeSpec?,
    modifier: Modifier = Modifier,
    hint: SwipeHint? = null,
    content: @Composable () -> Unit,
) {
    val drag = rememberSwipeDrag(max = ROW_SWIPE_MAX, resistance = ROW_SWIPE_RESISTANCE)
    // A tip slides the row a little to show what a swipe does (#28).
    val tip = rememberTipState()
    SwipeHintEffect(hint, drag, tip)
    val elements = LocalAppSettings.current.motion.elements
    val pop = animateFloatAsState(
        targetValue = if (drag.armed && elements) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipe-pop",
    )
    // Only which side is out changes what's drawn; the distance is read while drawing.
    val direction by remember(drag) { derivedStateOf { sign(drag.offset) } }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .swipeToAct(drag, right?.onSwipe, left?.onSwipe)
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
            direction > 0f -> right
            direction < 0f -> left
            else -> null
        }
        if (shown != null) {
            val toRight = direction > 0f
            val background = if (shown.destructive) colors.errorContainer else colors.secondaryContainer
            val tint = if (shown.destructive) colors.onErrorContainer else colors.onSecondaryContainer
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val progress = if (toRight) drag.rightProgress else drag.leftProgress
                        alpha = if (drag.armed) 1f else 0.35f + 0.5f * progress
                    }
                    .background(background),
            )
            Row(
                Modifier
                    .align(if (toRight) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                    .absolutePadding(left = if (toRight) 20.dp else 0.dp, right = if (toRight) 0.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    shown.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val progress = if (toRight) drag.rightProgress else drag.leftProgress
                            val scale = (0.7f + 0.3f * progress) * pop.value
                            alpha = progress
                            scaleX = scale
                            scaleY = scale
                        },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    shown.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = if (toRight) drag.rightProgress else drag.leftProgress },
                )
            }
        }
        Box(
            Modifier
                .absoluteOffset { IntOffset(drag.offset.roundToInt(), 0) }
                .background(colors.background),
        ) { content() }
        TipPill(tip, Modifier.align(Alignment.Center))
    }
}