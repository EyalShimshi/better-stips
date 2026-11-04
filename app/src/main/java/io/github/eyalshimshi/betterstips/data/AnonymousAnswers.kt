package io.github.eyalshimshi.betterstips.data

import android.content.Context

/**
 * Questions the user answered anonymously from this phone, so their next answer there starts anonymous too (#4).
 * Stips also marks the user's own anonymous answers (`meta.permissions.itemOwner`), which covers other devices.
 */
class AnonymousAnswers(context: Context) {
    private val prefs = context.getSharedPreferences("anonymous_answers", Context.MODE_PRIVATE)

    fun contains(questionId: Long): Boolean = prefs.contains(questionId.toString())

    fun remember(questionId: Long) {
        val editor = prefs.edit().putLong(questionId.toString(), System.currentTimeMillis())
        val all = prefs.all
        if (all.size >= MAX_KEPT) {
            // Forget the oldest ones; a few hundred questions is plenty.
            all.entries.sortedBy { (it.value as? Long) ?: 0L }.take(all.size - MAX_KEPT + 1).forEach { editor.remove(it.key) }
        }
        editor.apply()
    }

    private companion object {
        const val MAX_KEPT = 500
    }
}
