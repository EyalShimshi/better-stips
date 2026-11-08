package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.Motion
import io.github.eyalshimshi.betterstips.data.MotionPreset
import io.github.eyalshimshi.betterstips.data.QuestionSwipe
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.util.NotificationCategories.Category
import io.github.eyalshimshi.betterstips.util.NotificationCategories.Day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure rules added in v0.15 (#13, #18): the notification list's categories, days, summary and pinned team notices;
 * the animation presets; and which swipe actions apply where.
 */
class RulesV15Test {

    // --- Animations (#18) ---

    @Test
    fun motion_presetsAreTheOwners() {
        val full = Motion.of(MotionPreset.Full, Motion.OFF)
        assertTrue(full.screens && full.jumps && full.scrolls && full.glow && full.elements)
        assertFalse(full.quick)
        assertEquals(1f, full.pace)

        // "Less": no screen transitions; jumps, scrolls, the glow and moving elements stay, faster.
        val less = Motion.of(MotionPreset.Less, Motion.FULL)
        assertFalse(less.screens)
        assertTrue(less.jumps && less.scrolls && less.glow && less.elements)
        assertTrue(less.quick)
        assertTrue(less.pace < 1f)

        val off = Motion.of(MotionPreset.Off, Motion.FULL)
        assertFalse(off.screens || off.jumps || off.scrolls || off.glow || off.elements)

        val custom = Motion(screens = true, jumps = false, scrolls = true, glow = false, elements = true, quick = true)
        assertEquals("Customized plays the user's own", custom, Motion.of(MotionPreset.Custom, custom))
    }

    @Test
    fun motion_savedAsBitsAndFromTheOldSwitch() {
        for (bits in 0 until 64) assertEquals(bits, Motion.fromBits(bits).toBits())
        assertEquals(Motion.LESS, Motion.fromBits(Motion.LESS.toBits()))
        assertEquals(Motion.OFF, Motion.fromBits(0))
        assertEquals(MotionPreset.Less, Motion.presetFrom("Less", legacyAnimations = false))
        assertEquals("v0.14 with animations on", MotionPreset.Full, Motion.presetFrom(null, legacyAnimations = true))
        assertEquals("v0.14 with animations off", MotionPreset.Off, Motion.presetFrom(null, legacyAnimations = false))
        assertEquals(MotionPreset.Full, Motion.presetFrom("Bouncy", legacyAnimations = true))
    }

    // --- Swipes (#18) ---

    @Test
    fun swipes_questionActionsApplyWhereTheyMakeSense() {
        fun available(action: QuestionSwipe, owner: Boolean = false, author: Long? = 7, anonymous: Boolean = false, me: Long? = 1) =
            SwipeRules.questionAvailable(action, owner, author, anonymous, me)
        assertFalse(available(QuestionSwipe.None))
        assertTrue(available(QuestionSwipe.Pin))
        assertTrue(available(QuestionSwipe.Export, owner = true))
        assertTrue(available(QuestionSwipe.Report))
        assertFalse("your own question isn't reported", available(QuestionSwipe.Report, owner = true))
        assertFalse(available(QuestionSwipe.Hide, owner = true))
        assertTrue(available(QuestionSwipe.HideUser))
        assertFalse("an anonymous asker can't be hidden", available(QuestionSwipe.HideUser, anonymous = true, author = null))
        assertFalse("nor yourself", available(QuestionSwipe.HideUser, author = 1))
        assertEquals(QuestionSwipe.entries, SwipeRules.QUESTION_CHOICES)
    }

    @Test
    fun swipes_answerChoicesFollowTheirMenus() {
        assertEquals(AnswerSwipe.None, SwipeRules.OTHERS_ANSWER_CHOICES.first())
        assertEquals(AnswerSwipe.None, SwipeRules.MY_ANSWER_CHOICES.first())
        assertTrue(AnswerSwipe.Edit in SwipeRules.MY_ANSWER_CHOICES)
        assertFalse("you don't report your own answer", AnswerSwipe.Report in SwipeRules.MY_ANSWER_CHOICES)
        assertFalse("nor edit someone else's", AnswerSwipe.Edit in SwipeRules.OTHERS_ANSWER_CHOICES)
        // Every choice is one of the two lists, and the defaults are among them.
        assertEquals(AnswerSwipe.entries.toSet(), (SwipeRules.OTHERS_ANSWER_CHOICES + SwipeRules.MY_ANSWER_CHOICES).toSet())
    }

    // --- The notification list (#13) ---

    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-11-08T12:00:00Z")
    private val today = LocalDate.of(2026, 11, 8)

    private fun notification(
        id: Long,
        type: String,
        text: String = "text",
        daysAgo: Long = 0,
        question: Long? = 100,
        note: String? = null,
        points: Int = 0,
    ) = StipsNotification(
        id = id,
        text = text,
        type = type,
        viewed = true,
        time = now.minus(Duration.ofDays(daysAgo)),
        target = question?.let { NotificationTarget.OpenQuestion(it) },
        note = note,
        pointsRemoved = points,
    )

    @Test
    fun categories_updatesFlowersAndTheRest() {
        assertEquals(Category.Updates, NotificationCategories.of(notification(1, "new_ask_ans_registered")))
        assertEquals(Category.FlowersAndThanks, NotificationCategories.of(notification(2, "new_flower")))
        assertEquals(Category.FlowersAndThanks, NotificationCategories.of(notification(3, "thanks", question = null)))
        assertEquals("a removal", Category.System, NotificationCategories.of(notification(4, "x", points = 5)))
        assertEquals("a note from the team", Category.System, NotificationCategories.of(notification(5, "ask_note", note = "Please add details")))
        assertEquals("a kind Stips+ doesn't know", Category.System, NotificationCategories.of(notification(6, "something_new")))
    }

    @Test
    fun days_sectionsInOrder() {
        assertEquals(Day.Today, NotificationCategories.day(now, today, zone))
        assertEquals("a clock a little ahead", Day.Today, NotificationCategories.day(now.plusSeconds(3_600), today, zone))
        assertEquals(Day.Yesterday, NotificationCategories.day(now.minus(Duration.ofDays(1)), today, zone))
        assertEquals(Day.ThisWeek, NotificationCategories.day(now.minus(Duration.ofDays(2)), today, zone))
        assertEquals(Day.ThisWeek, NotificationCategories.day(now.minus(Duration.ofDays(6)), today, zone))
        assertEquals(Day.Earlier, NotificationCategories.day(now.minus(Duration.ofDays(7)), today, zone))
        assertEquals(Day.Earlier, NotificationCategories.day(null, today, zone))

        val items = listOf(
            notification(9, "new_flower", daysAgo = 0),
            notification(8, "new_ask_ans_registered", daysAgo = 1),
            notification(7, "thanks", daysAgo = 20),
            notification(6, "new_flower", daysAgo = 0),
        )
        val sections = NotificationCategories.sections(items, today, zone)
        assertEquals(listOf(Day.Today, Day.Yesterday, Day.Earlier), sections.map { it.first })
        assertEquals("the list's order inside a day", listOf(9L, 6L), sections[0].second.map { it.id })
    }

    @Test
    fun summary_countsAnswersOnQuestions() {
        val summary = NotificationCategories.summary(
            listOf(
                notification(1, "new_ask_ans_registered", "התקבלו 3 תשובות חדשות לשאלה שלך", question = 100),
                notification(2, "new_ask_ans_registered", "התקבלו שתי תשובות חדשות", question = 100),
                notification(3, "new_ask_ans_registered", "something without a number", question = 200),
                notification(4, "new_flower"),
                notification(5, "new_flower"),
                notification(6, "thanks", question = null),
                notification(7, "x", points = 3),
            ),
        )
        assertEquals(NotificationCategories.Summary(answers = 6, questions = 2, flowers = 2, thanks = 1, system = 1), summary)
        assertFalse(summary.isEmpty)
        assertTrue(NotificationCategories.summary(emptyList()).isEmpty)
    }

    @Test
    fun pinned_teamNoticesUntilOpened() {
        val removal = notification(10, "x", "השאלה שלך נמחקה", points = 5)
        val note = notification(11, "ask_note", note = "Please be kind")
        val old = notification(12, "x", "התשובה שלך הוסרה", daysAgo = NotificationCategories.PIN_DAYS + 1, points = 2)
        val answers = notification(13, "new_ask_ans_registered")
        val items = listOf(removal, note, old, answers)
        assertEquals(listOf(10L, 11L), NotificationCategories.pinned(items, opened = emptySet(), now = now).map { it.id })
        assertEquals("opened ones are no longer pinned", listOf(11L), NotificationCategories.pinned(items, opened = setOf(10L), now = now).map { it.id })
        assertEquals(100L, NotificationCategories.questionId(answers))
        assertEquals(null, NotificationCategories.questionId(notification(14, "thanks", question = null)))
    }
}
