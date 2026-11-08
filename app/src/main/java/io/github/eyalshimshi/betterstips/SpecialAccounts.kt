package io.github.eyalshimshi.betterstips

/**
 * Stips+'s special accounts (#12): a badge by someone's name on their profile, questions and answers, and the blue
 * check of organizations that work inside Stips. To add or change one, edit [ALL]; it reaches phones with the next
 * release. It's code on purpose, not a file downloaded at run time: nobody can put a label on an account without
 * a signed release (#14).
 */
object SpecialAccounts {
    data class Entry(
        /** The Stips user id, as in `https://stips.co.il/profile/<id>`. */
        val userId: Long,
        /** The badge's words in Hebrew and in English; keep them short (about 20 characters at most). */
        val badgeHe: String? = null,
        val badgeEn: String? = null,
        /** The badge's color as 0xFFRRGGBB (the app tones it for light and dark themes); null for the app's accent. */
        val color: Int? = null,
        /** An organization: it gets the blue check by its name. */
        val verified: Boolean = false,
    ) {
        val hasBadge: Boolean get() = badgeHe != null || badgeEn != null
    }

    val ALL: List<Entry> = listOf(
        // ד"ר נדב דפני, who created Stips (#12).
        Entry(2042, badgeHe = "יוצר סטיפס", badgeEn = "Stips' creator", color = 0xFFD99A00.toInt()),
        // Sahar: emotional support volunteers (#8).
        Entry(10486, verified = true),
        // Net Knights (אבירי הרשת): online safety (#8).
        Entry(253000, verified = true),
    )

    private val byId: Map<Long, Entry> = ALL.associateBy { it.userId }

    /** The organizations' accounts, for the blue check. */
    val verifiedIds: Set<Long> = ALL.filter { it.verified }.map { it.userId }.toSet()

    fun entry(userId: Long?): Entry? = userId?.let { byId[it] }

    /** The account's badge, when it has one. Callers pass null for anonymous authors. */
    fun badge(userId: Long?): Entry? = entry(userId)?.takeIf { it.hasBadge }
}
