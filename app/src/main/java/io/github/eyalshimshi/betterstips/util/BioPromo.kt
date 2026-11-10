package io.github.eyalshimshi.betterstips.util

/**
 * "גם אני משתמש בסטיפס+" at the end of a bio (#12), with the app's page, so friends can find it. The line is added
 * below the bio as it is; nothing in it changes. Pure.
 */
object BioPromo {
    /** The app's page (#24, v0.18): it leads to the newest build, and to Google Play once the app is there. */
    const val LINK = "https://www.shimshii.com/stips-plus"

    /** The Google Play page v0.14–v0.17 wrote, from the application id; bios that have it still count. */
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=io.github.eyalshimshi.betterstips"

    /** Stips' limit for a bio: its profile page's form allows 1000 characters (#29; 250 until v0.17 was a guess). */
    const val BIO_MAX = 1000

    private val MARKERS = listOf("details?id=io.github.eyalshimshi.betterstips", "shimshii.com/stips-plus")

    /** The words of [line] without the link, in both forms: a bio keeps counting if the link was taken out (v0.16). */
    private val PHRASES = listOf("משתמש בסטיפס+", "משתמשת בסטיפס+")

    /** The words added, in Hebrew like Stips itself, gendered as the profile is. */
    fun line(female: Boolean): String = (if (female) "גם אני משתמשת בסטיפס+" else "גם אני משתמש בסטיפס+") + "\n" + LINK

    /**
     * The bio with the line below it, after a blank line; the bio itself stays as written. A bio that ends with its
     * quote (`~words~`, #29) keeps it last, under the line, so Stips still shows it as its quote.
     */
    fun appended(bio: String?, female: Boolean): String {
        val current = bio.orEmpty().trimEnd()
        if (current.isBlank()) return line(female)
        val quote = BioText.quoteLine(current)
        if (quote != null) {
            val above = current.trim().removeSuffix(quote).trimEnd()
            return above + "\n\n" + line(female) + "\n" + quote
        }
        return current + "\n\n" + line(female)
    }

    /**
     * The bio has the line, its words or its link (#12): the button isn't offered, and the profile shows a Stips+
     * badge (v0.16). Anyone could write it; the badge only says what the bio says.
     */
    fun has(bio: String?): Boolean = bio != null && (MARKERS.any { bio.contains(it) } || PHRASES.any { bio.contains(it) })

    /** How long the bio would be with the line. */
    fun lengthWith(bio: String?, female: Boolean): Int = appended(bio, female).length

    fun fits(bio: String?, female: Boolean): Boolean = lengthWith(bio, female) <= BIO_MAX
}
