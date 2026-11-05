package io.github.eyalshimshi.betterstips.data.api

import io.github.eyalshimshi.betterstips.diag.NetLog
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Keeps Stips' session cookies for this process only; a restart restores the session with the token.
 *
 * Each sign-in starts a new [generation] ([StaleCookieGuard]). Answers to requests sent before it carry the fresh
 * anonymous session Stips gave a request without one; arriving after the sign-in, they used to replace its cookie, so
 * chats and notifications came back empty until the app restarted (#9).
 */
class SessionCookieJar : CookieJar {
    private val store = LinkedHashMap<String, Cookie>()

    /** Counts sign-ins in this process; a request is "stale" when one finished while it was out. */
    @Volatile
    var generation = 0L
        private set

    @Synchronized
    fun nextGeneration() {
        generation++
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) store["${cookie.name}|${cookie.domain}|${cookie.path}"] = cookie
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        store.values.removeAll { it.expiresAt < now }
        return store.values.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        store.clear()
    }
}

/** The sign-in generation a call started in ([CookieGenerationStamp]). */
data class CookieGeneration(val value: Long)

/**
 * An application interceptor: stamps each call with the sign-in generation it starts in, before its cookies are added
 * and before it waits for a connection (a new one can take hundreds of milliseconds, time enough for a sign-in).
 */
class CookieGenerationStamp(private val jar: SessionCookieJar) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().tag(CookieGeneration::class.java, CookieGeneration(jar.generation)).build())
}

/**
 * A network interceptor (#9): an answer to a request that started before the latest sign-in may not set cookies, so
 * its late anonymous session can't replace the signed-in one. A sign-in's own answer starts the next generation,
 * before its cookies are saved.
 */
class StaleCookieGuard(private val jar: SessionCookieJar) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedIn = request.tag(CookieGeneration::class.java)?.value ?: jar.generation
        val response = chain.proceed(request)
        val name = request.tag(ApiTag::class.java)?.name
        if (name == SIGN_IN) {
            jar.nextGeneration()
            return response
        }
        if (jar.generation == startedIn || response.headers("Set-Cookie").isEmpty()) return response
        NetLog.event("session", "kept the signed-in session: ${name ?: "a request"} from before the sign-in brought another")
        return response.newBuilder().removeHeader("Set-Cookie").build()
    }

    private companion object {
        const val SIGN_IN = "user.login"
    }
}

/** Identifies the app honestly, so Stips can see (and contact or limit) it. */
class UserAgentInterceptor(userAgent: String) : Interceptor {
    private val value = userAgent.filter { it.code in 0x20..0x7E }

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", value).build())
}

/** Marks an API request with its method name, so the network log can say which call it was. */
data class ApiTag(val name: String)

/**
 * Writes one line per API call to [NetLog]: method, status, protocol, new or reused connection (with its address),
 * time, size and Cloudflare's ray id. For images ([verbose] false) only failures and new connections. Never
 * parameters, cookies or bodies (#3).
 */
class NetLogListener private constructor(private val verbose: Boolean) : EventListener() {
    private val startNs = System.nanoTime()
    private var label = "?"
    private var newConnection = false
    private var connectionText = ""
    private var status = 0
    private var headersText = ""
    private var bytes = -1L
    private var canceled = false

    override fun callStart(call: Call) {
        val request = call.request()
        label = request.tag(ApiTag::class.java)?.name
            ?: (request.url.host + " .../" + request.url.pathSegments.lastOrNull().orEmpty().take(40))
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        newConnection = true
    }

    override fun connectFailed(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: IOException) {
        val address = inetSocketAddress.address?.hostAddress ?: inetSocketAddress.hostString
        NetLog.event("conn", "$label: connecting to $address failed: ${describe(ioe)}")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        val id = Integer.toHexString(System.identityHashCode(connection))
        val address = connection.route().socketAddress.address?.hostAddress.orEmpty()
        connectionText = "${connection.protocol()} ${if (newConnection) "new" else "reused"} #$id $address"
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        status = response.code
        headersText = listOfNotNull(
            response.header("cf-ray")?.let { "ray=$it" },
            response.header("cf-cache-status")?.let { "cache=$it" },
            response.header("cf-mitigated")?.let { "mitigated=$it" },
            response.header("Retry-After")?.let { "retry-after=$it" },
            if (response.isSuccessful) null else response.header("Content-Type")?.let { "type=$it" },
        ).joinToString(" ")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        bytes = byteCount
    }

    override fun canceled(call: Call) {
        canceled = true
    }

    override fun callEnd(call: Call) {
        if (!verbose && status in 200..399 && !newConnection) return
        val size = if (bytes >= 0) " | " + (if (bytes >= 1024) "${bytes / 1024} KB" else "$bytes B") else ""
        val headers = if (headersText.isEmpty()) "" else " | $headersText"
        NetLog.event(kind(), "$label -> $status | $connectionText | ${elapsedMs()} ms$size$headers")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        // Images are canceled all the time by scrolling past them; that's not news.
        if (canceled && !verbose) return
        val reason = if (canceled) "canceled" else describe(ioe)
        NetLog.event(kind(), "$label FAILED $reason | ${connectionText.ifEmpty { "no connection" }} | ${elapsedMs()} ms")
    }

    private fun kind() = if (verbose) "http" else "image"

    private fun elapsedMs() = (System.nanoTime() - startNs) / 1_000_000

    companion object {
        fun factory(verbose: Boolean): EventListener.Factory = EventListener.Factory { NetLogListener(verbose) }

        fun describe(e: Throwable): String = e.javaClass.simpleName + ": " + e.message.orEmpty().take(140)
    }
}
