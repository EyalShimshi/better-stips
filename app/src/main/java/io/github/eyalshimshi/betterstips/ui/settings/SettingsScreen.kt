package io.github.eyalshimshi.betterstips.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AnswersOrder
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.HomeLayout
import io.github.eyalshimshi.betterstips.data.StartTab
import io.github.eyalshimshi.betterstips.data.ThemeMode
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.components.AnonymousModeDialog
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.rememberAnonymousModeOn
import io.github.eyalshimshi.betterstips.ui.feed.windowLabel
import io.github.eyalshimshi.betterstips.ui.home.LiveControls
import io.github.eyalshimshi.betterstips.ui.notifications.PhoneNotificationsStatus
import io.github.eyalshimshi.betterstips.ui.notifications.rememberPhoneNotificationsToggle
import io.github.eyalshimshi.betterstips.ui.profile.GroupLabel
import io.github.eyalshimshi.betterstips.ui.profile.GroupRow
import io.github.eyalshimshi.betterstips.widget.requestPinChatsWidget
import io.github.eyalshimshi.betterstips.widget.requestPinHotWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/** The pages of Settings (#8): the main page lists them, and each opens on its own (route `settings/{page}`). */
enum class SettingsPage(val route: String) {
    Account("account"),
    Reading("reading"),
    Appearance("appearance"),
    Notifications("notifications"),
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
 * Settings: the main page ([page] null) lists the pages, expert mode, and (in expert mode) diagnostics; a page shows
 * its own options. Dialogs and messages live here so every page can use them.
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
            SettingsPage.Reading -> R.string.settings_reading
            SettingsPage.Appearance -> R.string.settings_appearance
            SettingsPage.Notifications -> R.string.settings_notifications_widgets
            SettingsPage.Privacy -> R.string.settings_privacy_safety
            SettingsPage.Stips -> R.string.settings_stips
            SettingsPage.About -> R.string.settings_about
            SettingsPage.Diagnostics -> R.string.settings_diagnostics
        },
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
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
                SettingsPage.Reading -> ReadingPage(settings)
                SettingsPage.Appearance -> AppearancePage(settings)
                SettingsPage.Notifications -> NotificationsPage(settings, snackbar)
                SettingsPage.Privacy -> PrivacyPage(settings, snackbar)
                SettingsPage.Stips -> StipsPage()
                SettingsPage.About -> AboutPage(container)
                SettingsPage.Diagnostics -> DiagnosticsPage(container, onOpenNetLog)
            }
        }
    }
}

@Composable
private fun MainPage(settings: AppSettings, onOpenPage: (SettingsPage) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val store = container.settings
    Spacer(Modifier.height(8.dp))
    val rows = 7
    if (session.signedIn) {
        GroupRow(
            stringResource(R.string.settings_account),
            0,
            rows,
            { onOpenPage(SettingsPage.Account) },
            summary = session.profile?.nickname?.let { stringResource(R.string.settings_account_as, it) }
                ?: stringResource(R.string.settings_account_summary),
            icon = Icons.Outlined.AccountCircle,
        )
    } else {
        GroupRow(
            stringResource(R.string.sign_in_title),
            0,
            rows,
            onSignIn,
            summary = stringResource(R.string.settings_sign_in_summary),
            icon = Icons.AutoMirrored.Outlined.Login,
        )
    }
    GroupRow(
        stringResource(R.string.settings_reading),
        1,
        rows,
        { onOpenPage(SettingsPage.Reading) },
        summary = stringResource(R.string.settings_reading_summary),
        icon = Icons.Outlined.Home,
    )
    GroupRow(
        stringResource(R.string.settings_appearance),
        2,
        rows,
        { onOpenPage(SettingsPage.Appearance) },
        summary = stringResource(R.string.settings_appearance_summary),
        icon = Icons.Outlined.Palette,
    )
    GroupRow(
        stringResource(R.string.settings_notifications_widgets),
        3,
        rows,
        { onOpenPage(SettingsPage.Notifications) },
        summary = stringResource(R.string.settings_notifications_widgets_summary),
        icon = Icons.Outlined.Notifications,
    )
    GroupRow(
        stringResource(R.string.settings_privacy_safety),
        4,
        rows,
        { onOpenPage(SettingsPage.Privacy) },
        summary = stringResource(R.string.settings_privacy_safety_summary),
        icon = Icons.Outlined.Lock,
    )
    GroupRow(
        stringResource(R.string.settings_stips),
        5,
        rows,
        { onOpenPage(SettingsPage.Stips) },
        summary = stringResource(R.string.settings_stips_summary),
        icon = Icons.Outlined.Public,
    )
    GroupRow(
        stringResource(R.string.settings_about),
        6,
        rows,
        { onOpenPage(SettingsPage.About) },
        summary = stringResource(R.string.settings_about_summary, container.versionName),
        icon = Icons.Outlined.Info,
    )
    GroupLabel(stringResource(R.string.settings_expert_group))
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
        5,
        onOpenMyProfile,
        summary = stringResource(R.string.settings_edit_profile_summary),
    )
    GroupRow(
        title = stringResource(R.string.settings_safe_mode),
        summary = stringResource(R.string.settings_safe_mode_summary),
        index = 1,
        count = 5,
        onClick = if (safeBusy) null else ({ toggleSafeMode() }),
        trailing = { Switch(checked = session.safeFilter, onCheckedChange = null, enabled = !safeBusy) },
    )
    GroupRow(
        stringResource(R.string.settings_account_details),
        2,
        5,
        { openInBrowser(context, "$SITE_URL/settings/account-details") },
        summary = stringResource(R.string.settings_on_site),
        trailing = external,
    )
    GroupRow(
        stringResource(R.string.settings_change_password),
        3,
        5,
        { openInBrowser(context, "$SITE_URL/settings/change-password") },
        summary = stringResource(R.string.settings_on_site),
        trailing = external,
    )
    GroupRow(stringResource(R.string.sign_out), 4, 5, { scope.launch { container.session.signOut() } })
}

/** Home's layout and hot questions, how answers are ordered, and how times show (#8). */
@Composable
private fun ReadingPage(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    GroupLabel(stringResource(R.string.settings_home_layout))
    val layouts = listOf(
        Triple(HomeLayout.HotAndNew, R.string.layout_hot_new, R.string.layout_hot_new_summary),
        Triple(HomeLayout.LikeWebsite, R.string.layout_website, R.string.layout_website_summary),
        Triple(HomeLayout.NewOnly, R.string.layout_new_only, R.string.layout_new_only_summary),
    )
    layouts.forEachIndexed { index, (layout, label, summary) ->
        GroupRow(
            title = stringResource(label),
            summary = stringResource(summary),
            index = index,
            count = layouts.size,
            onClick = { store.update { it.copy(homeLayout = layout) } },
            trailing = { RadioButton(selected = settings.homeLayout == layout, onClick = null) },
        )
    }

    GroupLabel(stringResource(R.string.settings_home))
    val hotShown = settings.homeLayout != HomeLayout.NewOnly
    ChoiceBox(
        title = stringResource(R.string.settings_hot_count),
        summary = if (hotShown) null else stringResource(R.string.settings_hot_hidden),
        options = AppSettings.HOT_COUNTS.map { it to it.toString() },
        selected = settings.hotCount,
        onSelect = { count -> store.update { it.copy(hotCount = count) } },
        index = 0,
        count = 4,
    )
    ChoiceBox(
        title = stringResource(R.string.settings_hot_window),
        summary = stringResource(R.string.settings_hot_window_summary),
        options = AppSettings.HOT_WINDOWS.map { it to windowLabel(it) },
        selected = settings.hotWindow,
        onSelect = { window -> store.update { it.copy(hotWindow = window) } },
        index = 1,
        count = 4,
    )
    GroupRow(
        title = stringResource(R.string.settings_channel_chips),
        summary = stringResource(R.string.settings_channel_chips_summary),
        index = 2,
        count = 4,
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
        index = 3,
        count = 4,
    )

    GroupLabel(stringResource(R.string.settings_answers_order))
    val orders = listOf(
        AnswersOrder.AsOnStips to R.string.answers_order_stips,
        AnswersOrder.Newest to R.string.answers_order_newest,
        AnswersOrder.Oldest to R.string.answers_order_oldest,
        AnswersOrder.TopVoted to R.string.answers_order_top,
    )
    orders.forEachIndexed { index, (order, label) ->
        GroupRow(
            title = stringResource(label),
            index = index,
            count = orders.size,
            onClick = { store.update { it.copy(answersOrder = order) } },
            trailing = { RadioButton(selected = settings.answersOrder == order, onClick = null) },
        )
    }

    GroupLabel(stringResource(R.string.settings_replies))
    // Hiding the carets only matters while quotes show what they point at (v0.11).
    val repliesCount = if (settings.replyQuotes) 4 else 3
    GroupRow(
        title = stringResource(R.string.settings_easy_reply),
        summary = stringResource(R.string.settings_easy_reply_summary),
        index = 0,
        count = repliesCount,
        onClick = { store.update { it.copy(easyReply = !it.easyReply) } },
        trailing = { Switch(checked = settings.easyReply, onCheckedChange = null) },
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
    GroupRow(
        title = stringResource(R.string.settings_moderator_badges),
        summary = stringResource(R.string.settings_moderator_badges_summary),
        index = repliesCount - 1,
        count = repliesCount,
        onClick = { store.update { it.copy(moderatorBadges = !it.moderatorBadges) } },
        trailing = { Switch(checked = settings.moderatorBadges, onCheckedChange = null) },
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

@Composable
private fun AppearancePage(settings: AppSettings) {
    val store = LocalAppContainer.current.settings
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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
                val selected = !settings.dynamicColor && settings.accent == accent
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
        count = 2,
        onClick = { store.update { it.copy(showImages = !it.showImages) } },
        trailing = { Switch(checked = settings.showImages, onCheckedChange = null) },
    )
    GroupRow(
        title = stringResource(R.string.settings_animations),
        summary = stringResource(R.string.settings_animations_summary),
        index = 1,
        count = 2,
        onClick = { store.update { it.copy(animations = !it.animations) } },
        trailing = { Switch(checked = settings.animations, onCheckedChange = null) },
    )
}

@Composable
private fun NotificationsPage(settings: AppSettings, snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = container.settings
    val toggleNotifications = rememberPhoneNotificationsToggle(snackbar)
    val notificationsOn = settings.backgroundNotifications && Notifier.canPost(context)
    val widgetByHandText = stringResource(R.string.settings_widget_by_hand)
    // While they're on: how often to check, and how the checks are doing (v0.11).
    val updatesCount = if (notificationsOn) 4 else 2
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
    GroupBox(index = updatesCount - 1, count = updatesCount) {
        LiveControls(
            settings = settings,
            onChange = { store.update(it) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
    GroupLabel(stringResource(R.string.settings_widgets))
    GroupRow(
        title = stringResource(R.string.settings_widget),
        summary = stringResource(R.string.settings_widget_summary),
        index = 0,
        count = 2,
        onClick = {
            if (!requestPinHotWidget(context)) scope.launch { snackbar.showSnackbar(widgetByHandText) }
        },
    )
    GroupRow(
        title = stringResource(R.string.settings_widget_chats),
        summary = stringResource(R.string.settings_widget_chats_summary),
        index = 1,
        count = 2,
        onClick = {
            if (!requestPinChatsWidget(context)) scope.launch { snackbar.showSnackbar(widgetByHandText) }
        },
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

/** The pages from the site's settings and footer, opened in the browser. */
@Composable
private fun StipsPage() {
    val context = LocalContext.current
    val external = externalIcon()
    Spacer(Modifier.height(8.dp))
    GroupRow(stringResource(R.string.settings_contact), 0, 4, { openInBrowser(context, "$SITE_URL/contact-us") }, trailing = external)
    GroupRow(stringResource(R.string.settings_rules), 1, 4, { openInBrowser(context, "$SITE_URL/rules") }, trailing = external)
    GroupRow(stringResource(R.string.settings_about_stips), 2, 4, { openInBrowser(context, "$SITE_URL/about") }, trailing = external)
    GroupRow(
        stringResource(R.string.settings_delete_account),
        3,
        4,
        { openInBrowser(context, "$SITE_URL/delete-account") },
        summary = stringResource(R.string.settings_on_site),
        trailing = external,
    )
}

@Composable
private fun AboutPage(container: AppContainer) {
    val context = LocalContext.current
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

@Composable
private fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit) {
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
