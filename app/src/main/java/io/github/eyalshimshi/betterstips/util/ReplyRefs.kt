package io.github.eyalshimshi.betterstips.util

/**
 * Stips' reply convention (v0.10). The site can't reply to an answer, so an answer points at the one it replies to with
 * carets, one per answer up the list ("^^" = two up), or a number and a caret for more ("4^", also "^4"). Mostly at
 * the start, on its own line or straight before the words; sometimes later in the text, and sometimes stacked
 * ("^^^" then "^^" on the next line) to answer two people at once. Answers count in Stips' order, oldest first
 * (SPEC-04 §2.12). Pure.
 */
object ReplyRefs {
    /** One pointer: [count] answers up, at [range] in the text; [leading] when it opens the answer. */
    data class Ref(val count: Int, val range: IntRange, val leading: Boolean)

    /** The pointers in a text, and where its words start after the opening pointers ([bodyStart], 0 without any). */
    data class Parsed(val refs: List<Ref>, val bodyStart: Int)

    /**
     * An answer read with its pointers resolved: [targets] (indexes in Stips' order, first pointer first, at most
     * [MAX_QUOTES]), the [body] to show (the text as written, or without its opening pointers when the user hides
     * them), and the pointers in the body, [inBody], as ranges of [body] with the index each points at.
     */
    data class Resolved(val targets: List<Int>, val body: String, val inBody: List<Pair<IntRange, Int>>)

    /** The most single carets Stips+ writes; from 4 up it writes the number, "4^". */
    const val MAX_CARETS = 3
    const val MAX_QUOTES = 3
    private const val MAX_COUNT = 999

    // Smileys made of carets ("^_^", "^.^", "^-^", "^o^") point nowhere.
    private val emoticon = Regex("""\^[_.\-~oOwW0]+\^""")

    // "4^" or "4 ^", "^4", or a run of carets. Not inside a word or a number ("x^2", "2^3"), not followed by more.
    private val token = Regex("""(?<![\p{L}\p{N}^])(?:(\d{1,3}) ?\^(?![\^\p{N}])|\^(\d{1,3})(?!\p{N})|\^{1,12}(?![\^\p{N}_]))""")
    private val spaces = Regex("""\s+""")

    private fun isSeparator(c: Char) = c.isWhitespace() || c == ',' || c == ':' || c == '-' || c == '\u2013'

    fun parse(text: String): Parsed {
        if ('^' !in text) return Parsed(emptyList(), 0)
        val masked = emoticon.replace(text) { " ".repeat(it.value.length) }
        val found = token.findAll(masked).mapNotNull { m ->
            val number = m.groups[1]?.value ?: m.groups[2]?.value
            val count = number?.toIntOrNull() ?: m.value.length
            if (count in 1..MAX_COUNT) m.range to count else null
        }.toList()
        var pos = 0
        while (pos < text.length && isSeparator(text[pos])) pos++
        var bodyStart = 0
        var leading = 0
        for ((range, _) in found) {
            if (range.first != pos) break
            leading++
            pos = range.last + 1
            while (pos < text.length && isSeparator(text[pos])) pos++
            bodyStart = pos
        }
        return Parsed(found.mapIndexed { i, (range, count) -> Ref(count, range, leading = i < leading) }, bodyStart)
    }

    /** How many answers up the answer's opening pointer points, or null when it doesn't open with one. */
    fun leadingCount(text: String): Int? = parse(text).refs.firstOrNull()?.takeIf { it.leading }?.count

    /** "^", "^^", "^^^", then "4^", "5^"… */
    fun prefix(count: Int): String = if (count <= MAX_CARETS) "^".repeat(count.coerceAtLeast(1)) else "$count^"

    /**
     * [text] replying to the answer [count] up: its opening pointer becomes [prefix]; without one, the pointer goes on
     * a line of its own above the text, as people write it.
     */
    fun withReply(text: String, count: Int): String {
        val first = parse(text).refs.firstOrNull()
        return if (first != null && first.leading) {
            text.replaceRange(first.range, prefix(count))
        } else {
            prefix(count) + "\n" + text.trimStart()
        }
    }

    /** [text] without its opening pointer and the space or line break after it (cancelling an easy reply). */
    fun withoutReply(text: String): String {
        val first = parse(text).refs.firstOrNull()?.takeIf { it.leading } ?: return text
        var end = first.range.last + 1
        while (end < text.length && isSeparator(text[end])) end++
        return text.removeRange(first.range.first, end)
    }

    /** The index answer [index] points at with [count] (both in Stips' order), or null above the first answer. */
    fun target(index: Int, count: Int): Int? = (index - count).takeIf { count >= 1 && it in 0 until index }

    /**
     * Reads the pointers of the answer at [index] (in Stips' order) against the answers above it. The text stays as
     * written, and each pointer that resolves is tappable (v0.11). With [hideLeading] (a setting, off by default), the
     * opening pointers are left out of [Resolved.body] when they all resolve, since the quotes show them.
     */
    fun resolve(text: String, index: Int, hideLeading: Boolean = false): Resolved {
        val parsed = parse(text)
        if (parsed.refs.isEmpty()) return Resolved(emptyList(), text, emptyList())
        val leading = parsed.refs.filter { it.leading }
        // Hidden only when they all point at answers, and never when they're all the answer says.
        val strip = hideLeading && leading.isNotEmpty() && leading.all { target(index, it.count) != null } &&
            parsed.bodyStart < text.length
        val shift = if (strip) parsed.bodyStart else 0
        val inBody = parsed.refs
            .filterNot { strip && it.leading }
            .mapNotNull { ref -> target(index, ref.count)?.let { (ref.range.first - shift)..(ref.range.last - shift) to it } }
        val targets = parsed.refs.mapNotNull { target(index, it.count) }.distinct().take(MAX_QUOTES)
        return Resolved(targets, if (strip) text.substring(parsed.bodyStart) else text, inBody)
    }

    /** A one-paragraph preview of an answer for a quote; with [hideLeading], without its own opening pointers. */
    fun snippet(text: String, max: Int = 160, hideLeading: Boolean = false): String {
        val start = if (hideLeading) parse(text).bodyStart else 0
        val body = if (start in 1 until text.length) text.substring(start) else text
        val flat = body.replace(spaces, " ").trim()
        return if (flat.length <= max) flat else flat.take(max).trimEnd() + "\u2026"
    }

    /**
     * A chat's last message as the chats list, the widget and message notifications show it (v0.17, build 30): without
     * its opening pointers and the line break after them, whatever the chat's own setting, on one line. A message
     * that's only a pointer stays as written. A Stips link on a line of its own is left out too, as its bubble shows
     * it as a card (v0.18).
     */
    fun preview(text: String): String = snippet(ChatLinks.withoutLink(text), PREVIEW_MAX, hideLeading = true)

    private const val PREVIEW_MAX = 200
}
