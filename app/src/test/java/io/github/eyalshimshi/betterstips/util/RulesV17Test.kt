package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.ui.chats.ChatRules
import io.github.eyalshimshi.betterstips.widget.ChatsWidgetFeed
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure rules added in v0.17: legacy emoji codes in chats (#22), Stips' system account (#21), the DM page's search,
 * shelf and notes to self (#20), day headers in chats, the bell's own count, and one row per question for updates.
 */
class RulesV17Test {
    private val t0: Instant = Instant.parse("2026-11-09T10:00:00Z")

    @After
    fun forgetEmoji() = StipsEmoji.install(emptyMap())

    // Legacy emoji codes (#22)

    @Test
    fun emoji_listLinesBecomeEmoji() {
        val map = StipsEmoji.parse(sequenceOf("# EmojiOne 2.2.7", "", "nerd 1f913", "sob 1f62d", "flag_il 1f1ee-1f1f1", "bad zz", "lonely"))
        assertEquals("\uD83E\uDD13", map["nerd"])
        assertEquals("\uD83D\uDE2D", map["sob"])
        assertEquals("\uD83C\uDDEE\uD83C\uDDF1", map["flag_il"])
        assertNull(map["bad"])
        assertEquals(3, map.size)
    }

    @Test
    fun emoji_codesInChatsBecomeTheEmojiTheyName() {
        StipsEmoji.install(mapOf("nerd" to "\uD83E\uDD13", "sob" to "\uD83D\uDE2D"))
        assertEquals("היי\uD83E\uDD13", StipsEmoji.inChat("היי:nerd:"))
        assertEquals("each colon may open a code", "10:30\uD83E\uDD13", StipsEmoji.inChat("10:30:nerd:"))
        assertEquals("a\uD83D\uDE2D\uD83D\uDE2D", StipsEmoji.inChat("a:sob::sob:"))
        assertEquals("the site knows codes in lower case only", ":NERD:", StipsEmoji.inChat(":NERD:"))
        assertEquals(":unknown: stays", StipsEmoji.inChat(":unknown: stays"))
        assertEquals("older codes go", "hi", StipsEmoji.inChat("{ic:12}hi"))
        assertEquals("no colons, no change", "שלום", StipsEmoji.inChat("שלום"))
    }

    @Test
    fun emoji_withoutTheListOnlyOldCodesGo() {
        StipsEmoji.install(emptyMap())
        assertEquals(":nerd:", StipsEmoji.inChat("{ic:3}:nerd:"))
    }

    // Stips' system account (#21) and its chats

    @Test
    fun officialAccount_isStipsOwn() {
        assertTrue(SpecialAccounts.isOfficial(50916L))
        assertFalse(SpecialAccounts.isOfficial(2042L))
        assertFalse(SpecialAccounts.isOfficial(null))
        StipsEmoji.install(mapOf("nerd" to "\uD83E\uDD13"))
        val chats = Parsers.conversations(
            Parsers.envelope(
                """{"status":"ok","data":{"messages":[
                {"itemProfile":{"userid":50916,"nickname":"מערכת סטיפס"},"msg":"שלום :nerd:","time":"2026/11/09 10:00:00","newCount":1,"fromuserid":50916},
                {"itemProfile":{"userid":7,"nickname":"דנה"},"msg":"10:30","time":"2026/11/09 09:00:00","newCount":0,"fromuserid":275580}]}}""",
            ),
            275580L,
        )
        assertTrue(chats[0].other.platform)
        assertTrue(chats[0].other.verified)
        assertEquals("שלום \uD83E\uDD13", chats[0].lastMessage)
        assertFalse(chats[1].other.platform)
        assertTrue(chats[1].lastFromMe)
    }

    // The DM page (#20)

    private fun person(id: Long, name: String, online: Boolean = false) = Author(name, id, null, anonymous = false, online = online)

    private fun chat(other: Author, minute: Long = 0, fromMe: Boolean = false) =
        Conversation(other, "m", t0.minusSeconds(minute * 60), null, 0, fromMe)

    @Test
    fun notes_areTheChatWithYourself() {
        assertTrue(ChatRules.isNotes(9L, 9L))
        assertFalse(ChatRules.isNotes(8L, 9L))
        assertFalse("signed out, nothing is notes", ChatRules.isNotes(9L, null))
    }

    @Test
    fun shelf_showsWhoIsOnlineMostRecentFirstWithoutYou() {
        val list = listOf(
            chat(person(1, "א", online = true), 1),
            chat(person(9, "me", online = true), 2, fromMe = true),
            chat(person(2, "ב"), 3),
            chat(person(3, "ג", online = true), 4),
        )
        assertEquals(listOf(1L, 3L), ChatRules.onlineShelf(list, 9L).map { it.userId })
        assertTrue(ChatRules.onlineShelf(emptyList(), 9L).isEmpty())
    }

    @Test
    fun search_findsChatsByNameAndNotesByTheirs() {
        val list = listOf(chat(person(1, "Abby")), chat(person(2, "Bob")), chat(person(9, "Me Myself"), fromMe = true))
        assertEquals(listOf(1L), ChatRules.matching(list, " ab ", 9L, "Notes to self").map { it.other.userId })
        assertEquals(listOf(9L), ChatRules.matching(list, "notes", 9L, "Notes to self").map { it.other.userId })
        assertTrue(ChatRules.matching(list, "  ", 9L, "Notes to self").isEmpty())
    }

    @Test
    fun widget_leavesNotesOut() {
        val list = listOf(chat(person(1, "א")), chat(person(9, "me"), fromMe = true))
        assertEquals(listOf(1L), ChatsWidgetFeed.chatsOf(list, 9L).map { it.userId })
        assertEquals(listOf(1L, 9L), ChatsWidgetFeed.chatsOf(list).map { it.userId })
    }

    // Day headers in chats

    private fun message(id: Long, time: Instant?) = ChatMessage(id, "m$id", time, fromUserId = 1, toUserId = 2)

    @Test
    fun dayHeaders_startEachDay() {
        val zone = ZoneOffset.UTC
        val morning = message(1, Instant.parse("2026-11-08T08:00:00Z"))
        val night = message(2, Instant.parse("2026-11-08T23:59:00Z"))
        val next = message(3, Instant.parse("2026-11-09T00:01:00Z"))
        assertTrue("the oldest shown starts its day", ChatRules.startsDay(morning, null, zone))
        assertFalse(ChatRules.startsDay(night, morning, zone))
        assertTrue(ChatRules.startsDay(next, night, zone))
        assertFalse("no time, no header", ChatRules.startsDay(message(4, null), next, zone))
        assertFalse(ChatRules.startsDay(next, message(5, null), zone))
    }

    @Test
    fun dayHeaders_sayTodayYesterdayTheWeekdayOrTheDate() {
        val today = LocalDate.of(2026, 11, 9)
        assertEquals(ChatRules.DayKind.Today, ChatRules.dayKind(today, today))
        assertEquals(ChatRules.DayKind.Yesterday, ChatRules.dayKind(today.minusDays(1), today))
        assertEquals(ChatRules.DayKind.ThisWeek, ChatRules.dayKind(today.minusDays(6), today))
        assertEquals(ChatRules.DayKind.ThisYear, ChatRules.dayKind(today.minusDays(7), today))
        assertEquals(ChatRules.DayKind.Older, ChatRules.dayKind(LocalDate.of(2025, 12, 31), today))
    }

    // The bell: one row per question, its own count

    private fun notification(
        id: Long,
        type: String = "new_ask_ans_registered",
        text: String = "התקבלה תשובה חדשה",
        question: Long? = 100,
        viewed: Boolean = false,
        minute: Long = id,
    ) = StipsNotification(
        id = id,
        text = text,
        type = type,
        viewed = viewed,
        time = t0.plusSeconds(minute * 60),
        target = question?.let { NotificationTarget.OpenQuestion(it) },
    )

    @Test
    fun rows_putAQuestionsUpdatesTogetherWhereTheNewestIs() {
        val list = listOf(
            notification(10, question = 100),
            notification(9, type = "new_flower", text = "פרח", question = 100),
            notification(8, question = 200),
            notification(7, question = 100),
            notification(6, type = "thanks", text = "תודה", question = null),
        )
        val rows = NotificationCategories.rows(list)
        assertEquals(listOf("q100", "n9", "q200", "n6"), rows.map { it.key })
        assertEquals(listOf(10L, 7L), rows[0].items.map { it.id })
        assertEquals(100L, rows[0].questionId)
        assertNull("a flower isn't an update", rows[1].questionId)
        val sections = NotificationCategories.sectionsBy(rows, LocalDate.of(2026, 11, 9), ZoneOffset.UTC) { it.newest.time }
        assertEquals(1, sections.size)
        assertEquals(4, sections[0].second.size)
    }

    @Test
    fun rows_openAtTheFirstOfAllTheirNewAnswers() {
        val newest = notification(10, text = "התקבלו 3 תשובות חדשות לשאלה: למה?")
        val older = notification(7, text = "התקבלו שתי תשובות חדשות לשאלה: למה?")
        assertEquals(5, NotificationCategories.answers(listOf(newest, older)))
        assertEquals(QuestionFocus.NewAnswers(5, newest.time), NotificationCategories.focusFor(listOf(newest, older)))
        assertEquals("one opens as it always has", QuestionFocus.NewAnswers(3, newest.time), NotificationCategories.focusFor(listOf(newest)))
        assertNull(NotificationCategories.focusFor(emptyList()))
    }

    @Test
    fun bell_countsWaitingRowsNotNotifications() {
        val waiting = mapOf(10L to "q100", 9L to "q100", 8L to "n8", 5L to "q200")
        assertEquals("a question's updates are one row; what the bell showed is gone", 2, SeenNotifications.rows(waiting, 6L))
        assertEquals(3, SeenNotifications.rows(waiting, 0L))
        assertEquals(0, SeenNotifications.rows(waiting, 10L))
    }

    @Test
    fun bell_newIsPastWhatTheBellShowedAndWhatChecksRead() {
        val page = listOf(12L to true, 11L to false, 10L to true, 9L to false)
        assertEquals("before anything, what Stips has unread", setOf(11L, 9L), SeenNotifications.fresh(page, lastSeen = 0L, learned = 0L))
        assertEquals("then anything newer, though other apps saw it", setOf(12L, 11L), SeenNotifications.fresh(page, lastSeen = 10L, learned = 0L))
        assertEquals("checks' own reading counts too", setOf(12L), SeenNotifications.fresh(page, lastSeen = 0L, learned = 11L))
        assertEquals(setOf(12L), SeenNotifications.fresh(page, lastSeen = 10L, learned = 11L))
        assertEquals("a check whose answer crossed the bell's brings nothing back", emptySet<Long>(), SeenNotifications.fresh(page, lastSeen = 12L, learned = 0L))
    }

    @Test
    fun bell_readsThePageWhenStipsCountsMoreOrEveryMinuteOnHome() {
        assertTrue("Stips' count went up", BellRules.listNow(1, 0, 5_000L, onHome = false))
        assertTrue(BellRules.listNow(0, 0, BellRules.LIST_EVERY_MS, onHome = true))
        assertFalse(BellRules.listNow(0, 0, BellRules.LIST_EVERY_MS, onHome = false))
        assertFalse(BellRules.listNow(2, 2, 10_000L, onHome = true))
        assertEquals("until the page is read, Stips' count fills in", 5, BellRules.badge(rows = 2, stips = 5, listed = false))
        assertEquals("once read, the rows say it all", 2, BellRules.badge(rows = 2, stips = 5, listed = true))
        assertEquals(3, BellRules.badge(rows = 3, stips = 0, listed = false))
    }

    // Phone notifications: new by Stips+'s own record (v0.17)

    private fun note(id: Long, viewed: Boolean = false, text: String = "n$id") =
        notification(id, text = text, viewed = viewed, question = null)

    @Test
    fun phone_postsWhatOtherAppsAlreadyMarkedSeen() {
        val known = NotifyRules.pick(listOf(note(1), note(2)), emptyMap(), first = true).known
        // The official app read the list before the check: Stips says all are viewed.
        val pick = NotifyRules.pick(listOf(note(1, viewed = true), note(2, viewed = true), note(3, viewed = true)), known, first = false)
        assertEquals(listOf(3L), pick.post.map { it.id })
        assertTrue(pick.read.isEmpty())
    }

    @Test
    fun phone_doneWithWhatTheBellShowed() {
        val known = NotifyRules.pick(listOf(note(1), note(2), note(3)), emptyMap(), first = true).known
        val list = listOf(note(1, viewed = true), note(2, viewed = true), note(3, viewed = true), note(4, viewed = true))
        val pick = NotifyRules.pick(list, known, first = false, seenInApp = 3L)
        assertEquals(listOf(1L, 2L, 3L), pick.read)
        assertEquals(listOf(4L), pick.post.map { it.id })
        assertFalse(1L in pick.known)
        // Stips updated #2 after the bell showed it: new again.
        val again = NotifyRules.pick(listOf(note(2, text = "3 new answers")), pick.known, first = false, seenInApp = 3L)
        assertEquals(listOf(2L), again.post.map { it.id })
    }

    @Test
    fun phone_neverPostsOldOnesItDidntRecord() {
        val known = mapOf(5L to NotifyRules.signature(note(5, viewed = true)))
        val pick = NotifyRules.pick(listOf(note(3, viewed = true), note(5, viewed = true)), known, first = false)
        assertTrue(pick.post.isEmpty())
        assertTrue(3L in pick.known)
    }
}
