package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.AccountBan
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.model.UnreadMark
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.ui.chats.ChatRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Pure rules added in v0.13: Stips' credibility points (its rule book and rate table), the team's notifications, and
 * the "unread" bars in questions and chats.
 */
class RulesV13Test {

    @Test
    fun credRules_ratingsFollowStipsTable() {
        assertEquals(CredRules.Rating.Perfect, CredRules.rating(100))
        assertEquals(CredRules.Rating.Perfect, CredRules.rating(90))
        assertEquals(CredRules.Rating.VeryReliable, CredRules.rating(89))
        assertEquals(CredRules.Rating.VeryReliable, CredRules.rating(70))
        assertEquals(CredRules.Rating.Reliable, CredRules.rating(69))
        assertEquals(CredRules.Rating.Reliable, CredRules.rating(60))
        assertEquals(CredRules.Rating.Okay, CredRules.rating(59))
        assertEquals(CredRules.Rating.Okay, CredRules.rating(40))
        assertEquals(CredRules.Rating.Problematic, CredRules.rating(39))
        assertEquals(CredRules.Rating.Problematic, CredRules.rating(30))
        assertEquals(CredRules.Rating.Harmful, CredRules.rating(29))
        assertEquals(CredRules.Rating.Harmful, CredRules.rating(20))
        assertEquals(CredRules.Rating.VeryHarmful, CredRules.rating(19))
        assertEquals(CredRules.Rating.VeryHarmful, CredRules.rating(0))
        assertEquals("out of range is clamped", CredRules.Rating.VeryHarmful, CredRules.rating(-5))
        assertEquals(CredRules.Rating.Perfect, CredRules.rating(150))

        assertEquals(listOf(100, 89, 69, 59, 39, 29, 19), CredRules.Rating.entries.map { it.to })
        assertEquals(listOf(3, 2, 1, 1, 1, 1, null), CredRules.Rating.entries.map { it.minDays })
        assertEquals(listOf(7, 2, 1, 1, 1, 1, null), CredRules.Rating.entries.map { it.maxDays })
        assertFalse(CredRules.Rating.VeryHarmful.rises)
        assertTrue(CredRules.Rating.Harmful.rises)
    }

    @Test
    fun credRules_editingAndBlocks() {
        assertTrue(CredRules.canEdit(50))
        assertFalse("below 50 editing is off", CredRules.canEdit(49))

        assertEquals(CredRules.Block.None, CredRules.block(30))
        assertEquals(CredRules.Block.Temporary(1), CredRules.block(29))
        assertEquals("the rule book's example: 23 is a week", CredRules.Block.Temporary(7), CredRules.block(23))
        assertEquals(CredRules.Block.Temporary(10), CredRules.block(20))
        assertEquals(CredRules.Block.Permanent, CredRules.block(19))
        assertEquals(CredRules.Block.Permanent, CredRules.block(0))
    }

    @Test
    fun credRules_banScreenSaysWhatStipsAndThePointsSay() {
        assertNull(CredRules.banState(93, null))
        assertNull("unknown points never block", CredRules.banState(null, null))
        assertEquals(CredRules.BanState(permanent = false, daysLeft = 5, reason = null, endsText = null), CredRules.banState(25, null))
        assertEquals(CredRules.BanState(permanent = true, daysLeft = null, reason = null, endsText = null), CredRules.banState(10, null))
        assertEquals(
            "Stips' own block, with its reason and end",
            CredRules.BanState(permanent = false, daysLeft = null, reason = "ספאם", endsText = "10/11/2026"),
            CredRules.banState(93, AccountBan("ספאם", "10/11/2026")),
        )
        assertEquals(
            "blocked with no end date",
            CredRules.BanState(permanent = true, daysLeft = null, reason = "x", endsText = null),
            CredRules.banState(93, AccountBan("x", null)),
        )
        assertEquals(
            "Stips' block without an end, at 25 points: a temporary one by the rules",
            CredRules.BanState(permanent = false, daysLeft = 5, reason = null, endsText = null),
            CredRules.banState(25, AccountBan(null, null)),
        )
    }

    @Test
    fun credRules_projection() {
        assertEquals("seven points at a point every 3 to 7 days", 21..49, CredRules.daysToReach(93, 100))
        assertEquals(2..2, CredRules.daysToReach(89, 90))
        assertEquals("the rate changes on the way", 7..11, CredRules.daysToReach(88, 91))
        assertEquals(5..5, CredRules.daysToReach(25, 30))
        assertEquals(95..135, CredRules.daysToReach(45, 100))
        assertEquals(0..0, CredRules.daysToReach(50, 50))
        assertNull("below 20 nothing rises", CredRules.daysToReach(19, 25))

        val today = LocalDate.parse("2026-11-05")
        assertEquals(LocalDate.parse("2026-11-07") to LocalDate.parse("2026-11-07"), CredRules.dates(89, 90, today))
        assertNull(CredRules.dates(10, 30, today))

        assertEquals(listOf(100), CredRules.milestones(93))
        assertEquals(listOf(50, 60, 70, 90, 100), CredRules.milestones(45))
        assertEquals(listOf(30, 40, 50, 60, 70, 90, 100), CredRules.milestones(25))
        assertEquals(emptyList<Int>(), CredRules.milestones(100))
    }

    @Test
    fun credRules_monthsOnStipsFromTheProfilePhrase() {
        assertEquals(81, CredRules.monthsOnStips("6 שנים ו-9 חודשים"))
        assertEquals(15, CredRules.monthsOnStips("שנה ו-3 חודשים"))
        assertEquals(13, CredRules.monthsOnStips("שנה וחודש"))
        assertEquals(24, CredRules.monthsOnStips("שנתיים"))
        assertEquals(28, CredRules.monthsOnStips("שנתיים ו-4 חודשים"))
        assertEquals(36, CredRules.monthsOnStips("3 שנים"))
        assertEquals(1, CredRules.monthsOnStips("חודש"))
        assertEquals(2, CredRules.monthsOnStips("חודשיים"))
        assertEquals("a new account, in days", 0, CredRules.monthsOnStips("12 ימים"))
        assertNull(CredRules.monthsOnStips(null))
        assertNull(CredRules.monthsOnStips(""))
        assertNull(CredRules.monthsOnStips("hello"))
    }

    private fun notification(type: String, text: String, note: String? = null, points: Int = 0) = StipsNotification(
        id = 1,
        text = text,
        type = type,
        viewed = false,
        time = Instant.parse("2026-11-05T10:00:00Z"),
        target = NotificationTarget.OpenQuestion(9),
        note = note,
        pointsRemoved = points,
    )

    @Test
    fun notificationKinds_tellTheTeamsNoticesApart() {
        fun kind(type: String, text: String, note: String? = null, points: Int = 0) = NotificationKinds.kind(type, text, note, points)

        assertEquals(
            "a question's title can say anything",
            NotificationKinds.Kind.Answers,
            kind("new_ask_ans_registered", "התקבלו 3 תשובות חדשות לשאלה: למה הסטורי שלי נמחק?"),
        )
        assertEquals(NotificationKinds.Kind.Flower, kind("new_flower", "קיבלת פרח"))
        assertEquals(NotificationKinds.Kind.Thanks, kind("thanks_msg", "כתבו לך בקיר התודות"))
        assertEquals(NotificationKinds.Kind.Removal, kind("ans_deleted", "התשובה שלך נמחקה", "פרסום", 2))
        assertEquals(NotificationKinds.Kind.Removal, kind("ask_deleted", "השאלה שלך נמחקה"))
        assertEquals("points taken mean a removal", NotificationKinds.Kind.Removal, kind("new_ask_ans_registered", "x", null, 3))
        assertEquals(NotificationKinds.Kind.Report, kind("report_handled", "טופל"))
        assertEquals(NotificationKinds.Kind.Report, kind("system", "השאלה שדיווחת עליה הוסרה. תודה על הדיווח!"))
        assertEquals(NotificationKinds.Kind.Removal, kind("", "התשובה שלך הוסרה על ידי צוות האתר", "תוכן פוגעני"))
        assertEquals(
            "removed after someone else's report is still a removal",
            NotificationKinds.Kind.Removal,
            kind("", "התשובה שלך נמחקה בעקבות דיווח", "תוכן פוגעני"),
        )
        assertEquals(NotificationKinds.Kind.TeamNote, kind("", "הערה מצוות האתר", "אנא הקפידו על כללי הכתיבה"))
        assertEquals(NotificationKinds.Kind.TeamNote, kind("moderator_note", "הערה"))
        assertEquals(NotificationKinds.Kind.Other, kind("", "קיבלת פרח על התשובה שלך"))

        assertTrue(NotificationKinds.fromTeam(NotificationKinds.Kind.Removal))
        assertTrue(NotificationKinds.fromTeam(NotificationKinds.Kind.Report))
        assertTrue(NotificationKinds.fromTeam(NotificationKinds.Kind.TeamNote))
        assertFalse(NotificationKinds.fromTeam(NotificationKinds.Kind.Answers))
    }

    @Test
    fun jumpRules_teamNoticesOpenAtTheTop() {
        assertNull(JumpRules.focusFor(notification("ans_deleted", "התשובה שלך נמחקה", "פרסום", 2)))
        assertNull(JumpRules.focusFor(notification("", "הערה מצוות האתר", "אנא הקפידו")))
        assertEquals(
            QuestionFocus.NewAnswers(3, Instant.parse("2026-11-05T10:00:00Z")),
            JumpRules.focusFor(notification("new_ask_ans_registered", "התקבלו 3 תשובות חדשות")),
        )
        assertEquals("a flower without a type still opens at it", QuestionFocus.MyFlower, JumpRules.focusFor(notification("", "קיבלת פרח על התשובה שלך")))
    }

    private fun answer(id: Long, owner: Boolean = false) = Answer(
        id = id,
        questionId = 1,
        text = "x",
        postedAt = null,
        score = 0,
        userVote = 0,
        flowered = false,
        author = Author(nickname = "a", userId = id, photoStamp = null, anonymous = false, online = false),
        links = emptyList(),
        isOwner = owner,
    )

    @Test
    fun jumpRules_unreadBarCountsOthersAnswers() {
        val answers = listOf(answer(1), answer(2), answer(3), answer(4), answer(5, owner = true), answer(6))
        assertEquals(UnreadMark(4, 2), JumpRules.unread(answers, 4))
        assertEquals(UnreadMark(1, 5), JumpRules.unread(answers, 1))
        assertNull("not on the page", JumpRules.unread(answers, 40))
        assertNull("only the user's own", JumpRules.unread(answers.take(5), 5))
    }

    private fun message(id: Long, from: Long) = ChatMessage(id = id, text = "m$id", time = null, fromUserId = from, toUserId = if (from == ME) THEM else ME)

    @Test
    fun chatRules_firstUnreadMessage() {
        val messages = listOf(message(1, ME), message(2, THEM), message(3, ME), message(4, THEM), message(5, THEM), message(6, THEM))
        assertEquals("Stips' count: their last two", UnreadMark(5, 2), ChatRules.firstUnread(messages, ME, 2, null))
        assertNull(ChatRules.firstUnread(messages, ME, 0, 1))
        assertEquals("more than loaded: from the first of theirs", UnreadMark(2, 4), ChatRules.firstUnread(messages, ME, 10, null))
        assertEquals("without the count: since the last visit", UnreadMark(4, 3), ChatRules.firstUnread(messages, ME, null, 3))
        assertNull(ChatRules.firstUnread(messages, ME, null, 6))
        assertNull("nothing known", ChatRules.firstUnread(messages, ME, null, null))
        assertNull(ChatRules.firstUnread(messages, null, 2, null))
    }

    @Test
    fun parsers_appUserBlockAndModerator() {
        val banned = Parsers.appUser(
            Parsers.envelope(
                """{"status":"ok","data":{"appUser":{"id":5,"points":25,"flowersCount":3,"gender":"male","moderator":"senior",""" +
                    """"ban":{"banned":true,"msg":"<b>ספאם</b> בתשובות","endDateText":"10/11/2026"}}}}""",
            ),
        )!!
        assertEquals(25, banned.points)
        assertEquals(3, banned.flowers)
        assertEquals("senior", banned.moderator)
        assertTrue(banned.isModerator)
        assertEquals(AccountBan("ספאם בתשובות", "10/11/2026"), banned.ban)
        assertTrue(banned.ban!!.temporary)

        val plain = Parsers.appUser(Parsers.envelope("""{"status":"ok","data":{"appUser":{"id":6,"moderator":"","ban":{"banned":false,"msg":"","endDateText":""}}}}"""))!!
        assertNull("missing points stay unknown, never 0", plain.points)
        assertNull(plain.flowers)
        assertNull(plain.moderator)
        assertFalse(plain.isModerator)
        assertNull(plain.ban)

        val forGood = Parsers.appUser(Parsers.envelope("""{"status":"ok","data":{"appUser":{"id":7,"points":12,"ban":{"banned":true,"msg":"","endDateText":""}}}}"""))!!
        assertEquals(AccountBan(null, null), forGood.ban)
        assertFalse(forGood.ban!!.temporary)
    }

    private companion object {
        const val ME = 100L
        const val THEM = 200L
    }
}
