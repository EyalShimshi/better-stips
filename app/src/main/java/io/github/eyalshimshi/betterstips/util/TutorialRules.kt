package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.ChatMessage

/** Feature tips (#28): when one shows, and on what. Pure. */
object TutorialRules {
    /**
     * A tip shows once (D-149; three times until build 35): one that showed doesn't come back, after an update either,
     * since the count stays with the install. Settings → About → *Show the tips again* starts them over.
     */
    const val MAX_SHOWS = 1

    fun due(shown: Int, done: Boolean): Boolean = !done && shown < MAX_SHOWS

    /** The message a chat's easy-reply tip shows on: the other person's newest, or null while they haven't written. */
    fun chatTipTarget(messages: List<ChatMessage>, myId: Long?): ChatMessage? =
        if (myId == null) null else messages.lastOrNull { it.fromUserId != null && it.fromUserId != myId }

    /** The question a row of Home's lists stands for, by its key: its id, or "section|title|id" in the website-like layout. */
    fun questionIdOfKey(key: Any?): Long? = when (key) {
        is Long -> key
        is String -> if (key.startsWith("section|")) key.substringAfterLast('|').toLongOrNull() else null
        else -> null
    }
}
