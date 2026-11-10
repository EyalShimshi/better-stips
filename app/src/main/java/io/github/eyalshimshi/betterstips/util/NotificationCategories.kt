package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * How the bell's list is organized (#13, the owner's decisions): three categories, sections by day, a line that sums
 * up what's new, Stips' team's notices pinned on top until they're opened, and one row per question for its updates
 * (v0.17). Pure.
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
        sectionsBy(items, today, zone) { it.time }

    /** As [sections], for anything with a [time]: the list's rows go in the section of their newest notification. */
    fun <T> sectionsBy(items: List<T>, today: LocalDate, zone: ZoneId, time: (T) -> Instant?): List<Pair<Day, List<T>>> =
        items.groupBy { day(time(it), today, zone) }.toList().sortedBy { it.first.ordinal }

    /**
     * A row of the bell's list (v0.17): one notification, or all the updates about one question, newest first. Stips
     * sends a notification per burst of answers ("2 new answers", then "3 new answers"), and starts a new one once the
     * last was seen in any app, so a busy question filled the list; now it takes one row.
     */
    data class Row(val items: List<StipsNotification>) {
        val newest: StipsNotification get() = items.first()

        /** The question this row's updates are about, or null for a row that isn't an update. */
        val questionId: Long? get() = groupKey(newest)

        val key: String get() = rowKey(newest)
    }

    /** The question an update is about, which its row is grouped by; null for anything else. */
    fun groupKey(n: StipsNotification): Long? = questionId(n)?.takeIf { of(n) == Category.Updates }

    /** Which row [n] goes in: its question's for an update, its own otherwise. Also the list's key for that row. */
    fun rowKey(n: StipsNotification): String = groupKey(n)?.let { "q$it" } ?: "n${n.id}"

    /** [items] (newest first) as the list's rows: each question's updates together, where the newest of them is. */
    fun rows(items: List<StipsNotification>): List<Row> {
        val rows = LinkedHashMap<String, MutableList<StipsNotification>>()
        for (n in items) rows.getOrPut(rowKey(n)) { ArrayList() } += n
        return rows.values.map { Row(it) }
    }

    /** How many answers an update announces ("3 תשובות חדשות"): its number, or one. */
    fun answerCount(n: StipsNotification): Int = JumpRules.newAnswerCount(n.text) ?: 1

    /** How many answers a question's [updates] announce together; a question update without answers counts none. */
    fun answers(updates: List<StipsNotification>): Int =
        updates.filter { NotificationKinds.kind(it) == NotificationKinds.Kind.Answers }.sumOf { answerCount(it) }

    /**
     * Where a question opens from several of its updates (v0.17), newest first: at the first of all the answers they
     * announce, as of the newest one's time. A single one opens as it always has.
     */
    fun focusFor(updates: List<StipsNotification>): QuestionFocus? {
        val newest = updates.firstOrNull() ?: return null
        val focus = JumpRules.focusFor(newest)
        if (updates.size == 1 || focus !is QuestionFocus.NewAnswers) return focus
        return QuestionFocus.NewAnswers(answers(updates).takeIf { it > 0 }, newest.time)
    }

    // Stips' count of new answers in its sentence: "התקבלו 3 תשובות חדשות", "התקבלו שתי תשובות חדשות", "התקבלה תשובה
    // חדשה", "תשובה חדשה". The verb is optional; the first such phrase is the count (a title quoted later may repeat it).
    private val countPhrase = Regex(
        """(התקבלו\s+|התקבלה\s+)?(?:(?:\d{1,4}|שתי|שלוש|ארבע|חמש|שש|שבע|שמונה|תשע|עשר)\s+)?""" +
            """(?:תשובות\s+חדשות|תשובה\s+(?:אחת\s+)?חדשה)""",
    )

    /**
     * What a question's row says for several of its updates (v0.18, #27, the owner's choice): one sentence of Stips',
     * the newest about answers, with all their answers in it ("התקבלו 6 תשובות חדשות לשאלה: …"), as if Stips had sent
     * them together. Its own sentence when it's one, or when the count can't be found in the words.
     */
    fun combinedText(updates: List<StipsNotification>): String {
        val newest = updates.firstOrNull() ?: return ""
        if (updates.size == 1) return newest.text
        val base = updates.firstOrNull { NotificationKinds.kind(it) == NotificationKinds.Kind.Answers } ?: return newest.text
        val total = answers(updates)
        if (total <= 0) return base.text
        val match = countPhrase.find(base.text) ?: return base.text
        val verb = match.groups[1] != null
        val phrase = (if (verb) "התקבלו " else "") + "$total תשובות חדשות"
        return base.text.replaceRange(match.range, phrase)
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
