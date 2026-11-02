package io.github.eyalshimshi.betterstips.util

/** Small text helpers for what Stips sends as HTML (notifications) or as Hebrew phrases (account age). Pure, testable. */
object TextUtils {
    private val lineBreaks = Regex("(?i)<br\\s*/?>|</p>|</div>")
    private val tags = Regex("<[^>]+>")
    private val numericEntity = Regex("&#(x?[0-9a-fA-F]+);")
    private val namedEntities = mapOf(
        "&quot;" to "\"", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&apos;" to "'", "&#39;" to "'", "&nbsp;" to " ",
    )

    /** Strips tags and decodes the common entities. */
    fun htmlToPlain(html: String?): String {
        if (html.isNullOrBlank()) return ""
        var text = html.replace(lineBreaks, "\n").replace(tags, "")
        namedEntities.forEach { (entity, value) -> text = text.replace(entity, value) }
        text = numericEntity.replace(text) { match ->
            val raw = match.groupValues[1]
            val code = if (raw.startsWith("x") || raw.startsWith("X")) raw.drop(1).toIntOrNull(16) else raw.toIntOrNull()
            if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else match.value
        }
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
