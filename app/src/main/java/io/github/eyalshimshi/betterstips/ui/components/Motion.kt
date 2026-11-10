package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import io.github.eyalshimshi.betterstips.data.Motion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The light on an answer or a message that was jumped to (v0.10): two breaths, then back to normal. With the glow off
 * (#18) it stays lit for [holdMs] instead; "quick" makes both shorter.
 */
suspend fun Animatable<Float, AnimationVector1D>.glowOnce(motion: Motion, holdMs: Long) {
    if (!motion.glow) {
        snapTo(1f)
        delay((holdMs * motion.pace).toLong())
        snapTo(0f)
        return
    }
    fun ms(base: Int) = (base * motion.pace).roundToInt()
    snapTo(0f)
    animateTo(1f, tween(ms(350), easing = FastOutSlowInEasing))
    animateTo(0.35f, tween(ms(450), easing = FastOutSlowInEasing))
    animateTo(1f, tween(ms(350), easing = FastOutSlowInEasing))
    animateTo(0f, tween(ms(850), easing = FastOutSlowInEasing))
}

/** A quick scroll (#18): short and decelerating, instead of the default spring. */
internal const val QUICK_SCROLL_MS = 220

/**
 * How a row of a list moves when the list changes (v0.18, #30): it slides to its new place and fades in or out, as Live
 * and coming back to a tab add rows on top. A refresh by hand ([swapped]) replaces the list, and if the rows' order
 * shifted they all slid over each other at once; so that change happens in place. Nothing moves with moving elements
 * off (#18).
 */
fun LazyItemScope.listItemMotion(moving: Boolean, swapped: Boolean): Modifier = when {
    !moving -> Modifier
    swapped -> Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    else -> Modifier.animateItem()
}
