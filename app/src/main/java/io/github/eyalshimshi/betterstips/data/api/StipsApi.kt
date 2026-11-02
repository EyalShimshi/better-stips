package io.github.eyalshimshi.betterstips.data.api

import io.github.eyalshimshi.betterstips.data.parse.Parsers
import kotlinx.coroutines.Dispatchers
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
 * form POST, so passwords and answers never appear in URLs.
 */
class StipsApi(private val client: OkHttpClient) {

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
        val request = if (post) {
            Request.Builder().url(BASE_URL).post(formEncode(fields).toRequestBody(FORM)).build()
        } else {
            val url = BASE_URL.toHttpUrl().newBuilder().apply {
                fields.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
            Request.Builder().url(url).get().build()
        }
        val text = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException(ApiException.HTTP, "HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw ApiException(ApiException.NETWORK, e.message)
        }
        Parsers.envelope(text)
    }

    /** Multipart upload (the profile photo). Returns the raw response; the upload script doesn't use the envelope. */
    suspend fun upload(url: String, fileName: String, bytes: ByteArray, mediaType: String): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mediaType.toMediaType()))
            .build()
        val request = Request.Builder().url(url).post(body).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException(ApiException.HTTP, "HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw ApiException(ApiException.NETWORK, e.message)
        }
    }

    companion object {
        const val BASE_URL = "https://stips.co.il/api"

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
