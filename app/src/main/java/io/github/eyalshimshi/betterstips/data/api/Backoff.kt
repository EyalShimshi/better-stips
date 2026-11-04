package io.github.eyalshimshi.betterstips.data.api

import io.github.eyalshimshi.betterstips.diag.NetLog

/**
 * When Stips (through its Cloudflare front) answers "too many requests" or refuses a request, the background
 * checks pause: Live, the unread badges and phone notifications. What the user taps still goes through, and the
 * first success lifts the pause (#3).
 */
object Backoff {
    private const val BASE_MS = 60_000L
    private const val MAX_MS = 15 * 60_000L
    private const val MAX_ASKED_S = 3_600L

    @Volatile
    private var until = 0L

    @Volatile
    private var strikes = 0

    fun remainingMs(now: Long = System.currentTimeMillis()): Long = (until - now).coerceAtLeast(0L)

    val active: Boolean get() = remainingMs() > 0L

    @Synchronized
    fun onLimited(httpCode: Int, retryAfterSeconds: Long?, now: Long = System.currentTimeMillis()) {
        strikes++
        val wait = delayMs(strikes, retryAfterSeconds)
        until = maxOf(until, now + wait)
        NetLog.event("backoff", "HTTP $httpCode: background checks pause for ${wait / 1000} s (strike $strikes)")
    }

    fun onSuccess() {
        if (strikes == 0 && until == 0L) return
        synchronized(this) {
            if (strikes > 0) NetLog.event("backoff", "Stips answered again; background checks resume")
            strikes = 0
            until = 0L
        }
    }

    /** 1, 2, 4, 8 minutes, then 15; a longer Retry-After from the server wins (capped at an hour). Pure. */
    fun delayMs(strikes: Int, retryAfterSeconds: Long?): Long {
        val exponential = (BASE_MS shl (strikes - 1).coerceIn(0, 4)).coerceAtMost(MAX_MS)
        val asked = (retryAfterSeconds ?: 0L).coerceIn(0L, MAX_ASKED_S) * 1000L
        return maxOf(exponential, asked)
    }
}
