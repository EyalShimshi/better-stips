package io.github.eyalshimshi.betterstips.util

/**
 * A bio as Stips shows it (#29, from the site's profile code): when it has more than one line and its last line is
 * `~words~`, that line is its "quote name", shown under the rest as "~ words ~", smaller and lighter. The first line
 * of that shape must be the last one, as the site's search finds only the first. Pure.
 */
object BioText {
    /** The bio's [text] and, when it ends with one, its [quote] (the words between the tildes, trimmed). */
    data class Parsed(val text: String, val quote: String?)

    private val quoteLine = Regex("^~(.+)~$", RegexOption.MULTILINE)
    private val lineBreak = Regex("\r\n|\r|\n")

    fun parse(bio: String): Parsed {
        val match = match(bio) ?: return Parsed(bio, null)
        val trimmed = bio.trim()
        return Parsed(trimmed.substring(0, match.range.first).trim(), match.groupValues[1].trim())
    }

    /** The bio's last line when it's its quote, as written ("~ words ~"), or null. */
    fun quoteLine(bio: String): String? = match(bio)?.value

    private fun match(bio: String): MatchResult? {
        val trimmed = bio.trim()
        if (trimmed.split(lineBreak).size <= 1) return null
        val match = quoteLine.find(trimmed) ?: return null
        if (match.range.last + 1 != trimmed.length || match.groupValues[1].isBlank()) return null
        return match
    }
}
