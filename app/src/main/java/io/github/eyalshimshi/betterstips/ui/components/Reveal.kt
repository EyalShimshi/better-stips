package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import io.github.eyalshimshi.betterstips.util.JumpScroll

/**
 * Brings item [index] into sight the way a reply jump should (#12): no scroll when it's already in sight whole;
 * otherwise the least scroll that shows it whole, a tenth of the visible height from the edge it comes in at.
 * [startInset] is the part of the list's start hidden under a bar (the question screen's folded top bar). [quick]
 * scrolls in a short decelerating move instead of the default spring (#18).
 */
suspend fun LazyListState.reveal(index: Int, animate: Boolean, startInset: Int = 0, quick: Boolean = false) {
    val info = layoutInfo
    if (index !in 0 until info.totalItemsCount) return
    val windowStart = info.viewportStartOffset + startInset
    val windowEnd = info.viewportEndOffset
    val visible = info.visibleItemsInfo
    val laidOut = visible.firstOrNull { it.index == index }
    val move = JumpScroll.plan(
        index = index,
        item = laidOut?.let { JumpScroll.Item(it.offset, it.size) },
        before = index < (visible.firstOrNull()?.index ?: 0),
        lastIndex = info.totalItemsCount - 1,
        windowStart = windowStart,
        windowEnd = windowEnd,
        margin = JumpScroll.margin(windowStart, windowEnd),
        readsFromEnd = info.reverseLayout,
        spacing = info.mainAxisItemSpacing,
    )
    when (move) {
        JumpScroll.Move.Stay -> Unit
        is JumpScroll.Move.By -> when {
            !animate -> scrollBy(move.pixels.toFloat())
            quick -> animateScrollBy(move.pixels.toFloat(), tween(QUICK_SCROLL_MS, easing = LinearOutSlowInEasing))
            else -> animateScrollBy(move.pixels.toFloat())
        }
        is JumpScroll.Move.ToItem ->
            if (animate) animateScrollToItem(move.index, move.scrollOffset) else scrollToItem(move.index, move.scrollOffset)
    }
}
