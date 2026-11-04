package io.github.eyalshimshi.betterstips.data.api

import io.github.eyalshimshi.betterstips.diag.NetLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Paces the app and handles Stips' per-address limit (#5). Stips answers `IP_RATE_LIMIT` ("ip … reached rate
 * limit") to every call from an address that sent about 100 calls without its site's Referer, at any pace from 150 s
 * to 6 minutes. Calls with the Referer aren't limited that way (test clients passed 200 in under 5 minutes; the
 * official app made 245 in 4½), and Stips+ sends it (D-49, SPEC-04 §2.10). So this is a safety net and a courtesy:
 *
 * - **A request budget** close to the official app's pace: 40 at once, then one more every 2 seconds (30 a minute,
 *   at most 190 in any five minutes). Automatic checks (Live, badges, phone notifications) run only while at least
 *   half of it is left, so they give way to what the user does; the user's requests wait a moment when it's empty.
 * - **A pause** if Stips limits us anyway (`IP_RATE_LIMIT`, HTTP 429/403): nothing is sent, except one try a
 *   minute to see whether it's over. The first normal answer to a request sent after the refusal ends it.
 *
 * Both write to the network log what the next report needs: how many requests went out before a refusal, and
 * how long the refusal lasted.
 */
object RateGuard {
    const val CAPACITY = 40.0
    const val REFILL_MS = 2_000L
    const val BACKGROUND_FLOOR = 20.0

    /** How many of the user's requests may queue for budget (about 10 s); beyond that they're refused at once. */
    const val MAX_DEBT = 5.0
    const val PROBE_MS = 60_000L

    /**
     * Refusals this close together are one refusal: every request already on its way comes back refused at once
     * (6 within 300 ms in #5). Longer than a call may take (30 s), shorter than the minute between tries.
     */
    const val STRIKE_GAP_MS = 45_000L

    private const val BASE_PAUSE_MS = 60_000L
    private const val MAX_PAUSE_MS = 15 * 60_000L
    private const val MAX_ASKED_S = 3_600L
    private const val HISTORY = 512

    private var tokens = CAPACITY
    private var refilledAt = 0L
    @Volatile private var pausedUntil = 0L
    private var nextProbeAt = 0L
    @Volatile private var strikes = 0
    private var lastStrikeAt = 0L
    private var limitedSince = 0L
    private var lastRefusalLog = 0L
    private val _pausedUntil = MutableStateFlow(0L)

    // When the last [HISTORY] requests went out, for the log line written when Stips refuses.
    private val sentAt = LongArray(HISTORY)
    private var sentTotal = 0L
    private var countingSince = System.currentTimeMillis()

    /** When the current pause ends (epoch ms), or 0: for the banner. */
    val pausedUntilFlow: StateFlow<Long> = _pausedUntil.asStateFlow()

    val paused: Boolean get() = pausedUntil > System.currentTimeMillis()

    /**
     * Before every request. Returns how long to wait (ms) for budget; throws [ApiException.RATE_LIMITED] during a
     * pause (except for the once-a-minute try) and [ApiException.THROTTLED] when too many requests already wait.
     */
    @Synchronized
    fun acquire(now: Long = System.currentTimeMillis()): Long {
        if (pausedUntil > now) {
            if (now < nextProbeAt) {
                if (now - lastRefusalLog > 10_000L) {
                    lastRefusalLog = now
                    NetLog.event("limit", "not sent: Stips paused this phone (${(pausedUntil - now) / 1000} s left)")
                }
                throw ApiException(ApiException.RATE_LIMITED)
            }
            nextProbeAt = now + PROBE_MS
            NetLog.event("limit", "trying once to see whether Stips' pause is over")
            record(now)
            return 0L
        }
        refill(now)
        if (tokens - 1 < -MAX_DEBT) throw ApiException(ApiException.THROTTLED)
        tokens -= 1
        record(now)
        return if (tokens >= 0) 0L else (-tokens * REFILL_MS).toLong()
    }

    /** Whether an automatic check may run now. Doesn't spend budget; [acquire] does when it's sent. */
    @Synchronized
    fun allowBackground(now: Long = System.currentTimeMillis()): Boolean {
        refill(now)
        return pausedUntil <= now && tokens >= BACKGROUND_FLOOR
    }

    /** Stips refused a request because of the limit. */
    @Synchronized
    fun onLimited(reason: String, retryAfterSeconds: Long?, now: Long = System.currentTimeMillis()) {
        if (strikes > 0 && now - lastStrikeAt < STRIKE_GAP_MS) return
        if (strikes == 0) {
            limitedSince = now
            NetLog.event(
                "limit",
                "before this refusal: ${sentWithin(60_000L, now)} requests in the last minute, " +
                    "${sentWithin(5 * 60_000L, now)} in 5 minutes, ${sentWithin(10 * 60_000L, now)} in 10 minutes " +
                    "($sentTotal since ${clock(now - countingSince)} ago)",
            )
        }
        strikes++
        lastStrikeAt = now
        val wait = delayMs(strikes, retryAfterSeconds)
        pausedUntil = maxOf(pausedUntil, now + wait)
        nextProbeAt = now + PROBE_MS
        // Start slowly once it's over.
        tokens = 0.0
        refilledAt = now
        _pausedUntil.value = pausedUntil
        NetLog.event("limit", "$reason: no requests for up to ${wait / 1000} s, one try a minute (refusal $strikes)")
    }

    /**
     * Stips answered a request normally, so any pause is over. An answer to a request sent before Stips started
     * refusing says nothing about that, so it's ignored.
     */
    fun onSuccess(sentAt: Long, now: Long = System.currentTimeMillis()) {
        if (strikes == 0 && pausedUntil == 0L) return
        synchronized(this) {
            if (strikes == 0 && pausedUntil == 0L) return
            if (sentAt < lastStrikeAt) return
            NetLog.event("limit", "Stips answers again, ${clock(now - limitedSince)} after it started refusing; the pause is over")
            strikes = 0
            pausedUntil = 0L
            lastStrikeAt = 0L
            limitedSince = 0L
            _pausedUntil.value = 0L
        }
    }

    private fun refill(now: Long) {
        if (refilledAt == 0L) refilledAt = now
        val gained = (now - refilledAt).toDouble() / REFILL_MS
        if (gained > 0) {
            tokens = minOf(CAPACITY, tokens + gained)
            refilledAt = now
        }
    }

    private fun record(at: Long) {
        sentAt[(sentTotal % HISTORY).toInt()] = at
        sentTotal++
    }

    /** Requests sent in the [windowMs] up to [now], as far as the history reaches. */
    @Synchronized
    fun sentWithin(windowMs: Long, now: Long = System.currentTimeMillis()): Int {
        val known = minOf(sentTotal, HISTORY.toLong()).toInt()
        var count = 0
        for (i in 0 until known) if (sentAt[i] in (now - windowMs)..now) count++
        return count
    }

    /** Back to a full budget, no pause and no history (tests). */
    @Synchronized
    fun reset(now: Long = System.currentTimeMillis()) {
        tokens = CAPACITY
        refilledAt = now
        pausedUntil = 0L
        nextProbeAt = 0L
        strikes = 0
        lastStrikeAt = 0L
        limitedSince = 0L
        sentTotal = 0L
        countingSince = now
        _pausedUntil.value = 0L
    }

    /** 1, 2, 4, 8 minutes, then 15; a longer Retry-After from the server wins (capped at an hour). Pure. */
    fun delayMs(strikes: Int, retryAfterSeconds: Long?): Long {
        val exponential = (BASE_PAUSE_MS shl (strikes - 1).coerceIn(0, 4)).coerceAtMost(MAX_PAUSE_MS)
        val asked = (retryAfterSeconds ?: 0L).coerceIn(0L, MAX_ASKED_S) * 1000L
        return maxOf(exponential, asked)
    }

    /** "m:ss". Pure. */
    fun clock(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) + 500L) / 1000L
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }
}
