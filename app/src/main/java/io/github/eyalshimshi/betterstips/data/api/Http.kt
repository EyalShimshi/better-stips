package io.github.eyalshimshi.betterstips.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** Keeps Stips' ASP session cookie for this process only; a restart restores the session with the token. */
class SessionCookieJar : CookieJar {
    private val store = LinkedHashMap<String, Cookie>()

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

/** Identifies the app honestly, so Stips can see (and contact or limit) it. */
class UserAgentInterceptor(userAgent: String) : Interceptor {
    private val value = userAgent.filter { it.code in 0x20..0x7E }

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", value).build())
}
