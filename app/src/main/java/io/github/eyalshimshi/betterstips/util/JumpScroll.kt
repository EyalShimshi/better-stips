package io.github.eyalshimshi.betterstips.util

/**
 * How a list moves to show an answer or message that a reply points at (#12): not at all when the whole item is
 * already in sight (it only lights up); otherwise the least it can, so the item comes in whole, [margin] pixels from
 * the edge it comes in at. Works in the list's own coordinates: offsets from its start edge (the top, or the bottom
 * in a reversed list such as a chat), with the window being the part the reader sees (bars over it taken out). Pure.
 */
object JumpScroll {
    sealed interface Move {
        /** Already fully in sight: only light it up. */
        data object Stay : Move

        /** Scroll by this many pixels; positive scrolls toward the list's end. */
        data class By(val pixels: Int) : Move

        /** Scroll so item [index] starts at -[scrollOffset] from the list's start, as Compose's `scrollToItem` does. */
        data class ToItem(val index: Int, val scrollOffset: Int) : Move
    }

    /** Where a laid-out item is: its start edge's offset and its size, along the list. */
    data class Item(val offset: Int, val size: Int)

    /**
     * @param item the target's place when it's laid out (in sight, whole or in part), else null.
     * @param before for a target that isn't laid out: whether it comes before the laid-out items, toward the start.
     * @param readsFromEnd a reversed list: there an item's top, where reading starts, is its far edge.
     * @param spacing the gap the list leaves between items.
     */
    fun plan(
        index: Int,
        item: Item?,
        before: Boolean,
        lastIndex: Int,
        windowStart: Int,
        windowEnd: Int,
        margin: Int,
        readsFromEnd: Boolean,
        spacing: Int = 0,
    ): Move {
        if (item == null) {
            return when {
                // It comes in at the start edge: its start goes a margin inside it.
                before -> Move.ToItem(index, -(windowStart + margin))
                // It comes in at the far edge: the next item starts right after it, a margin inside that edge.
                index < lastIndex -> Move.ToItem(index + 1, -(windowEnd - margin + spacing))
                // The last item: the list can't go past its end anyway.
                else -> Move.ToItem(index, -(windowStart + margin))
            }
        }
        val start = item.offset
        val end = item.offset + item.size
        if (start >= windowStart && end <= windowEnd) return Move.Stay
        // Taller than the window allows: show where it begins, its top.
        if (item.size > windowEnd - windowStart - margin) {
            return if (readsFromEnd) Move.By(end - (windowEnd - margin)) else Move.By(start - (windowStart + margin))
        }
        return if (end > windowEnd) Move.By(end - (windowEnd - margin)) else Move.By(start - (windowStart + margin))
    }

    /** A tenth of the window: the room the owner asked for between a jumped-to item and the screen's edge. */
    fun margin(windowStart: Int, windowEnd: Int): Int = ((windowEnd - windowStart).coerceAtLeast(0) / 10)
}
