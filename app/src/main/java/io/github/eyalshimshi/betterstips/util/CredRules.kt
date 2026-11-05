package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.AccountBan
import java.time.LocalDate

/**
 * Stips' credibility points ("נקודות אמינות", 0–100), by the site's rules (Stips' rule book 3.2.0, p. 9, and its table
 * of how points rise; v0.13). Points go down only when a moderator takes some away for a broken rule; they come back
 * by themselves, faster when they're low. Pure.
 */
object CredRules {
    const val MAX = 100

    /** From here questions and answers can be edited ("מתחת ל-50 אחוזי אמינות נחסמת האפשרות לערוך"). */
    const val EDIT_FROM = 50

    /** Below this the account is blocked from activity until it's back here: 30 − points days, a point a day. */
    const val ACTIVE_FROM = 30

    /** Below this the account is blocked for good, and points no longer rise. */
    const val RISES_FROM = 20

    /** The senior answerer badge ("עונה בכיר") comes by itself at this many flowers. */
    const val SENIOR_ANSWERER_FLOWERS = 500

    /** What Stips asks of someone applying to be a moderator, besides knowing the rules (rule book, p. 14). */
    const val MODERATOR_AGE = 15
    const val MODERATOR_FLOWERS = 3_000
    const val MODERATOR_MONTHS = 18

    /** Stips' ratings, each with how often a point is added: every [minDays] to [maxDays] days, or never. */
    enum class Rating(val from: Int, val minDays: Int?, val maxDays: Int?) {
        Perfect(90, 3, 7),
        VeryReliable(70, 2, 2),
        Reliable(60, 1, 1),
        Okay(40, 1, 1),
        Problematic(30, 1, 1),
        Harmful(20, 1, 1),
        VeryHarmful(0, null, null),
        ;

        /** The rating's top: one below the next rating up, or [MAX]. */
        val to: Int get() = if (ordinal == 0) CredRules.MAX else Rating.entries[ordinal - 1].from - 1

        val rises: Boolean get() = minDays != null
    }

    fun rating(points: Int): Rating {
        val p = points.coerceIn(0, MAX)
        return Rating.entries.first { p >= it.from }
    }

    fun canEdit(points: Int): Boolean = points >= EDIT_FROM

    sealed interface Block {
        data object None : Block

        /** Blocked for about [days] more days: a point a day until [ACTIVE_FROM]. */
        data class Temporary(val days: Int) : Block

        data object Permanent : Block
    }

    /** "ב-30-20 אחוזי אמינות נחסמים לכמה ימים בהתאם למספר הנותר להשלים ל-30; מתחת ל-20 נחסמים לצמיתות." */
    fun block(points: Int): Block = when {
        points >= ACTIVE_FROM -> Block.None
        points >= RISES_FROM -> Block.Temporary(ACTIVE_FROM - points)
        else -> Block.Permanent
    }

    /**
     * What the block screen says (v0.13): Stips' own block when it sent one (its reason and end, in its words), else
     * what the points mean by the rules. [daysLeft] counts from the points (a point a day up to 30). Null: not blocked.
     */
    data class BanState(val permanent: Boolean, val daysLeft: Int?, val reason: String?, val endsText: String?)

    fun banState(points: Int?, ban: AccountBan?): BanState? {
        val byPoints = points?.let(::block) ?: Block.None
        if (ban == null && byPoints == Block.None) return null
        val days = (byPoints as? Block.Temporary)?.days
        val permanent = when {
            ban?.temporary == true -> false
            byPoints == Block.Permanent -> true
            days != null -> false
            // Blocked by Stips with no end date.
            else -> true
        }
        return BanState(permanent, if (permanent) null else days, ban?.message, ban?.endsText)
    }

    /**
     * Days to rise from [from] to [target] if no points are taken away: the fewest and the most (from 90 a point takes
     * 3 to 7 days). 0..0 when [from] is there already; null when it can't rise (below [RISES_FROM]).
     */
    fun daysToReach(from: Int, target: Int): IntRange? {
        val start = from.coerceIn(0, MAX)
        val goal = target.coerceIn(0, MAX)
        var min = 0
        var max = 0
        for (p in start until goal) {
            val r = rating(p)
            min += r.minDays ?: return null
            max += r.maxDays ?: return null
        }
        return min..max
    }

    /** The days [target] would be reached on, counted from [today]; null if it can't be. */
    fun dates(from: Int, target: Int, today: LocalDate): Pair<LocalDate, LocalDate>? =
        daysToReach(from, target)?.let { today.plusDays(it.first.toLong()) to today.plusDays(it.last.toLong()) }

    /** Points above [from] that change something: back to activity, editing, each rating, the top. */
    fun milestones(from: Int): List<Int> =
        (Rating.entries.map { it.from }.filter { it >= RISES_FROM } + listOf(EDIT_FROM, MAX))
            .filter { it > from }
            .distinct()
            .sorted()

    private val days = Regex("""ימים|יום""")

    /**
     * Months on Stips from the profile's phrase ("6 שנים ו-9 חודשים", "שנתיים", "חודש"), for the moderators' year and
     * a half; 0 for a phrase in days. Null when it can't tell.
     */
    fun monthsOnStips(text: String?): Int? {
        val span = TextUtils.parseHebrewSpan(text)
        if (span != null) return span.years * 12 + span.months
        return if (text != null && days.containsMatchIn(text)) 0 else null
    }
}
