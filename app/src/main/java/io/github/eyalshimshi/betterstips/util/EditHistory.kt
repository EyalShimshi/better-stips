package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.AnswerRevision
import java.time.Instant

/** An answer's versions from Stips' revisions, newest first, as the site's "גרסאות קודמות" shows them (v0.13, #10). Pure. */
object EditHistory {
    enum class Editor { Owner, Moderator, Stips, Unknown }

    data class Version(
        val text: String,
        val links: List<AnswerLink>,
        /** Who made the edit that produced this version; null for the original. */
        val editedBy: Editor?,
        val editedAt: Instant?,
        val editedAtText: String?,
        val current: Boolean,
    ) {
        val original: Boolean get() = editedBy == null
    }

    /** Stips' `editor`: "owner" (the one who answered), "moderator", or "platformAdmin" (Stips itself). */
    fun editor(value: String?): Editor = when (value?.trim()?.lowercase()) {
        "owner" -> Editor.Owner
        "moderator" -> Editor.Moderator
        "platformadmin" -> Editor.Stips
        else -> Editor.Unknown
    }

    /**
     * The versions, newest first: the current one, then each revision's. A revision keeps what its edit changed as it
     * was before; the rest shows as it is now, as on the site. The newest revision's edit made the current version, the
     * next one's made the version before it, and so on; the oldest version is the original.
     */
    fun versions(currentText: String, currentLinks: List<AnswerLink>, revisions: List<AnswerRevision>): List<Version> {
        val result = ArrayList<Version>(revisions.size + 1)
        result += version(currentText, currentLinks, revisions.getOrNull(0), current = true)
        revisions.forEachIndexed { i, revision ->
            result += version(revision.text ?: currentText, revision.links ?: currentLinks, revisions.getOrNull(i + 1), current = false)
        }
        return result
    }

    private fun version(text: String, links: List<AnswerLink>, madeBy: AnswerRevision?, current: Boolean) = Version(
        text = text,
        links = links,
        editedBy = madeBy?.let { editor(it.editor) },
        editedAt = madeBy?.time,
        editedAtText = madeBy?.timeText,
        current = current,
    )
}
