package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.ui.chats.ChatRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure rules added in v0.12: Stips' escaped text shown as written, and easy reply in chats. */
class RulesV12Test {

    @Test
    fun entities_decodedOnceAsWritten() {
        assertEquals("a > b", TextUtils.decodeEntities("a &gt; b"))
        assertEquals("<3 & \"hi\" 'yo'", TextUtils.decodeEntities("&lt;3 &amp; &quot;hi&quot; &#39;yo&#39;"))
        assertEquals("what its author wrote", "&lt;", TextUtils.decodeEntities("&amp;lt;"))
        assertEquals("\u05d0", TextUtils.decodeEntities("&#1488;"))
        assertEquals("\u05d0", TextUtils.decodeEntities("&#x5D0;"))
        assertEquals("\u200f", TextUtils.decodeEntities("&rlm;"))
        assertEquals("not references", "AT&T, a & b, &unknownthing; &#;", TextUtils.decodeEntities("AT&T, a & b, &unknownthing; &#;"))
        assertEquals("past Unicode stays", "&#1114112;", TextUtils.decodeEntities("&#1114112;"))
        assertEquals("שלום", TextUtils.decodeEntities("שלום"))
        assertEquals("& more", TextUtils.htmlToPlain("&amp; more"))
        assertEquals("an escaped tag is text", "<b>", TextUtils.htmlToPlain("&lt;b&gt;"))
    }

    @Test
    fun parsers_showWhatPeopleWrote() {
        val answer = Parsers.answers(Parsers.envelope("""{"status":"ok","data":[{"objType":"ans","data":{"id":10,"a":"x &gt; y &amp;&amp; z"}}]}""")).single()
        assertEquals("x > y && z", answer.text)

        val question = Parsers.questionItem(
            Parsers.envelope("""{"status":"ok","data":{"omniOmniObj":{"data":{"id":5,"q":"&quot;why&quot; &lt;3","text_content":"a&#39;b","tagslist":["R&amp;B"]}}}}"""),
        )!!
        assertEquals("\"why\" <3", question.title)
        assertEquals("a'b", question.body)
        assertEquals(listOf("R&B"), question.topics)

        val messages = Parsers.messages(Parsers.envelope("""{"status":"ok","data":{"messages":[{"id":1,"msg":"^^ &lt;- this","fromuserid":2,"touserid":3}]}}"""))
        assertEquals("^^ <- this", messages.single().text)
    }

    @Test
    fun chats_replyCountsFromTheBottom() {
        val messages = (1L..5L).map { ChatMessage(id = it, text = "m$it", time = null, fromUserId = if (it % 2 == 0L) 7L else 8L, toUserId = null) }
        assertEquals("the newest: one up", 1, ChatRules.replyCount(messages, 5L))
        assertEquals(3, ChatRules.replyCount(messages, 3L))
        assertEquals(5, ChatRules.replyCount(messages, 1L))
        assertNull("not loaded", ChatRules.replyCount(messages, 99L))

        // Written by easy reply, and read back by the quote once it's sent: the same message.
        val text = ReplyRefs.withReply("ok", ChatRules.replyCount(messages, 3L)!!)
        assertEquals("^^^\nok", text)
        val sent = messages + ChatMessage(id = 6L, text = text, time = null, fromUserId = 8L, toUserId = 7L)
        val target = ReplyRefs.resolve(text, sent.lastIndex).targets.single()
        assertEquals(3L, sent[target].id)
    }
}
