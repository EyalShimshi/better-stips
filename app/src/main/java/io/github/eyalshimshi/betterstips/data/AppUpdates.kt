package io.github.eyalshimshi.betterstips.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import io.github.eyalshimshi.betterstips.util.UpdateRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

/**
 * Stips+'s own updates (#25), while it's shared as an APK: once a day GitHub's list of releases says whether a newer
 * build is out, and Home and Settings → About offer it. *Update* opens the new APK in the browser (v0.20): the browser
 * downloads it, and Android's installer installs it only over an app signed with the same key, as when Stips+ was
 * first installed. Stips+ itself doesn't download or install apps, and doesn't ask for the permission to: an app that
 * does is what Play Protect takes for malware that drops other apps (D-145). Nothing here talks to Stips.
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
        /** Home's card was put off for this build ("Later"); Settings → About still offers it. */
        val laterBuild: Int = 0,
    ) {
        /** Home shows its card: a newer build that wasn't put off ("Later"). */
        val showCard: Boolean get() = available != null && available.build != laterBuild
    }

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

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

    init {
        // 0.18 and 0.19 downloaded updates into the cache themselves: none of that is needed any more.
        scope.launch(Dispatchers.IO) { File(context.cacheDir, "updates").deleteRecursively() }
    }

    private fun restore(): State {
        val build = prefs.getInt(KEY_BUILD, 0)
        val release = if (build > versionCode) {
            UpdateRules.Release(
                build = build,
                versionName = prefs.getString(KEY_NAME, null) ?: "build $build",
                apkUrl = prefs.getString(KEY_URL, null).orEmpty(),
                apkBytes = prefs.getLong(KEY_BYTES, 0L),
                pageUrl = prefs.getString(KEY_PAGE, null).orEmpty(),
            ).takeIf { UpdateRules.isOurDownload(it.apkUrl) && it.apkBytes in 1..UpdateRules.MAX_APK_BYTES }
        } else {
            null
        }
        return State(available = release, checkedAt = prefs.getLong(KEY_CHECKED, 0L), laterBuild = prefs.getInt(KEY_LATER, 0))
    }

    private fun persist(release: UpdateRules.Release?, checkedAt: Long) {
        // The SHA-256 0.18 and 0.19 kept to check their own downloads goes too.
        val edit = prefs.edit().putLong(KEY_CHECKED, checkedAt).remove(KEY_OLD_SHA256)
        if (release == null) {
            listOf(KEY_BUILD, KEY_NAME, KEY_URL, KEY_BYTES, KEY_PAGE).forEach { edit.remove(it) }
        } else {
            edit.putInt(KEY_BUILD, release.build)
                .putString(KEY_NAME, release.versionName)
                .putString(KEY_URL, release.apkUrl)
                .putLong(KEY_BYTES, release.apkBytes)
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
            _state.update { it.copy(available = available, checkedAt = now, checkFailed = false) }
            true
        } finally {
            // Also when the check is cut short: the row never stays on "Checking…".
            _state.update { it.copy(checking = false) }
        }
    }

    /** Home's card waits for the next build. */
    fun later() {
        val build = _state.value.available?.build ?: return
        prefs.edit().putInt(KEY_LATER, build).apply()
        _state.update { it.copy(laterBuild = build) }
    }

    private companion object {
        const val KEY_CHECKED = "checkedAt"
        const val KEY_BUILD = "build"
        const val KEY_NAME = "versionName"
        const val KEY_URL = "apkUrl"
        const val KEY_BYTES = "apkBytes"
        const val KEY_PAGE = "pageUrl"
        const val KEY_LATER = "laterBuild"

        /** Kept by 0.18 and 0.19, which checked their own downloads; removed on the next check. */
        const val KEY_OLD_SHA256 = "sha256"
    }
}
