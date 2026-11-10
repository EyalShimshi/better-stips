package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.SITE_URL

/**
 * Stips links in chat messages (v0.18): the first link to a Stips question or profile shows as a card in its bubble.
 * When the link has a line of its own, as "reply in private" writes it, the card takes its place, as a reply's quote
 * takes the place of its carets; a link inside a sentence stays where it is, with the card above the words. Pure.
 */
object ChatLinks {
    /** What a card shows: a question or a person, by the [link] as written. */
    sealed interface Target {
        val link: String

        data class Question(val id: Long, override val link: String) : Target

        data class Profile(val id: Long, override val link: String) : Target
    }

    /**
     * A message's text read for its card: the [target] (null without a Stips question or profile link), the [body] to
     * show, and the range of the text left out of it ([removed]: the link's line and the space around it), if any.
     */
    data class Read(val target: Target?, val body: String, val removed: IntRange?)

    /** The link "reply in private" writes for question [id]. */
    fun questionLink(id: Long): String = "$SITE_URL/ask/$id"

    /** A card's target for [link], or null when it isn't a Stips question or profile. */
    fun target(link: String): Target? {
        Links.questionId(link)?.let { return Target.Question(it, link) }
        Links.profileId(link)?.let { return Target.Profile(it, link) }
        return null
    }

    fun read(text: String): Read {
        val (range, target) = Links.find(text)
            .firstNotNullOfOrNull { range -> target(text.substring(range))?.let { range to it } }
            ?: return Read(null, text, null)
        val lineStart = text.lastIndexOf('\n', range.first - 1) + 1
        val lineEnd = text.indexOf('\n', range.last + 1).let { if (it < 0) text.length else it }
        val alone = text.substring(lineStart, range.first).isBlank() && text.substring(range.last + 1, lineEnd).isBlank()
        if (!alone) return Read(target, text, null)
        var start = lineStart
        var end = lineEnd
        when {
            // The first line: the space after it goes too, so the words start the bubble.
            start == 0 -> while (end < text.length && text[end].isWhitespace()) end++
            // The last line: the space before it goes too.
            end == text.length -> while (start > 0 && text[start - 1].isWhitespace()) start--
            // A line in between: with its line break.
            else -> end++
        }
        return Read(target, text.removeRange(start, end), start until end)
    }

    /** [range] of a text as it reads with [removed] left out, or null when it was in that part. */
    fun shift(range: IntRange, removed: IntRange?): IntRange? {
        if (removed == null || removed.isEmpty()) return range
        val length = removed.last - removed.first + 1
        return when {
            range.last < removed.first -> range
            range.first > removed.last -> (range.first - length)..(range.last - length)
            else -> null
        }
    }

    /**
     * [text] as the chats list, notifications and quotes show it: without a link that has a line of its own, unless
     * nothing but the link (and reply carets) would be left.
     */
    fun withoutLink(text: String): String {
        val read = read(text)
        if (read.removed == null) return text
        val start = ReplyRefs.parse(read.body).bodyStart.coerceAtMost(read.body.length)
        return if (read.body.substring(start).isBlank()) text else read.body
    }

    /**
     * A message about [link] ("reply in private"): the link on a line of its own first — after the reply carets, so an
     * easy reply still opens the message — then [text].
     */
    fun withLink(text: String, link: String): String {
        val start = ReplyRefs.parse(text).bodyStart.coerceIn(0, text.length)
        val pointers = text.substring(0, start).trim()
        val words = text.substring(start).trim()
        return listOf(pointers, link, words).filter { it.isNotEmpty() }.joinToString("\n")
    }
}
