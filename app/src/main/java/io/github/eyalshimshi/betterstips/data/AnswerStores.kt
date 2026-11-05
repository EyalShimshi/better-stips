package io.github.eyalshimshi.betterstips.data

import android.content.Context
import android.content.SharedPreferences
import io.github.eyalshimshi.betterstips.data.model.ModeratorRank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/** The newest answer Stips+ showed on each question, so a notification can open at what's new since (v0.10). */
class ReadMarks(context: Context) {
    private val prefs = context.getSharedPreferences("read_marks", Context.MODE_PRIVATE)

    fun newestSeen(questionId: Long): Long? = prefs.getLong(questionId.toString(), 0L).takeIf { it > 0 }

    fun markSeen(questionId: Long, newestAnswerId: Long) {
        if (newestAnswerId <= (newestSeen(questionId) ?: 0L)) return
        prefs.edit().putLong(questionId.toString(), newestAnswerId).apply()
        keepNewest(prefs, MAX)
    }

    private companion object {
        const val MAX = 500
    }
}

/**
 * The newest message Stips+ showed in each chat (v0.13), so the next visit can mark what came since as unread when
 * Stips' own count isn't at hand. Keyed by the other person's id; the chats with the oldest marks go first past [MAX].
 */
class ChatReadMarks(context: Context) {
    private val prefs = context.getSharedPreferences("chat_read_marks", Context.MODE_PRIVATE)

    fun newestSeen(userId: Long): Long? = prefs.getLong(userId.toString(), 0L).takeIf { it > 0 }

    fun markSeen(userId: Long, newestMessageId: Long) {
        if (newestMessageId <= (newestSeen(userId) ?: 0L)) return
        prefs.edit().putLong(userId.toString(), newestMessageId).apply()
        val all = prefs.all
        if (all.size <= MAX) return
        val drop = all.entries.sortedBy { (it.value as? Long) ?: 0L }.take(all.size - MAX).map { it.key }
        prefs.edit().apply { drop.forEach { remove(it) } }.apply()
    }

    private companion object {
        const val MAX = 300
    }
}

/** Which answer each of the user's easy replies points at, so its carets can be corrected after posting (v0.10). */
class ReplyTargets(context: Context) {
    private val prefs = context.getSharedPreferences("reply_targets", Context.MODE_PRIVATE)

    fun target(answerId: Long): Long? = prefs.getLong(answerId.toString(), 0L).takeIf { it > 0 }

    fun remember(answerId: Long, targetId: Long) {
        prefs.edit().putLong(answerId.toString(), targetId).apply()
        keepNewest(prefs, MAX)
    }

    private companion object {
        const val MAX = 300
    }
}

/** Keeps the [max] entries with the highest ids (Stips' ids grow with time), so these lists stay small. */
private fun keepNewest(prefs: SharedPreferences, max: Int) {
    val keys = prefs.all.keys
    if (keys.size <= max) return
    val drop = keys.sortedBy { it.toLongOrNull() ?: 0L }.take(keys.size - max)
    prefs.edit().apply { drop.forEach { remove(it) } }.apply()
}

/**
 * The badges of people who answer (`profile.page_data`), kept for two weeks, so answers can show who's a moderator
 * without opening profiles (v0.10). A moderator's entry also keeps the gender, for "נאמן" or "נאמנת".
 */
class UserBadges(context: Context) {
    data class Entry(val badges: Set<String>, val female: Boolean?, val checkedAt: Long) {
        val moderator: ModeratorRank? get() = ModeratorRank.of(badges)
    }

    private val prefs = context.getSharedPreferences("user_badges", Context.MODE_PRIVATE)
    private val entries = ConcurrentHashMap<Long, Entry>()

    @Volatile private var loaded = false

    private val _version = MutableStateFlow(0)

    /** Changes when someone's moderator mark does, so screens draw it. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun entry(userId: Long): Entry? {
        load()
        return entries[userId]
    }

    /** Whether [userId] hasn't been checked in the last two weeks. */
    fun needsLookup(userId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val entry = entry(userId) ?: return true
        return now - entry.checkedAt >= TTL_MS
    }

    fun record(userId: Long, badges: Collection<String>, female: Boolean? = null, now: Long = System.currentTimeMillis()) {
        load()
        val before = entries[userId]
        val entry = Entry(badges.toSet(), female ?: before?.female, now)
        entries[userId] = entry
        prefs.edit().putString(userId.toString(), encode(entry)).apply()
        if (entries.size > MAX) trim()
        if (before?.moderator != entry.moderator || before?.female != entry.female) _version.update { it + 1 }
    }

    /** A profile was opened: its gender, for a moderator's mark. */
    fun recordGender(userId: Long, female: Boolean) {
        load()
        val before = entries[userId] ?: return
        if (before.female == female) return
        val entry = before.copy(female = female)
        entries[userId] = entry
        prefs.edit().putString(userId.toString(), encode(entry)).apply()
        _version.update { it + 1 }
    }

    private fun load() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            prefs.all.forEach { (key, value) ->
                val id = key.toLongOrNull() ?: return@forEach
                val entry = (value as? String)?.let(::decode) ?: return@forEach
                entries.putIfAbsent(id, entry)
            }
            loaded = true
        }
    }

    private fun trim() {
        val drop = entries.entries.sortedBy { it.value.checkedAt }.take(entries.size - MAX).map { it.key }
        drop.forEach { entries.remove(it) }
        prefs.edit().apply { drop.forEach { remove(it.toString()) } }.apply()
    }

    companion object {
        const val TTL_MS = 14L * 24 * 60 * 60 * 1000
        const val MAX = 3_000

        /** "moderator,old_expert|f|1730000000000" (gender "f", "m" or empty). Pure. */
        fun encode(entry: Entry): String {
            val gender = when (entry.female) {
                true -> "f"
                false -> "m"
                null -> ""
            }
            return entry.badges.joinToString(",") + "|" + gender + "|" + entry.checkedAt
        }

        /** The entry [encode] wrote, or null. Pure. */
        fun decode(text: String): Entry? {
            val parts = text.split('|')
            if (parts.size != 3) return null
            val at = parts[2].toLongOrNull() ?: return null
            val female = when (parts[1]) {
                "f" -> true
                "m" -> false
                else -> null
            }
            return Entry(parts[0].split(',').filter { it.isNotBlank() }.toSet(), female, at)
        }
    }
}
