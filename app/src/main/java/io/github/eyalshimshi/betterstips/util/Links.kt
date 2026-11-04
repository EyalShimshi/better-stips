package io.github.eyalshimshi.betterstips.util

object Links {
    private val url = Regex("""https?://[^\s<>"'\u05f3\u05f4]+""", RegexOption.IGNORE_CASE)
    private val trailing = ".,;:!?)]}\u05be"
    private val questionLink = Regex("""^https?://(?:www\.)?stips\.co\.il/ask/(\d+)""", RegexOption.IGNORE_CASE)
    private val profileLink = Regex("""^https?://(?:www\.)?stips\.co\.il/profile/(\d+)""", RegexOption.IGNORE_CASE)

    /** Where the URLs are in [text], without trailing punctuation. */
    fun find(text: String): List<IntRange> = url.findAll(text).mapNotNull { match ->
        var end = match.range.last
        while (end > match.range.first && text[end] in trailing) end--
        val range = match.range.first..end
        if (text.substring(range).length > "https://".length) range else null
    }.toList()

    /** The question id in a Stips question link, or null. */
    fun questionId(link: String): Long? = questionLink.find(link.trim())?.groupValues?.get(1)?.toLongOrNull()

    /** The user id in a Stips profile link, or null. */
    fun profileId(link: String): Long? = profileLink.find(link.trim())?.groupValues?.get(1)?.toLongOrNull()

    /** "example.com" for a link, for compact labels. */
    fun host(link: String): String = Regex("""^https?://(?:www\.)?([^/?#]+)""", RegexOption.IGNORE_CASE)
        .find(link.trim())?.groupValues?.get(1) ?: link

    private val messagesLink = Regex("""^https?://(?:www\.)?stips\.co\.il/messages(?:[/?#].*)?$""", RegexOption.IGNORE_CASE)

    /** Where a Stips link leads inside the app, or null for links that belong in the browser. */
    fun deepLink(link: String?): DeepLink? {
        if (link.isNullOrBlank()) return null
        questionId(link)?.let { return DeepLink.Question(it) }
        profileId(link)?.let { return DeepLink.Profile(it) }
        if (messagesLink.matches(link.trim())) return DeepLink.Messages
        return null
    }
}

/** A place in the app that a link, a phone notification or a launcher shortcut opens. */
sealed interface DeepLink {
    data class Question(val id: Long) : DeepLink

    data class Profile(val id: Long) : DeepLink

    data object Messages : DeepLink

    /** The full Hot list (the launcher shortcut). */
    data object Hot : DeepLink
}

/** Launcher shortcuts (`res/xml/shortcuts.xml`): each opens MainActivity with its own action. */
object Shortcuts {
    const val ACTION_HOT = "io.github.eyalshimshi.betterstips.action.HOT"
    const val ACTION_CHATS = "io.github.eyalshimshi.betterstips.action.CHATS"
    const val ID_HOT = "hot"
    const val ID_CHATS = "chats"

    /** Where a shortcut's intent action leads, or null for any other intent. */
    fun link(action: String?): DeepLink? = when (action) {
        ACTION_HOT -> DeepLink.Hot
        ACTION_CHATS -> DeepLink.Messages
        else -> null
    }
}
