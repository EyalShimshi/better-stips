package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.SpecialAccounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure rules added in v0.19: text shared to Stips+ or forwarded from a chat, sent to up to five chats; and the
 * creator's badge on his profile only.
 */
class RulesV19Test {

    // --- Sharing text to Stips+ ---

    @Test
    fun share_takesPlainTextOnly() {
        assertEquals("hello", ShareRules.clean("  hello \n"))
        assertEquals("line one\nline two", ShareRules.clean("line one\r\nline two"))
        assertEquals("a\nb", ShareRules.clean("a\rb"))
        assertEquals("tabs\tstay", ShareRules.clean("tabs\tstay"))
        assertEquals("no bell, no null", ShareRules.clean("no bell\u0007, no null\u0000"))
        assertEquals("a picture's place goes", "text", ShareRules.clean("\uFFFCtext"))
        assertEquals("emoji stay whole", "hi 👋🏽 👨‍👩‍👧", ShareRules.clean("hi 👋🏽 👨‍👩‍👧"))
        assertEquals("https://stips.co.il/ask/1", ShareRules.clean("https://stips.co.il/ask/1"))
        assertNull(ShareRules.clean(null))
        assertNull(ShareRules.clean("   \n\t "))
        assertNull(ShareRules.clean("\uFFFC"))
    }

    @Test
    fun share_cutsVeryLongTextWithAnEllipsis() {
        val long = "x".repeat(ShareRules.MAX_TEXT + 50)
        val cut = ShareRules.clean(long)!!
        assertEquals(ShareRules.MAX_TEXT, cut.length)
        assertTrue(cut.endsWith("\u2026"))
        assertEquals("x".repeat(ShareRules.MAX_TEXT), ShareRules.clean("x".repeat(ShareRules.MAX_TEXT)))
        // Never half an emoji at the cut.
        val emoji = "a".repeat(ShareRules.MAX_TEXT - 2) + "😀😀😀"
        val cutEmoji = ShareRules.clean(emoji)!!
        assertTrue(cutEmoji.length <= ShareRules.MAX_TEXT)
        assertTrue(!Character.isHighSurrogate(cutEmoji[cutEmoji.length - 2]))
    }

    // --- Up to five chats ---

    @Test
    fun recipients_atMostFiveAndAlwaysFive() {
        assertEquals(5, ShareRules.MAX_RECIPIENTS)
        var picked = emptyList<Long>()
        for (id in 1L..5L) picked = ShareRules.toggle(picked, id)!!
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), picked)
        assertNull("a sixth is refused", ShareRules.toggle(picked, 6L))
        assertEquals("a tap lets one go", listOf(1L, 2L, 4L, 5L), ShareRules.toggle(picked, 3L))
        assertEquals("then there's room again", listOf(1L, 2L, 4L, 5L, 6L), ShareRules.toggle(ShareRules.toggle(picked, 3L)!!, 6L))
        assertEquals(listOf(9L), ShareRules.toggle(emptyList(), 9L))
        assertEquals(emptyList<Long>(), ShareRules.toggle(listOf(9L), 9L))
    }

    // --- Forwarding and sharing a chat message ---

    @Test
    fun forward_leavesOutTheCaretsThatPointIntoTheChat() {
        assertEquals("thanks!", ShareRules.forwardText("^^\nthanks!"))
        assertEquals("thanks!", ShareRules.forwardText("4^ thanks!"))
        assertEquals("both", ShareRules.forwardText("^^^\n^^\nboth"))
        assertEquals("a link stays, for its card", "https://stips.co.il/ask/5\nlook", ShareRules.forwardText("^\nhttps://stips.co.il/ask/5\nlook"))
        assertEquals("only carets stay as written", "^^", ShareRules.forwardText("^^"))
        assertEquals("smileys aren't carets", "^_^ hi", ShareRules.forwardText("^_^ hi"))
        assertEquals("plain", ShareRules.forwardText("plain"))
    }

    // --- The creator's badge ---

    @Test
    fun creatorBadge_onTheProfileOnly() {
        val creator = SpecialAccounts.CREATOR_ID
        assertNotNull("the profile shows it", SpecialAccounts.badge(creator))
        assertNull("not by his name on answers, questions or cards", SpecialAccounts.contentBadge(creator))
        assertNotNull("Stips' creator keeps his everywhere", SpecialAccounts.contentBadge(2042L))
        assertNull(SpecialAccounts.contentBadge(null))
        assertNull("organizations have no badge", SpecialAccounts.contentBadge(10486L))
    }
}
