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

    private fun key(userId: Long) = "u$userId"

    companion object {
        /** Which notifications to show as new. Pure: [lastSeen] 0 means "never opened here", so only Stips' flag counts. */
        fun newIds(ids: List<Pair<Long, Boolean>>, lastSeen: Long): Set<Long> =
            ids.filter { (id, viewed) -> !viewed || (lastSeen > 0 && id > lastSeen) }.map { it.first }.toSet()
    }
}
