package io.github.eyalshimshi.betterstips.util

/**
 * Sending text to chats (v0.19): what another app shared to Stips+, or a message forwarded from a chat, goes to up to
 * [MAX_RECIPIENTS] chats at once, as WhatsApp does. Only plain text: Stips' messages are text. Pure.
 */
object ShareRules {
    /** The most chats one message goes to at once. Fixed, not a setting, so sharing can't be used to spam. */
    const val MAX_RECIPIENTS = 5

    /** The longest text taken: Stips' own pages don't limit a message, so a generous cap keeps a request sane. */
    const val MAX_TEXT = 4000

    // Control characters other than line breaks and tabs, and the placeholder rich text leaves where a picture was.
    private val unwanted = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\uFFFC]")
    private val lineBreaks = Regex("\\r\\n?")

    /**
     * [text] as plain text Stips can take: no formatting (only the characters), no control characters, Unix line
     * breaks, trimmed, at most [MAX_TEXT] characters (ending with "…" when cut). Null when nothing is left.
     */
    fun clean(text: CharSequence?): String? {
        if (text == null) return null
        val plain = unwanted.replace(lineBreaks.replace(text.toString(), "\n"), "").trim()
        if (plain.isEmpty()) return null
        if (plain.length <= MAX_TEXT) return plain
        var end = MAX_TEXT - 1
        // Don't cut an emoji or another two-unit character in half.
        if (Character.isHighSurrogate(plain[end - 1])) end--
        return plain.substring(0, end).trimEnd() + "\u2026"
    }

    /**
     * The chats picked after a tap on [id]: picked ones are let go, others added in order; null when [MAX_RECIPIENTS]
     * are picked already, so it can't be.
     */
    fun toggle(selected: List<Long>, id: Long): List<Long>? = when {
        id in selected -> selected - id
        selected.size >= MAX_RECIPIENTS -> null
        else -> selected + id
    }

    /**
     * A chat message as it's forwarded or shared: without the reply carets it opens with, which point at messages in
     * the chat it came from. A message that's only carets stays as written.
     */
    fun forwardText(text: String): String {
        val start = ReplyRefs.parse(text).bodyStart
        return if (start in 1 until text.length) text.substring(start).trim() else text.trim()
    }
}
