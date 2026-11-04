package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.HotWindow
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.widget.HotWidgetFeed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApiAndDataTest {

    @Test
    fun plusSignsAreSentAsTheSitesMarker() {
        val params = buildJsonObject { put("topic", "C++") }
        val fields = StipsApi.buildFields("objectlist", params, null, null)
        assertEquals("name" to "objectlist", fields[0])
        assertEquals("""{"topic":"C~MODU_PLUS_SIGN~~MODU_PLUS_SIGN~"}""", fields[1].second)
        assertFalse(fields.any { it.second.contains('+') })
    }

    @Test
    fun formEncodingIsUtf8() {
        val body = StipsApi.formEncode(listOf("name" to "user.login", "api_params" to """{"a":"שלום עולם"}"""))
        assertTrue(body.startsWith("name=user.login&api_params="))
        assertTrue("Hebrew is percent-encoded as UTF-8", body.contains("%D7%A9%D7%9C%D7%95%D7%9D"))
        assertFalse("no raw quotes or braces", body.contains("{") || body.contains("\""))
    }

    @Test
    fun omniobjAndRestActionAreIncluded() {
        val fields = StipsApi.buildFields(
            "omniobj",
            null,
            "PUT",
            buildJsonObject { put("objType", "ans") },
        ).toMap()
        assertEquals("PUT", fields["rest_action"])
        assertEquals("""{"objType":"ans"}""", fields["omniobj"])
    }

    @Test
    fun requestsCarryTheSitesReferer() {
        val request = StipsApi.siteRequest(StipsApi.BASE_URL + "?name=objectlist").build()
        assertEquals("https://stips.co.il/", request.header("Referer"))
        assertNull("the honest User-Agent comes from the client's interceptor", request.header("User-Agent"))
    }

    @Test
    fun widgetShowsHomesHotListAndAsksAtMostEveryHalfHour() {
        assertEquals(
            "Home's top cards load the widget's list, so the widget updates for free",
            FeedQuery(sort = FeedSort.Hot, channelName = null).normalized(),
            HotWidgetFeed.QUERY,
        )
        assertEquals(HotWindow.Now, HotWidgetFeed.QUERY.window)
        val now = 1_800_000_000_000L
        assertFalse(HotWidgetFeed.isStale(savedAt = now - 29 * 60_000L, now = now))
        assertTrue(HotWidgetFeed.isStale(savedAt = now - 30 * 60_000L, now = now))
        assertTrue("never saved", HotWidgetFeed.isStale(savedAt = 0L, now = now))
    }

    @Test
    fun hotWindowsFollowTheSite() {
        assertEquals(listOf(HotWindow.Now, HotWindow.Today, HotWindow.Week), FeedQuery().hotWindows)
        assertEquals(listOf(HotWindow.Week, HotWindow.Month), FeedQuery(channelName = "לימודים").hotWindows)
        val normalized = FeedQuery(sort = FeedSort.Hot, window = HotWindow.Now, topic = "מתמטיקה").normalized()
        assertEquals(HotWindow.Week, normalized.window)
        assertEquals(HotWindow.Today, FeedQuery(window = HotWindow.Today).normalized().window)
    }

    @Test
    fun cacheKeysDifferPerList() {
        assertNotEquals(FeedQuery().cacheKey, FeedQuery(sort = FeedSort.Hot).cacheKey)
        assertNotEquals(FeedQuery(channelName = "א").cacheKey, FeedQuery(channelName = "ב").cacheKey)
        assertEquals("the window doesn't matter for New", FeedQuery(window = HotWindow.Week).cacheKey, FeedQuery().cacheKey)
    }

    /** The bundled snapshot must stay valid: it's what the app shows before the network answers. */
    @Test
    fun bundledChannelsSnapshotIsValid() {
        val file = listOf(File("src/main/assets/channels.json"), File("app/src/main/assets/channels.json")).first { it.exists() }
        val channels = Parsers.channelsFromAsset(file.readText(Charsets.UTF_8))
        assertEquals(6, channels.size)
        assertEquals("ids are unique", channels.size, channels.map { it.id }.toSet().size)
        channels.forEach { channel ->
            assertTrue("name of ${channel.id}", channel.name.isNotBlank())
            assertTrue("English name of ${channel.id}", !channel.nameEn.isNullOrBlank())
            assertTrue("colors of ${channel.id}", channel.colors.isNotEmpty())
        }
    }
}
