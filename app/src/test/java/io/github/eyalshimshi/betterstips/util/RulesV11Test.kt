package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Pure rules added in v0.11: which Stips notifications and chats reach the phone; answers' carets kept as written. */
class RulesV11Test {
    private val t0: Instant = Instant.parse("2026-11-05T12:00:00Z")

    private fun note(id: Long, minute: Long = id, viewed: Boolean = false, text: String = "n$id") = StipsNotification(
        id = id,
        text = text,
        type = "new_ask_ans_registered",
        viewed = viewed,
        time = t0.plusSeconds(minute * 60),
        target = null,
    )

    private fun chat(userId: Long, minute: Long, unread: Int = 1, last: String = "hi", fromMe: Boolean = false) = Conversation(
        other = Author(nickname = "u$userId", userId = userId, photoStamp = null, anonymous = false, online = false),
        lastMessage = last,
        time = t0.plusSeconds(minute * 60),
        timeText = null,
        unread = unread,
        lastFromMe = fromMe,
    )

    @Test
    fun notifications_firstCheckRecordsWithoutPosting() {
        val pick = NotifyRules.pick(listOf(note(1), note(2), note(3, viewed = true)), emptyMap(), first = true)
        assertTrue(pick.post.isEmpty())
        // Seen ones are recorded too since v0.17: new is decided by what checks saw, not only by Stips' flag.
        assertEquals(setOf(1L, 2L, 3L), pick.known.keys)
        assertTrue(pick.read.isEmpty())
    }

    @Test
    fun notifications_newOnesAndOnesStipsUpdatedInPlace() {
        val known = NotifyRules.pick(listOf(note(1), note(2)), emptyMap(), first = true).known
        // A new one, and #2 updated by Stips: "3 new answers", a later time, the same id.
        val later = listOf(note(1), note(2, minute = 30, text = "3 new answers"), note(4))
        val pick = NotifyRules.pick(later, known, first = false)
        assertEquals(listOf(2L, 4L), pick.post.map { it.id })
        assertTrue("nothing changed since", NotifyRules.pick(later, pick.known, first = false).post.isEmpty())
    }

    @Test
    fun notifications_readOnesLeaveThePhone() {
        val known = NotifyRules.pick(listOf(note(1), note(2)), emptyMap(), first = true).known
        // Read means shown in Stips+'s bell since v0.17 (seenInApp); Stips' flag alone is set by other apps too.
        val pick = NotifyRules.pick(listOf(note(1, viewed = true), note(2)), known, first = false, seenInApp = 1L)
        assertEquals(listOf(1L), pick.read)
        assertFalse(1L in pick.known)
        assertTrue(pick.post.isEmpty())
    }

    @Test
    fun notifications_upgradingFromV010DoesNotReplay() {
        val pick = NotifyRules.pick(listOf(note(5), note(6), note(7)), emptyMap(), first = false, legacyNewest = 6L)
        assertEquals(listOf(7L), pick.post.map { it.id })
        assertEquals(setOf(5L, 6L, 7L), pick.known.keys)
    }

    @Test
    fun notifications_atMostFiveAndTheNewestRemembered() {
        val pick = NotifyRules.pick((1L..9L).map { note(it) }, emptyMap(), first = false)
        assertEquals(listOf(5L, 6L, 7L, 8L, 9L), pick.post.map { it.id })
        assertEquals(9, pick.known.size)

        val many = NotifyRules.pick((1L..250L).map { note(it) }, emptyMap(), first = true).known
        assertEquals(NotifyRules.MAX_KNOWN, many.size)
        assertEquals(51L, many.keys.minOrNull())
    }

    @Test
    fun chats_onePerChatWithANewMessage() {
        val first = NotifyRules.pickChats(listOf(chat(10, 1), chat(11, 2, unread = 0)), emptyMap(), first = true)
        assertTrue(first.post.isEmpty())
        assertEquals(setOf(10L), first.known.keys)

        // 10 wrote again, 12 is new, 11 stays read, and 13's last message is the user's own.
        val list = listOf(
            chat(10, 5, unread = 2, last = "again"),
            chat(11, 2, unread = 0),
            chat(12, 4),
            chat(13, 6, fromMe = true),
        )
        val pick = NotifyRules.pickChats(list, first.known, first = false)
        assertEquals("newest first", listOf(10L, 12L), pick.post.map { it.other.userId })

        // 10 was read elsewhere: its notification goes.
        val read = NotifyRules.pickChats(listOf(chat(10, 5, unread = 0, last = "again"), chat(12, 4)), pick.known, first = false)
        assertEquals(listOf(10L), read.read)
        assertTrue(read.post.isEmpty())
    }

    @Test
    fun chats_listedWhenTheCountSaysSo() {
        assertFalse(NotifyRules.listChats(0, 3, 0L, 1_000L))
        assertTrue("first check", NotifyRules.listChats(2, null, 0L, 1_000L))
        assertTrue("the count changed", NotifyRules.listChats(2, 1, 1_000L, 1_000L))
        assertFalse("the same count, just listed", NotifyRules.listChats(2, 2, 1_000L, 2_000L))
        assertTrue("the same count for a while", NotifyRules.listChats(2, 2, 0L, NotifyRules.CHATS_RECHECK_MS))
    }

    @Test
    fun replyRefs_keepTheTextAsWrittenByDefault() {
        val reply = ReplyRefs.resolve("^^\nשלום", 3)
        assertEquals(listOf(1), reply.targets)
        assertEquals("the carets stay", "^^\nשלום", reply.body)
        assertEquals("and lead to the answer", listOf((0..1) to 1), reply.inBody)

        val stacked = ReplyRefs.resolve("^^^\n^^\nx", 5)
        assertEquals(listOf(2, 3), stacked.targets)
        assertEquals("^^^\n^^\nx", stacked.body)
        assertEquals(listOf((0..2) to 2, (4..5) to 3), stacked.inBody)

        val mixed = ReplyRefs.resolve("^^^ את קבעת\n\n^^ דור המבול", 6)
        assertEquals("^^^ את קבעת\n\n^^ דור המבול", mixed.body)
        assertEquals(listOf((0..2) to 3, (13..14) to 4), mixed.inBody)

        assertEquals("a quote shows the answer as written", "^^ שלום עולם", ReplyRefs.snippet("^^\nשלום   עולם"))
        assertEquals("hidden only on request", "x", ReplyRefs.resolve("^^\nx", 3, hideLeading = true).body)
    }

    @Test
    fun alarmAndStorage() {
        assertEquals(60_000L, NotifyRules.alarmMs(1))
        assertEquals(120_000L, NotifyRules.alarmMs(2))
        assertNull("15: the job alone", NotifyRules.alarmMs(15))

        val map = mapOf(3L to "1730000000000:-12345", 9L to "0:7")
        assertEquals(map, NotifyRules.decode(NotifyRules.encode(map)))
        assertTrue(NotifyRules.decode("").isEmpty())
        assertTrue(NotifyRules.decode(null).isEmpty())
        assertEquals(mapOf(4L to "x"), NotifyRules.decode("junk;4=x;=y"))

        val failed = NotifyRules.Status(1_730_000_000_000L, NotifyRules.Outcome.Failed, 0, "NETWORK")
        assertEquals(failed, NotifyRules.decodeStatus(NotifyRules.encodeStatus(failed)))
        val posted = NotifyRules.Status(5L, NotifyRules.Outcome.Posted, 3)
        assertEquals(posted, NotifyRules.decodeStatus(NotifyRules.encodeStatus(posted)))
        assertNull(NotifyRules.decodeStatus("5|Posted"))
        assertNull(NotifyRules.decodeStatus(null))
    }
}
