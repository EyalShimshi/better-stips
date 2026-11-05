package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.StipsNotification

/**
 * What a Stips notification is about (v0.13). Stips' types are words like "new_ask_ans_registered" and "new_flower".
 * Its team's notices (a removed question or answer, a report's outcome, a note) carry the team's words (`msgText`,
 * which the site shows in a box of its own) and the credibility points taken away (`pointsRemoved`). The rule book
 * (p. 10) lists them: new answers, flowers, thanks-wall messages, removals by the team and notes. Pure.
 */
object NotificationKinds {
    enum class Kind { Answers, Flower, Thanks, Question, Message, Badge, Removal, Report, TeamNote, Other }

    private val removalTypes = listOf("delet", "remov", "hid", "violat", "penal", "punish")
    private val teamTypes = listOf("moder", "admin", "system", "staff", "team", "note", "warn")

    // "נמחק/ה/ו", "הוסר/ה/ו", "הוסתר/ה": removed, deleted, hidden.
    private val removalWords = Regex("נמחק|הוסר|הוסתר|removed|deleted", RegexOption.IGNORE_CASE)

    // "הדיווח שלך", "דיווחת", "דיווחך", "תודה על הדיווח": the user's own report, not one about them.
    private val reportWords = Regex("הדיווח שלך|דיווחת|דיווחך|תודה על הדיווח|your report|you reported", RegexOption.IGNORE_CASE)

    fun kind(n: StipsNotification): Kind = kind(n.type, n.text, n.note, n.pointsRemoved)

    /**
     * Points taken away always mean a removal. Otherwise the type decides; the words decide only for the team's notices
     * and types Stips+ doesn't know, since an ordinary notification quotes a question's title, which may say anything.
     */
    fun kind(type: String, text: String, note: String?, pointsRemoved: Int): Kind {
        val t = type.lowercase()
        if (pointsRemoved > 0) return Kind.Removal
        if ("report" in t) return Kind.Report
        if (removalTypes.any { it in t }) return Kind.Removal
        val usual = usualKind(t)
        val fromTeam = !note.isNullOrBlank()
        if (usual != null && !fromTeam) return usual
        val words = text + "\n" + note.orEmpty()
        return when {
            reportWords.containsMatchIn(words) -> Kind.Report
            removalWords.containsMatchIn(words) -> Kind.Removal
            fromTeam || teamTypes.any { it in t } -> Kind.TeamNote
            else -> usual ?: Kind.Other
        }
    }

    private fun usualKind(t: String): Kind? = when {
        "flower" in t -> Kind.Flower
        "thank" in t -> Kind.Thanks
        "ans" in t -> Kind.Answers
        "ask" in t || "question" in t -> Kind.Question
        "msg" in t || "message" in t -> Kind.Message
        "badge" in t || "rank" in t -> Kind.Badge
        else -> null
    }

    /** Sent by Stips' team: such notifications don't open at an answer, and say who they're from. */
    fun fromTeam(kind: Kind): Boolean = kind == Kind.Removal || kind == Kind.Report || kind == Kind.TeamNote
}
