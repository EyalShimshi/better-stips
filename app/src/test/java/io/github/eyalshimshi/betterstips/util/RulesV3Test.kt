package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.diag.NetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.ZoneOffset

/** Pure rules added in v0.3 and v0.4: the request budget and pauses, Live's rate and cost, the ask form, new notifications, the network log. */
class RulesV3Test {

    @Test
    fun pausesGrowUpToFifteenMinutesAndHonorRetryAfter() {
        assertEquals(60_000L, RateGuard.delayMs(1, null))
        assertEquals(120_000L, RateGuard.delayMs(2, null))
        assertEquals(480_000L, RateGuard.delayMs(4, null))
        assertEquals(900_000L, RateGuard.delayMs(5, null))
        assertEquals(900_000L, RateGuard.delayMs(12, null))
        assertEquals("a longer Retry-After wins", 1_200_000L, RateGuard.delayMs(1, 1_200))
        assertEquals("but never more than an hour", 3_600_000L, RateGuard.delayMs(1, 99_999))
        assertEquals(60_000L, RateGuard.delayMs(1, 5))
    }

    @Test
    fun requestBudgetKeepsUsUnderStipsLimit() {
        val t0 = 1_000_000L
        RateGuard.reset(t0)
        repeat(30) { assertEquals("30 at once", 0L, RateGuard.acquire(t0)) }
        assertFalse("automatic checks give way when the budget is low", RateGuard.allowBackground(t0))
        assertEquals("then the user's requests wait for budget", 5_000L, RateGuard.acquire(t0))
        assertEquals(10_000L, RateGuard.acquire(t0))
        assertEquals(15_000L, RateGuard.acquire(t0))
        try {
            RateGuard.acquire(t0)
            fail("too many waiting: refused at once")
        } catch (e: ApiException) {
            assertEquals(ApiException.THROTTLED, e.code)
        }
        assertTrue("12 a minute come back", RateGuard.allowBackground(t0 + 150_000L))
        RateGuard.reset()
    }

    @Test
    fun pauseRefusesThenTriesOnceAMinute() {
        val t0 = 2_000_000L
        RateGuard.reset(t0)
        repeat(6) { RateGuard.onLimited("IP_RATE_LIMIT", null, now = t0 + it * 60L) }
        assertEquals("requests already on their way, refused together, are one refusal", t0 + 60_000L, RateGuard.pausedUntilFlow.value)
        RateGuard.onSuccess(sentAt = t0 - 200L, now = t0 + 400L)
        assertEquals("a late answer to a request sent before the refusal doesn't end it", t0 + 60_000L, RateGuard.pausedUntilFlow.value)
        assertFalse(RateGuard.allowBackground(t0 + 1_000L))
        try {
            RateGuard.acquire(t0 + 1_000L)
            fail("nothing is sent during a pause")
        } catch (e: ApiException) {
            assertEquals(ApiException.RATE_LIMITED, e.code)
        }
        assertEquals("after the pause requests go again", 0L, RateGuard.acquire(t0 + 60_000L))
        RateGuard.onLimited("IP_RATE_LIMIT", null, now = t0 + 60_100L)
        assertEquals("refused again: two minutes", t0 + 180_100L, RateGuard.pausedUntilFlow.value)
        try {
            RateGuard.acquire(t0 + 61_000L)
            fail("not before the next try")
        } catch (e: ApiException) {
            assertEquals(ApiException.RATE_LIMITED, e.code)
        }
        assertEquals("one try a minute", 0L, RateGuard.acquire(t0 + 120_100L))
        try {
            RateGuard.acquire(t0 + 121_000L)
            fail("and then not again for another minute")
        } catch (e: ApiException) {
            assertEquals(ApiException.RATE_LIMITED, e.code)
        }
        assertEquals("the log counts what was sent", 2, RateGuard.sentWithin(5 * 60_000L, now = t0 + 121_000L))
        RateGuard.onSuccess(sentAt = t0 + 120_100L, now = t0 + 120_400L)
        assertEquals("a normal answer to the try ends the pause", 0L, RateGuard.pausedUntilFlow.value)
        assertFalse(RateGuard.paused)
        assertEquals("2:00", RateGuard.clock(120_000L))
        assertEquals("0:05", RateGuard.clock(4_600L))
        RateGuard.reset()
    }

    @Test
    fun liveRateAndCost() {
        assertEquals(30_000L, AppSettings.liveDelayMs(30, 0))
        assertEquals(10_000L, AppSettings.liveDelayMs(10, 0))
        assertEquals("faster than 10 s isn't allowed (#5)", 10_000L, AppSettings.liveDelayMs(2, 0))
        assertEquals("failures slow it down", 40_000L, AppSettings.liveDelayMs(10, 2))
        assertEquals("but never past a minute", 60_000L, AppSettings.liveDelayMs(30, 3))
        assertEquals(1.11, AppSettings.liveMegabytesPerHour(30), 0.01)
        assertEquals(3.34, AppSettings.liveMegabytesPerHour(10), 0.01)
        assertEquals(AppSettings.liveMegabytesPerHour(10), AppSettings.liveMegabytesPerHour(2), 0.0)
        assertTrue(AppSettings.liveMegabytesPerHour(15) > AppSettings.liveMegabytesPerHour(20))
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
