package io.github.eyalshimshi.betterstips.ui.signin

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.HomeLayout
import io.github.eyalshimshi.betterstips.data.ThemeMode
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.notifications.rememberPhoneNotificationsToggle
import io.github.eyalshimshi.betterstips.ui.settings.HomeLayoutPicker
import io.github.eyalshimshi.betterstips.ui.settings.LookPreview
import io.github.eyalshimshi.betterstips.ui.settings.NotificationPreview
import io.github.eyalshimshi.betterstips.ui.settings.RepliesPreview
import io.github.eyalshimshi.betterstips.ui.settings.Swatch

/** The intro's steps, in order (#12). */
private enum class IntroStep { Welcome, Look, Home, Replies, Account, Notifications, Done }

/**
 * The first screen after installing (#8). Since #12 it's a short intro that sets the app up: the look, what Home
 * shows, how replies show, then signing in (or looking around as a guest) and phone notifications. Each step shows
 * what its choices do, and they apply at once. It covers the app until it's done; links and shortcuts that open
 * the app skip it, and Settings → About shows it again (without the sign-in step when signed in).
 */
@Composable
fun IntroScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val store = container.settings
    val settings by store.settings.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Before any branch: Android's answer about the notification permission comes back to this.
    val toggleNotifications = rememberPhoneNotificationsToggle(snackbar)
    var step by rememberSaveable { mutableStateOf(IntroStep.Welcome) }
    val startedSignedIn = rememberSaveable { session.signedIn }
    val steps = remember(startedSignedIn, session.signedIn) {
        IntroStep.entries.filter { candidate ->
            when (candidate) {
                IntroStep.Account -> !startedSignedIn
                IntroStep.Notifications -> session.signedIn
                else -> true
            }
        }
    }
    val index = steps.indexOf(step).coerceAtLeast(0)

    fun next() {
        step = steps.getOrElse(index + 1) { IntroStep.Done }
    }

    fun back() {
        if (index > 0) step = steps[index - 1]
    }

    // From the choices straight to what matters: signing in, or the end when already signed in.
    fun skip() {
        if (startedSignedIn) onFinish() else step = steps.first { it >= IntroStep.Account }
    }

    fun answeredNotifications() = store.update { it.copy(notificationsPrompted = true) }

    LaunchedEffect(session.signedIn) {
        if (session.signedIn && step == IntroStep.Account) step = IntroStep.Notifications
    }
    BackHandler(enabled = index > 0 && step != IntroStep.Done) { back() }

    val animate = settings.motion.screens
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val notificationsOn = settings.backgroundNotifications && Notifier.canPost(context)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
            ) {
                IntroTopBar(
                    showBack = index > 0 && step != IntroStep.Done,
                    onBack = { back() },
                    position = index,
                    count = steps.size,
                    onSkip = if (step == IntroStep.Look || step == IntroStep.Home || step == IntroStep.Replies) ({ skip() }) else null,
                )
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (!animate) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            // Forward comes in from the side the reading goes to.
                            val forward = targetState > initialState
                            val direction = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                            (slideInHorizontally { it * direction / 5 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it * direction / 5 } + fadeOut())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "intro",
                ) { shown ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (shown) {
                            IntroStep.Welcome -> WelcomeStep()
                            IntroStep.Look -> LookStep(settings)
                            IntroStep.Home -> HomeStep(settings)
                            IntroStep.Replies -> RepliesStep(settings)
                            IntroStep.Account -> AccountStep(onGuest = { next() }, onNext = { next() })
                            IntroStep.Notifications -> NotificationsStep(notificationsOn)
                            IntroStep.Done -> DoneStep()
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (step) {
                        IntroStep.Welcome -> {
                            TextButton(onClick = { skip() }) { Text(stringResource(R.string.intro_skip)) }
                            Button(onClick = { next() }) { Text(stringResource(R.string.intro_start)) }
                        }
                        IntroStep.Look, IntroStep.Home, IntroStep.Replies -> {
                            Button(onClick = { next() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.intro_next)) }
                        }
                        // Signing in, or going on as a guest, are the step's own buttons.
                        IntroStep.Account -> Unit
                        IntroStep.Notifications -> if (notificationsOn) {
                            Button(
                                onClick = {
                                    answeredNotifications()
                                    next()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.intro_next)) }
                        } else {
                            TextButton(
                                onClick = {
                                    answeredNotifications()
                                    next()
                                },
                            ) { Text(stringResource(R.string.notify_prompt_later)) }
                            Button(
                                onClick = {
                                    answeredNotifications()
                                    toggleNotifications(true)
                                    next()
                                },
                            ) { Text(stringResource(R.string.notifications_phone_turn_on)) }
                        }
                        IntroStep.Done -> {
                            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.intro_finish)) }
                        }
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

/** Back, the steps as dots (the current one long), and "Skip" on the steps with choices. */
@Composable
private fun IntroTopBar(showBack: Boolean, onBack: () -> Unit, position: Int, count: Int, onSkip: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val stepText = stringResource(R.string.intro_step, position + 1, count)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = 72.dp)) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        }
        Row(
            Modifier
                .weight(1f)
                .semantics { contentDescription = stepText },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(count) { i ->
                val active = i == position
                val width by animateDpAsState(if (active) 20.dp else 8.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(RoundedCornerShape(50))
                        .background(if (active || i < position) colors.primary else colors.outlineVariant),
                )
            }
        }
        Box(Modifier.widthIn(min = 72.dp), contentAlignment = Alignment.CenterEnd) {
            if (onSkip != null) TextButton(onClick = onSkip) { Text(stringResource(R.string.intro_skip)) }
        }
    }
}

@Composable
private fun StepHeader(title: String, body: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A line with a switch, the whole line tappable. */
@Composable
private fun SwitchLine(title: String, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    // Exactly "Welcome" on its own: the launch test looks for it on a fresh install.
    Text(
        stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(R.string.intro_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LookPreview()
}

@Composable
private fun LookStep(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    StepHeader(stringResource(R.string.intro_look_title), stringResource(R.string.intro_look_body))
    LookPreview()
    val modes = listOf(
        ThemeMode.System to stringResource(R.string.theme_system),
        ThemeMode.Light to stringResource(R.string.theme_light),
        ThemeMode.Dark to stringResource(R.string.theme_dark),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { i, (mode, label) ->
            SegmentedButton(
                selected = settings.themeMode == mode,
                onClick = { store.update { it.copy(themeMode = mode) } },
                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
    if (dynamicAvailable) {
        SwitchLine(
            title = stringResource(R.string.settings_dynamic),
            summary = stringResource(R.string.settings_dynamic_summary),
            checked = settings.dynamicColor,
            onChange = { on -> store.update { it.copy(dynamicColor = on) } },
        )
    }
    Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleSmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(AppSettings.ACCENTS) { accent ->
            val selected = !(settings.dynamicColor && dynamicAvailable) && settings.accent == accent
            Swatch(accent, selected) { store.update { it.copy(accent = accent, dynamicColor = false) } }
        }
    }
}

@Composable
private fun HomeStep(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    StepHeader(stringResource(R.string.intro_home_title), stringResource(R.string.intro_home_body))
    HomeLayoutPicker(selected = settings.homeLayout, onSelect = { layout -> store.update { it.copy(homeLayout = layout) } })
    Text(
        stringResource(
            when (settings.homeLayout) {
                HomeLayout.HotAndNew -> R.string.layout_hot_new_summary
                HomeLayout.LikeWebsite -> R.string.layout_website_summary
                HomeLayout.NewOnly -> R.string.layout_new_only_summary
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SwitchLine(
        title = stringResource(R.string.settings_live_new),
        summary = stringResource(R.string.live_what),
        checked = settings.autoRefreshNew,
        onChange = { on -> store.update { it.copy(autoRefreshNew = on) } },
    )
}

/** Replies in answers and in chats together; Settings has a switch for each. */
@Composable
private fun RepliesStep(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    StepHeader(stringResource(R.string.intro_replies_title), stringResource(R.string.intro_replies_body))
    RepliesPreview(
        easyReply = settings.easyReply,
        quotes = settings.replyQuotes,
        hideCarets = settings.hideReplyCarets,
        moderatorBadges = settings.moderatorBadges,
    )
    SwitchLine(
        title = stringResource(R.string.settings_easy_reply),
        summary = stringResource(R.string.intro_easy_reply_summary),
        checked = settings.easyReply,
        onChange = { on -> store.update { it.copy(easyReply = on, chatEasyReply = on) } },
    )
    SwitchLine(
        title = stringResource(R.string.settings_reply_quotes),
        summary = stringResource(R.string.intro_quotes_summary),
        checked = settings.replyQuotes,
        onChange = { on -> store.update { it.copy(replyQuotes = on, chatReplyQuotes = on) } },
    )
    if (settings.replyQuotes) {
        SwitchLine(
            title = stringResource(R.string.settings_hide_reply_carets),
            summary = null,
            checked = settings.hideReplyCarets,
            onChange = { on -> store.update { it.copy(hideReplyCarets = on, chatHideReplyCarets = on) } },
        )
    }
    Text(
        stringResource(R.string.intro_replies_both),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Sign in, or look around as a guest (the old welcome screen's choice, #8). */
@Composable
private fun AccountStep(onGuest: () -> Unit, onNext: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val vm = appViewModel { SignInViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    StepHeader(stringResource(R.string.sign_in_title), stringResource(R.string.welcome_body))
    if (session.signedIn) {
        Text(
            stringResource(R.string.settings_account_as, session.profile?.nickname.orEmpty()),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.intro_next)) }
        return
    }
    SignInFields(vm, state)
    OutlinedButton(onClick = onGuest, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.welcome_guest))
    }
    Text(
        stringResource(R.string.welcome_guest_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SignInLinks()
}

@Composable
private fun NotificationsStep(on: Boolean) {
    StepHeader(stringResource(R.string.notify_prompt_title), stringResource(R.string.notify_prompt_body))
    NotificationPreview()
    if (on) {
        Text(stringResource(R.string.intro_notifications_on), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    } else {
        Text(
            stringResource(R.string.intro_notifications_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DoneStep() {
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
    }
    StepHeader(stringResource(R.string.intro_done_title), stringResource(R.string.intro_done_body))
}
