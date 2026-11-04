package io.github.eyalshimshi.betterstips.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Content the user chose to hide (#4): single items, or everything by one user. Like Stips' own app, the list lives
 * on this phone (the site keeps `bc-item-…`/`bc-user-…` flags locally too); Stips is told, so it can show less.
 */
class BlockStore(context: Context) {
    private val prefs = context.getSharedPreferences("content_blocks", Context.MODE_PRIVATE)
    private val _version = MutableStateFlow(0)

    /** Changes whenever the list does, so screens can hide what was just blocked. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun isBlocked(objType: String, id: Long, authorId: Long?): Boolean =
        prefs.contains(itemKey(objType, id)) || (authorId != null && prefs.contains(userKey(authorId)))

    fun isUserBlocked(userId: Long): Boolean = prefs.contains(userKey(userId))

    fun block(objType: String, id: Long, authorId: Long?) {
        val editor = prefs.edit().putLong(itemKey(objType, id), System.currentTimeMillis())
        if (authorId != null) editor.putLong(userKey(authorId), System.currentTimeMillis())
        editor.apply()
        _version.value = _version.value + 1
    }

    val count: Int get() = prefs.all.size

    /** "Remove all content blocks", as in the site's settings. Message blocks are separate and stay. */
    fun clear() {
        prefs.edit().clear().apply()
        _version.value = _version.value + 1
    }

    companion object {
        fun itemKey(objType: String, id: Long) = "bc-item-$objType-$id"

        fun userKey(userId: Long) = "bc-user-$userId"
    }
}
