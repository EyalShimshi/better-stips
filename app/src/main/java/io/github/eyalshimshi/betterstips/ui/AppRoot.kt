package io.github.eyalshimshi.betterstips.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.crash.CrashReporter
import io.github.eyalshimshi.betterstips.data.StartTab
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.api.ServerClock
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.ui.ask.AskScreen
import io.github.eyalshimshi.betterstips.ui.chats.ChatsScreen
import io.github.eyalshimshi.betterstips.ui.chats.ConversationScreen
import io.github.eyalshimshi.betterstips.ui.chats.SendToScreen
import io.github.eyalshimshi.betterstips.ui.account.BanScreen
import io.github.eyalshimshi.betterstips.ui.account.knownCredibility
import io.github.eyalshimshi.betterstips.ui.components.ImageLinkViewer
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalNow
import io.github.eyalshimshi.betterstips.ui.components.LocalOpenProfile
import io.github.eyalshimshi.betterstips.ui.components.LocalReplyInPrivate
import io.github.eyalshimshi.betterstips.ui.components.LocalTipsAllowed
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.ui.components.LocalShowIntro
import io.github.eyalshimshi.betterstips.ui.components.LocalTabReselects
import io.github.eyalshimshi.betterstips.ui.components.LocalTabRefreshes
import io.github.eyalshimshi.betterstips.ui.components.TabRefreshes
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.feed.HotScreen
import io.github.eyalshimshi.betterstips.ui.feed.MyListScreen
import io.github.eyalshimshi.betterstips.ui.feed.SavedScreen
import io.github.eyalshimshi.betterstips.ui.feed.TopicScreen
import io.github.eyalshimshi.betterstips.ui.home.HomeScreen
import io.github.eyalshimshi.betterstips.ui.notifications.NotificationsPrompt
import io.github.eyalshimshi.betterstips.ui.notifications.NotificationsScreen
import io.github.eyalshimshi.betterstips.ui.notifications.TeamNoticeDialog
import io.github.eyalshimshi.betterstips.ui.penpals.PenPalsScreen
import io.github.eyalshimshi.betterstips.ui.profile.ProfileScreen
import io.github.eyalshimshi.betterstips.ui.question.QuestionActionsHost
import io.github.eyalshimshi.betterstips.ui.question.QuestionScreen
import io.github.eyalshimshi.betterstips.ui.settings.NetLogScreen
import io.github.eyalshimshi.betterstips.ui.settings.SettingsPage
import io.github.eyalshimshi.betterstips.ui.settings.SettingsScreen
import io.github.eyalshimshi.betterstips.ui.signin.SignInScreen
import io.github.eyalshimshi.betterstips.ui.signin.IntroScreen
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.user.UserProfileScreen
import io.github.eyalshimshi.betterstips.util.BellRules
import io.github.eyalshimshi.betterstips.util.CredRules
import io.github.eyalshimshi.betterstips.util.DeepLink
import io.github.eyalshimshi.betterstips.util.ImageLinks
import io.github.eyalshimshi.betterstips.util.JumpRules
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.Shortcuts
import io.github.eyalshimshi.betterstips.util.UpdateRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

object Routes {
    const val HOME = "home"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val QUESTION = "question/{id}?focus={focus}&count={count}&at={at}"
    const val TOPIC = "topic/{name}"
    const val CONVERSATION = "conversation/{userId}?name={name}&about={about}"
    const val MY_LIST = "mylist/{kind}"
    const val SAVED = "saved"
    const val SIGN_IN = "signin"
    const val SETTINGS = "settings"
    const val SETTINGS_PAGE = "settings/{page}"
    const val USER = "user/{id}"
    const val NOTIFICATIONS = "notifications"
    const val HOT = "hot"
    const val NETLOG = "netlog"
    const val PENPALS = "penpals"
    const val ASK = "ask?channel={channel}"
    const val SEND_TO = "send?text={text}"

    /** A question; from a notification, with where to open it (v0.10). */
    fun question(id: Long, focus: QuestionFocus? = null): String =
        if (focus == null) {
            "question/$id"
        } else {
            "question/$id?focus=${JumpRules.kind(focus)}&count=${JumpRules.count(focus)}&at=${JumpRules.at(focus)}"
        }
    fun topic(name: String) = "topic/" + Uri.encode(name)
    /** [about]: "reply in private" on that question (v0.18). */
    fun conversation(userId: Long, name: String, about: Long? = null) =
        "conversation/$userId?name=" + Uri.encode(name) + (about?.let { "&about=$it" } ?: "")
    fun myList(kind: MyList) = "mylist/" + kind.name
    fun user(id: Long) = "user/$id"
    fun ask(channelId: Int?) = if (channelId != null) "ask?channel=$channelId" else "ask"
    fun settingsPage(page: SettingsPage) = "settings/" + page.route

    /** Picking up to five chats for [text] (v0.19): shared from another app, or forwarded from a chat. */
    fun sendTo(text: String) = "send?text=" + Uri.encode(text)

    /** The tab the app opens on (#8). */
    fun tab(start: StartTab) = when (start) {
        StartTab.Home -> HOME
        StartTab.PenPals -> PENPALS
        StartTab.Chats -> CHATS
        StartTab.Profile -> PROFILE
    }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.PENPALS, R.string.tab_penpals, Icons.Outlined.Groups, Icons.Filled.Groups),
    Tab(Routes.CHATS, R.string.tab_chats, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Tab(Routes.PROFILE, R.string.tab_profile, Icons.Outlined.Person, Icons.Filled.Person),
)

/** Unread chats and notifications are asked this often while the app is on screen (#6; the site's lists: 5–10 s). */
private const val COUNTS_INTERVAL_MS = 10_000L

@Composable
fun AppRoot(pendingLink: DeepLink?, onLinkHandled: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }
    val animate = LocalAppSettings.current.motion.screens
    val snackbar = remember { SnackbarHostState() }
    val signedOutText = stringResource(R.string.error_signed_out)
    val session by container.session.session.collectAsStateWithLifecycle()
    val signedIn = session.signedIn
    var counts by remember { mutableStateOf(UnreadCounts()) }
    // Stips' own bell count as last read (zero once a listing cleared it): the badge is recomputed from it, since a
    // badge recomputed from itself can never drop (v0.18).
    val stipsBellCount = remember { intArrayOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Tapping the open tab again sends its route here; that tab scrolls back to the top (v0.12).
    val tabReselects = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    // Entering Home, Pen pals or Chats, or coming back to the app on one, refreshes it (v0.16).
    val tabRefreshes = remember { TabRefreshes() }
    val currentRoute by rememberUpdatedState(route)
    DisposableEffect(lifecycleOwner, tabRefreshes) {
        var stopped = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stopped = true
            if (event == Lifecycle.Event.ON_START && stopped) {
                stopped = false
                currentRoute?.takeIf { it in REFRESHED_TABS }?.let(tabRefreshes::request)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The first launch after installing shows the intro (#8, #12): set the app up, then sign in or go on as a guest.
    // Links and shortcuts skip it; Settings → About shows it again.
    var welcome by rememberSaveable {
        mutableStateOf(
            !container.settings.settings.value.onboarded && !container.session.session.value.signedIn &&
                pendingLink == null && isFreshInstall(context),
        )
    }
    fun finishWelcome() {
        welcome = false
        container.settings.update { it.copy(onboarded = true) }
    }
    val showIntro = remember { { welcome = true } }

    // Stips blocks the account (v0.13): a screen over the app once per visit, and again whenever Stips refuses an
    // action for it. Its own block when it sends one; else 20–29 credibility points (temporary) or below 20 (for good).
    val scope = rememberCoroutineScope()
    val account by container.session.account.collectAsStateWithLifecycle()
    val bannedSignal by container.session.bannedSignals.collectAsStateWithLifecycle()
    val ban = if (signedIn) account?.let { CredRules.banState(it.points, it.ban) } else null
    var banDismissed by rememberSaveable(bannedSignal) { mutableStateOf(false) }
    var lastBan by remember { mutableStateOf<CredRules.BanState?>(null) }
    LaunchedEffect(ban) {
        if (ban != null) lastBan = ban
    }
    val banVisible = ban != null && !banDismissed && !welcome

    // "Count the seconds" (#8): times under an hour tick every second while the app is on screen.
    val liveTimes = LocalAppSettings.current.liveTimes
    val now by produceState<Instant?>(initialValue = null, liveTimes, lifecycleOwner) {
        value = null
        if (!liveTimes) return@produceState
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = ServerClock.now()
                delay(1_000L - System.currentTimeMillis() % 1_000L)
            }
        }
    }

    val currentOnLinkHandled by rememberUpdatedState(onLinkHandled)
    // A notice from Stips' team whose phone notification was tapped (v0.18, #27): shown in full over the app.
    var teamNotice by remember { mutableStateOf<StipsNotification?>(null) }
    LaunchedEffect(pendingLink) {
        val link = pendingLink ?: return@LaunchedEffect
        welcome = false
        if (link is DeepLink.TeamNotice) teamNotice = link.notice else openLink(navController, link)
        currentOnLinkHandled()
        // A tapped phone notification marked its notifications read: the bell's badge follows at once.
        if (signedIn) counts = counts.copy(notifications = bellBadge(container, stipsBellCount[0], listed = false))
    }
    LaunchedEffect(Unit) {
        container.session.expired.collect { snackbar.showSnackbar(signedOutText) }
    }

    // Stips+'s own updates (#25): GitHub is asked once a day as the app comes to the screen, and a download the user
    // asked for opens Android's installer once it's checked (when the app is on screen: Android doesn't let an app
    // open a screen from the background).
    val checkUpdates = LocalAppSettings.current.checkUpdates
    LaunchedEffect(checkUpdates, lifecycleOwner) {
        if (!checkUpdates || !UpdateRules.SELF_UPDATE) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { container.updates.checkIfDue() }
    }
    val update by container.updates.state.collectAsStateWithLifecycle()
    val installFailedText = stringResource(R.string.update_install_failed)
    LaunchedEffect(update.ready, update.installWhenReady, lifecycleOwner) {
        if (update.ready == null || !update.installWhenReady) return@LaunchedEffect
        // Android doesn't let an app open a screen from the background: the installer waits until the app is on screen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Opening the installer restarts this effect, so the message goes on the screen's own scope.
            if (!container.updates.install(context)) scope.launch { snackbar.showSnackbar(installFailedText) }
        }
    }

    // Unread chats and notifications: one tiny request every 10 s, only while the app is on screen and signed in,
    // and only while the request budget has room (#6). The bell counts what Stips+ knows is new (v0.17): other apps
    // on the account zero Stips' count as they read the list, so its first page is read too when that count goes up,
    // and every minute on Home.
    LaunchedEffect(signedIn, lifecycleOwner) {
        if (!signedIn) {
            counts = UnreadCounts()
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var stipsBell = 0
            var listedAt = 0L
            while (true) {
                if (RateGuard.allowBackground()) {
                    fetchCounts(container)?.let { fetched ->
                        val now = System.currentTimeMillis()
                        val listNow = BellRules.listNow(fetched.notifications, stipsBell, now - listedAt, currentRoute == Routes.HOME)
                        stipsBell = fetched.notifications
                        var listed = false
                        if (listNow && RateGuard.allowBackground()) {
                            listedAt = now
                            listed = learnBell(container)
                        }
                        stipsBellCount[0] = if (listed) 0 else fetched.notifications
                        counts = fetched.copy(notifications = bellBadge(container, fetched.notifications, listed))
                    }
                }
                delay(COUNTS_INTERVAL_MS)
            }
        }
    }
    // Reading notifications or a chat changes the counts, so ask again on the way out.
    val previousRoute = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(route) {
        val left = previousRoute[0]
        previousRoute[0] = route
        reportShortcutUsed(context, route)
        if (signedIn && (left == Routes.NOTIFICATIONS || left == Routes.CONVERSATION)) {
            fetchCounts(container)?.let {
                stipsBellCount[0] = it.notifications
                counts = it.copy(notifications = bellBadge(container, it.notifications, listed = false))
            }
        }
    }

    // Stips links inside the app (questions, profiles, chats) open here; picture links open in the viewer (#15);
    // everything else goes to the browser.
    val imageLink = remember { mutableStateOf<ImageLinks.Source?>(null) }
    val uriHandler = remember(navController) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val link = Links.deepLink(uri)
                val image = if (link == null) ImageLinks.classify(uri) else null
                when {
                    link != null -> openLink(navController, link)
                    image != null -> imageLink.value = image
                    else -> openInBrowser(context, uri)
                }
            }
        }
    }
    val openProfile = remember(navController) { { id: Long -> navController.navigate(Routes.user(id)) } }
    // "Reply in private" (v0.18): a chat with the asker or answerer, about the question; signing in first when needed.
    val replyInPrivate = remember(navController, container) {
        { author: Author, questionId: Long ->
            val userId = author.userId
            when {
                userId == null || author.anonymous -> Unit
                !container.session.session.value.signedIn -> navController.navigate(Routes.SIGN_IN) { launchSingleTop = true }
                else -> navController.navigate(Routes.conversation(userId, author.nickname, about = questionId))
            }
        }
    }

    CompositionLocalProvider(
        LocalUriHandler provides uriHandler,
        LocalOpenProfile provides openProfile,
        LocalReplyInPrivate provides replyInPrivate,
        LocalUnreadCounts provides counts,
        LocalNow provides now,
        LocalTabReselects provides tabReselects,
        LocalTabRefreshes provides tabRefreshes,
        LocalShowIntro provides showIntro,
        // Tips (#28) wait while the intro or the blocked-account screen covers the app.
        LocalTipsAllowed provides (!welcome && !banVisible),
    ) {
        // Swiping a question in any list runs the actions picked in Settings (#18); their dialogs show over the app.
        QuestionActionsHost(snackbar, onSignIn = { navController.navigate(Routes.SIGN_IN) { launchSingleTop = true } }) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                // Under the welcome screen the app stays ready (links may open into it) but hidden from TalkBack.
                modifier = if (welcome) Modifier.clearAndSetSemantics { } else Modifier,
                bottomBar = {
                    Column {
                        RateLimitBanner()
                        AnimatedVisibility(
                            visible = showBar,
                            enter = if (animate) expandVertically(expandFrom = Alignment.Top) + fadeIn() else EnterTransition.None,
                            exit = if (animate) shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut() else ExitTransition.None,
                        ) {
                            BottomBar(
                                navController,
                                route,
                                counts,
                                onReselect = { tabReselects.tryEmit(it) },
                                onEnter = { if (it in REFRESHED_TABS) tabRefreshes.request(it) },
                            )
                        }
                    }
                },
                snackbarHost = { SwipeAwaySnackbarHost(snackbar) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
                AppNavHost(
                    navController,
                    Modifier.padding(padding),
                    onNotificationsSeen = { counts = counts.copy(notifications = 0) },
                    onMessage = { text -> scope.launch { snackbar.showSnackbar(text) } },
                )
            }
            AnimatedVisibility(
                visible = welcome,
                // On a fresh install it's there from the start; shown again from Settings, it fades in.
                enter = if (animate) fadeIn() else EnterTransition.None,
                exit = if (animate) fadeOut() else ExitTransition.None,
            ) {
                IntroScreen(onFinish = ::finishWelcome)
            }
            AnimatedVisibility(
                visible = banVisible,
                enter = if (animate) fadeIn() else EnterTransition.None,
                exit = if (animate) fadeOut() else ExitTransition.None,
            ) {
                (ban ?: lastBan)?.let { shown ->
                    BanScreen(
                        state = shown,
                        points = account?.points,
                        onContinue = { banDismissed = true },
                        onSignOut = {
                            banDismissed = true
                            scope.launch { container.session.signOut() }
                        },
                    )
                }
            }
        }
        }
    }
    CrashDialog()
    teamNotice?.let { notice ->
        val target = notice.target
        TeamNoticeDialog(
            item = notice,
            points = knownCredibility(),
            onOpen = when (target) {
                is NotificationTarget.OpenQuestion -> ({
                    teamNotice = null
                    navController.navigate(Routes.question(target.id, null))
                })
                is NotificationTarget.OpenProfile -> ({
                    teamNotice = null
                    navController.navigate(Routes.user(target.id))
                })
                null -> null
            },
            onDismiss = { teamNotice = null },
        )
    }
    imageLink.value?.let { source -> ImageLinkViewer(source, onDismiss = { imageLink.value = null }) }
    // Once: phone notifications on, and allowed to run in the background (v0.11).
    NotificationsPrompt(active = !welcome && !banVisible, snackbar = snackbar)
}

/** Whether this is the version first installed (not an update): only then does the welcome screen show. */
@Suppress("DEPRECATION")
private fun isFreshInstall(context: Context): Boolean = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    info.firstInstallTime == info.lastUpdateTime
} catch (e: Exception) {
    false
}

private suspend fun fetchCounts(container: AppContainer): UnreadCounts? = try {
    container.repository.unreadCounts()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Reads the bell's first page and keeps what's new in it until the bell shows it (v0.17). False when it couldn't. */
private suspend fun learnBell(container: AppContainer): Boolean {
    val me = container.session.session.value.userId ?: return false
    return try {
        container.seenNotifications.learn(me, container.repository.notifications(page = 1))
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}

/** The bell's badge: the rows Stips+ knows are new, or Stips' own count until the page is read (v0.17). */
private fun bellBadge(container: AppContainer, stips: Int, listed: Boolean): Int {
    val me = container.session.session.value.userId ?: return stips
    return BellRules.badge(container.seenNotifications.waitingRows(me), stips, listed)
}

private fun openLink(navController: NavHostController, link: DeepLink) {
    when (link) {
        is DeepLink.Question -> navController.navigate(Routes.question(link.id, link.focus))
        is DeepLink.Profile -> navController.navigate(Routes.user(link.id))
        DeepLink.Messages -> navigateToTab(navController, Routes.CHATS)
        is DeepLink.Conversation -> navController.navigate(Routes.conversation(link.userId, link.name))
        DeepLink.Hot -> navController.navigate(Routes.HOT) { launchSingleTop = true }
        // Shown over the app by AppRoot, not a screen to go to (v0.18).
        is DeepLink.TeamNotice -> Unit
        is DeepLink.SendTo -> navController.navigate(Routes.sendTo(link.text))
    }
}

/** Tells the launcher a shortcut's screen was opened (from the shortcut or inside the app), for its suggestions. */
private fun reportShortcutUsed(context: Context, route: String?) {
    val id = when (route) {
        Routes.HOT -> Shortcuts.ID_HOT
        Routes.CHATS -> Shortcuts.ID_CHATS
        else -> return
    }
    try {
        ShortcutManagerCompat.reportShortcutUsed(context, id)
    } catch (e: RuntimeException) {
        // Only a hint for the launcher.
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** While Stips has paused this phone for too many requests (#5): what happened and when the app tries again. */
@Composable
private fun RateLimitBanner() {
    val until by RateGuard.pausedUntilFlow.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(until) {
        now = System.currentTimeMillis()
        while (until > now) {
            delay(1_000L)
            now = System.currentTimeMillis()
        }
    }
    if (until <= now) return
    val seconds = ((until - now) / 1000L).coerceAtLeast(1L)
    val left = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.HourglassTop, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.limit_banner, left), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The tabs that refresh when entered from the bottom bar or when the app comes back to them (v0.16); not Profile. */
private val REFRESHED_TABS = setOf(Routes.HOME, Routes.PENPALS, Routes.CHATS)

@Composable
private fun BottomBar(
    navController: NavHostController,
    route: String?,
    counts: UnreadCounts,
    onReselect: (String) -> Unit,
    onEnter: (String) -> Unit,
) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = route == tab.route
            val unread = if (tab.route == Routes.CHATS) counts.messages else 0
            NavigationBarItem(
                selected = selected,
                // The bar shows only on the tabs' own screens, so the open tab is at its top level: back to its top.
                onClick = {
                    if (selected) {
                        onReselect(tab.route)
                    } else {
                        onEnter(tab.route)
                        navigateToTab(navController, tab.route)
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                        },
                    ) {
                        Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                    }
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/**
 * Screen-to-screen motion (DESIGN-PHILOSOPHY §5). A new screen slides in over the current one, which drifts a
 * quarter of the way back; going back reverses it. Tabs cross-fade. During the predictive back gesture the screen
 * follows the finger toward the side it was swiped from, instead of shrinking. Short since v0.15 (#18): 200 ms with
 * Material's decelerating curve, so most of the move happens at once.
 */
private class NavMotion(private val animate: Boolean) {
    private val tabRoutes = tabs.map { it.route }.toSet()
    private val emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val slide = tween<IntOffset>(SLIDE_MS, easing = emphasized)
    private val follow = tween<IntOffset>(SLIDE_MS, easing = LinearEasing)

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
        initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            !animate -> EnterTransition.None
            betweenTabs() -> fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS))
            else -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide)
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !animate -> ExitTransition.None
            betweenTabs() -> fadeOut(tween(FADE_OUT_MS))
            else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) { it / 4 }
        }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            !animate -> EnterTransition.None
            betweenTabs() -> fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS))
            else -> slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) { it / 4 }
        }
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !animate -> ExitTransition.None
            betweenTabs() -> fadeOut(tween(FADE_OUT_MS))
            else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide)
        }
    }

    // The Int is the edge the back swipe started from (0 = left, 1 = right).
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = { edge ->
        when {
            !animate -> EnterTransition.None
            betweenTabs() -> fadeIn(tween(SLIDE_MS, easing = LinearEasing))
            else -> slideInHorizontally(follow) { full -> if (edge == EDGE_LEFT) -full / 4 else full / 4 }
        }
    }
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { edge ->
        when {
            !animate -> ExitTransition.None
            betweenTabs() -> fadeOut(tween(SLIDE_MS, easing = LinearEasing))
            else -> slideOutHorizontally(follow) { full -> if (edge == EDGE_LEFT) full else -full }
        }
    }

    private companion object {
        const val SLIDE_MS = 200
        const val FADE_OUT_MS = 60
        const val FADE_IN_MS = 120
        const val EDGE_LEFT = 0
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier, onNotificationsSeen: () -> Unit, onMessage: (String) -> Unit) {
    val animate = LocalAppSettings.current.motion.screens
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val signedIn = session.signedIn
    val motion = remember(animate) { NavMotion(animate) }
    // The start tab is read once, so changing it in Settings applies the next time the app opens (#8).
    val startRoute = rememberSaveable { Routes.tab(container.settings.settings.value.startTab) }
    fun openQuestion(id: Long) = navController.navigate(Routes.question(id))
    fun openTopic(name: String) = navController.navigate(Routes.topic(name))
    fun openUser(id: Long) = navController.navigate(Routes.user(id))
    fun openConversation(userId: Long, name: String) = navController.navigate(Routes.conversation(userId, name))
    fun openMyList(kind: MyList) = navController.navigate(Routes.myList(kind))
    fun openSaved() = navController.navigate(Routes.SAVED)
    fun openSettings() = navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
    fun signIn() = navController.navigate(Routes.SIGN_IN) { launchSingleTop = true }
    fun sendTo(text: String) = navController.navigate(Routes.sendTo(text))
    fun back() {
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier,
        enterTransition = motion.enter,
        exitTransition = motion.exit,
        popEnterTransition = motion.popEnter,
        popExitTransition = motion.popExit,
        predictivePopEnterTransition = motion.predictivePopEnter,
        predictivePopExitTransition = motion.predictivePopExit,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenQuestion = ::openQuestion,
                onOpenTopic = ::openTopic,
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } },
                onOpenHot = { navController.navigate(Routes.HOT) { launchSingleTop = true } },
                onAsk = { channelId -> if (signedIn) navController.navigate(Routes.ask(channelId)) { launchSingleTop = true } else signIn() },
                onOpenProfile = ::openUser,
            )
        }
        composable(Routes.PENPALS) {
            PenPalsScreen(onOpenConversation = ::openConversation, onSignIn = ::signIn)
        }
        composable(
            Routes.ASK,
            arguments = listOf(
                navArgument("channel") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
        ) { entry ->
            AskScreen(
                channelId = entry.arguments?.getInt("channel")?.takeIf { it > 0 },
                onClose = ::back,
                onPosted = { id ->
                    navController.navigate(Routes.question(id)) {
                        popUpTo(Routes.ASK) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CHATS) {
            ChatsScreen(onOpenConversation = ::openConversation, onSignIn = ::signIn)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onSignIn = ::signIn,
                onOpenMyList = ::openMyList,
                onOpenSaved = ::openSaved,
                onOpenSettings = ::openSettings,
                onOpenQuestion = ::openQuestion,
                onOpenConversation = ::openConversation,
            )
        }
        composable(
            Routes.QUESTION,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("focus") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("count") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("at") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val args = entry.arguments
            QuestionScreen(
                questionId = args?.getLong("id") ?: 0L,
                onBack = ::back,
                onOpenTopic = ::openTopic,
                onSignIn = ::signIn,
                focus = JumpRules.decode(args?.getString("focus"), args?.getInt("count") ?: -1, args?.getLong("at") ?: -1L),
            )
        }
        composable(Routes.USER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            UserProfileScreen(
                userId = entry.arguments?.getLong("id") ?: 0L,
                onBack = ::back,
                onOpenQuestion = ::openQuestion,
                onOpenConversation = ::openConversation,
                onOpenMyList = ::openMyList,
                onOpenSaved = ::openSaved,
                onOpenSettings = ::openSettings,
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = ::back,
                onOpenQuestion = { id, focus -> navController.navigate(Routes.question(id, focus)) },
                onOpenProfile = ::openUser,
                onSignIn = ::signIn,
                onSeen = onNotificationsSeen,
            )
        }
        composable(Routes.HOT) {
            HotScreen(onBack = ::back, onOpenQuestion = ::openQuestion)
        }
        composable(Routes.TOPIC, arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
            TopicScreen(
                topic = entry.arguments?.getString("name").orEmpty(),
                onBack = ::back,
                onOpenQuestion = ::openQuestion,
            )
        }
        composable(
            Routes.CONVERSATION,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("about") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            ConversationScreen(
                userId = entry.arguments?.getLong("userId") ?: 0L,
                name = entry.arguments?.getString("name").orEmpty(),
                onBack = ::back,
                onForward = ::sendTo,
                about = entry.arguments?.getLong("about")?.takeIf { it > 0L },
            )
        }
        composable(
            Routes.SEND_TO,
            arguments = listOf(
                navArgument("text") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val notesName = stringResource(R.string.notes_title)
            SendToScreen(
                text = entry.arguments?.getString("text").orEmpty(),
                onClose = ::back,
                onSignIn = ::signIn,
                onSent = { sent, failed ->
                    // One chat: it opens, as in WhatsApp (or the chat it was forwarded from shows again). Several: back.
                    val one = sent.singleOrNull()
                    val oneId = one?.userId
                    val previous = navController.previousBackStackEntry
                    val sameChat = previous != null && previous.destination.route == Routes.CONVERSATION &&
                        previous.arguments?.getLong("userId") == oneId
                    if (one != null && oneId != null && !sameChat) {
                        navController.navigate(Routes.conversation(oneId, one.nickname)) {
                            popUpTo(Routes.SEND_TO) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                    val names = failed.joinToString(", ") { if (it.userId == session.userId) notesName else it.nickname }
                    val message = when {
                        failed.isNotEmpty() -> context.getString(R.string.send_to_partly, names)
                        sent.size > 1 -> context.resources.getQuantityString(R.plurals.send_to_sent, sent.size, sent.size)
                        else -> null
                    }
                    if (message != null) onMessage(message)
                },
            )
        }
        composable(Routes.MY_LIST, arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
            val kind = MyList.entries.firstOrNull { it.name == entry.arguments?.getString("kind") } ?: MyList.Asked
            MyListScreen(kind = kind, onBack = ::back, onOpenQuestion = ::openQuestion)
        }
        composable(Routes.SAVED) {
            SavedScreen(onBack = ::back, onOpenQuestion = ::openQuestion)
        }
        composable(Routes.SIGN_IN) {
            SignInScreen(onDone = ::back)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                page = null,
                onBack = ::back,
                onOpenPage = { page -> navController.navigate(Routes.settingsPage(page)) { launchSingleTop = true } },
                onOpenMyProfile = { navigateToTab(navController, Routes.PROFILE) },
                onOpenNetLog = { navController.navigate(Routes.NETLOG) { launchSingleTop = true } },
                onSignIn = ::signIn,
            )
        }
        composable(Routes.SETTINGS_PAGE, arguments = listOf(navArgument("page") { type = NavType.StringType })) { entry ->
            SettingsScreen(
                page = SettingsPage.of(entry.arguments?.getString("page")),
                onBack = ::back,
                onOpenPage = { page -> navController.navigate(Routes.settingsPage(page)) { launchSingleTop = true } },
                onOpenMyProfile = { navigateToTab(navController, Routes.PROFILE) },
                onOpenNetLog = { navController.navigate(Routes.NETLOG) { launchSingleTop = true } },
                onSignIn = ::signIn,
            )
        }
        composable(Routes.NETLOG) {
            NetLogScreen(onBack = ::back)
        }
    }
}

/** After a crash, offer to copy or report it (DESIGN-PHILOSOPHY §6). Nothing is sent automatically. */
@Composable
private fun CrashDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        report = withContext(Dispatchers.IO) { CrashReporter.pending(context) }
    }
    val text = report ?: return
    fun close() {
        report = null
        CrashReporter.clear(context)
    }
    AlertDialog(
        onDismissRequest = { close() },
        title = { Text(stringResource(R.string.crash_title)) },
        text = { Text(stringResource(R.string.crash_body)) },
        confirmButton = {
            TextButton(onClick = {
                openInBrowser(context, CrashReporter.issueUrl(text))
                close()
            }) { Text(stringResource(R.string.crash_report)) }
        },
        dismissButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Stips+ crash", text))
                close()
            }) { Text(stringResource(R.string.crash_copy)) }
        },
    )
}
