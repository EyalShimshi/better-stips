package io.github.eyalshimshi.betterstips.util

/** Small text helpers for what Stips sends as HTML (notifications) or as Hebrew phrases (account age). Pure, testable. */
object TextUtils {
    private val lineBreaks = Regex("(?i)<br\\s*/?>|</p>|</div>")
    private val tags = Regex("<[^>]+>")
    private val entity = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});")
    private val namedEntities = mapOf(
        "amp" to "&", "AMP" to "&", "lt" to "<", "LT" to "<", "gt" to ">", "GT" to ">", "quot" to "\"", "QUOT" to "\"",
        "apos" to "'", "nbsp" to " ", "ndash" to "\u2013", "mdash" to "\u2014", "hellip" to "\u2026", "laquo" to "\u00ab",
        "raquo" to "\u00bb", "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201c", "rdquo" to "\u201d",
        "bull" to "\u2022", "middot" to "\u00b7", "times" to "\u00d7", "divide" to "\u00f7", "deg" to "\u00b0",
        "copy" to "\u00a9", "reg" to "\u00ae", "trade" to "\u2122", "euro" to "\u20ac", "shy" to "\u00ad",
        "lrm" to "\u200e", "rlm" to "\u200f", "zwj" to "\u200d", "zwnj" to "\u200c",
    )

    /**
     * Decodes HTML character references once: "&gt;" → ">", "&#1488;" → "א", and "&amp;lt;" → "&lt;" (what its author
     * wrote). Stips stores what people write HTML-escaped, and its site decodes it on display, so every text from
     * Stips goes through here (v0.12). Unknown names and anything that isn't a reference stay as they are ("AT&T").
     */
    fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return entity.replace(text) { match ->
            val body = match.groupValues[1]
            val decoded = when {
                body.startsWith("#x") || body.startsWith("#X") -> codePoint(body.drop(2).toIntOrNull(16))
                body.startsWith("#") -> codePoint(body.drop(1).toIntOrNull())
                else -> namedEntities[body]
            }
            decoded ?: match.value
        }
    }

    private fun codePoint(code: Int?): String? =
        if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else null

    /** Strips tags and decodes the entities, once. */
    fun htmlToPlain(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val text = decodeEntities(html.replace(lineBreaks, "\n").replace(tags, ""))
        return text.lines().joinToString("\n") { it.trim() }.trim()
    }

    data class Span(val years: Int, val months: Int)

    private val yearsNumber = Regex("(\\d+)\\s*\u05e9\u05e0\u05d9\u05dd")
    private val monthsNumber = Regex("(\\d+)\\s*\u05d7\u05d5\u05d3\u05e9\u05d9\u05dd")

    /** Reads Stips' "6 שנים ו-9 חודשים" (also "שנה", "שנתיים", "חודש", "חודשיים"). Null when it can't tell. */
    fun parseHebrewSpan(text: String?): Span? {
        if (text.isNullOrBlank()) return null
        val years = yearsNumber.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: when {
                text.contains("\u05e9\u05e0\u05ea\u05d9\u05d9\u05dd") -> 2
                text.contains("\u05e9\u05e0\u05d4") -> 1
                else -> 0
            }
        val months = monthsNumber.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: when {
                text.contains("\u05d7\u05d5\u05d3\u05e9\u05d9\u05d9\u05dd") -> 2
                text.contains("\u05d7\u05d5\u05d3\u05e9") && !text.contains("\u05d7\u05d5\u05d3\u05e9\u05d9\u05dd") -> 1
                else -> 0
            }
        return if (years == 0 && months == 0) null else Span(years, months)
    }
}
