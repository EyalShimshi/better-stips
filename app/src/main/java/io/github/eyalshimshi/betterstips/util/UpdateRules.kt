package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.parse.arr
import io.github.eyalshimshi.betterstips.data.parse.asArray
import io.github.eyalshimshi.betterstips.data.parse.asObject
import io.github.eyalshimshi.betterstips.data.parse.bool
import io.github.eyalshimshi.betterstips.data.parse.long
import io.github.eyalshimshi.betterstips.data.parse.str
import kotlinx.serialization.json.Json

/**
 * Stips+'s own updates (#25), while it isn't on Google Play: which GitHub release is newer than the app. Every build
 * is a pre-release `build-N` with one file, `stipsplus-N.apk`, N being the app's version code (the CI run). Pure.
 */
object UpdateRules {
    /**
     * Off for a Google Play build: Play updates apps itself, and its policy ("Device and network abuse") doesn't let an
     * app update itself any other way, nor point to its own builds elsewhere (MAINTAINING.md).
     */
    const val SELF_UPDATE = true

    /** The newest releases, pre-releases included (GitHub's "latest" leaves them out). GitHub allows 60 calls an hour. */
    const val RELEASES_URL = "https://api.github.com/repos/EyalShimshi/stips-plus/releases?per_page=10"

    /** Where the releases' files are: *Update* opens nothing else. */
    const val DOWNLOAD_PREFIX = "https://github.com/EyalShimshi/stips-plus/releases/download/"

    /** How often Stips+ asks GitHub by itself. */
    const val CHECK_EVERY_MS = 24L * 60 * 60 * 1000

    /** After a check that got no answer, how long Stips+ waits before asking again by itself (GitHub allows 60 an hour). */
    const val RETRY_AFTER_FAILURE_MS = 60L * 60 * 1000

    /** An APK is a few MB; anything far bigger isn't one of ours. */
    const val MAX_APK_BYTES = 100L * 1024 * 1024

    data class Release(
        /** N in `build-N`: the version code of the app it installs. */
        val build: Int,
        /** As the app shows its own, e.g. "0.18.32". */
        val versionName: String,
        val apkUrl: String,
        val apkBytes: Long,
        /** The release's page, with what's new. */
        val pageUrl: String,
    )

    private val buildTag = Regex("""^build-(\d{1,9})$""")

    /** N from `build-N`, or null for any other tag. */
    fun buildNumber(tag: String?): Int? = tag?.let { buildTag.find(it.trim()) }?.groupValues?.get(1)?.toIntOrNull()

    /** The newest release with its APK in GitHub's list of releases ([json]), or null when there's none or it can't be read. */
    fun newest(json: String?): Release? {
        if (json.isNullOrBlank()) return null
        val list = runCatching { Json.parseToJsonElement(json) }.getOrNull().asArray() ?: return null
        return list.mapNotNull { element ->
            val release = element.asObject() ?: return@mapNotNull null
            if (release.bool("draft") == true) return@mapNotNull null
            val build = buildNumber(release.str("tag_name")) ?: return@mapNotNull null
            val apk = release.arr("assets").orEmpty().mapNotNull { it.asObject() }
                .firstOrNull { it.str("name") == "stipsplus-$build.apk" } ?: return@mapNotNull null
            val url = apk.str("browser_download_url")?.takeIf { isOurDownload(it) } ?: return@mapNotNull null
            val bytes = apk.long("size")?.takeIf { it in 1..MAX_APK_BYTES } ?: return@mapNotNull null
            Release(
                build = build,
                versionName = versionName(release.str("name"), build),
                apkUrl = url,
                apkBytes = bytes,
                pageUrl = release.str("html_url")?.takeIf { it.startsWith("https://github.com/") }
                    ?: "https://github.com/EyalShimshi/stips-plus/releases/tag/build-$build",
            )
        }.maxByOrNull { it.build }
    }

    /** "Stips+ 0.18.32" → "0.18.32"; a release without a usable name is called by its build. */
    fun versionName(name: String?, build: Int): String =
        name?.trim()?.removePrefix("Stips+")?.trim()?.takeIf { it.isNotEmpty() && it.length <= 24 } ?: "build $build"

    /** [release] is newer than the app at [versionCode]. */
    fun isNewer(release: Release?, versionCode: Long): Boolean = release != null && release.build > versionCode

    /**
     * A day went by since the last answer from GitHub (or the clock went back), and an hour since a check that got no
     * answer ([lastFailureMs]; 0 when there was none).
     */
    fun dueForCheck(lastCheckMs: Long, nowMs: Long, lastFailureMs: Long = 0L): Boolean =
        nowMs - lastCheckMs !in 0 until CHECK_EVERY_MS &&
            (lastFailureMs <= 0L || nowMs - lastFailureMs !in 0 until RETRY_AFTER_FAILURE_MS)

    /** A release file of Stips+ on GitHub: the only place updates come from. */
    fun isOurDownload(url: String): Boolean =
        url.startsWith(DOWNLOAD_PREFIX) && !url.contains("..") && url.none { it.isWhitespace() }

    /**
     * What *Update* opens in the browser (v0.20): the release's APK, which the browser downloads; or, should that link
     * not be one of ours, the release's page, where the APK is too.
     */
    fun downloadUrl(release: Release): String = release.apkUrl.takeIf { isOurDownload(it) } ?: release.pageUrl
}
