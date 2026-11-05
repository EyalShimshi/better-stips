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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.ui.ask.AskScreen
import io.github.eyalshimshi.betterstips.ui.chats.ChatsScreen
import io.github.eyalshimshi.betterstips.ui.chats.ConversationScreen
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalNow
import io.github.eyalshimshi.betterstips.ui.components.LocalOpenProfile
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.feed.HotScreen
import io.github.eyalshimshi.betterstips.ui.feed.MyListScreen
import io.github.eyalshimshi.betterstips.ui.feed.SavedScreen
import io.github.eyalshimshi.betterstips.ui.feed.TopicScreen
import io.github.eyalshimshi.betterstips.ui.home.HomeScreen
import io.github.eyalshimshi.betterstips.ui.notifications.NotificationsScreen
import io.github.eyalshimshi.betterstips.ui.penpals.PenPalsScreen
import io.github.eyalshimshi.betterstips.ui.profile.ProfileScreen
import io.github.eyalshimshi.betterstips.ui.question.QuestionScreen
import io.github.eyalshimshi.betterstips.ui.settings.NetLogScreen
import io.github.eyalshimshi.betterstips.ui.settings.SettingsPage
import io.github.eyalshimshi.betterstips.ui.settings.SettingsScreen
import io.github.eyalshimshi.betterstips.ui.signin.SignInScreen
import io.github.eyalshimshi.betterstips.ui.signin.WelcomeScreen
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.user.UserProfileScreen
import io.github.eyalshimshi.betterstips.util.DeepLink
import io.github.eyalshimshi.betterstips.util.JumpRules
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.Shortcuts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

object Routes {
    const val HOME = "home"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val QUESTION = "question/{id}?focus={focus}&count={count}&at={at}"
    const val TOPIC = "topic/{name}"
    const val CONVERSATION = "conversation/{userId}?name={name}"
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

    /** A question; from a notification, with where to open it (v0.10). */
    fun question(id: Long, focus: QuestionFocus? = null): String =
        if (focus == null) {
            "question/$id"
        } else {
            "question/$id?focus=${JumpRules.kind(focus)}&count=${JumpRules.count(focus)}&at=${JumpRules.at(focus)}"
        }
    fun topic(name: String) = "topic/" + Uri.encode(name)
    fun conversation(userId: Long, name: String) = "conversation/$userId?name=" + Uri.encode(name)
    fun myList(kind: MyList) = "mylist/" + kind.name
    fun user(id: Long) = "user/$id"
    fun ask(channelId: Int?) = if (channelId != null) "ask?channel=$channelId" else "ask"
    fun settingsPage(page: SettingsPage) = "settings/" + page.route

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
    val animate = LocalAppSettings.current.animations
    val snackbar = remember { SnackbarHostState() }
    val signedOutText = stringResource(R.string.error_signed_out)
    val session by container.session.session.collectAsStateWithLifecycle()
    val signedIn = session.signedIn
    var counts by remember { mutableStateOf(UnreadCounts()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // The first launch after installing asks to sign in or go on as a guest (#8). Links and shortcuts skip it.
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
    LaunchedEffect(signedIn) {
        if (signedIn && welcome) finishWelcome()
    }

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
    LaunchedEffect(pendingLink) {
        val link = pendingLink ?: return@LaunchedEffect
        welcome = false
        openLink(navController, link)
        currentOnLinkHandled()
    }
    LaunchedEffect(Unit) {
        container.session.expired.collect { snackbar.showSnackbar(signedOutText) }
    }

    // Unread chats and notifications: one tiny request every 10 s, only while the app is on screen and signed in,
    // and only while the request budget has room (#6).
    LaunchedEffect(signedIn, lifecycleOwner) {
        if (!signedIn) {
            counts = UnreadCounts()
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (RateGuard.allowBackground()) fetchCounts(container)?.let { counts = it }
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
            fetchCounts(container)?.let { counts = it }
        }
    }

    // Stips links inside the app (questions, profiles, chats) open here; everything else goes to the browser.
    val uriHandler = remember(navController) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val link = Links.deepLink(uri)
                if (link != null) openLink(navController, link) else openInBrowser(context, uri)
            }
        }
    }
    val openProfile = remember(navController) { { id: Long -> navController.navigate(Routes.user(id)) } }

    CompositionLocalProvider(
        LocalUriHandler provides uriHandler,
        LocalOpenProfile provides openProfile,
        LocalUnreadCounts provides counts,
        LocalNow provides now,
    ) {
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
                            BottomBar(navController, route, counts)
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
                AppNavHost(
                    navController,
                    Modifier.padding(padding),
                    onNotificationsSeen = { counts = counts.copy(notifications = 0) },
                )
            }
            AnimatedVisibility(
                visible = welcome,
                enter = EnterTransition.None,
                exit = if (animate) fadeOut() else ExitTransition.None,
            ) {
                WelcomeScreen(onGuest = ::finishWelcome)
            }
        }
    }
    CrashDialog()
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

private fun openLink(navController: NavHostController, link: DeepLink) {
    when (link) {
        is DeepLink.Question -> navController.navigate(Routes.question(link.id, link.focus))
        is DeepLink.Profile -> navController.navigate(Routes.user(link.id))
        DeepLink.Messages -> navigateToTab(navController, Routes.CHATS)
        is DeepLink.Conversation -> navController.navigate(Routes.conversation(link.userId, link.name))
        DeepLink.Hot -> navController.navigate(Routes.HOT) { launchSingleTop = true }
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

@Composable
private fun BottomBar(navController: NavHostController, route: String?, counts: UnreadCounts) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = route == tab.route
            val unread = if (tab.route == Routes.CHATS) counts.messages else 0
            NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(navController, tab.route) },
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
 * follows the finger toward the side it was swiped from, instead of shrinking.
 */
private class NavMotion(private val animate: Boolean) {
    private val tabRoutes = tabs.map { it.route }.toSet()
    private val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
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
        const val SLIDE_MS = 350
        const val FADE_OUT_MS = 90
        const val FADE_IN_MS = 210
        const val EDGE_LEFT = 0
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier, onNotificationsSeen: () -> Unit) {
    val animate = LocalAppSettings.current.animations
    val container = LocalAppContainer.current
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
            ),
        ) { entry ->
            ConversationScreen(
                userId = entry.arguments?.getLong("userId") ?: 0L,
                name = entry.arguments?.getString("name").orEmpty(),
                onBack = ::back,
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
