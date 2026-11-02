package io.github.eyalshimshi.betterstips.util

/** The checks Stips' own answer form runs before posting, so we never send what the server rejects. */
object AnswerRules {
    const val MAX_LENGTH = 10_000

    enum class Problem { Empty, TooLong, RepeatedLetters }

    // From the site's "doubleLetters" validator: no Hebrew letter three times in a row (ח and "n" four times).
    private val stripped = Regex("[><]")
    private val repeatedLetter = Regex("([\u05d0-\u05d6\u05d8-\u05ea])\\1{2,}")
    private val repeatedHet = Regex("([\u05d7n])\\1{3,}")

    fun hasRepeatedLetters(text: String): Boolean {
        val cleaned = text.replace(stripped, "").replace("\u05de\u05de\u05de\u05e9", "")
        return repeatedLetter.containsMatchIn(cleaned) || repeatedHet.containsMatchIn(cleaned)
    }

    fun check(text: String, maxLength: Int = MAX_LENGTH): Problem? = when {
        text.isBlank() -> Problem.Empty
        text.length > maxLength -> Problem.TooLong
        hasRepeatedLetters(text) -> Problem.RepeatedLetters
        else -> null
    }
}
