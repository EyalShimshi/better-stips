package io.github.eyalshimshi.betterstips.data.api

import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.diag.NetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

/**
 * Stips' single JSON endpoint (SPEC-04 §2). Reads go by GET; writes, sign-in and long payloads go by
 * form POST, so passwords and answers never appear in URLs. Every request carries the site's Referer, as the
 * website's and the official app's own calls do (D-49).
 */
class StipsApi(private val client: OkHttpClient) {

    /**
     * Called when Stips refuses a call because the account is blocked (`API_ERROR_BANNED`, v0.13), with whether it was
     * a write (an action the user took: writes go by POST) rather than a read.
     */
    @Volatile
    var onBanned: ((write: Boolean) -> Unit)? = null

    suspend fun call(
        name: String,
        apiParams: JsonObject? = null,
        restAction: String? = null,
        omniobj: JsonObject? = null,
        forcePost: Boolean = false,
    ): JsonElement? = withContext(Dispatchers.IO) {
        val fields = buildFields(name, apiParams, restAction, omniobj)
        val jsonLength = fields.filter { it.first == "api_params" || it.first == "omniobj" }.sumOf { it.second.length }
        val post = forcePost || restAction == "PUT" || restAction == "POST" || jsonLength > 200
        val builder = if (post) {
            siteRequest(BASE_URL).post(formEncode(fields).toRequestBody(FORM))
        } else {
            val url = BASE_URL.toHttpUrl().newBuilder().apply {
                fields.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
            siteRequest(url.toString()).get()
        }
        val request = builder.tag(ApiTag::class.java, ApiTag(name)).build()
        pace(name)
        val sentAt = System.currentTimeMillis()
        val text = execute(request)
        try {
            val data = Parsers.envelope(text)
            RateGuard.onSuccess(sentAt)
            data
        } catch (e: ApiException) {
            if (e.code.contains(RATE_LIMIT_CODE)) {
                // Stips' own per-address limit, sent as an ordinary answer (#5).
                NetLog.event("api", "$name: ${e.code} ${e.serverMessage.orEmpty().take(150)}")
                RateGuard.onLimited("Stips said ${e.code}", null)
                throw ApiException(ApiException.RATE_LIMITED, e.serverMessage)
            }
            val start = if (e.code == ApiException.BAD_RESPONSE) " (body starts: " + text.take(40).replace(Regex("\\s+"), " ") + ")" else ""
            NetLog.event("api", "$name: ${e.code}$start")
            if (e.code == ApiException.BANNED) onBanned?.invoke(post)
            throw e
        }
    }

    /** Waits for request budget (or refuses during a pause), so the app stays under Stips' limit. */
    private suspend fun pace(name: String) {
        val wait = RateGuard.acquire()
        if (wait > 0) {
            NetLog.event("limit", "$name waits ${wait / 1000.0} s to stay under Stips' limit")
            delay(wait)
        }
    }

    /** Multipart upload (the profile photo). Returns the raw response; the upload script doesn't use the envelope. */
    suspend fun upload(url: String, fileName: String, bytes: ByteArray, mediaType: String): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mediaType.toMediaType()))
            .build()
        pace("upload")
        val sentAt = System.currentTimeMillis()
        val text = execute(siteRequest(url).post(body).tag(ApiTag::class.java, ApiTag("upload")).build())
        RateGuard.onSuccess(sentAt)
        text
    }

    /** Runs a request on the calling (IO) thread and returns the body, or throws an [ApiException] that says why not. */
    private fun execute(request: Request): String {
        val text = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = ApiException.forHttp(response.code)
                    if (error.code == ApiException.RATE_LIMITED || error.code == ApiException.BLOCKED) {
                        RateGuard.onLimited("HTTP ${response.code}", response.header("Retry-After")?.trim()?.toLongOrNull())
                    }
                    throw error
                }
                ServerClock.observe(response.header("Date"), System.currentTimeMillis())
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            // A dead connection can outlive the network it was made on; drop the idle ones so the next try starts
            // fresh, as a network change would (#3).
            client.connectionPool.evictAll()
            throw ApiException(ApiException.NETWORK, NetLogListener.describe(e))
        }
        return text
    }

    companion object {
        const val BASE_URL = "https://stips.co.il/api"

        /** Every call carries the site's Referer, as its website's and official app's calls do (D-49); the User-Agent still says Stips+. */
        const val SITE_REFERER = "https://stips.co.il/"

        /** A request to Stips, with the site's Referer. */
        fun siteRequest(url: String): Request.Builder = Request.Builder().url(url).header("Referer", SITE_REFERER)

        /** Stips' error code when one address sends too much (`IP_RATE_LIMIT`, measured in #5). */
        private const val RATE_LIMIT_CODE = "RATE_LIMIT"

        /** The server reads "+" as a space, so the website sends this marker instead (verified with topic "C++"). */
        const val PLUS_MARKER = "~MODU_PLUS_SIGN~"

        private val FORM = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

        fun encodeJson(json: String): String = json.replace("+", PLUS_MARKER)

        fun buildFields(
            name: String,
            apiParams: JsonObject?,
            restAction: String?,
            omniobj: JsonObject?,
        ): List<Pair<String, String>> = buildList {
            add("name" to name)
            if (restAction != null) add("rest_action" to restAction)
            if (apiParams != null) add("api_params" to encodeJson(apiParams.toString()))
            if (omniobj != null) add("omniobj" to encodeJson(omniobj.toString()))
        }

        fun formEncode(fields: List<Pair<String, String>>): String = fields.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
    }
}
