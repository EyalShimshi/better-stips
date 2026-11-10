package io.github.eyalshimshi.betterstips

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.eyalshimshi.betterstips.data.AnonymousAnswers
import io.github.eyalshimshi.betterstips.data.AppUpdates
import io.github.eyalshimshi.betterstips.data.BlockStore
import io.github.eyalshimshi.betterstips.data.ChannelsSource
import io.github.eyalshimshi.betterstips.data.ChatReadMarks
import io.github.eyalshimshi.betterstips.data.FeedCache
import io.github.eyalshimshi.betterstips.data.ImageLinkResolver
import io.github.eyalshimshi.betterstips.data.LinkPreviews
import io.github.eyalshimshi.betterstips.data.Tutorials
import io.github.eyalshimshi.betterstips.data.ReadMarks
import io.github.eyalshimshi.betterstips.data.ReplyTargets
import io.github.eyalshimshi.betterstips.data.SavedStore
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.SettingsStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.UserBadges
import io.github.eyalshimshi.betterstips.data.api.CookieGenerationStamp
import io.github.eyalshimshi.betterstips.data.api.NetLogListener
import io.github.eyalshimshi.betterstips.data.api.SessionCookieJar
import io.github.eyalshimshi.betterstips.data.api.StaleCookieGuard
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.api.UserAgentInterceptor
import io.github.eyalshimshi.betterstips.diag.NetLog
import io.github.eyalshimshi.betterstips.util.StipsEmoji
import io.github.eyalshimshi.betterstips.widget.ChatsWidgetFeed
import io.github.eyalshimshi.betterstips.widget.HotWidgetFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
    private val userAgent = UserAgentInterceptor("StipsPlus/$versionName (Android ${Build.VERSION.RELEASE}; unofficial; +$REPO_URL)")

    /**
     * API calls share one HTTP/2 connection, as the official app's do. v0.3 used HTTP/1.1 because #3 looked like a
     * stalled connection; it was Stips' request limit (#5), and HTTP/1.1 only added a TLS handshake per extra
     * connection (about 20 in four minutes). A dead connection still fails within the timeouts and is dropped.
     */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(userAgent)
        // A late answer from before a sign-in can't replace its session cookie (#9).
        .addInterceptor(CookieGenerationStamp(cookieJar))
        .addNetworkInterceptor(StaleCookieGuard(cookieJar))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .eventListenerFactory(NetLogListener.factory(verbose = true))
        .build()

    /** Images (cdn.stips.co.il, Unsplash) on their own connections, without cookies, so they never share the API's. */
    private val imageClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgent)
        .connectionPool(ConnectionPool())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .eventListenerFactory(NetLogListener.factory(verbose = false))
        .build()

    private val api = StipsApi(httpClient)

    /**
     * GitHub, for Stips+'s own updates (#25): its own connections, without cookies, never Stips'. No time limit for
     * the whole call: an APK of a few MB can take a while on a slow network.
     */
    private val updateClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgent)
            .connectionPool(ConnectionPool())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .eventListenerFactory(NetLogListener.factory(verbose = false))
            .build()
    }

    /** New versions of Stips+ from GitHub (#25); built when the screens first ask, not for the widget or the checks. */
    val updates: AppUpdates by lazy { AppUpdates(app, updateClient, appScope) }

    val settings = SettingsStore(app)
    val session = SessionStore(app, api, cookieJar)
    val saved = SavedStore(app)
    val anonymousAnswers = AnonymousAnswers(app)
    val seenNotifications = SeenNotifications(app)
    val timings = Timings()
    val channels = ChannelsSource(app, api)
    val blocks = BlockStore(app)
    val userBadges = UserBadges(app)
    val readMarks = ReadMarks(app)
    val chatReadMarks = ChatReadMarks(app)
    val replyTargets = ReplyTargets(app)
    val repository = StipsRepository(api, session, FeedCache(File(app.filesDir, "feeds")), channels, timings, blocks, userBadges)

    /** Which picture an image link shows (#15): imgbb's small oEmbed answers, without cookies. */
    val imageLinks = ImageLinkResolver(imageClient)

    /** What the cards for Stips question and profile links in chats show (v0.18), once per session each. */
    val linkPreviews = LinkPreviews(repository)

    /** Feature tips (#28): which showed, and which the user has done. */
    val tutorials = Tutorials(app)

    /** The "Hot right now" widget's list (D-51). */
    val hotWidget = HotWidgetFeed(app, repository, appScope)

    /** The chats widget's list (#8). */
    val chatsWidget = ChatsWidgetFeed(app, repository, session, appScope)

    val imageLoader: ImageLoader = ImageLoader.Builder(app)
        .okHttpClient(imageClient)
        .memoryCache { MemoryCache.Builder(app).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(File(app.cacheDir, "images")).maxSizeBytes(64L * 1024 * 1024).build() }
        .crossfade(true)
        .build()

    /** Runs for every process start, including the widget's and the notification check's: nothing that costs requests. */
    fun start() {
        // Read on first use (a chat with a :code: in it), or early by warmUp(); a file in the app, not a request (#22).
        StipsEmoji.source { app.assets.open(StipsEmoji.ASSET).bufferedReader().useLines { StipsEmoji.parse(it) } }
        watchNetwork()
        repository.onFirstPage = { query, list -> if (query == HotWidgetFeed.QUERY) hotWidget.publish(list) }
        // Stips refused a call because the account is blocked: read why, and show the block screen again (v0.13).
        api.onBanned = { write -> appScope.launch { session.onBanned(write) } }
        session.onSignedOut = ::forgetAccountData
        chatsWidget.start()
    }

    /**
     * Signing out leaves nothing of the account on the phone (#14): its notifications, which questions it answered
     * anonymously, what it read and replied to. Settings, saved questions and hidden content are the phone's.
     */
    private fun forgetAccountData() {
        io.github.eyalshimshi.betterstips.notify.Notifier.forgetAccount(app)
        anonymousAnswers.clear()
        readMarks.clear()
        chatReadMarks.clear()
        replyTargets.clear()
        seenNotifications.clear()
    }

    private val warmedUp = AtomicBoolean(false)

    /**
     * The screens are opening: refresh the channels and restore the session, once per process. Background work (the
     * widget, phone notifications) skips this; account calls restore the session when they need it.
     */
    fun warmUp() {
        if (!warmedUp.compareAndSet(false, true)) return
        appScope.launch(Dispatchers.IO) { StipsEmoji.warmUp() }
        appScope.launch { channels.refresh() }
        if (session.session.value.signedIn) {
            appScope.launch {
                runCatching { session.ensureSession() }
                session.refreshProfile()
                // Credibility, flowers and any block, for the screens that depend on them (v0.13).
                session.refreshAccount()
            }
        }
    }

    /** First lines of a shared network log: which app and phone it came from. */
    fun diagnosticsHeader(): String =
        "Stips+ $versionName | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"

    /** Closes idle connections (off the main thread: closing TLS is network I/O), so the next request starts fresh. */
    fun dropIdleConnections() {
        appScope.launch(Dispatchers.IO) {
            httpClient.connectionPool.evictAll()
            imageClient.connectionPool.evictAll()
        }
    }

    /** Logs network changes and drops connections made on the previous network (#3). */
    private fun watchNetwork() {
        val connectivity = app.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var last: String? = null

            override fun onAvailable(network: Network) {
                NetLog.event("network", "network available")
                dropIdleConnections()
            }

            override fun onLost(network: Network) {
                NetLog.event("network", "network lost")
                dropIdleConnections()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val text = describeNetwork(networkCapabilities)
                if (text != last) {
                    last = text
                    NetLog.event("network", text)
                }
            }
        }
        try {
            connectivity.registerDefaultNetworkCallback(callback)
        } catch (e: RuntimeException) {
            NetLog.event("network", "can't watch network changes: ${e.javaClass.simpleName}")
        }
    }

    private fun describeNetwork(caps: NetworkCapabilities): String {
        val transports = listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "wifi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "cellular",
            NetworkCapabilities.TRANSPORT_ETHERNET to "ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "vpn",
        ).filter { caps.hasTransport(it.first) }.joinToString("+") { it.second }.ifEmpty { "other" }
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return "now on $transports" + if (validated) "" else " (no internet yet)"
    }

    companion object {
        const val REPO_URL = "https://github.com/EyalShimshi/stips-plus"
    }
}

class StipsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // Before the libraries' start-up providers run (WorkManager's crashed v0.6-v0.7 there, #7), so a crash at
        // that point still leaves a report.
        io.github.eyalshimshi.betterstips.crash.CrashReporter.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
        val settings = container.settings.settings.value
        if (!io.github.eyalshimshi.betterstips.notify.Notifier.sync(this, settings) && settings.backgroundNotifications) {
            // Android refused the job; show the switch as off rather than pretend.
            container.settings.update { it.copy(backgroundNotifications = false) }
        }
    }
}
