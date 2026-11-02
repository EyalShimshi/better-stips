package io.github.eyalshimshi.betterstips.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.crash.CrashReporter
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.ui.chats.ChatsScreen
import io.github.eyalshimshi.betterstips.ui.chats.ConversationScreen
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.feed.MyListScreen
import io.github.eyalshimshi.betterstips.ui.feed.SavedScreen
import io.github.eyalshimshi.betterstips.ui.feed.TopicScreen
import io.github.eyalshimshi.betterstips.ui.home.HomeScreen
import io.github.eyalshimshi.betterstips.ui.profile.ProfileScreen
import io.github.eyalshimshi.betterstips.ui.question.QuestionScreen
import io.github.eyalshimshi.betterstips.ui.settings.SettingsScreen
import io.github.eyalshimshi.betterstips.ui.signin.SignInScreen
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.Links
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Routes {
    const val HOME = "home"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val QUESTION = "question/{id}"
    const val TOPIC = "topic/{name}"
    const val CONVERSATION = "conversation/{userId}?name={name}"
    const val MY_LIST = "mylist/{kind}"
    const val SAVED = "saved"
    const val SIGN_IN = "signin"
    const val SETTINGS = "settings"

    fun question(id: Long) = "question/$id"
    fun topic(name: String) = "topic/" + Uri.encode(name)
    fun conversation(userId: Long, name: String) = "conversation/$userId?name=" + Uri.encode(name)
    fun myList(kind: MyList) = "mylist/" + kind.name
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.CHATS, R.string.tab_chats, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Tab(Routes.PROFILE, R.string.tab_profile, Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun AppRoot(initialQuestionId: Long?) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val signedOutText = stringResource(R.string.error_signed_out)
    var linkHandled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialQuestionId) {
        if (initialQuestionId != null && !linkHandled) {
            linkHandled = true
            navController.navigate(Routes.question(initialQuestionId))
        }
    }
    LaunchedEffect(Unit) {
        container.session.expired.collect { snackbar.showSnackbar(signedOutText) }
    }

    // Stips question links inside answers open here; everything else goes to the browser.
    val uriHandler = remember(navController) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val id = Links.questionId(uri)
                if (id != null) navController.navigate(Routes.question(id)) else openInBrowser(context, uri)
            }
        }
    }

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Scaffold(
            bottomBar = { if (showBar) BottomBar(navController, route) },
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            AppNavHost(navController, Modifier.padding(padding))
        }
    }
    CrashDialog()
}

@Composable
private fun BottomBar(navController: NavHostController, route: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = route == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier) {
    val animate = LocalAppSettings.current.animations
    val enter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        if (animate) {
            fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) { it / 10 }
        } else {
            EnterTransition.None
        }
    }
    val exit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        if (animate) fadeOut(tween(160)) else ExitTransition.None
    }
    val popEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        if (animate) fadeIn(tween(220)) else EnterTransition.None
    }
    val popExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        if (animate) {
            fadeOut(tween(160)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) { it / 10 }
        } else {
            ExitTransition.None
        }
    }
    fun openQuestion(id: Long) = navController.navigate(Routes.question(id))
    fun openTopic(name: String) = navController.navigate(Routes.topic(name))
    fun signIn() = navController.navigate(Routes.SIGN_IN) { launchSingleTop = true }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenQuestion = ::openQuestion,
                onOpenTopic = ::openTopic,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHATS) {
            ChatsScreen(
                onOpenConversation = { userId, name -> navController.navigate(Routes.conversation(userId, name)) },
                onSignIn = ::signIn,
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onSignIn = ::signIn,
                onOpenMyList = { navController.navigate(Routes.myList(it)) },
                onOpenSaved = { navController.navigate(Routes.SAVED) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.QUESTION, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            QuestionScreen(
                questionId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { navController.popBackStack() },
                onOpenTopic = ::openTopic,
                onSignIn = ::signIn,
            )
        }
        composable(Routes.TOPIC, arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
            TopicScreen(
                topic = entry.arguments?.getString("name").orEmpty(),
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MY_LIST, arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
            val kind = MyList.entries.firstOrNull { it.name == entry.arguments?.getString("kind") } ?: MyList.Asked
            MyListScreen(kind = kind, onBack = { navController.popBackStack() }, onOpenQuestion = ::openQuestion)
        }
        composable(Routes.SAVED) {
            SavedScreen(onBack = { navController.popBackStack() }, onOpenQuestion = ::openQuestion)
        }
        composable(Routes.SIGN_IN) {
            SignInScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
