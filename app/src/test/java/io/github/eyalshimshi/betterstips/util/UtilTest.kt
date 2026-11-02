package io.github.eyalshimshi.betterstips.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UtilTest {

    @Test
    fun stipsTimeIsIsraelLocalTime() {
        // Winter (UTC+2) and summer (UTC+3).
        assertEquals(Instant.parse("2026-11-02T14:59:56Z"), StipsTime.parse("2026/11/02 16:59:56"))
        assertEquals(Instant.parse("2026-07-01T09:00:00Z"), StipsTime.parse("2026/07/01 12:00:00"))
        assertEquals(Instant.parse("2026-01-05T07:03:07Z"), StipsTime.parse("2026/1/5 9:3:7"))
        assertNull(StipsTime.parse(""))
        assertNull(StipsTime.parse(null))
        assertNull(StipsTime.parse("yesterday"))
    }

    @Test
    fun agoBuckets() {
        val now = Instant.parse("2026-11-02T12:00:00Z")
        assertEquals(StipsTime.Ago.JustNow, StipsTime.ago(now.minusSeconds(30), now))
        assertEquals(StipsTime.Ago.Minutes(5), StipsTime.ago(now.minus(Duration.ofMinutes(5)), now))
        assertEquals(StipsTime.Ago.Hours(3), StipsTime.ago(now.minus(Duration.ofHours(3)), now))
        assertEquals(StipsTime.Ago.Days(2), StipsTime.ago(now.minus(Duration.ofDays(2)), now))
        assertEquals(
            StipsTime.Ago.OnDate(LocalDate.of(2026, 10, 20)),
            StipsTime.ago(Instant.parse("2026-10-20T12:00:00Z"), now, ZoneOffset.UTC),
        )
    }

    @Test
    fun answerRulesMatchTheSite() {
        assertEquals(AnswerRules.Problem.Empty, AnswerRules.check("   "))
        assertEquals(AnswerRules.Problem.TooLong, AnswerRules.check("א".repeat(11), maxLength = 10))
        assertEquals(AnswerRules.Problem.RepeatedLetters, AnswerRules.check("שלוםםם"))
        assertEquals(AnswerRules.Problem.RepeatedLetters, AnswerRules.check("חחחח"))
        assertNull("three ח are fine", AnswerRules.check("חחח"))
        assertNull("the site allows the word מממש", AnswerRules.check("זה מממש את החלום"))
        assertNull(AnswerRules.check("Hello, this is fine!"))
        assertFalse(AnswerRules.hasRepeatedLetters("ממ"))
    }

    @Test
    fun linksAreFoundWithoutTrailingPunctuation() {
        val text = "ראו https://example.com/a?b=1. וגם (http://x.co/y)"
        val links = Links.find(text).map { text.substring(it) }
        assertEquals(listOf("https://example.com/a?b=1", "http://x.co/y"), links)
        assertTrue(Links.find("no links here").isEmpty())
    }

    @Test
    fun stipsQuestionLinksAreRecognized() {
        assertEquals(19745126L, Links.questionId("https://stips.co.il/ask/19745126/%D7%90%D7%99%D7%9A"))
        assertEquals(42L, Links.questionId("https://www.stips.co.il/ask/42"))
        assertNull(Links.questionId("https://stips.co.il/profile/42"))
        assertNull(Links.questionId("https://evil.example/ask/42"))
    }
}
