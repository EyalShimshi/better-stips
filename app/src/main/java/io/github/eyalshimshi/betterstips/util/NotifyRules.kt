package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.StipsNotification

/**
 * Which Stips notifications and chats become phone notifications (v0.11). Stips has no push service for other apps,
 * so Stips+ looks every few minutes and compares with what it saw before. Pure.
 *
 * Each item is remembered with a signature that changes when Stips changes it, so a notification Stips updates in
 * place (the same id with "3 new answers") counts as new again, and so does a chat with a newer message. v0.10 kept
 * only the newest id and the total of unread messages, and missed both.
 */
object NotifyRules {
    /** At most this many phone notifications from one check (the newest); the rest wait in the app. */
    const val MAX_POSTS = 5
    const val MAX_CHATS = 4

    /** How many remembered notifications are kept (the newest ids). */
    const val MAX_KNOWN = 200

    /** An unchanged count of unread messages is listed again this often: one chat read and another new cancel out. */
    const val CHATS_RECHECK_MS = 30 * 60_000L

    /** At this many minutes or more, only the background job checks (Android's minimum is 15). */
    const val JOB_MINUTES = 15

    data class NotificationPick(val post: List<StipsNotification>, val known: Map<Long, String>, val read: List<Long>)

    data class ChatPick(val post: List<Conversation>, val known: Map<Long, String>, val read: List<Long>)

    /** What a check found, for Settings: when, and how it went. */
    data class Status(val at: Long, val outcome: Outcome, val posted: Int = 0, val error: String? = null)

    enum class Outcome { Posted, Nothing, Failed }

    /** Changes when Stips changes the notification: its time or its words. */
    fun signature(n: StipsNotification): String = "${n.time?.toEpochMilli() ?: 0}:${n.text.hashCode()}"

    /** Changes with the chat's newest message. */
    fun chatSignature(c: Conversation): String = "${c.time?.toEpochMilli() ?: 0}:${c.lastMessage.hashCode()}"

    /**
     * The notifications of [list] (Stips' first page) to post, given what earlier checks [known] (id → signature).
     * - The [first] check after turning this on records what's there without posting it, so old ones don't come back.
     * - [legacyNewest]: v0.10 kept only the newest id it had seen; unread ones up to it are recorded, not posted.
     * - New is decided by Stips+ (v0.17), not by Stips' "viewed" flag alone: other apps on the account (the official
     *   one, the site) set it as they list the bell, often before a check runs. So a notification is new when Stips
     *   still has it unread, when it's newer than everything earlier checks saw, or when Stips changed one they saw.
     * - [seenInApp]: the newest notification the bell showed in Stips+. Those up to it, unchanged since, are done:
     *   they come back in [NotificationPick.read], so their phone notifications can go.
     */
    fun pick(
        list: List<StipsNotification>,
        known: Map<Long, String>,
        first: Boolean,
        legacyNewest: Long? = null,
        seenInApp: Long = 0L,
    ): NotificationPick {
        val newestKnown = known.keys.maxOrNull() ?: 0L
        val next = known.toMutableMap()
        val post = ArrayList<StipsNotification>()
        val read = ArrayList<Long>()
        for (n in list.sortedBy { it.id }) {
            if (n.viewed && n.id <= seenInApp) {
                if (next.remove(n.id) != null) read += n.id
                continue
            }
            val signature = signature(n)
            val before = known[n.id]
            if (before == signature) continue
            next[n.id] = signature
            val fresh = !n.viewed || n.id > newestKnown || before != null
            val seenByOldVersion = legacyNewest != null && n.id <= legacyNewest && before == null
            if (fresh && !first && !seenByOldVersion) post += n
        }
        return NotificationPick(post.takeLast(MAX_POSTS), keepNewest(next), read)
    }

    /**
     * The chats of [list] (the first page) to post: unread, the last message from the other person, and newer than
     * what [known] has (user id → signature). The [first] check only records. Chats read since come back in
     * [ChatPick.read] and are forgotten.
     */
    fun pickChats(list: List<Conversation>, known: Map<Long, String>, first: Boolean): ChatPick {
        val next = known.toMutableMap()
        val post = ArrayList<Conversation>()
        val read = ArrayList<Long>()
        for (c in list) {
            val id = c.other.userId ?: continue
            if (c.unread <= 0 || c.lastFromMe) {
                if (next.remove(id) != null) read += id
                continue
            }
            val signature = chatSignature(c)
            if (known[id] == signature) continue
            next[id] = signature
            if (!first) post += c
        }
        val listed = list.mapNotNullTo(HashSet()) { it.other.userId }
        val kept = if (next.size <= MAX_KNOWN) next else next.filterKeys { it in listed }
        return ChatPick(post.sortedByDescending { it.time?.toEpochMilli() ?: 0L }.take(MAX_CHATS), kept, read)
    }

    /**
     * Whether to ask for the chats list: there are unread messages, and it's the first check, their count changed,
     * or the same count has stood for [CHATS_RECHECK_MS].
     */
    fun listChats(unread: Int, lastUnread: Int?, listedAt: Long, now: Long): Boolean = when {
        unread <= 0 -> false
        lastUnread == null || unread != lastUnread -> true
        else -> now - listedAt >= CHATS_RECHECK_MS
    }

    /** The wait between checks while the phone is awake, or null when only the background job checks. */
    fun alarmMs(minutes: Int): Long? = if (minutes >= JOB_MINUTES) null else minutes.coerceAtLeast(1) * 60_000L

    /** "id=signature;…", for SharedPreferences. */
    fun encode(map: Map<Long, String>): String = map.entries.joinToString(";") { "${it.key}=${it.value}" }

    fun decode(text: String?): Map<Long, String> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split(';').mapNotNull { part ->
            val at = part.indexOf('=')
            if (at <= 0) return@mapNotNull null
            val id = part.substring(0, at).toLongOrNull() ?: return@mapNotNull null
            id to part.substring(at + 1)
        }.toMap()
    }

    fun encodeStatus(status: Status): String =
        listOf(status.at.toString(), status.outcome.name, status.posted.toString(), status.error.orEmpty().replace('|', '/')).joinToString("|")

    fun decodeStatus(text: String?): Status? {
        val parts = text?.split('|') ?: return null
        if (parts.size != 4) return null
        val at = parts[0].toLongOrNull() ?: return null
        val outcome = Outcome.entries.firstOrNull { it.name == parts[1] } ?: return null
        return Status(at, outcome, parts[2].toIntOrNull() ?: 0, parts[3].ifEmpty { null })
    }

    private fun keepNewest(map: Map<Long, String>): Map<Long, String> =
        if (map.size <= MAX_KNOWN) map else map.entries.sortedByDescending { it.key }.take(MAX_KNOWN).associate { it.key to it.value }
}
