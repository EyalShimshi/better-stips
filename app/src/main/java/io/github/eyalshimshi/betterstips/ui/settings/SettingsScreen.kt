package io.github.eyalshimshi.betterstips.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.TouchApp
import android.widget.Toast
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.SpecialAccounts

import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.HomeLayout
import io.github.eyalshimshi.betterstips.data.Motion
import io.github.eyalshimshi.betterstips.data.MotionPreset
import io.github.eyalshimshi.betterstips.data.StartTab
import io.github.eyalshimshi.betterstips.data.ThemeMode
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.account.knownCredibility
import io.github.eyalshimshi.betterstips.ui.chats.ChatSettingsContent
import io.github.eyalshimshi.betterstips.ui.components.AnonymousModeDialog
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalOpenProfile
import io.github.eyalshimshi.betterstips.ui.components.LocalShowIntro
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.rememberAnonymousModeOn
import io.github.eyalshimshi.betterstips.ui.feed.windowLabel
import io.github.eyalshimshi.betterstips.ui.home.LiveControls
import io.github.eyalshimshi.betterstips.ui.notifications.PhoneNotificationsStatus
import io.github.eyalshimshi.betterstips.ui.notifications.rememberPhoneNotificationsToggle
import io.github.eyalshimshi.betterstips.ui.profile.GroupLabel
import io.github.eyalshimshi.betterstips.ui.profile.GroupRow
import io.github.eyalshimshi.betterstips.util.CredRules
import io.github.eyalshimshi.betterstips.util.SwipeRules
import io.github.eyalshimshi.betterstips.util.NotificationCategories
import io.github.eyalshimshi.betterstips.util.UpdateRules
import io.github.eyalshimshi.betterstips.ui.updates.updateRowSummary
import io.github.eyalshimshi.betterstips.ui.updates.updateRowTitle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import io.github.eyalshimshi.betterstips.ui.question.answerSwipeIcon
import io.github.eyalshimshi.betterstips.ui.question.answerSwipeLabel
import io.github.eyalshimshi.betterstips.ui.question.questionSwipeIcon
import io.github.eyalshimshi.betterstips.ui.question.questionSwipeLabel
import io.github.eyalshimshi.betterstips.widget.requestPinChatsWidget
import io.github.eyalshimshi.betterstips.widget.requestPinHotWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The pages of Settings (#8, rethought in #12): the main page lists them in three groups, and each opens on its
 * own (route `settings/{page}`). Pages with something to see start with a picture of it.
 */
enum class SettingsPage(val route: String) {
    Account("account"),
    Look("appearance"),
    Home("home"),
    Answers("reading"),
    Chats("chats"),
    Notifications("notifications"),
    Widgets("widgets"),
    Privacy("privacy"),
    Stips("stips"),
    About("about"),
    Diagnostics("diagnostics"),
    ;

    companion object {
        fun of(route: String?): SettingsPage? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Settings: the main page ([page] null) lists the pages; a page shows its own options. Dialogs and messages live here
 * so every page can use them.
 */
@Composable
fun SettingsScreen(
    page: SettingsPage?,
    onBack: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenNetLog: () -> Unit,
    onSignIn: () -> Unit,
) {
    val container = LocalAppContainer.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val title = stringResource(
        when (page) {
            null -> R.string.settings_title
            SettingsPage.Account -> R.string.settings_account
            SettingsPage.Look -> R.string.settings_appearance
            SettingsPage.Home -> R.string.settings_home
            SettingsPage.Answers -> R.string.settings_answers
            SettingsPage.Chats -> R.string.dm_settings
            SettingsPage.Notifications -> R.string.notifications_title
            SettingsPage.Widgets -> R.string.settings_widgets
            SettingsPage.Privacy -> R.string.settings_privacy_safety
            SettingsPage.Stips -> R.string.settings_stips
            SettingsPage.About -> R.string.settings_about
            SettingsPage.Diagnostics -> R.string.settings_diagnostics
        },
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SwipeAwaySnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            when (page) {
                null -> MainPage(settings, onOpenPage, onSignIn)
                SettingsPage.Account -> AccountPage(snackbar, onOpenMyProfile, onSignIn)
                SettingsPage.Look -> LookPage(settings)
                SettingsPage.Home -> HomePage(settings)
                SettingsPage.Answers -> AnswersPage(settings)
                // The same rows as the DM settings sheet in the chats screens (v0.12), with its preview (#12).
                SettingsPage.Chats -> ChatSettingsContent(snackbar)
                SettingsPage.Notifications -> NotificationsPage(settings, snackbar)
                SettingsPage.Widgets -> WidgetsPage(snackbar)
                SettingsPage.Privacy -> PrivacyPage(settings, snackbar)
                SettingsPage.Stips -> StipsPage()
                SettingsPage.About -> AboutPage(container, settings, onOpenPage)
                SettingsPage.Diagnostics -> DiagnosticsPage(container, onOpenNetLog)
            }
        }
    }
}

/** "A · B · C" from the parts that apply. */
private fun joined(vararg parts: String?): String = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")

@Composable
private fun MainPage(settings: AppSettings, onOpenPage: (SettingsPage) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val session by container.session.session.collectAsStateWithLifecycle()
    Spacer(Modifier.height(8.dp))
    AccountCard(onOpen = { onOpenPage(SettingsPage.Account) }, onSignIn = onSignIn)

    // Make it yours: how the app looks and reads. Each row says what's chosen now.
    GroupLabel(stringResource(R.string.settings_group_yours))
    val theme = stringResource(
        when (settings.themeMode) {
            ThemeMode.System -> R.string.theme_system
            ThemeMode.Light -> R.string.theme_light
            ThemeMode.Dark -> R.string.theme_dark
        },
    )
    val dynamicOn = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    GroupRow(
        stringResource(R.string.settings_appearance),
        0,
        4,
        { onOpenPage(SettingsPage.Look) },
        summary = stringResource(
            R.string.settings_look_summary,
            theme,
            stringResource(if (dynamicOn) R.string.settings_dynamic else R.string.settings_accent),
        ),
        icon = Icons.Outlined.Palette,
    )
    GroupRow(
        stringResource(R.string.settings_home),
        1,
        4,
        { onOpenPage(SettingsPage.Home) },
        summary = joined(
            stringResource(layoutLabel(settings.homeLayout)),
            if (settings.autoRefreshNew) stringResource(R.string.settings_live_new) else null,
        ),
        icon = Icons.Outlined.Home,
    )
    GroupRow(
        stringResource(R.string.settings_answers),
        2,
        4,
        { onOpenPage(SettingsPage.Answers) },
        summary = joined(
            if (settings.easyReply) stringResource(R.string.settings_easy_reply) else null,
            if (settings.replyQuotes) stringResource(R.string.settings_short_quotes) else null,
        ).ifEmpty { stringResource(R.string.settings_answers_summary) },
        icon = Icons.Outlined.Forum,
    )
    val phoneOn = settings.backgroundNotifications && Notifier.canPost(context)
    GroupRow(
        stringResource(R.string.dm_settings),
        3,
        4,
        { onOpenPage(SettingsPage.Chats) },
        summary = joined(
            if (settings.chatEasyReply) stringResource(R.string.settings_easy_reply) else null,
            if (settings.chatReplyQuotes) stringResource(R.string.settings_short_quotes) else null,
            if (phoneOn && settings.chatNotifications) stringResource(R.string.settings_chat_notifications) else null,
        ).ifEmpty { stringResource(R.string.dm_settings_summary) },
        icon = Icons.AutoMirrored.Outlined.Chat,
    )

    // On your phone: what reaches you outside the app.
    GroupLabel(stringResource(R.string.settings_group_phone))
    GroupRow(
        stringResource(R.string.notifications_title),
        0,
        2,
        { onOpenPage(SettingsPage.Notifications) },
        summary = when {
            !session.signedIn -> stringResource(R.string.settings_phone_notifications_signed_out)
            phoneOn -> joined(stringResource(R.string.settings_on), stringResource(R.string.check_minutes, settings.checkMinutes))
            else -> stringResource(R.string.settings_off)
        },
        icon = Icons.Outlined.Notifications,
    )
    GroupRow(
        stringResource(R.string.settings_widgets),
        1,
        2,
        { onOpenPage(SettingsPage.Widgets) },
        summary = stringResource(R.string.settings_widgets_summary),
        icon = Icons.Outlined.Widgets,
    )

    GroupLabel(stringResource(R.string.settings_group_more))
    val anonymousOn = rememberAnonymousModeOn(settings.anonymousUntil)
    GroupRow(
        stringResource(R.string.settings_privacy_safety),
        0,
        3,
        { onOpenPage(SettingsPage.Privacy) },
        summary = if (anonymousOn) stringResource(R.string.settings_anon_on) else stringResource(R.string.settings_privacy_safety_summary),
        icon = Icons.Outlined.Lock,
    )
    GroupRow(
        stringResource(R.string.settings_stips),
        1,
        3,
        { onOpenPage(SettingsPage.Stips) },
        summary = stringResource(R.string.settings_stips_summary),
        icon = Icons.Outlined.Public,
    )
    GroupRow(
        stringResource(R.string.settings_about),
        2,
        3,
        { onOpenPage(SettingsPage.About) },
        summary = stringResource(R.string.settings_about_summary, container.versionName),
        icon = Icons.Outlined.Info,
    )
    CreatorCredit()
}

/**
 * Who made Stips+ (#26): a small line at the very bottom of Settings, a button to his profile in the app, and one to buy
 * him a coffee (v0.19), which opens in the browser.
 */
@Composable
private fun CreatorCredit() {
    val openProfile = LocalOpenProfile.current
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.credit_made_by),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { openProfile(SpecialAccounts.CREATOR_ID) },
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.credit_profile), style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(
                onClick = { openInBrowser(context, AppContainer.COFFEE_URL) },
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Outlined.LocalCafe, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.credit_coffee), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Who's signed in, as a card that opens the account's page; signed out, a row to sign in. */
@Composable
private fun AccountCard(onOpen: () -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    if (!session.signedIn) {
        GroupRow(
            stringResource(R.string.sign_in_title),
            0,
            1,
            onSignIn,
            summary = stringResource(R.string.settings_sign_in_summary),
            icon = Icons.AutoMirrored.Outlined.Login,
        )
        return
    }
    val profile = session.profile
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (profile != null) {
                Avatar(profile.avatar, 48.dp, clickable = false)
            } else {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.nickname ?: stringResource(R.string.settings_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.settings_account_summary), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun AccountPage(snackbar: SnackbarHostState, onOpenMyProfile: () -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failedText = stringResource(R.string.action_failed)
    var safeBusy by remember { mutableStateOf(false) }
    if (!session.signedIn) {
        Spacer(Modifier.height(8.dp))
        GroupRow(
            stringResource(R.string.sign_in_title),
            0,
            1,
            onSignIn,
            summary = stringResource(R.string.settings_sign_in_summary),
            icon = Icons.AutoMirrored.Outlined.Login,
        )
        return
    }
    fun toggleSafeMode() {
        safeBusy = true
        scope.launch {
            try {
                container.repository.setSafeMode(!session.safeFilter)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(failedText)
            } finally {
                safeBusy = false
            }
        }
    }
    val external = externalIcon()
    Spacer(Modifier.height(8.dp))
    GroupRow(
        stringResource(R.string.settings_edit_profile),
        0,
        2,
        onOpenMyProfile,
        summary = stringResource(R.string.settings_edit_profile_summary),
    )
    GroupRow(
        title = stringResource(R.string.settings_safe_mode),
        summary = stringResource(R.string.settings_safe_mode_summary),
        index = 1,
        count = 2,
        onClick = if (safeBusy) null else ({ toggleSafeMode() }),
        trailing = { Switch(checked = session.safeFilter, onCheckedChange = null, enabled = !safeBusy) },
    )
    // What only the website can change (#12: deleting the account moved here from the Stips page).
    GroupLabel(stringResource(R.string.settings_on_site))
    GroupRow(
        stringResource(R.string.settings_account_details),
        0,
        3,
        { openInBrowser(context, "$SITE_URL/settings/account-details") },
        trailing = external,
    )
    GroupRow(
        stringResource(R.string.settings_change_password),
        1,
        3,
        { openInBrowser(context, "$SITE_URL/settings/change-password") },
        trailing = external,
    )
    GroupRow(
        stringResource(R.string.settings_delete_account),
        2,
        3,
        { openInBrowser(context, "$SITE_URL/delete-account") },
        trailing = external,
    )
    Spacer(Modifier.height(16.dp))
    GroupRow(stringResource(R.string.sign_out), 0, 1, { scope.launch { container.session.signOut() } })
}

private fun layoutLabel(layout: HomeLayout): Int = when (layout) {
    HomeLayout.HotAndNew -> R.string.layout_hot_new
    HomeLayout.LikeWebsite -> R.string.layout_website
    HomeLayout.NewOnly -> R.string.layout_new_only
}

private fun layoutSummary(layout: HomeLayout): Int = when (layout) {
    HomeLayout.HotAndNew -> R.string.layout_hot_new_summary
    HomeLayout.LikeWebsite -> R.string.layout_website_summary
    HomeLayout.NewOnly -> R.string.layout_new_only_summary
}


/** Colors and theme, with a picture of the app in them (#12), then images and animations. */
@Composable
private fun LookPage(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    LookPreview(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    GroupLabel(stringResource(R.string.settings_colors))
    GroupRow(
        title = stringResource(R.string.settings_dynamic),
        summary = stringResource(if (dynamicAvailable) R.string.settings_dynamic_summary else R.string.settings_dynamic_unavailable),
        index = 0,
        count = 4,
        onClick = if (dynamicAvailable) ({ store.update { it.copy(dynamicColor = !it.dynamicColor) } }) else null,
        trailing = {
            Switch(
                checked = settings.dynamicColor && dynamicAvailable,
                onCheckedChange = null,
                enabled = dynamicAvailable,
            )
        },
    )
    GroupRow(
        title = stringResource(R.string.settings_accent),
        summary = stringResource(R.string.settings_accent_summary),
        index = 1,
        count = 4,
        onClick = null,
    )
    GroupBox(index = 1, count = 4) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(AppSettings.ACCENTS) { accent ->
                val selected = !(settings.dynamicColor && dynamicAvailable) && settings.accent == accent
                Swatch(accent, selected) { store.update { it.copy(accent = accent, dynamicColor = false) } }
            }
        }
    }
    ChoiceBox(
        title = stringResource(R.string.settings_theme),
        summary = null,
        options = listOf(
            ThemeMode.System to stringResource(R.string.theme_system),
            ThemeMode.Light to stringResource(R.string.theme_light),
            ThemeMode.Dark to stringResource(R.string.theme_dark),
        ),
        selected = settings.themeMode,
        onSelect = { mode -> store.update { it.copy(themeMode = mode) } },
        index = 2,
        count = 4,
    )
    GroupRow(
        title = stringResource(R.string.settings_pure_black),
        summary = stringResource(R.string.settings_pure_black_summary),
        index = 3,
        count = 4,
        onClick = { store.update { it.copy(pureBlack = !it.pureBlack) } },
        trailing = { Switch(checked = settings.pureBlack, onCheckedChange = null) },
    )
    GroupLabel(stringResource(R.string.settings_interface))
    GroupRow(
        title = stringResource(R.string.settings_images),
        summary = stringResource(R.string.settings_images_summary),
        index = 0,
        count = 1,
        onClick = { store.update { it.copy(showImages = !it.showImages) } },
        trailing = { Switch(checked = settings.showImages, onCheckedChange = null) },
    )
    MotionGroup(settings)
}

/**
 * Animations (#18): three presets first; "Customized" shows every kind of animation with its own switch, starting
 * from what plays now.
 */
@Composable
private fun MotionGroup(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    GroupLabel(stringResource(R.string.settings_animations))
    val presets = listOf(
        Triple(MotionPreset.Full, R.string.motion_full, R.string.motion_full_summary),
        Triple(MotionPreset.Less, R.string.motion_less, R.string.motion_less_summary),
        Triple(MotionPreset.Off, R.string.motion_off, R.string.motion_off_summary),
        Triple(MotionPreset.Custom, R.string.motion_custom, R.string.motion_custom_summary),
    )
    val custom = settings.motionPreset == MotionPreset.Custom
    val m = settings.customMotion
    val kinds: List<Triple<Int, Int, Pair<Boolean, (Motion) -> Motion>>> = listOf(
        Triple(R.string.motion_screens, R.string.motion_screens_summary, m.screens to { x: Motion -> x.copy(screens = !x.screens) }),
        Triple(R.string.motion_jumps, R.string.motion_jumps_summary, m.jumps to { x: Motion -> x.copy(jumps = !x.jumps) }),
        Triple(R.string.motion_scrolls, R.string.motion_scrolls_summary, m.scrolls to { x: Motion -> x.copy(scrolls = !x.scrolls) }),
        Triple(R.string.motion_glow, R.string.motion_glow_summary, m.glow to { x: Motion -> x.copy(glow = !x.glow) }),
        Triple(R.string.motion_elements, R.string.motion_elements_summary, m.elements to { x: Motion -> x.copy(elements = !x.elements) }),
        Triple(R.string.motion_quick, R.string.motion_quick_summary, m.quick to { x: Motion -> x.copy(quick = !x.quick) }),
    )
    val rows = presets.size + if (custom) kinds.size else 0
    presets.forEachIndexed { index, (preset, title, summary) ->
        GroupRow(
            title = stringResource(title),
            summary = stringResource(summary),
            index = index,
            count = rows,
            onClick = {
                store.update { s ->
                    // Customizing starts from what plays now.
                    if (preset == MotionPreset.Custom && s.motionPreset != MotionPreset.Custom) {
                        s.copy(motionPreset = preset, customMotion = s.motion)
                    } else {
                        s.copy(motionPreset = preset)
                    }
                }
            },
            trailing = { RadioButton(selected = settings.motionPreset == preset, onClick = null) },
        )
    }
    if (custom) {
        kinds.forEachIndexed { i, (title, summary, state) ->
            val (on, flip) = state
            GroupRow(
                title = stringResource(title),
                summary = stringResource(summary),
                index = presets.size + i,
                count = rows,
                onClick = { store.update { it.copy(customMotion = flip(it.customMotion)) } },
                trailing = { Switch(checked = on, onCheckedChange = null) },
            )
        }
    }
}

/** Home: its layout as pictures to pick from (#12), its hot questions, Live, and where the app opens. */
@Composable
private fun HomePage(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    GroupLabel(stringResource(R.string.settings_home_layout))
    GroupBox(index = 0, count = 1) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeLayoutPicker(selected = settings.homeLayout, onSelect = { layout -> store.update { it.copy(homeLayout = layout) } })
            Text(
                stringResource(layoutSummary(settings.homeLayout)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    GroupLabel(stringResource(R.string.settings_hot_group))
    val hotShown = settings.homeLayout != HomeLayout.NewOnly
    ChoiceBox(
        title = stringResource(R.string.settings_hot_count),
        summary = if (hotShown) null else stringResource(R.string.settings_hot_hidden),
        options = AppSettings.HOT_COUNTS.map { it to it.toString() },
        selected = settings.hotCount,
        onSelect = { count -> store.update { it.copy(hotCount = count) } },
        index = 0,
        count = 2,
    )
    ChoiceBox(
        title = stringResource(R.string.settings_hot_window),
        summary = stringResource(R.string.settings_hot_window_summary),
        options = AppSettings.HOT_WINDOWS.map { it to windowLabel(it) },
        selected = settings.hotWindow,
        onSelect = { window -> store.update { it.copy(hotWindow = window) } },
        index = 1,
        count = 2,
    )

    // Live (#4) belongs to Home's new questions, so it moved here from Notifications (#12).
    GroupLabel(stringResource(R.string.settings_live_new))
    GroupBox(index = 0, count = 1) {
        LiveControls(
            settings = settings,
            onChange = { store.update(it) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }

    GroupLabel(stringResource(R.string.settings_more_home))
    GroupRow(
        title = stringResource(R.string.settings_channel_chips),
        summary = stringResource(R.string.settings_channel_chips_summary),
        index = 0,
        count = 2,
        onClick = { store.update { it.copy(showChannelChips = !it.showChannelChips) } },
        trailing = { Switch(checked = settings.showChannelChips, onCheckedChange = null) },
    )
    ChoiceBox(
        title = stringResource(R.string.settings_start_tab),
        summary = stringResource(R.string.settings_start_tab_summary),
        options = listOf(
            StartTab.Home to stringResource(R.string.tab_home),
            StartTab.PenPals to stringResource(R.string.tab_penpals),
            StartTab.Chats to stringResource(R.string.tab_chats),
            StartTab.Profile to stringResource(R.string.tab_profile),
        ),
        selected = settings.startTab,
        onSelect = { tab -> store.update { it.copy(startTab = tab) } },
        index = 1,
        count = 2,
    )

    // Swiping a question in a list (#18): Home, topics, channels and your lists.
    GroupLabel(stringResource(R.string.settings_swipe_questions))
    QuestionSwipePicture(settings, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    SwipeChoiceRow(
        title = stringResource(R.string.settings_swipe_right),
        options = SwipeRules.QUESTION_CHOICES.map { it to questionSwipeLabel(it) },
        selected = settings.swipeRight,
        onSelect = { action -> store.update { it.copy(swipeRight = action) } },
        index = 0,
        count = 2,
    )
    SwipeChoiceRow(
        title = stringResource(R.string.settings_swipe_left),
        options = SwipeRules.QUESTION_CHOICES.map { it to questionSwipeLabel(it) },
        selected = settings.swipeLeft,
        onSelect = { action -> store.update { it.copy(swipeLeft = action) } },
        index = 1,
        count = 2,
    )
}

/** A setting picked from a list in a dialog (#18): the row says what's picked now. */
@Composable
private fun <T> SwipeChoiceRow(
    title: String,
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    index: Int,
    count: Int,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: options.first().second
    GroupRow(title = title, summary = stringResource(current), index = index, count = count, onClick = { open = true })
    if (open) OptionsDialog(title, options, selected, onSelect, onDismiss = { open = false })
}

/** Picking one of [options] (a value and its words) in a dialog; choosing closes it. Settings and the intro (v0.16). */
@Composable
internal fun <T> OptionsDialog(title: String, options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** Swiping a question (#18), drawn from the current choices: Settings → Home and the intro (v0.16). */
@Composable
internal fun QuestionSwipePicture(settings: AppSettings, modifier: Modifier = Modifier) {
    SwipePreview(
        right = questionSwipeLabel(settings.swipeRight) to questionSwipeIcon(settings.swipeRight),
        left = questionSwipeLabel(settings.swipeLeft) to questionSwipeIcon(settings.swipeLeft),
        modifier = modifier,
    )
}

/**
 * Swiping an answer to the left (v0.16), drawn from the current choices: someone's answer, and one under the
 * signed-in reader's own nickname. Settings → Questions and answers and the intro.
 */
@Composable
internal fun AnswerSwipePicture(settings: AppSettings, modifier: Modifier = Modifier) {
    val session by LocalAppContainer.current.session.session.collectAsStateWithLifecycle()
    fun shown(action: AnswerSwipe) = if (action == AnswerSwipe.None) null else answerSwipeLabel(action) to answerSwipeIcon(action)
    AnswerSwipePreview(
        others = shown(settings.answerSwipe),
        mine = shown(settings.myAnswerSwipe),
        myName = session.profile?.nickname?.takeIf { it.isNotBlank() } ?: stringResource(R.string.preview_you),
        modifier = modifier,
    )
}

/** Answers: a picture of two answers that changes with the switches (#12), replies, swipes and times. */
@Composable
private fun AnswersPage(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    // Easy reply fixes its carets by editing the answer; below 50 credibility points Stips allows no editing (v0.13).
    val points = knownCredibility()
    val editLocked = points != null && !CredRules.canEdit(points)
    RepliesPreview(
        easyReply = settings.easyReply && !editLocked,
        quotes = settings.replyQuotes,
        hideCarets = settings.hideReplyCarets,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    GroupLabel(stringResource(R.string.settings_replies))
    // Hiding the carets only matters while quotes show what they point at (v0.11).
    val repliesCount = if (settings.replyQuotes) 3 else 2
    GroupRow(
        title = stringResource(R.string.settings_easy_reply),
        summary = if (editLocked) {
            stringResource(R.string.settings_easy_reply_locked, points ?: 0)
        } else {
            stringResource(R.string.settings_easy_reply_summary)
        },
        index = 0,
        count = repliesCount,
        onClick = if (editLocked) null else ({ store.update { it.copy(easyReply = !it.easyReply) } }),
        trailing = { Switch(checked = settings.easyReply && !editLocked, onCheckedChange = null, enabled = !editLocked) },
    )
    GroupRow(
        title = stringResource(R.string.settings_reply_quotes),
        summary = stringResource(R.string.settings_reply_quotes_summary),
        index = 1,
        count = repliesCount,
        onClick = { store.update { it.copy(replyQuotes = !it.replyQuotes) } },
        trailing = { Switch(checked = settings.replyQuotes, onCheckedChange = null) },
    )
    if (settings.replyQuotes) {
        GroupRow(
            title = stringResource(R.string.settings_hide_reply_carets),
            summary = stringResource(R.string.settings_hide_reply_carets_summary),
            index = 2,
            count = repliesCount,
            onClick = { store.update { it.copy(hideReplyCarets = !it.hideReplyCarets) } },
            trailing = { Switch(checked = settings.hideReplyCarets, onCheckedChange = null) },
        )
    }

    // Swiping an answer to the left (#18); to the right it replies (easy reply, above). With a picture (v0.16).
    GroupLabel(stringResource(R.string.settings_swipe_answers))
    AnswerSwipePicture(settings, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    SwipeChoiceRow(
        title = stringResource(R.string.settings_swipe_others_answer),
        options = SwipeRules.OTHERS_ANSWER_CHOICES.map { it to answerSwipeLabel(it) },
        selected = settings.answerSwipe,
        onSelect = { action -> store.update { it.copy(answerSwipe = action) } },
        index = 0,
        count = 2,
    )
    SwipeChoiceRow(
        title = stringResource(R.string.settings_swipe_my_answer),
        options = SwipeRules.MY_ANSWER_CHOICES.map { it to answerSwipeLabel(it) },
        selected = settings.myAnswerSwipe,
        onSelect = { action -> store.update { it.copy(myAnswerSwipe = action) } },
        index = 1,
        count = 2,
    )


    GroupLabel(stringResource(R.string.settings_times))
    GroupRow(
        title = stringResource(R.string.settings_live_times),
        summary = stringResource(R.string.settings_live_times_summary),
        index = 0,
        count = 1,
        onClick = { store.update { it.copy(liveTimes = !it.liveTimes) } },
        trailing = { Switch(checked = settings.liveTimes, onCheckedChange = null) },
    )
}

/** Phone notifications, with a picture of one (#12): the switch, how often, and how the checks are doing. */
@Composable
private fun NotificationsPage(settings: AppSettings, snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val store = container.settings
    val toggleNotifications = rememberPhoneNotificationsToggle(snackbar)
    val notificationsOn = settings.backgroundNotifications && Notifier.canPost(context)
    NotificationPreview(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    // While they're on: how often to check, and how the checks are doing (v0.11).
    val updatesCount = if (notificationsOn) 3 else 1
    GroupLabel(stringResource(R.string.settings_updates))
    GroupRow(
        title = stringResource(R.string.settings_phone_notifications),
        summary = stringResource(
            if (session.signedIn) R.string.settings_phone_notifications_summary else R.string.settings_phone_notifications_signed_out,
        ),
        index = 0,
        count = updatesCount,
        onClick = { toggleNotifications(!notificationsOn) },
        trailing = { Switch(checked = notificationsOn, onCheckedChange = null) },
    )
    if (notificationsOn) {
        ChoiceBox(
            title = stringResource(R.string.settings_check_every),
            summary = stringResource(R.string.settings_check_every_summary),
            options = AppSettings.CHECK_MINUTES.map { it to stringResource(R.string.check_minutes, it) },
            selected = settings.checkMinutes,
            onSelect = { minutes ->
                store.update { it.copy(checkMinutes = minutes) }
                Notifier.schedule(context.applicationContext, true, minutes)
            },
            index = 1,
            count = updatesCount,
        )
        GroupBox(index = 2, count = updatesCount) {
            PhoneNotificationsStatus(snackbar, Modifier.padding(16.dp))
        }
    }

    // The three kinds of notifications (#13): each has its own sound and pop-up in Android's settings.
    GroupLabel(stringResource(R.string.settings_notification_kinds))
    val kinds = listOf(
        Triple(NotificationCategories.Category.Updates, R.string.notif_category_updates, R.string.notif_category_updates_summary),
        Triple(NotificationCategories.Category.FlowersAndThanks, R.string.notif_category_flowers, R.string.notif_category_flowers_summary),
        Triple(NotificationCategories.Category.System, R.string.notif_category_system, R.string.notif_category_system_summary),
    )
    kinds.forEachIndexed { index, (category, title, summary) ->
        GroupRow(
            title = stringResource(title),
            summary = stringResource(summary),
            index = index,
            count = kinds.size,
            onClick = { Notifier.openCategorySettings(context, category) },
            trailing = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
        )
    }

    // The list behind the bell (#13).
    GroupLabel(stringResource(R.string.settings_notification_list))
    GroupRow(
        title = stringResource(R.string.settings_notification_details),
        summary = stringResource(R.string.settings_notification_details_summary),
        index = 0,
        count = 1,
        onClick = { store.update { it.copy(notificationDetails = !it.notificationDetails) } },
        trailing = { Switch(checked = settings.notificationDetails, onCheckedChange = null) },
    )
    Text(
        stringResource(R.string.settings_notifications_chats_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
    )
}

@Composable
private fun WidgetsPage(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val widgetByHandText = stringResource(R.string.settings_widget_by_hand)
    Spacer(Modifier.height(8.dp))
    GroupRow(
        title = stringResource(R.string.settings_widget),
        summary = stringResource(R.string.settings_widget_summary),
        index = 0,
        count = 2,
        onClick = {
            if (!requestPinHotWidget(context)) scope.launch { snackbar.showSnackbar(widgetByHandText) }
        },
        icon = Icons.Outlined.Widgets,
    )
    GroupRow(
        title = stringResource(R.string.settings_widget_chats),
        summary = stringResource(R.string.settings_widget_chats_summary),
        index = 1,
        count = 2,
        onClick = {
            if (!requestPinChatsWidget(context)) scope.launch { snackbar.showSnackbar(widgetByHandText) }
        },
        icon = Icons.AutoMirrored.Outlined.Chat,
    )
}

@Composable
private fun PrivacyPage(settings: AppSettings, snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var anonymousDialog by remember { mutableStateOf(false) }
    var clearBlocks by remember { mutableStateOf(false) }
    val blockVersion by container.blocks.version.collectAsStateWithLifecycle()
    val blockCount = remember(blockVersion) { container.blocks.count }
    val anonymousOn = rememberAnonymousModeOn(settings.anonymousUntil)
    val blocksClearedText = stringResource(R.string.blocks_cleared)
    if (anonymousDialog) {
        AnonymousModeDialog(
            until = settings.anonymousUntil,
            onSet = { value ->
                container.settings.update { it.copy(anonymousUntil = value) }
                anonymousDialog = false
            },
            onDismiss = { anonymousDialog = false },
        )
    }
    if (clearBlocks) {
        AlertDialog(
            onDismissRequest = { clearBlocks = false },
            title = { Text(stringResource(R.string.blocks_clear_title)) },
            text = { Text(stringResource(R.string.blocks_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    clearBlocks = false
                    container.blocks.clear()
                    scope.launch { snackbar.showSnackbar(blocksClearedText) }
                }) { Text(stringResource(R.string.blocks_clear_confirm)) }
            },
            dismissButton = { TextButton(onClick = { clearBlocks = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    Spacer(Modifier.height(8.dp))
    GroupRow(
        title = stringResource(R.string.anon_mode_title),
        summary = stringResource(if (anonymousOn) R.string.settings_anon_on else R.string.settings_anon_off),
        index = 0,
        count = 2,
        onClick = { anonymousDialog = true },
        icon = Icons.Outlined.TheaterComedy,
    )
    GroupRow(
        title = stringResource(R.string.settings_blocks),
        summary = if (blockCount == 0) {
            stringResource(R.string.settings_blocks_none)
        } else {
            LocalContext.current.resources.getQuantityString(R.plurals.settings_blocks_count, blockCount, blockCount)
        },
        index = 1,
        count = 2,
        onClick = if (blockCount == 0) null else ({ clearBlocks = true }),
        icon = Icons.Outlined.VisibilityOff,
    )
}

/** The pages from the site's footer, opened in the browser. */
@Composable
private fun StipsPage() {
    val context = LocalContext.current
    val external = externalIcon()
    Spacer(Modifier.height(8.dp))
    GroupRow(stringResource(R.string.settings_contact), 0, 3, { openInBrowser(context, "$SITE_URL/contact-us") }, trailing = external)
    GroupRow(stringResource(R.string.settings_rules), 1, 3, { openInBrowser(context, "$SITE_URL/rules") }, trailing = external)
    GroupRow(stringResource(R.string.settings_about_stips), 2, 3, { openInBrowser(context, "$SITE_URL/about") }, trailing = external)
}

/** The app itself: version and code, the intro again, and expert mode with its tools (moved here in #12). */
@Composable
private fun AboutPage(container: AppContainer, settings: AppSettings, onOpenPage: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val showIntro = LocalShowIntro.current
    Spacer(Modifier.height(8.dp))
    GroupRow(stringResource(R.string.about_version), 0, 5, null, summary = container.versionName)
    GroupRow(
        stringResource(R.string.about_source),
        1,
        5,
        { openInBrowser(context, AppContainer.REPO_URL) },
        summary = stringResource(R.string.about_source_summary),
    )
    GroupRow(
        stringResource(R.string.about_license),
        2,
        5,
        { openInBrowser(context, "https://www.gnu.org/licenses/gpl-3.0.html") },
        summary = stringResource(R.string.about_license_summary),
    )
    GroupRow(
        stringResource(R.string.about_report),
        3,
        5,
        { openInBrowser(context, AppContainer.REPO_URL + "/issues/new") },
        summary = stringResource(R.string.about_report_summary),
    )
    GroupRow(stringResource(R.string.disclaimer), 4, 5, null)

    // Stips+'s own updates from GitHub (#25), while it isn't on Google Play.
    if (UpdateRules.SELF_UPDATE) {
        val updates = container.updates
        val update by updates.state.collectAsStateWithLifecycle()
        GroupLabel(stringResource(R.string.settings_updates_group))
        GroupRow(
            title = updateRowTitle(update),
            summary = updateRowSummary(update),
            index = 0,
            count = 2,
            onClick = {
                val release = update.available
                when {
                    update.checking -> Unit
                    release != null -> openInBrowser(context, UpdateRules.downloadUrl(release))
                    else -> updates.checkNow()
                }
            },
            icon = Icons.Outlined.SystemUpdate,
            trailing = if (update.checking) {
                { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
            } else {
                null
            },
        )
        GroupRow(
            title = stringResource(R.string.settings_update_auto),
            summary = stringResource(R.string.settings_update_auto_summary),
            index = 1,
            count = 2,
            onClick = { container.settings.update { it.copy(checkUpdates = !it.checkUpdates) } },
            trailing = { Switch(checked = settings.checkUpdates, onCheckedChange = null) },
        )
    }

    GroupLabel(stringResource(R.string.settings_group_more))
    GroupRow(
        title = stringResource(R.string.settings_show_intro),
        summary = stringResource(R.string.settings_show_intro_summary),
        index = 0,
        count = 2,
        onClick = showIntro,
        icon = Icons.Outlined.AutoAwesome,
    )
    // The tips (#28) start over: each shows once more, unless its gesture is used first.
    val tipsResetText = stringResource(R.string.tips_reset_done)
    GroupRow(
        title = stringResource(R.string.settings_reset_tips),
        summary = stringResource(R.string.settings_reset_tips_summary),
        index = 1,
        count = 2,
        onClick = {
            container.tutorials.reset()
            Toast.makeText(context, tipsResetText, Toast.LENGTH_SHORT).show()
        },
        icon = Icons.Outlined.TouchApp,
    )

    GroupLabel(stringResource(R.string.settings_expert_group))
    val store = container.settings
    val expertRows = if (settings.expertMode) 2 else 1
    GroupRow(
        title = stringResource(R.string.settings_expert),
        summary = stringResource(R.string.settings_expert_summary),
        index = 0,
        count = expertRows,
        onClick = { store.update { it.copy(expertMode = !it.expertMode) } },
        icon = Icons.Outlined.Code,
        trailing = { Switch(checked = settings.expertMode, onCheckedChange = null) },
    )
    if (settings.expertMode) {
        GroupRow(
            stringResource(R.string.settings_diagnostics),
            1,
            expertRows,
            { onOpenPage(SettingsPage.Diagnostics) },
            summary = stringResource(R.string.settings_diagnostics_summary),
            icon = Icons.Outlined.BugReport,
        )
    }
}

/** Expert mode's tools (#8): the network log and recent load times. */
@Composable
private fun DiagnosticsPage(container: AppContainer, onOpenNetLog: () -> Unit) {
    val timings by container.timings.recent.collectAsStateWithLifecycle()
    Spacer(Modifier.height(8.dp))
    GroupRow(
        stringResource(R.string.netlog_title),
        0,
        1,
        onOpenNetLog,
        summary = stringResource(R.string.netlog_summary),
    )
    GroupLabel(stringResource(R.string.settings_performance))
    if (timings.isEmpty()) {
        GroupRow(stringResource(R.string.settings_performance_empty), 0, 1, null)
    } else {
        val shown = timings.take(8)
        shown.forEachIndexed { index, timing ->
            GroupRow(
                title = stringResource(if (timing.label == Timings.QUESTION) R.string.timing_question else R.string.timing_feed),
                index = index,
                count = shown.size,
                onClick = null,
                trailing = {
                    Text(stringResource(R.string.timing_value, String.format(Locale.getDefault(), "%.2f", timing.millis / 1000.0)))
                },
            )
        }
    }
}

@Composable
private fun externalIcon(): @Composable () -> Unit {
    val description = stringResource(R.string.opens_in_browser)
    return { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = description) }
}

/** A block in a settings group, shaped like [GroupRow] for its place in the group. */
@Composable
private fun GroupBox(index: Int, count: Int, content: @Composable () -> Unit) {
    val outer: Dp = 24.dp
    val inner: Dp = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 2.dp)
            .fillMaxWidth(),
    ) { content() }
}

/** A title, an optional line under it, and a row of buttons to pick one option. */
@Composable
private fun <T> ChoiceBox(
    title: String,
    summary: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    index: Int,
    count: Int,
) {
    GroupBox(index, count) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                options.forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** An accent color to pick (Look, the intro). */
@Composable
internal fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    val label = "#" + String.format(Locale.ROOT, "%06X", color and 0xFFFFFF)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
    }
}
