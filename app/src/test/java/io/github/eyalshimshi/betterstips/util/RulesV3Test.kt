package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import io.github.eyalshimshi.betterstips.data.api.Backoff
import io.github.eyalshimshi.betterstips.diag.NetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

/** Pure rules added in v0.3: back-off, Live's rate and cost, the ask form, new notifications, the network log. */
class RulesV3Test {

    @Test
    fun backoffDoublesUpToFifteenMinutesAndHonorsRetryAfter() {
        assertEquals(60_000L, Backoff.delayMs(1, null))
        assertEquals(120_000L, Backoff.delayMs(2, null))
        assertEquals(480_000L, Backoff.delayMs(4, null))
        assertEquals(900_000L, Backoff.delayMs(5, null))
        assertEquals(900_000L, Backoff.delayMs(12, null))
        assertEquals("a longer Retry-After wins", 1_200_000L, Backoff.delayMs(1, 1_200))
        assertEquals("but never more than an hour", 3_600_000L, Backoff.delayMs(1, 99_999))
        assertEquals(60_000L, Backoff.delayMs(1, 5))
    }

    @Test
    fun backoffPausesThenLiftsOnSuccess() {
        Backoff.onSuccess()
        assertFalse(Backoff.active)
        Backoff.onLimited(429, null)
        assertTrue(Backoff.remainingMs() in 1..60_000L)
        Backoff.onSuccess()
        assertFalse(Backoff.active)
    }

    @Test
    fun liveRateAndCost() {
        assertEquals(30_000L, AppSettings.liveDelayMs(30, 0))
        assertEquals(2_000L, AppSettings.liveDelayMs(2, 0))
        assertEquals("out-of-range rates are clamped", 2_000L, AppSettings.liveDelayMs(1, 0))
        assertEquals("failures slow it down", 8_000L, AppSettings.liveDelayMs(2, 2))
        assertEquals("but never past a minute", 60_000L, AppSettings.liveDelayMs(30, 3))
        assertEquals(1.11, AppSettings.liveMegabytesPerHour(30), 0.01)
        assertEquals(16.7, AppSettings.liveMegabytesPerHour(2), 0.1)
        assertTrue(AppSettings.liveMegabytesPerHour(5) > AppSettings.liveMegabytesPerHour(10))
        val defaults = AppSettings()
        assertEquals(30, defaults.liveIntervalSeconds)
        assertFalse(defaults.anonymousModeOn())
        assertTrue(AppSettings(anonymousUntil = AppSettings.ANONYMOUS_FOREVER).anonymousModeOn())
        assertFalse(AppSettings(anonymousUntil = 1_000L).anonymousModeOn(now = 2_000L))
        assertTrue(AppSettings(anonymousUntil = 3_000L).anonymousModeOn(now = 2_000L))
    }

    @Test
    fun askRulesMatchTheSite() {
        assertEquals(AskRules.Problem.Empty, AskRules.titleProblem("   "))
        assertEquals(AskRules.Problem.TooShort, AskRules.titleProblem("קצר מדי"))
        assertNull(AskRules.titleProblem("איך לומדים לבגרות במתמטיקה?"))
        assertEquals(AskRules.Problem.TooLong, AskRules.titleProblem("א".repeat(121)))
        assertEquals(AskRules.Problem.RepeatedLetters, AskRules.titleProblem("למה זה ככככה מסובך"))
        assertNull(AskRules.bodyProblem(""))
        assertEquals(AskRules.Problem.TooLong, AskRules.bodyProblem("x".repeat(2001)))
        assertTrue(AskRules.nicknameOk("אנונימי"))
        assertFalse(AskRules.nicknameOk("א"))
        assertFalse(AskRules.nicknameOk("x".repeat(19)))
        assertEquals("מתמטיקה", AskRules.cleanTag("  #מתמטיקה "))
        assertEquals("C++ קוד", AskRules.cleanTag("C++   קוד"))
        assertEquals(AskRules.Problem.TooLong, AskRules.penPalProblem("x".repeat(256)))
        assertNull(AskRules.penPalProblem("היי, מחפשת חברים לעט"))
    }

    @Test
    fun linksForQuestions() {
        assertEquals("https://example.com/a", AskRules.normalizeLink(" example.com/a "))
        assertEquals("http://x.co", AskRules.normalizeLink("http://x.co"))
        assertNull(AskRules.normalizeLink(""))
        assertNull(AskRules.normalizeLink("not a link"))
        assertTrue(AskRules.isYouTube("https://www.youtube.com/watch?v=1"))
        assertTrue(AskRules.isYouTube("https://youtu.be/1"))
        assertTrue(AskRules.isYouTube("https://m.youtube.com/watch?v=1"))
        assertFalse(AskRules.isYouTube("https://notyoutube.com/x"))
        assertFalse(AskRules.isYouTube(null))
    }

    @Test
    fun newNotifications() {
        val list = listOf(10L to true, 9L to false, 8L to true)
        assertEquals("first visit: only Stips' flag", setOf(9L), SeenNotifications.newIds(list, lastSeen = 0))
        assertEquals("later: also anything newer than what we showed", setOf(10L, 9L), SeenNotifications.newIds(list, lastSeen = 8))
        assertEquals(setOf(9L), SeenNotifications.newIds(list, lastSeen = 10))
    }

    @Test
    fun networkLogFormat() {
        val text = NetLog.format(
            listOf(NetLog.Entry(0L, "http", "objectlist -> 200"), NetLog.Entry(61_234L, "session", "signed in")),
            header = "Stips+ test",
            zone = ZoneOffset.UTC,
        )
        val lines = text.lines()
        assertEquals("Stips+ test", lines[0])
        assertTrue(lines[1].startsWith("Events: 2"))
        assertEquals("00:00:00.000  http     objectlist -> 200", lines[3])
        assertEquals("00:01:01.234  session  signed in", lines[4])
    }
}
