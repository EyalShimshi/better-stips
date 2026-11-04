package io.github.eyalshimshi.betterstips.ui

import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.ui.chats.ChatRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The chat screen's pure rules (#6). */
class ChatRulesTest {
    private fun message(id: Long, from: Long) =
        ChatMessage(id = id, text = "m$id", time = null, fromUserId = from, toUserId = if (from == 1L) 2L else 1L)

    @Test
    fun polledSentAndOlderMessagesShowOnceOldestFirst() {
        val shown = listOf(message(10, 1), message(11, 2))
        val polled = listOf(message(11, 2), message(12, 1))
        val older = listOf(message(9, 2), message(8, 2))
        val merged = ChatRules.merge(ChatRules.merge(shown, polled), older)
        assertEquals(listOf(8L, 9L, 10L, 11L, 12L), merged.map { it.id })
    }

    @Test
    fun theJumpButtonCountsTheirMessagesAfterTheLastOneSeen() {
        val me = 1L
        val thread = listOf(message(10, 2), message(11, 1), message(12, 2), message(13, 2), message(14, 1))
        assertEquals("nothing before the first page is shown", 0, ChatRules.unseen(thread, 0L, me))
        assertEquals(2, ChatRules.unseen(thread, 11L, me))
        assertEquals("my own messages don't count", 0, ChatRules.unseen(thread, 13L, me))
        assertEquals(0, ChatRules.unseen(thread, 14L, me))
    }

    @Test
    fun messagesFromOnePersonSitTogether() {
        assertFalse("the first message", ChatRules.continuesRun(message(1, 1), null))
        assertTrue(ChatRules.continuesRun(message(2, 1), message(1, 1)))
        assertFalse(ChatRules.continuesRun(message(3, 2), message(2, 1)))
    }
}
