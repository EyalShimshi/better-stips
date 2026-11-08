package io.github.eyalshimshi.betterstips.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI

/**
 * Picture links Stips+ can show itself (#15). Stips can't hold pictures, so people upload them to imgbb (ibb.co) and
 * attach the link; this knows imgbb's pages and the image files of imgbb, Imgur and Postimages. Everything else
 * stays a link for the browser. Pure.
 */
object ImageLinks {
    /** Where a picture link leads: straight to the file, or to an imgbb page that names it. */
    sealed interface Source {
        /** The link as written, for the browser. */
        val link: String

        data class Direct(override val link: String, val imageUrl: String) : Source

        data class ImgbbPage(override val link: String, val id: String) : Source {
            /** imgbb's oEmbed answer for the page: a few hundred bytes of JSON with the picture's address. */
            val oembedUrl: String get() = "https://ibb.co/$id/oembed.json"
        }
    }

    /** A picture's file and, when known, a small version that can show first. */
    data class Picture(val imageUrl: String, val thumbnailUrl: String?)

    /** The most of imgbb's oEmbed answer worth reading (it is under 1 KB). */
    const val MAX_OEMBED_BYTES = 16 * 1024

    private const val MAX_LINK = 2048
    private val DIRECT_HOSTS = setOf("i.ibb.co", "i.imgur.com", "i.postimg.cc")
    private val PAGE_HOSTS = setOf("ibb.co", "www.ibb.co")
    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val PAGE_ID = Regex("[A-Za-z0-9]{4,16}")

    /** imgbb's own pages, which look like picture pages but aren't. */
    private val NOT_PICTURES = setOf(
        "about", "account", "album", "albums", "api", "contact", "explore", "followers", "following", "images",
        "liked", "login", "logout", "page", "pages", "plugin", "privacy", "search", "settings", "signup", "tos", "upload",
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** What [link] is, when it is a picture Stips+ can show; null for every other link. */
    fun classify(link: String?): Source? {
        val trimmed = link?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > MAX_LINK) return null
        val uri = webUri(trimmed) ?: return null
        val host = uri.host.lowercase().removeSuffix(".")
        val path = uri.rawPath.orEmpty()
        return when (host) {
            in DIRECT_HOSTS -> if (isImagePath(path)) Source.Direct(trimmed, "https://$host$path") else null
            in PAGE_HOSTS -> {
                val id = path.trim('/').split('/').singleOrNull()
                if (id != null && PAGE_ID.matches(id) && id.lowercase() !in NOT_PICTURES) Source.ImgbbPage(trimmed, id) else null
            }
            else -> null
        }
    }

    fun isImageLink(link: String?): Boolean = classify(link) != null

    /** The picture in imgbb's oEmbed answer; only an https image file on i.ibb.co is taken from it. */
    fun fromOembed(text: String): Picture? {
        if (text.length > MAX_OEMBED_BYTES) return null
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: IllegalArgumentException) {
            // SerializationException is one.
            null
        } ?: return null
        val url = root.string("url")?.takeIf(::isImgbbFile) ?: return null
        return Picture(url, root.string("thumbnail_url")?.takeIf(::isImgbbFile))
    }

    /** An https image file on imgbb's image host. */
    fun isImgbbFile(url: String): Boolean {
        val uri = webUri(url.trim()) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("i.ibb.co", ignoreCase = true) &&
            isImagePath(uri.rawPath.orEmpty())
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun isImagePath(path: String): Boolean {
        val name = path.substringAfterLast('/')
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in EXTENSIONS && name.length > extension.length + 1
    }

    /** A web address with a plain host: no user name in it, no unusual port. */
    private fun webUri(link: String): URI? {
        val uri = try {
            URI(link)
        } catch (e: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return null
        if (uri.host.isNullOrEmpty() || uri.rawUserInfo != null) return null
        if (uri.port != -1 && uri.port != 80 && uri.port != 443) return null
        return uri
    }
}
