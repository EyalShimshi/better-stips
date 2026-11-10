package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.ChatMessage

/** Feature tips (#28): when one shows, and on what. Pure. */
object TutorialRules {
    /** A tip shows until the user does what it teaches, or this many times when they don't. */
    const val MAX_SHOWS = 3

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
