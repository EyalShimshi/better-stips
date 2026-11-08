package io.github.eyalshimshi.betterstips.util

/**
 * Stips keeps no emojis in questions and answers (#12). Its server stores their text in a way that can't hold them and
 * cuts the text at the first one, so an answer that starts with an emoji is posted empty. They're left out as they're
 * typed or pasted. Chats keep them: Stips stores messages differently, and its own chat has an emoji picker. Pure.
 *
 * Left out: every character outside the Basic Multilingual Plane (all the newer emojis, flags and skin tones, and rare
 * symbols the server can't store either); the older symbols that show as emojis by default (⌚, ☕, ⚽, ✅, ⭐…); any
 * symbol followed by the emoji selector U+FE0F, which asks for its emoji look (❤️, ☺️, 1️⃣); the selectors and the
 * keycap mark themselves; and a zero-width joiner next to anything left out. Symbols written as text (♥, ✓, ★, →)
 * stay.
 */
object EmojiFilter {
    data class Result(val text: String, val selectionStart: Int, val selectionEnd: Int, val removed: Boolean)

    private const val ZWJ = '\u200D'
    private const val TEXT_SELECTOR = '\uFE0E'
    private const val EMOJI_SELECTOR = '\uFE0F'
    private const val KEYCAP = '\u20E3'

    /** The Basic Multilingual Plane's characters that show as emojis unless asked otherwise (Unicode's Emoji_Presentation). */
    private val presentation: List<IntRange> = listOf(
        0x231A..0x231B, 0x23E9..0x23EC, 0x23F0..0x23F0, 0x23F3..0x23F3, 0x25FD..0x25FE, 0x2614..0x2615,
        0x2648..0x2653, 0x267F..0x267F, 0x2693..0x2693, 0x26A1..0x26A1, 0x26AA..0x26AB, 0x26BD..0x26BE,
        0x26C4..0x26C5, 0x26CE..0x26CE, 0x26D4..0x26D4, 0x26EA..0x26EA, 0x26F2..0x26F3, 0x26F5..0x26F5,
        0x26FA..0x26FA, 0x26FD..0x26FD, 0x2705..0x2705, 0x270A..0x270B, 0x2728..0x2728, 0x274C..0x274C,
        0x274E..0x274E, 0x2753..0x2755, 0x2757..0x2757, 0x2795..0x2797, 0x27B0..0x27B0, 0x27BF..0x27BF,
        0x2B1B..0x2B1C, 0x2B50..0x2B50, 0x2B55..0x2B55,
    )

    private fun showsAsEmoji(c: Char): Boolean = presentation.any { c.code in it }

    /** [text] without emojis. */
    fun strip(text: String): String = strip(text, text.length, text.length).text

    /** Whether [text] has anything [strip] would leave out. */
    fun hasEmoji(text: String): Boolean = strip(text, 0, 0).removed

    /** [text] without emojis, with a selection (or the cursor) moved to where it lands in the shorter text. */
    fun strip(text: String, selectionStart: Int, selectionEnd: Int): Result {
        val drop = BooleanArray(text.length)
        var any = false
        for (i in text.indices) {
            val c = text[i]
            // A symbol or digit followed by the emoji selector asks for its emoji look; a letter can't have one.
            val emojiStyled = text.getOrNull(i + 1) == EMOJI_SELECTOR && !c.isLetter()
            if (c.isSurrogate() || showsAsEmoji(c) || c == EMOJI_SELECTOR || c == TEXT_SELECTOR || c == KEYCAP || emojiStyled) {
                drop[i] = true
                any = true
            }
        }
        if (!any) return Result(text, selectionStart, selectionEnd, removed = false)
        for (i in text.indices) {
            if (text[i] == ZWJ && (drop.getOrElse(i - 1) { false } || drop.getOrElse(i + 1) { false })) drop[i] = true
        }
        val kept = StringBuilder(text.length)
        for (i in text.indices) if (!drop[i]) kept.append(text[i])
        fun mapped(index: Int): Int {
            val end = index.coerceIn(0, text.length)
            var removedBefore = 0
            for (i in 0 until end) if (drop[i]) removedBefore++
            return end - removedBefore
        }
        return Result(kept.toString(), mapped(selectionStart), mapped(selectionEnd), removed = true)
    }
}
