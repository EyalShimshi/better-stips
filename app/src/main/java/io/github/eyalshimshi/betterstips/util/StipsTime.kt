package io.github.eyalshimshi.betterstips.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Stips sends local Israel time without a zone, for example "2026/11/02 16:59:56". */
object StipsTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d H:m:s")

    fun parse(text: String?): Instant? {
        if (text.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(text.trim(), FORMAT).atZone(ZONE).toInstant()
        } catch (e: Exception) {
            null
        }
    }

    /** A coarse "how long ago", for the UI to turn into words. */
    sealed interface Ago {
        data object JustNow : Ago
        data class Minutes(val count: Long) : Ago
        data class Hours(val count: Long) : Ago
        data class Days(val count: Long) : Ago
        data class OnDate(val date: LocalDate) : Ago
    }

    fun ago(then: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): Ago {
        val elapsed = Duration.between(then, now)
        val minutes = elapsed.toMinutes()
        return when {
            minutes < 1 -> Ago.JustNow
            minutes < 60 -> Ago.Minutes(minutes)
            minutes < 24 * 60 -> Ago.Hours(minutes / 60)
            minutes < 7 * 24 * 60 -> Ago.Days(minutes / (24 * 60))
            else -> Ago.OnDate(then.atZone(zone).toLocalDate())
        }
    }
}
