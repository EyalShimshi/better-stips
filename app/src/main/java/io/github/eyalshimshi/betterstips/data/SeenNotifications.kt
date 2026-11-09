package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.util.NotificationCategories
import io.github.eyalshimshi.betterstips.util.NotifyRules

/**
 * The newest notification the user has seen in the app, per account (#4). Stips marks notifications as viewed as
 * soon as they're listed (and the phone-notification check lists them too), so the app also remembers what it
 * showed: anything newer counts as new.
 */
class SeenNotifications(context: Context) {
    private val prefs = context.getSharedPreferences("notifications_seen", Context.MODE_PRIVATE)

    fun lastSeenId(userId: Long): Long = prefs.getLong(key(userId), 0L)

    @Synchronized
    fun markSeen(userId: Long, newestId: Long) {
        val edit = prefs.edit()
        if (newestId > lastSeenId(userId)) edit.putLong(key(userId), newestId)
        // What the bell has shown stops waiting, even when nothing newer came in since its last visit.
        val waiting = waiting(userId)
        if (waiting.keys.any { it <= newestId }) edit.putString(waitingKey(userId), NotifyRules.encode(waiting.filterKeys { it > newestId }))
        edit.apply()
    }

    /**
     * Notifications found new in a list read outside the bell (its badge's check, the phone check) that the bell
     * hasn't shown yet, with the row each goes in (v0.17). Reading the list makes Stips count them as seen, and other
     * apps on the account read it too, so Stips' own count can't say they're there; this does.
     */
    @Synchronized
    fun waiting(userId: Long): Map<Long, String> = NotifyRules.decode(prefs.getString(waitingKey(userId), null))

    /** Takes note of what's new in [list], the bell's first page read by a check, and of the newest one it read. */
    @Synchronized
    fun learn(userId: Long, list: List<StipsNotification>) {
        if (list.isEmpty()) return
        val learned = prefs.getLong(learnedKey(userId), 0L)
        val found = fresh(list.map { it.id to it.viewed }, lastSeenId(userId), learned)
        val edit = prefs.edit()
        val newest = list.maxOf { it.id }
        if (newest > learned) edit.putLong(learnedKey(userId), newest)
        if (found.isNotEmpty()) {
            val waiting = waiting(userId) + list.filter { it.id in found }.associate { it.id to NotificationCategories.rowKey(it) }
            val kept = waiting.entries.sortedByDescending { it.key }.take(MAX_WAITING).associate { it.key to it.value }
            edit.putString(waitingKey(userId), NotifyRules.encode(kept))
        }
        edit.apply()
    }

    /** How many rows the bell will show as new because of [waiting] ones: its badge (v0.17). */
    fun waitingRows(userId: Long): Int = rows(waiting(userId), lastSeenId(userId))

    /** Notifications opened or marked read in the app (#13): Stips' team's notices stay pinned on top until then. */
    fun opened(userId: Long): Set<Long> =
        prefs.getStringSet(openedKey(userId), null).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun markOpened(userId: Long, id: Long) {
        val kept = (opened(userId) + id).sortedDescending().take(MAX_OPENED)
        prefs.edit().putStringSet(openedKey(userId), kept.map { it.toString() }.toSet()).apply()
    }

    private fun openedKey(userId: Long) = "o$userId"

    private fun waitingKey(userId: Long) = "w$userId"

    private fun learnedKey(userId: Long) = "l$userId"

    /** Signing out forgets what was seen (#14). */
    fun clear() = prefs.edit().clear().apply()

    private fun key(userId: Long) = "u$userId"

    companion object {
        /** The most opened marks kept per account; the newest stay. */
        const val MAX_OPENED = 300

        /** The most waiting notifications kept per account; the newest stay. */
        const val MAX_WAITING = 200

        /** Which notifications to show as new. Pure: [lastSeen] 0 means "never opened here", so only Stips' flag counts. */
        fun newIds(ids: List<Pair<Long, Boolean>>, lastSeen: Long): Set<Long> =
            ids.filter { (id, viewed) -> !viewed || (lastSeen > 0 && id > lastSeen) }.map { it.first }.toSet()

        /**
         * What a check found new for the bell (v0.17), from a first page's ids and Stips' "viewed" flags. Pure. Newer
         * than both what the bell showed ([lastSeen]) and what checks read before ([learned]), whether other apps saw
         * it or not; before either, what Stips still has unread. Never one the bell already showed, though a check
         * whose answer crossed the bell's own may bring it back unread.
         */
        fun fresh(ids: List<Pair<Long, Boolean>>, lastSeen: Long, learned: Long): Set<Long> {
            val frontier = maxOf(lastSeen, learned)
            return ids.filter { (id, viewed) -> if (frontier > 0) id > frontier else !viewed }.map { it.first }.toSet()
        }

        /** The rows of the [waiting] notifications (id → row) the bell hasn't shown: each question's updates are one. Pure. */
        fun rows(waiting: Map<Long, String>, lastSeen: Long): Int = waiting.filterKeys { it > lastSeen }.values.toSet().size
    }
}
