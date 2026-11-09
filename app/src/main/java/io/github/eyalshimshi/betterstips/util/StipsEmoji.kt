package io.github.eyalshimshi.betterstips.util

/**
 * Stips' legacy emoji codes in private messages (#22). Real emoji work in chats, and they're the way to send them.
 * Older apps also sent `:name:` codes, which the website still draws in chats only, from EmojiOne 2.2.7's list
 * (bundled as [ASSET]). Stips+ shows each as the phone's own emoji, and never writes codes itself. As the site does, it
 * also drops the even older `{ic:12}` codes.
 */
object StipsEmoji {
    /** The list, under `assets/`: the name without colons, a space, the code points in hex joined by "-". */
    const val ASSET = "stips_emoji.txt"

    /** The longest name on the list has 59 characters: a longer run between two colons is never a code. */
    private const val MAX_NAME = 64

    private val OLD_CODES = Regex("""\{ic:[0-9]+\}""")

    @Volatile
    private var loader: (() -> Map<String, String>)? = null

    @Volatile
    private var table: Map<String, String>? = null

    /** Where the list comes from. It's read once, the first time a message needs it (or [warmUp]). */
    fun source(load: () -> Map<String, String>) {
        loader = load
    }

    /** Puts [map] in use at once (tests). */
    fun install(map: Map<String, String>) {
        table = map
    }

    /** Reads the list now, so the first chat doesn't wait for it. Call it off the main thread. */
    fun warmUp() {
        table()
    }

    /** The list's lines as a map from the name to its emoji; comments, blank lines and bad lines are skipped. */
    fun parse(lines: Sequence<String>): Map<String, String> {
        val map = HashMap<String, String>(3000)
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val space = line.indexOf(' ')
            if (space <= 0) continue
            val emoji = decode(line.substring(space + 1).trim()) ?: continue
            map.putIfAbsent(line.substring(0, space), emoji)
        }
        return map
    }

    private fun decode(hex: String): String? {
        val out = StringBuilder()
        for (part in hex.split('-')) {
            val codePoint = part.toIntOrNull(16) ?: return null
            if (!Character.isValidCodePoint(codePoint)) return null
            out.appendCodePoint(codePoint)
        }
        return out.toString().ifEmpty { null }
    }

    private fun table(): Map<String, String> {
        table?.let { return it }
        val load = loader ?: return emptyMap()
        synchronized(this) {
            table?.let { return it }
            val loaded = try {
                load()
            } catch (e: Exception) {
                emptyMap()
            }
            table = loaded
            return loaded
        }
    }

    /**
     * A chat message as the site shows it: `{ic:12}` codes gone, and each `:name:` on the list made the emoji it names.
     * As in the site's search, each colon may open a code that ends at the next one, so "10:30:nerd:" is "10:30🤓". The
     * site finds codes in any case but knows them in lower case only, so ":NERD:" stays as written.
     */
    fun inChat(text: String): String {
        val plain = if (text.contains("{ic:")) OLD_CODES.replace(text, "") else text
        var open = plain.indexOf(':')
        if (open < 0) return plain
        val table = table()
        if (table.isEmpty()) return plain
        val out = StringBuilder(plain.length)
        var copied = 0
        while (open >= 0) {
            val close = plain.indexOf(':', open + 1)
            if (close < 0) break
            val length = close - open - 1
            val name = if (length in 1..MAX_NAME) plain.substring(open + 1, close) else null
            val emoji = name?.let { table[it] }
            if (emoji != null) {
                out.append(plain, copied, open).append(emoji)
                copied = close + 1
                open = plain.indexOf(':', copied)
            } else if (name != null && name != name.lowercase() && table.containsKey(name.lowercase())) {
                // The site's search takes the whole code and gives it back unchanged, colons included.
                open = plain.indexOf(':', close + 1)
            } else {
                open = close
            }
        }
        if (copied == 0) return plain
        out.append(plain, copied, plain.length)
        return out.toString()
    }
}