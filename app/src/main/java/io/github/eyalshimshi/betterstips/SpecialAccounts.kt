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
        /** A badge in several colors instead (0xFFRRGGBB each, toned like [color]), running from one to the next (#26). */
        val gradient: List<Int>? = null,
        /** An organization: it gets the blue check by its name. */
        val verified: Boolean = false,
        /**
         * One of Stips' own accounts, which Stips doesn't flag itself (#21): treated as its flagged system account is,
         * with the blue check and "Stips' own account".
         */
        val official: Boolean = false,
        /** The badge shows on the account's profile only, never by its name on questions, answers or chat cards (v0.19). */
        val profileOnly: Boolean = false,
    ) {
        val hasBadge: Boolean get() = badgeHe != null || badgeEn != null
    }

    /** Stips+'s own colors, as on the bio button: Stips green into blue and purple (#12). */
    val STIPS_PLUS_COLORS: List<Int> = listOf(0xFF09A872.toInt(), 0xFF2F7BF6.toInt(), 0xFF8E44EC.toInt())

    val ALL: List<Entry> = listOf(
        // ד"ר נדב דפני, who created Stips (#12).
        Entry(2042, badgeHe = "יוצר סטיפס", badgeEn = "Stips' creator", color = 0xFFD99A00.toInt()),
        // Eyal Shimshi, who made Stips+ (#26), in its colors, on his profile only (v0.19). The left-to-right mark keeps
        // the "+" after "Stips" in Hebrew, where it would otherwise show as "+Stips"; "סטיפס+" would read almost as
        // Stips' creator's.
        Entry(CREATOR_ID, badgeHe = "יוצר Stips+\u200E", badgeEn = "Stips+ creator", gradient = STIPS_PLUS_COLORS, profileOnly = true),
        // Sahar: emotional support volunteers (#8).
        Entry(10486, verified = true),
        // Net Knights (אבירי הרשת): online safety (#8).
        Entry(253000, verified = true),
        // "מערכת סטיפס", Stips' system account (#21).
        Entry(50916, official = true),
    )

    private val byId: Map<Long, Entry> = ALL.associateBy { it.userId }

    /** The organizations' accounts, for the blue check. */
    val verifiedIds: Set<Long> = ALL.filter { it.verified }.map { it.userId }.toSet()

    /** Stips' own accounts that Stips doesn't flag (#21). */
    val officialIds: Set<Long> = ALL.filter { it.official }.map { it.userId }.toSet()

    fun isOfficial(userId: Long?): Boolean = userId != null && userId in officialIds

    fun entry(userId: Long?): Entry? = userId?.let { byId[it] }

    /** The account's badge, when it has one, for its profile. Callers pass null for anonymous authors. */
    fun badge(userId: Long?): Entry? = entry(userId)?.takeIf { it.hasBadge }

    /** The badge by the account's name on its questions, answers and chat cards: not one for the profile only (v0.19). */
    fun contentBadge(userId: Long?): Entry? = badge(userId)?.takeIf { !it.profileOnly }

    /** Who made Stips+ (#26): Settings ends with a line about him and a button to his profile. */
    const val CREATOR_ID: Long = 275580
}
