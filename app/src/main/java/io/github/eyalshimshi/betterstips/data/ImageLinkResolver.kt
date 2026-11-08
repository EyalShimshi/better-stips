package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.diag.NetLog
import io.github.eyalshimshi.betterstips.util.ImageLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Which picture an image link shows (#15). Image files need nothing; an imgbb page is asked through its oEmbed
 * answer (under 1 KB of JSON), only when the person taps to see the picture, without cookies. Answers are kept
 * for the session.
 */
class ImageLinkResolver(private val client: OkHttpClient) {
    private val cache = object : LinkedHashMap<String, ImageLinks.Picture>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageLinks.Picture>?): Boolean = size > CACHE_SIZE
    }

    /** The picture behind [source], or null when it can't be read. Throws only for cancellation. */
    suspend fun resolve(source: ImageLinks.Source): ImageLinks.Picture? = when (source) {
        is ImageLinks.Source.Direct -> ImageLinks.Picture(source.imageUrl, null)
        is ImageLinks.Source.ImgbbPage -> synchronized(cache) { cache[source.id] }
            ?: fetch(source)?.also { picture -> synchronized(cache) { cache[source.id] = picture } }
    }

    private suspend fun fetch(page: ImageLinks.Source.ImgbbPage): ImageLinks.Picture? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(page.oembedUrl).header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) return@use null
                // Read a little over the limit at most, so a huge answer can't fill memory.
                val source = body.source()
                source.request(ImageLinks.MAX_OEMBED_BYTES.toLong() + 1)
                if (source.buffer.size > ImageLinks.MAX_OEMBED_BYTES) null else ImageLinks.fromOembed(source.buffer.readUtf8())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetLog.event("images", "an imgbb page wasn't read: ${e.javaClass.simpleName}")
            null
        }
    }

    private companion object {
        const val CACHE_SIZE = 50
    }
}
