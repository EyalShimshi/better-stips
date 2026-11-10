package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.util.TutorialRules

/**
 * Feature tips (#28): each shows until the user does what it teaches, or three times if they don't, and at most once
 * each time the app starts. Kept on this phone only.
 */
class Tutorials(context: Context) {
    enum class Tip {
        /** Swiping an answer to the right replies to it (easy reply). */
        AnswerReply,

        /** Swiping your own answer to the left runs the action picked in Settings (edit, by default). */
        MyAnswerSwipe,

        /** Swiping a chat message to the right replies to it. */
        ChatReply,

        /** Swiping a question in a list to the right, and to the left, runs the actions picked in Settings. */
        HomeSwipeRight,
        HomeSwipeLeft,

        /** A long press on the Live chip sets how often it checks. */
        LiveOptions,

        /** The first circle on the chats shelf is the chat with yourself. */
        NotesShelf,
    }

    private val prefs = context.getSharedPreferences("tutorials", Context.MODE_PRIVATE)
    private val shownThisRun = mutableSetOf<Tip>()

    /** [tip] can show now: not done, not yet shown three times, nor since the app started. */
    @Synchronized
    fun due(tip: Tip): Boolean =
        tip !in shownThisRun && TutorialRules.due(prefs.getInt(shownKey(tip), 0), prefs.getBoolean(doneKey(tip), false))

    /** Takes [tip]'s turn to show now, counting it; false when it isn't due. */
    @Synchronized
    fun claim(tip: Tip): Boolean {
        if (!due(tip)) return false
        shownThisRun += tip
        prefs.edit().putInt(shownKey(tip), prefs.getInt(shownKey(tip), 0) + 1).apply()
        return true
    }

    /** The user did what [tip] teaches: it doesn't show again. */
    fun done(tip: Tip) {
        if (!prefs.getBoolean(doneKey(tip), false)) prefs.edit().putBoolean(doneKey(tip), true).apply()
    }

    /** Settings → About: every tip shows again. */
    @Synchronized
    fun reset() {
        prefs.edit().clear().apply()
        shownThisRun.clear()
    }

    private fun shownKey(tip: Tip) = tip.name + ".shown"

    private fun doneKey(tip: Tip) = tip.name + ".done"
}
