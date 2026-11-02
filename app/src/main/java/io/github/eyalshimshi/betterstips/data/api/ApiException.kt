package io.github.eyalshimshi.betterstips.data.api

/** A failure talking to Stips, with a code the UI can turn into plain words. */
class ApiException(val code: String, val serverMessage: String? = null) : Exception("$code ${serverMessage.orEmpty()}".trim()) {
    val isNetwork: Boolean get() = code == NETWORK || code == HTTP

    companion object {
        const val NETWORK = "NETWORK"
        const val HTTP = "HTTP"
        const val BAD_RESPONSE = "BAD_RESPONSE"
        const val NOT_SIGNED_IN = "NOT_SIGNED_IN"
        const val NOT_FOUND = "API_ERROR_EOF"
        const val BANNED = "API_ERROR_BANNED"
        const val GENERAL = "API_ERROR_GENERAL"
    }
}
