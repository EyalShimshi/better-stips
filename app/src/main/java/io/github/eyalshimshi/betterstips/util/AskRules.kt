package io.github.eyalshimshi.betterstips.util

/** The site's rules for asking a question and for pen-pal posts (from its form validators). Pure, unit-tested. */
object AskRules {
    const val TITLE_MIN = 10
    const val TITLE_MAX = 120
    const val BODY_MAX = 2000
    const val TAGS_MAX = 5
    const val NICKNAME_MIN = 2
    const val NICKNAME_MAX = 18
    const val PEN_PAL_MAX = 255

    enum class Problem { Empty, TooShort, TooLong, RepeatedLetters }

    fun titleProblem(title: String): Problem? {
        val t = title.trim()
        return when {
            t.isEmpty() -> Problem.Empty
            t.length < TITLE_MIN -> Problem.TooShort
            t.length > TITLE_MAX -> Problem.TooLong
            AnswerRules.hasRepeatedLetters(t) -> Problem.RepeatedLetters
            else -> null
        }
    }

    fun bodyProblem(body: String): Problem? = when {
        body.length > BODY_MAX -> Problem.TooLong
        AnswerRules.hasRepeatedLetters(body) -> Problem.RepeatedLetters
        else -> null
    }

    fun nicknameOk(nickname: String): Boolean = nickname.trim().length in NICKNAME_MIN..NICKNAME_MAX

    fun penPalProblem(text: String): Problem? {
        val t = text.trim()
        return when {
            t.isEmpty() -> Problem.Empty
            t.length > PEN_PAL_MAX -> Problem.TooLong
            AnswerRules.hasRepeatedLetters(t) -> Problem.RepeatedLetters
            else -> null
        }
    }

    /** A tag as the site keeps it: trimmed, without a leading "#", inner spaces collapsed. */
    fun cleanTag(raw: String): String = raw.trim().removePrefix("#").trim().replace(Regex("\\s+"), " ")

    /** With a YouTube link the site skips the photo step (the video is the picture). */
    fun isYouTube(link: String?): Boolean {
        val host = link?.trim()?.let { Links.host(it) }?.lowercase() ?: return false
        return host == "youtube.com" || host == "m.youtube.com" || host == "youtu.be" || host.endsWith(".youtube.com")
    }

    /** The link as the site expects it: http(s) and a dotted host, or null when empty or not a link. */
    fun normalizeLink(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val withScheme = if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) t else "https://$t"
        val host = Links.host(withScheme)
        return if (host.contains('.') && !withScheme.contains(' ')) withScheme else null
    }
}
