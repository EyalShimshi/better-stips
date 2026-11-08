package io.github.eyalshimshi.betterstips.util

/**
 * "גם אני משתמש בסטיפס+" at the end of a bio (#12), with the app's Google Play page, so friends can find it. The
 * line is added below the bio as it is; nothing in it changes. Pure.
 */
object BioPromo {
    /** The app's page on Google Play, from its application id (the page exists once the app is published there). */
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=io.github.eyalshimshi.betterstips"

    /** Stips' limit for a bio, as its form has it. */
    const val BIO_MAX = 250

    private const val MARKER = "details?id=io.github.eyalshimshi.betterstips"

    /** The words added, in Hebrew like Stips itself, gendered as the profile is. */
    fun line(female: Boolean): String = (if (female) "גם אני משתמשת בסטיפס+" else "גם אני משתמש בסטיפס+") + "\n" + PLAY_URL

    /** The bio with the line below it, after a blank line; the bio itself stays as written. */
    fun appended(bio: String?, female: Boolean): String {
        val current = bio.orEmpty().trimEnd()
        return if (current.isEmpty()) line(female) else current + "\n\n" + line(female)
    }

    /** The bio already has the line (or the link), so the button isn't offered. */
    fun has(bio: String?): Boolean = bio?.contains(MARKER) == true

    /** How long the bio would be with the line. */
    fun lengthWith(bio: String?, female: Boolean): Int = appended(bio, female).length

    fun fits(bio: String?, female: Boolean): Boolean = lengthWith(bio, female) <= BIO_MAX
}
