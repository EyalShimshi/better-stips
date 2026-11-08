package io.github.eyalshimshi.betterstips.data

import android.content.Context

/**
 * The newest notification the user has seen in the app, per account (#4). Stips marks notifications as viewed as
 * soon as they're listed (and the phone-notification check lists them too), so the app also remembers what it
 * showed: anything newer counts as new.
 */
class SeenNotifications(context: Context) {
    private val prefs = context.getSharedPreferences("notifications_seen", Context.MODE_PRIVATE)

    fun lastSeenId(userId: Long): Long = prefs.getLong(key(userId), 0L)

    fun markSeen(userId: Long, newestId: Long) {
        if (newestId > lastSeenId(userId)) prefs.edit().putLong(key(userId), newestId).apply()
    }

    /** Notifications opened or marked read in the app (#13): Stips' team's notices stay pinned on top until then. */
    fun opened(userId: Long): Set<Long> =
        prefs.getStringSet(openedKey(userId), null).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun markOpened(userId: Long, id: Long) {
        val kept = (opened(userId) + id).sortedDescending().take(MAX_OPENED)
        prefs.edit().putStringSet(openedKey(userId), kept.map { it.toString() }.toSet()).apply()
    }

    private fun openedKey(userId: Long) = "o$userId"

    /** Signing out forgets what was seen (#14). */
    fun clear() = prefs.edit().clear().apply()

    private fun key(userId: Long) = "u$userId"

    companion object {
        /** The most opened marks kept per account; the newest stay. */
        const val MAX_OPENED = 300

        /** Which notifications to show as new. Pure: [lastSeen] 0 means "never opened here", so only Stips' flag counts. */
        fun newIds(ids: List<Pair<Long, Boolean>>, lastSeen: Long): Set<Long> =
            ids.filter { (id, viewed) -> !viewed || (lastSeen > 0 && id > lastSeen) }.map { it.first }.toSet()
    }
}
