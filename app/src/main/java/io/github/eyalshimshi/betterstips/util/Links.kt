package io.github.eyalshimshi.betterstips.util

object Links {
    private val url = Regex("""https?://[^\s<>"'\u05f3\u05f4]+""", RegexOption.IGNORE_CASE)
    private val trailing = ".,;:!?)]}\u05be"
    private val questionLink = Regex("""^https?://(?:www\.)?stips\.co\.il/ask/(\d+)""", RegexOption.IGNORE_CASE)

    /** Where the URLs are in [text], without trailing punctuation. */
    fun find(text: String): List<IntRange> = url.findAll(text).mapNotNull { match ->
        var end = match.range.last
        while (end > match.range.first && text[end] in trailing) end--
        val range = match.range.first..end
        if (text.substring(range).length > "https://".length) range else null
    }.toList()

    /** The question id in a Stips question link, or null. */
    fun questionId(link: String): Long? = questionLink.find(link.trim())?.groupValues?.get(1)?.toLongOrNull()
}
