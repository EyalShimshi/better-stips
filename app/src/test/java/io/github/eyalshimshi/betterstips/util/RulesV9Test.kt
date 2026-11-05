package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswersOrder
import io.github.eyalshimshi.betterstips.data.api.ServerClock
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.ExpertFields
import io.github.eyalshimshi.betterstips.ui.components.ExpertJson
import io.github.eyalshimshi.betterstips.widget.ChatsWidgetFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** Pure rules added in v0.9 (#8). */
class RulesV9Test {

    private fun author(name: String = "דני", id: Long? = 5, anonymous: Boolean = false) =
        Author(nickname = name, userId = id, photoStamp = null, anonymous = anonymous, online = false)

    private fun answer(id: Long, posted: String?, score: Int = 0, by: Author = author()) = Answer(
        id = id,
        questionId = 1,
        text = "x",
        postedAt = posted?.let { Instant.parse(it) },
        score = score,
        userVote = 0,
        flowered = false,
        author = by,
        links = emptyList(),
    )

    @Test
    fun serverClock_correctsOnlyRealDifferences() {
        val received = 1_000_000_000L
        assertEquals("within the header's resolution", 0L, ServerClock.offset(received + 1_000, received))
        assertEquals(0L, ServerClock.offset(received - 2_000, received))
        assertEquals(5_500L, ServerClock.offset(received + 5_000, received))
        assertEquals(-9_500L, ServerClock.offset(received - 10_000, received))
        assertEquals(
            Instant.parse("2026-11-05T07:50:37Z").toEpochMilli(),
            ServerClock.parseHttpDate("Thu, 05 Nov 2026 07:50:37 GMT"),
        )
        assertNull(ServerClock.parseHttpDate("yesterday"))
        assertNull(ServerClock.parseHttpDate(null))
    }

    @Test
    fun secondsAgo_onlyUnderAMinute() {
        val now = Instant.parse("2026-11-05T12:00:00Z")
        assertEquals(0, StipsTime.secondsAgo(now, now))
        assertEquals(12, StipsTime.secondsAgo(now.minusSeconds(12), now))
        assertEquals(59, StipsTime.secondsAgo(now.minusSeconds(59), now))
        assertNull("from a minute on, times read in minutes", StipsTime.secondsAgo(now.minusSeconds(60), now))
        assertNull(StipsTime.secondsAgo(now.minusSeconds(75), now))
        assertEquals("a little in the future counts as now", 0, StipsTime.secondsAgo(now.plusSeconds(4), now))
        assertEquals(StipsTime.Ago.Minutes(1), StipsTime.ago(now.minusSeconds(75), now))
    }

    @Test
    fun answersOrder() {
        val a = answer(1, "2026-11-05T10:00:00Z", score = 2)
        val b = answer(2, "2026-11-05T11:00:00Z", score = 7)
        val c = answer(3, "2026-11-05T09:00:00Z", score = -1)
        val stips = listOf(a, b, c)
        assertEquals(stips, AnswerDisplay.ordered(stips, AnswersOrder.AsOnStips))
        assertEquals(listOf(2L, 1L, 3L), AnswerDisplay.ordered(stips, AnswersOrder.Newest).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), AnswerDisplay.ordered(stips, AnswersOrder.Oldest).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), AnswerDisplay.ordered(stips, AnswersOrder.TopVoted).map { it.id })
        val tie = listOf(answer(4, null, score = 1), answer(5, null, score = 1))
        assertEquals("ties keep Stips' order", listOf(4L, 5L), AnswerDisplay.ordered(tie, AnswersOrder.TopVoted).map { it.id })
    }

    @Test
    fun askerLabel_femininOnlyForAnAnonymousWoman() {
        assertTrue(AnswerDisplay.askerIsFemale(answer(1, null, by = author("אנונימית", null, anonymous = true))))
        assertFalse(AnswerDisplay.askerIsFemale(answer(1, null, by = author("אנונימי", null, anonymous = true))))
        assertFalse(AnswerDisplay.askerIsFemale(answer(1, null, by = author("דנה"))))
    }

    @Test
    fun organizations() {
        assertTrue(author(id = 10486).organization)
        assertTrue(author(id = 253000).verified)
        assertFalse(author(id = 5).verified)
        assertTrue(author(id = 5).copy(branding = "RANK_NET_KNIGHT").organization)
        val system = author("מערכת סטיפס", null, anonymous = true).copy(platform = true)
        assertTrue(system.verified)
        assertFalse(system.organization)
    }

    @Test
    fun chatLinks_openOneChat() {
        val uri = Shortcuts.chatUri(5, "דני & co?")
        assertEquals(DeepLink.Conversation(5, "דני & co?"), Shortcuts.link(Shortcuts.ACTION_CHAT, uri))
        assertEquals("no chat: the list", DeepLink.Messages, Shortcuts.link(Shortcuts.ACTION_CHAT, null))
        assertEquals(DeepLink.Messages, Shortcuts.link(Shortcuts.ACTION_CHAT, "stipsplus://chat/abc"))
        assertNull("only the widget's own intent opens a chat", Links.deepLink(uri))
        assertNull(Shortcuts.link(null, uri))
    }

    @Test
    fun chatsWidget_keepsPeopleAndRoundTrips() {
        val list = listOf(
            Conversation(author("א", 1), "שלום", Instant.parse("2026-11-05T10:00:00Z"), null, 2, false),
            Conversation(author("מחוק", null), "?", null, null, 0, false),
            Conversation(author("ב", 2), "מה נשמע", null, null, 0, true),
        )
        val chats = ChatsWidgetFeed.chatsOf(list)
        assertEquals(listOf(1L, 2L), chats.map { it.userId })
        assertEquals(2, chats[0].unread)
        assertTrue(chats[1].fromMe)
        val many = (1..30).map { Conversation(author("x", it.toLong()), "m", null, null, 0, false) }
        assertEquals(ChatsWidgetFeed.MAX_CHATS, ChatsWidgetFeed.chatsOf(many).size)

        val saved = ChatsWidgetFeed.Saved(userId = 9, savedAt = 123L, reportedUnread = 2, chats = chats)
        assertEquals(saved, ChatsWidgetFeed.decode(ChatsWidgetFeed.encode(saved)))
        assertEquals(saved.copy(reportedUnread = null), ChatsWidgetFeed.decode(ChatsWidgetFeed.encode(saved.copy(reportedUnread = null))))
        assertNull(ChatsWidgetFeed.decode("not json"))
        assertNull(ChatsWidgetFeed.decode("""{"chats":[]}"""))

        val now = Instant.parse("2026-11-05T12:00:00Z").toEpochMilli()
        assertFalse(ChatsWidgetFeed.isStale(now - 29 * 60_000L, now))
        assertTrue(ChatsWidgetFeed.isStale(0L, now))
        val zone = ZoneOffset.UTC
        assertEquals("08:05", ChatsWidgetFeed.timeLabel(Instant.parse("2026-11-05T08:05:00Z").toEpochMilli(), now, zone, Locale.US))
        assertEquals("3 May", ChatsWidgetFeed.timeLabel(Instant.parse("2026-05-03T08:05:00Z").toEpochMilli(), now, zone, Locale.US))
        assertEquals("3.5.25", ChatsWidgetFeed.timeLabel(Instant.parse("2025-05-03T08:05:00Z").toEpochMilli(), now, zone, Locale.US))
    }

    @Test
    fun expertMode_showsIdsAndPrettyJson() {
        val question = Question(
            id = 7,
            title = "t",
            body = "",
            topics = emptyList(),
            postedAt = null,
            answerCount = 0,
            pinCount = 0,
            channelId = 3,
            author = author(id = 9),
            cover = null,
        )
        assertEquals("ask #7 · channel 3 · user 9", ExpertFields.questionLine(question))
        assertEquals("ans #4 · user 5", ExpertFields.answerLine(answer(4, null)))
        assertEquals("ans #4", ExpertFields.answerLine(answer(4, null, by = author("אנונימי", null, anonymous = true))))
        assertTrue(ExpertFields.question(question, 0).contains("id" to "7"))
        val pretty = ExpertJson.format("""{"a":1,"b":[1,2]}""")
        assertTrue(pretty.contains("\n"))
        assertTrue(pretty.contains("\"a\": 1"))
        assertEquals("not json", ExpertJson.format("not json"))
    }
}
