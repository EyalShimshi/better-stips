package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * How the bell's list is organized (#13, the owner's decisions): three categories, sections by day, a line that sums
 * up what's new, and Stips' team's notices pinned on top until they're opened. Pure.
 */
object NotificationCategories {
    /**
     * [Updates]: new answers on questions you asked, answered or follow. [FlowersAndThanks]: a flower on your answer, a
     * message on your thanks wall. [System]: everything else, from Stips' team's notices (a removal, a report's
     * outcome, a note) to kinds Stips+ doesn't know yet.
     */
    enum class Category { Updates, FlowersAndThanks, System }

    fun of(kind: NotificationKinds.Kind): Category = when (kind) {
        NotificationKinds.Kind.Answers, NotificationKinds.Kind.Question -> Category.Updates
        NotificationKinds.Kind.Flower, NotificationKinds.Kind.Thanks -> Category.FlowersAndThanks
        else -> Category.System
    }

    fun of(n: StipsNotification): Category = of(NotificationKinds.kind(n))

    /** The list's sections, newest first. */
    enum class Day { Today, Yesterday, ThisWeek, Earlier }

    /** Which section [time] goes in. A time ahead of [today] (a clock off by a little) counts as today; none, as earlier. */
    fun day(time: Instant?, today: LocalDate, zone: ZoneId): Day {
        val date = time?.atZone(zone)?.toLocalDate() ?: return Day.Earlier
        return when {
            !date.isBefore(today) -> Day.Today
            date == today.minusDays(1) -> Day.Yesterday
            date.isAfter(today.minusDays(7)) -> Day.ThisWeek
            else -> Day.Earlier
        }
    }

    /** [items] in their day's sections, in order; each keeps the list's order inside. Empty sections are left out. */
    fun sections(items: List<StipsNotification>, today: LocalDate, zone: ZoneId): List<Pair<Day, List<StipsNotification>>> =
        items.groupBy { day(it.time, today, zone) }.toList().sortedBy { it.first.ordinal }

    /** What new notifications add up to: answers on how many questions, flowers, thanks and system messages. */
    data class Summary(val answers: Int = 0, val questions: Int = 0, val flowers: Int = 0, val thanks: Int = 0, val system: Int = 0) {
        val isEmpty: Boolean get() = answers == 0 && flowers == 0 && thanks == 0 && system == 0
    }

    /** Sums [items] up. A notification about new answers counts the answers its text announces ("3 תשובות חדשות"), or one. */
    fun summary(items: List<StipsNotification>): Summary {
        var answers = 0
        var flowers = 0
        var thanks = 0
        var system = 0
        val questions = mutableSetOf<Long>()
        for (n in items) {
            when (val kind = NotificationKinds.kind(n)) {
                NotificationKinds.Kind.Answers, NotificationKinds.Kind.Question -> {
                    answers += if (kind == NotificationKinds.Kind.Answers) JumpRules.newAnswerCount(n.text) ?: 1 else 1
                    (n.target as? NotificationTarget.OpenQuestion)?.let { questions += it.id }
                }
                NotificationKinds.Kind.Flower -> flowers++
                NotificationKinds.Kind.Thanks -> thanks++
                else -> system++
            }
        }
        return Summary(answers, questions.size, flowers, thanks, system)
    }

    /** Stips' team's notices stay pinned on top until opened, but only for this long: older ones just sit in their day. */
    const val PIN_DAYS = 30L

    /** The notices from Stips' team to pin on top (#13): not yet [opened], and from the last [PIN_DAYS] days. */
    fun pinned(items: List<StipsNotification>, opened: Set<Long>, now: Instant): List<StipsNotification> = items.filter { n ->
        n.id !in opened &&
            NotificationKinds.fromTeam(NotificationKinds.kind(n)) &&
            (n.time == null || Duration.between(n.time, now).toDays() < PIN_DAYS)
    }

    /** The question a notification is about, when it's about one. */
    fun questionId(n: StipsNotification): Long? = (n.target as? NotificationTarget.OpenQuestion)?.id
}
