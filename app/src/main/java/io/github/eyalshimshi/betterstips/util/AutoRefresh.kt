package io.github.eyalshimshi.betterstips.util

/**
 * Entering Home, Pen pals or Chats from the bottom bar, or coming back to the app on one of them, brings it up to date
 * (v0.16), unless it was just loaded. Pure.
 */
object AutoRefresh {
    /** Within this of the last load, entering again doesn't ask Stips. */
    const val FRESH_MS = 10_000L

    /** Whether a list last loaded at [lastLoadedAt] (0: never) should be refreshed at [now]; a clock gone back counts as old. */
    fun due(lastLoadedAt: Long, now: Long): Boolean {
        if (lastLoadedAt <= 0L) return true
        val age = now - lastLoadedAt
        return age < 0L || age >= FRESH_MS
    }
}
