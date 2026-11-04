package io.github.eyalshimshi.betterstips.data.api

/** A failure talking to Stips, with a code the UI can turn into plain words. */
class ApiException(val code: String, val serverMessage: String? = null) : Exception("$code ${serverMessage.orEmpty()}".trim()) {
    /** No answer at all: offline, a timeout, a dropped connection. */
    val isNetwork: Boolean get() = code == NETWORK

    /** Stips didn't serve this request, but nothing says the session is gone: never sign out for these (#3). */
    val isTransient: Boolean get() = code in TRANSIENT

    /** Stips said the user isn't signed in ("המשתמש אינו מחובר למערכת", "יש להתחבר"). */
    val looksSignedOut: Boolean
        get() = code == NOT_SIGNED_IN || serverMessage?.let { SIGNED_OUT_WORDS.any { word -> word in it } } == true

    companion object {
        const val NETWORK = "NETWORK"
        const val HTTP = "HTTP"

        /** HTTP 429: too many requests from this phone's address. */
        const val RATE_LIMITED = "RATE_LIMITED"

        /** HTTP 403: Stips' Cloudflare front refused the request. */
        const val BLOCKED = "BLOCKED"

        /** HTTP 5xx: Stips or Cloudflare is having trouble. */
        const val SERVER = "SERVER"
        const val BAD_RESPONSE = "BAD_RESPONSE"
        const val NOT_SIGNED_IN = "NOT_SIGNED_IN"
        const val NOT_FOUND = "API_ERROR_EOF"
        const val BANNED = "API_ERROR_BANNED"
        const val GENERAL = "API_ERROR_GENERAL"

        private val TRANSIENT = setOf(NETWORK, HTTP, RATE_LIMITED, BLOCKED, SERVER, BAD_RESPONSE)

        // "מחובר" (signed in, as in "not signed in") and "התחבר" (sign in).
        private val SIGNED_OUT_WORDS = listOf("\u05de\u05d7\u05d5\u05d1\u05e8", "\u05d4\u05ea\u05d7\u05d1\u05e8")

        /** Turns an HTTP status into the error the UI explains. Pure. */
        fun forHttp(status: Int): ApiException = when {
            status == 429 -> ApiException(RATE_LIMITED, "HTTP 429")
            status == 403 -> ApiException(BLOCKED, "HTTP 403")
            status in 500..599 -> ApiException(SERVER, "HTTP $status")
            else -> ApiException(HTTP, "HTTP $status")
        }
    }
}
