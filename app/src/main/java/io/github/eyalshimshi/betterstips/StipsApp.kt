package io.github.eyalshimshi.betterstips

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.eyalshimshi.betterstips.data.ChannelsSource
import io.github.eyalshimshi.betterstips.data.FeedCache
import io.github.eyalshimshi.betterstips.data.SavedStore
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.SettingsStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.api.SessionCookieJar
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.api.UserAgentInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** The app's objects, built once by hand (no dependency-injection framework, playbook §4). */
class AppContainer(private val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The application context, for work that outlives a screen (image decoding, notifications). */
    val appContext: android.content.Context get() = app

    @Suppress("DEPRECATION")
    val versionName: String = try {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    private val cookieJar = SessionCookieJar()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(UserAgentInterceptor("StipsPlus/$versionName (Android ${Build.VERSION.RELEASE}; unofficial; +$REPO_URL)"))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val api = StipsApi(httpClient)

    val settings = SettingsStore(app)
    val session = SessionStore(app, api, cookieJar)
    val saved = SavedStore(app)
    val timings = Timings()
    val channels = ChannelsSource(app, api)
    val repository = StipsRepository(api, session, FeedCache(File(app.filesDir, "feeds")), channels, timings)

    val imageLoader: ImageLoader = ImageLoader.Builder(app)
        .okHttpClient(httpClient)
        .memoryCache { MemoryCache.Builder(app).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(File(app.cacheDir, "images")).maxSizeBytes(64L * 1024 * 1024).build() }
        .crossfade(true)
        .build()

    fun start() {
        appScope.launch { channels.refresh() }
        if (session.session.value.signedIn) {
            appScope.launch {
                runCatching { session.ensureSession() }
                session.refreshProfile()
            }
        }
    }

    companion object {
        const val REPO_URL = "https://github.com/EyalShimshi/better-stips"
    }
}

class StipsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        io.github.eyalshimshi.betterstips.crash.CrashReporter.install(this)
        container = AppContainer(this)
        container.start()
        io.github.eyalshimshi.betterstips.notify.Notifier.schedule(this, container.settings.settings.value.backgroundNotifications)
    }
}
