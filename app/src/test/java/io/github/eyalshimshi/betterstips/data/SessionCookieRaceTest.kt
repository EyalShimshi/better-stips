package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiTag
import io.github.eyalshimshi.betterstips.data.api.CookieGenerationStamp
import io.github.eyalshimshi.betterstips.data.api.SessionCookieJar
import io.github.eyalshimshi.betterstips.data.api.StaleCookieGuard
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * #9: the first screens load while the session is restored. A feed request sent before the sign-in gets a new
 * anonymous session cookie from Stips; when that answer comes after the sign-in's, it must not replace the signed-in
 * cookie (it did, and chats came back empty until the app restarted).
 */
class SessionCookieRaceTest {
    private val server = MockWebServer()
    private val jar = SessionCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(jar)
        .addInterceptor(CookieGenerationStamp(jar))
        .addNetworkInterceptor(StaleCookieGuard(jar))
        .build()

    @Before
    fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/feed" -> MockResponse()
                    .setHeadersDelay(800, TimeUnit.MILLISECONDS)
                    .addHeader("Set-Cookie", "SID=anonymous; Path=/")
                    .setBody("{}")
                "/login" -> MockResponse().addHeader("Set-Cookie", "SID=signed-in; Path=/").setBody("{}")
                "/renew" -> MockResponse().addHeader("Set-Cookie", "SID=renewed; Path=/").setBody("{}")
                else -> MockResponse().setBody("{}")
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun request(path: String, name: String): Request =
        Request.Builder().url(server.url(path)).tag(ApiTag::class.java, ApiTag(name)).build()

    private fun sessionCookie(): String? =
        jar.loadForRequest(server.url("/messages")).firstOrNull { it.name == "SID" }?.value

    @Test
    fun aLateAnswerFromBeforeTheSignInKeepsTheSignedInSession() {
        val feedDone = CountDownLatch(1)
        client.newCall(request("/feed", "objectlist")).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) = feedDone.countDown()

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                    feedDone.countDown()
                }
            },
        )
        Thread.sleep(200)
        client.newCall(request("/login", "user.login")).execute().close()
        assertEquals("signed-in", sessionCookie())
        assertTrue(feedDone.await(5, TimeUnit.SECONDS))
        assertEquals("the feed's late anonymous session is dropped", "signed-in", sessionCookie())
    }

    @Test
    fun answersAfterTheSignInStillSetCookies() {
        client.newCall(request("/login", "user.login")).execute().close()
        client.newCall(request("/renew", "omniobj")).execute().close()
        assertEquals("renewed", sessionCookie())
    }
}
