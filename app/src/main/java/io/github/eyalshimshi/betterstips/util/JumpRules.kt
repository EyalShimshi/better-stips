package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.model.UnreadMark
import java.time.Instant

/** Where a question opens when a notification leads to it (v0.10). Pure. */
object JumpRules {
    /** Answers posted this soon after the notification's time still count as the ones it announced. */
    private const val SLACK_SECONDS = 120L

    private val digitsThenAnswers = Regex("""(\d{1,4})\s+(?:\S+\s+)?(?:תשובות|תגובות|answers|replies)""", RegexOption.IGNORE_CASE)
    private val wordsThenAnswers = Regex("""(שתי|שלוש|ארבע|חמש|שש|שבע|שמונה|תשע|עשר)\s+(?:\S+\s+)?(?:תשובות|תגובות)""")
    private val numberWords = mapOf(
        "שתי" to 2, "שלוש" to 3, "ארבע" to 4, "חמש" to 5, "שש" to 6, "שבע" to 7, "שמונה" to 8, "תשע" to 9, "עשר" to 10,
    )
    private val oneAnswer = Regex("""תשובה\s+(?:אחת\s+)?חדשה|תגובה\s+(?:אחת\s+)?חדשה|new answer|one answer""", RegexOption.IGNORE_CASE)

    /**
     * What a notification on a question is about, from Stips' type words ("new_ask_ans_registered") and its text: new
     * answers, a flower on the user's answer, or nothing to jump to.
     */
    fun focusFor(type: String, text: String, time: Instant?): QuestionFocus? {
        val t = type.lowercase()
        return when {
            "flower" in t -> QuestionFocus.MyFlower
            "ans" in t -> QuestionFocus.NewAnswers(newAnswerCount(text), time)
            t.isBlank() && ("פרח" in text || "flower" in text.lowercase()) -> QuestionFocus.MyFlower
            else -> null
        }
    }

    /** As above, for a whole notification: Stips' team's notices (a removal, a report, a note) open at the top (v0.13). */
    fun focusFor(n: StipsNotification): QuestionFocus? =
        if (NotificationKinds.fromTeam(NotificationKinds.kind(n))) null else focusFor(n.type, n.text, n.time)

    /**
     * The "unread" bar for a notification about new answers (v0.13): above the answer it opens at ([targetId]), with
     * how many answers from there on aren't the user's own. Null when there are none.
     */
    fun unread(answers: List<Answer>, targetId: Long): UnreadMark? {
        val index = answers.indexOfFirst { it.id == targetId }
        if (index < 0) return null
        val count = answers.subList(index, answers.size).count { !it.isOwner }
        return if (count > 0) UnreadMark(targetId, count) else null
    }

    /** How many new answers a notification's text announces ("3 תשובות חדשות", "שתי תשובות"), or null. */
    fun newAnswerCount(text: String): Int? {
        digitsThenAnswers.find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
        wordsThenAnswers.find(text)?.groupValues?.get(1)?.let { return numberWords[it] }
        return if (oneAnswer.containsMatchIn(text)) 1 else null
    }

    /**
     * The answer to open at in [answers] (Stips' order, oldest first), or null to open at the top.
     * - A flower: the user's newest answer that has one.
     * - New answers: the first of them. The count, when the notification has one, marks the last ones up to its time;
     *   [lastSeenId], the newest answer Stips+ showed here before, marks what's new since then. The later of the two
     *   wins (some counted ones may have been read in the app already); without either, the answer from the
     *   notification's time. The user's own answers are skipped.
     */
    fun target(answers: List<Answer>, focus: QuestionFocus, lastSeenId: Long?): Long? {
        if (answers.isEmpty()) return null
        return when (focus) {
            QuestionFocus.MyFlower -> answers.lastOrNull { it.isOwner && it.flowered }?.id
            is QuestionFocus.NewAnswers -> firstNew(answers, focus, lastSeenId)
        }
    }

    private fun firstNew(answers: List<Answer>, focus: QuestionFocus.NewAnswers, lastSeenId: Long?): Long {
        val limit = focus.at?.plusSeconds(SLACK_SECONDS)
        val end = if (limit == null) {
            answers.lastIndex
        } else {
            answers.indexOfLast { it.postedAt?.isAfter(limit) != true }.takeIf { it >= 0 } ?: answers.lastIndex
        }
        val byCount = focus.count?.let { (end - it + 1).coerceIn(0, end) }
        val byMark = lastSeenId?.let { mark -> answers.indexOfFirst { it.id > mark }.takeIf { it >= 0 } }
        val start = when {
            byCount != null && byMark != null -> maxOf(byCount, byMark)
            else -> byCount ?: byMark ?: end
        }
        val pick = (start..answers.lastIndex).firstOrNull { !answers[it].isOwner } ?: start
        return answers[pick].id
    }

    /** [focus] as plain values for a route or an intent: a kind ("answers", "flower" or ""), a count, a time (-1: none). */
    fun kind(focus: QuestionFocus?): String = when (focus) {
        null -> ""
        QuestionFocus.MyFlower -> KIND_FLOWER
        is QuestionFocus.NewAnswers -> KIND_ANSWERS
    }

    fun count(focus: QuestionFocus?): Int = (focus as? QuestionFocus.NewAnswers)?.count ?: -1

    fun at(focus: QuestionFocus?): Long = (focus as? QuestionFocus.NewAnswers)?.at?.toEpochMilli() ?: -1L

    fun decode(kind: String?, count: Int, at: Long): QuestionFocus? = when (kind) {
        KIND_FLOWER -> QuestionFocus.MyFlower
        KIND_ANSWERS -> QuestionFocus.NewAnswers(count.takeIf { it > 0 }, at.takeIf { it > 0 }?.let(Instant::ofEpochMilli))
        else -> null
    }

    private const val KIND_ANSWERS = "answers"
    private const val KIND_FLOWER = "flower"
}
