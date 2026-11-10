package io.github.eyalshimshi.betterstips.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import io.github.eyalshimshi.betterstips.util.UpdateRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Stips+'s own updates (#25), while it's shared as an APK: once a day GitHub's list of releases says whether a newer
 * build is out, and Home and Settings → About offer it. *Update* downloads the APK from GitHub, checks it's exactly
 * the file GitHub listed, and hands it to Android's installer, which asks to confirm and installs it only when it's
 * signed with the same key as the app. Nothing here talks to Stips.
 */
class AppUpdates(private val context: Context, private val client: OkHttpClient, private val scope: CoroutineScope) {
    data class State(
        /** A newer release, when one is known. */
        val available: UpdateRules.Release? = null,
        /** Asking GitHub now. */
        val checking: Boolean = false,
        /** When GitHub last answered; 0 before the first time. */
        val checkedAt: Long = 0L,
        /** The last check got no answer (offline, or GitHub's limit of 60 an hour). */
        val checkFailed: Boolean = false,
        /** Downloading: how far, 0 to 1. Null when not downloading. */
        val progress: Float? = null,
        /** The download, checked and ready for Android's installer. */
        val ready: File? = null,
        /** The installer opens by itself once the download the user asked for is ready. */
        val installWhenReady: Boolean = false,
        /** The last download didn't finish, or wasn't the file GitHub listed. */
        val downloadFailed: Boolean = false,
        /** Home's card was put off for this build ("Later"); Settings → About still offers it. */
        val laterBuild: Int = 0,
    ) {
        val downloading: Boolean get() = progress != null

        /** Home shows its card: a newer build that wasn't put off ("Later"), or one on its way. */
        val showCard: Boolean get() = available != null && (available.build != laterBuild || downloading)
    }

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val dir = File(context.cacheDir, "updates")

    /** The app's version code: the build number of the release it was installed from. */
    val versionCode: Long = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    } catch (e: Exception) {
        // Unknown: nothing is offered as newer.
        Long.MAX_VALUE
    }

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<State> = _state.asStateFlow()

    private val checkLock = Mutex()
    private val downloadLock = Mutex()

    init {
        // Files from an earlier download aren't needed: this version is installed, or it starts over.
        scope.launch(Dispatchers.IO) { downloadLock.withLock { dir.listFiles()?.forEach { it.delete() } } }
    }

    private fun restore(): State {
        val build = prefs.getInt(KEY_BUILD, 0)
        val release = if (build > versionCode) {
            UpdateRules.Release(
                build = build,
                versionName = prefs.getString(KEY_NAME, null) ?: "build $build",
                apkUrl = prefs.getString(KEY_URL, null).orEmpty(),
                apkBytes = prefs.getLong(KEY_BYTES, 0L),
                sha256 = prefs.getString(KEY_SHA256, null),
                pageUrl = prefs.getString(KEY_PAGE, null).orEmpty(),
            ).takeIf { UpdateRules.isOurDownload(it.apkUrl) && it.apkBytes in 1..UpdateRules.MAX_APK_BYTES }
        } else {
            null
        }
        return State(available = release, checkedAt = prefs.getLong(KEY_CHECKED, 0L), laterBuild = prefs.getInt(KEY_LATER, 0))
    }

    private fun persist(release: UpdateRules.Release?, checkedAt: Long) {
        val edit = prefs.edit().putLong(KEY_CHECKED, checkedAt)
        if (release == null) {
            listOf(KEY_BUILD, KEY_NAME, KEY_URL, KEY_BYTES, KEY_SHA256, KEY_PAGE).forEach { edit.remove(it) }
        } else {
            edit.putInt(KEY_BUILD, release.build)
                .putString(KEY_NAME, release.versionName)
                .putString(KEY_URL, release.apkUrl)
                .putLong(KEY_BYTES, release.apkBytes)
                .putString(KEY_SHA256, release.sha256)
                .putString(KEY_PAGE, release.pageUrl)
        }
        edit.apply()
    }

    /**
     * Asks GitHub when a day went by since its last answer (and an hour since a check that got none). Called as the app
     * comes to the screen; runs in the app's own scope, so the app going away doesn't leave it half done.
     */
    fun checkIfDue() {
        if (UpdateRules.SELF_UPDATE) scope.launch { check(onlyIfDue = true) }
    }

    /** Asks GitHub now (Settings → About), in the app's own scope: leaving the page doesn't stop it. */
    fun checkNow() {
        if (UpdateRules.SELF_UPDATE) scope.launch { check(onlyIfDue = false) }
    }

    /** When the last check got no answer, in this run of the app: automatic checks wait an hour after it. */
    @Volatile
    private var failedAt = 0L

    /** Asks GitHub for its newest release; false when it didn't answer (or wasn't due). One at a time. */
    private suspend fun check(onlyIfDue: Boolean): Boolean = checkLock.withLock {
        if (onlyIfDue && !UpdateRules.dueForCheck(_state.value.checkedAt, System.currentTimeMillis(), failedAt)) return@withLock false
        _state.update { it.copy(checking = true) }
        try {
            val json = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(UpdateRules.RELEASES_URL)
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .build()
                    client.newCall(request).execute().use { response -> if (response.isSuccessful) response.body?.string() else null }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Offline, or GitHub didn't answer: never a crash from the app's scope.
                    null
                }
            }
            if (json == null) {
                failedAt = System.currentTimeMillis()
                _state.update { it.copy(checkFailed = true) }
                return@withLock false
            }
            val now = System.currentTimeMillis()
            val available = UpdateRules.newest(json)?.takeIf { UpdateRules.isNewer(it, versionCode) }
            persist(available, now)
            _state.update { current ->
                // Another build than the one downloaded starts over.
                val same = current.available?.build == available?.build
                current.copy(
                    available = available,
                    checkedAt = now,
                    checkFailed = false,
                    ready = if (same) current.ready else null,
                    installWhenReady = same && current.installWhenReady,
                    downloadFailed = same && current.downloadFailed,
                )
            }
            true
        } finally {
            // Also when the check is cut short: the row never stays on "Checking…".
            _state.update { it.copy(checking = false) }
        }
    }

    /** Downloads the newer build in the app's own scope, so it goes on when the screen changes; then installs it. */
    fun download() {
        val current = _state.value
        val release = current.available ?: return
        if (current.downloading) return
        if (current.ready != null) {
            _state.update { it.copy(installWhenReady = true) }
            return
        }
        _state.update { it.copy(progress = 0f, downloadFailed = false, installWhenReady = true) }
        scope.launch(Dispatchers.IO) { downloadLock.withLock { downloadNow(release) } }
    }

    private suspend fun downloadNow(release: UpdateRules.Release) {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "stipsplus-${release.build}.apk.part")
        val file = File(dir, "stipsplus-${release.build}.apk")
        val ok = try {
            fetch(release, part) && part.renameTo(file)
        } catch (e: CancellationException) {
            part.delete()
            _state.update { it.copy(progress = null, installWhenReady = false) }
            throw e
        } catch (e: Exception) {
            false
        }
        if (!ok) part.delete()
        _state.update {
            it.copy(progress = null, ready = if (ok) file else null, downloadFailed = !ok, installWhenReady = ok && it.installWhenReady)
        }
    }

    /** Writes the APK to [part]; true when it's exactly the file GitHub listed. */
    private suspend fun fetch(release: UpdateRules.Release, part: File): Boolean {
        if (!UpdateRules.isOurDownload(release.apkUrl)) return false
        return client.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) return false
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var done = 0L
            var shown = -1
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        done += read
                        if (done > release.apkBytes) return false
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        val percent = (done * 100 / release.apkBytes).toInt()
                        if (percent != shown) {
                            shown = percent
                            _state.update { it.copy(progress = percent / 100f) }
                        }
                    }
                }
            }
            UpdateRules.matches(release, done, digest.digest().toHex())
        }
    }

    /** Home's card waits for the next build. */
    fun later() {
        val build = _state.value.available?.build ?: return
        prefs.edit().putInt(KEY_LATER, build).apply()
        _state.update { it.copy(laterBuild = build) }
    }

    /**
     * Opens Android's installer on the downloaded update. Android asks once to allow Stips+ to install apps, then to
     * confirm the update. False when there's no download or nothing to open it with.
     */
    fun install(from: Context): Boolean {
        _state.update { it.copy(installWhenReady = false) }
        val file = _state.value.ready ?: return false
        if (!file.isFile) {
            _state.update { it.copy(ready = null) }
            return false
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (from !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            from.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

    private companion object {
        /** The FileProvider in the manifest that hands the download to the installer. */
        const val AUTHORITY_SUFFIX = ".updates"
        const val APK_TYPE = "application/vnd.android.package-archive"
        const val KEY_CHECKED = "checkedAt"
        const val KEY_BUILD = "build"
        const val KEY_NAME = "versionName"
        const val KEY_URL = "apkUrl"
        const val KEY_BYTES = "apkBytes"
        const val KEY_SHA256 = "sha256"
        const val KEY_PAGE = "pageUrl"
        const val KEY_LATER = "laterBuild"
    }
}
