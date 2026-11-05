package io.github.eyalshimshi.betterstips.data.api

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * The time as Stips' servers see it, estimated from the Date header of their answers (#8). Times that count seconds
 * ("2m 05s") would show a wrong phone clock plainly, so a phone that's [THRESHOLD_MS] or more off is corrected.
 */
object ServerClock {
    /** Smaller differences are within the header's one-second resolution and the trip's own time. */
    const val THRESHOLD_MS = 2_000L

    @Volatile
    private var offsetMs = 0L

    fun nowMs(): Long = System.currentTimeMillis() + offsetMs

    fun now(): Instant = Instant.ofEpochMilli(nowMs())

    /** Records an answer's `Date` header, received at [receivedAtMs] on the phone's clock. */
    fun observe(dateHeader: String?, receivedAtMs: Long) {
        val serverMs = parseHttpDate(dateHeader) ?: return
        offsetMs = offset(serverMs, receivedAtMs)
    }

    /**
     * How far the phone's clock is behind the server's, or 0 when it's close enough. The header drops the
     * milliseconds, so the server's time is on average half a second later than it says. Pure.
     */
    fun offset(serverMs: Long, receivedAtMs: Long): Long {
        val difference = serverMs + 500 - receivedAtMs
        return if (abs(difference) < THRESHOLD_MS) 0L else difference
    }

    /** An HTTP date ("Thu, 05 Nov 2026 07:50:37 GMT") in epoch milliseconds, or null. Pure. */
    fun parseHttpDate(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        return try {
            ZonedDateTime.parse(text.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}
