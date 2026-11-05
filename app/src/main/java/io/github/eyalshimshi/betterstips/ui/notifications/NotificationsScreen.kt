package io.github.eyalshimshi.betterstips.ui.notifications

import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import androidx.compose.material.icons.outlined.Report
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.ExpertLine
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.ServerDetailsDialog
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.JumpRules
import io.github.eyalshimshi.betterstips.util.NotifyRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val items: List<StipsNotification> = emptyList(),
    val page: Int = 0,
    val end: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
    val error: ApiException? = null,
    /** New since the user last looked (kept for this visit, even after a refresh). */
    val newIds: Set<Long> = emptySet(),
)

class NotificationsViewModel(
    private val repository: StipsRepository,
    private val seen: SeenNotifications,
    private val userId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()
    private var generation = 0

    /** Opened during this visit: read, whatever Stips' flag says on a refresh. */
    private val opened = mutableSetOf<Long>()

    init {
        refresh()
    }

    fun refresh() {
        val gen = ++generation
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val list = repository.notifications(page = 1)
                if (gen != generation) return@launch
                markNew(list)
                _state.update { it.copy(items = list, page = 1, end = list.isEmpty(), refreshing = false, moreFailed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(refreshing = false, error = e.asApiException()) }
            }
        }
    }

    /** Remembers which items are new now, then records the newest one as seen. */
    private fun markNew(list: List<StipsNotification>) {
        val me = userId ?: return
        val fresh = SeenNotifications.newIds(list.map { it.id to it.viewed }, seen.lastSeenId(me)) - opened
        _state.update { it.copy(newIds = it.newIds + fresh) }
        list.maxOfOrNull { it.id }?.let { seen.markSeen(me, it) }
    }

    /** The user opened this one: it's read, so it leaves "New" right away rather than on the next visit (#6). */
    fun markRead(id: Long) {
        opened += id
        _state.update { it.copy(newIds = it.newIds - id) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.end || s.refreshing || s.page == 0) return
        val gen = generation
        val next = s.page + 1
        _state.update { it.copy(loadingMore = true, moreFailed = false) }
        viewModelScope.launch {
            try {
                val list = repository.notifications(page = next)
                if (gen != generation) return@launch
                _state.update { st ->
                    st.copy(items = (st.items + list).distinctBy { it.id }, page = next, end = list.isEmpty(), loadingMore = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(loadingMore = false, moreFailed = true) }
            }
        }
    }
}

/**
 * Stips' notification bell (answers to your questions, flowers, thanks, badges). Opening the list is what marks
 * them seen on Stips, as on the site, so [onSeen] clears the badge once the first page arrives.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenQuestion: (Long, QuestionFocus?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onSignIn: () -> Unit,
    onSeen: () -> Unit,
) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.notifications_title)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (session.signedIn) {
            NotificationsList(Modifier.padding(padding), snackbar, onOpenQuestion, onOpenProfile, onSeen)
        } else {
            Column(Modifier.padding(padding)) {
                MessageCard(
                    stringResource(R.string.notifications_signed_out_title),
                    stringResource(R.string.notifications_signed_out_body),
                    stringResource(R.string.action_sign_in),
                    onSignIn,
                )
            }
        }
    }
}

@Composable
private fun NotificationsList(
    modifier: Modifier,
    snackbar: SnackbarHostState,
    onOpenQuestion: (Long, QuestionFocus?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onSeen: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm = appViewModel { NotificationsViewModel(it.repository, it.seenNotifications, it.session.session.value.userId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val toggle = rememberPhoneNotificationsToggle(snackbar)
    val currentOnSeen by rememberUpdatedState(onSeen)
    var details by remember { mutableStateOf<StipsNotification?>(null) }
    details?.let { item ->
        ServerDetailsDialog(
            title = stringResource(R.string.expert_details),
            fields = notificationFields(item),
            raw = listOf("notifications.list" to item.raw),
            onDismiss = { details = null },
        )
    }
    val onDetails: ((StipsNotification) -> Unit)? = if (settings.expertMode) ({ details = it }) else null
    LaunchedEffect(state.page, state.refreshing) {
        if (state.page >= 1 && !state.refreshing) {
            currentOnSeen()
            // Seen here, so they leave the phone's notification shade (v0.11).
            Notifier.clearUpdates(context)
        }
    }
    PullToRefreshBox(isRefreshing = state.refreshing && state.page > 0, onRefresh = vm::refresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (!settings.backgroundNotifications || !Notifier.canPost(context)) {
                item(key = "phone") {
                    MessageCard(
                        stringResource(R.string.notifications_phone_title),
                        stringResource(R.string.notifications_phone_body),
                        stringResource(R.string.notifications_phone_turn_on),
                        { toggle(true) },
                    )
                }
            }
            val error = state.error
            when {
                state.page == 0 && error != null -> item(key = "error") { ErrorCard(error, onRetry = vm::refresh) }
                state.page == 0 -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.items.isEmpty() -> item(key = "empty") {
                    Text(
                        stringResource(R.string.notifications_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                    )
                }
                else -> {
                    val (fresh, earlier) = state.items.partition { it.id in state.newIds }
                    val open = { item: StipsNotification ->
                        targetAction(item, onOpenQuestion, onOpenProfile)?.let { action -> { vm.markRead(item.id); action() } }
                    }
                    if (fresh.isNotEmpty()) {
                        item(key = "new-label") { SectionLabel(stringResource(R.string.notifications_new)) }
                        items(fresh, key = { it.id }) { item -> NotificationRow(item, isNew = true, onClick = open(item), onDetails = onDetails) }
                    }
                    if (earlier.isNotEmpty()) {
                        if (fresh.isNotEmpty()) item(key = "earlier-label") { SectionLabel(stringResource(R.string.notifications_earlier)) }
                        items(earlier, key = { it.id }) { item -> NotificationRow(item, isNew = false, onClick = open(item), onDetails = onDetails) }
                    }
                    item(key = "more") {
                        when {
                            state.loadingMore -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }
                            state.moreFailed -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.action_retry)) } }
                            !state.end -> LaunchedEffect(state.page) { vm.loadMore() }
                        }
                    }
                }
            }
        }
    }
}

private fun targetAction(item: StipsNotification, onOpenQuestion: (Long, QuestionFocus?) -> Unit, onOpenProfile: (Long) -> Unit): (() -> Unit)? =
    when (val target = item.target) {
        // New answers open at the first new one, a flower at the answer that got it (v0.10).
        is NotificationTarget.OpenQuestion -> ({ onOpenQuestion(target.id, JumpRules.focusFor(item.type, item.text, item.time)) })
        is NotificationTarget.OpenProfile -> ({ onOpenProfile(target.id) })
        null -> null
    }

/** Expert mode's details of a notification: what Stips sent, and where the app opens it (v0.10). */
private fun notificationFields(item: StipsNotification): List<Pair<String, String>> {
    val focus = (item.target as? NotificationTarget.OpenQuestion)?.let { JumpRules.focusFor(item.type, item.text, item.time) }
    return listOf(
        "id" to item.id.toString(),
        "type" to item.type.ifBlank { "-" },
        "target" to when (val target = item.target) {
            is NotificationTarget.OpenQuestion -> "ask ${target.id}"
            is NotificationTarget.OpenProfile -> "profile ${target.id}"
            null -> "-"
        },
        "viewed" to item.viewed.toString(),
        "time" to (item.time?.toString() ?: "-"),
        "opens at" to when (focus) {
            null -> "-"
            QuestionFocus.MyFlower -> "your answer with a flower"
            is QuestionFocus.NewAnswers -> "new answers" + (focus.count?.let { " ($it)" } ?: "")
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** One notification: the same height and layout for all, two lines at most, new ones highlighted (#4). */
@Composable
private fun NotificationRow(item: StipsNotification, isNew: Boolean, onClick: (() -> Unit)?, onDetails: ((StipsNotification) -> Unit)?) {
    val background = if (isNew) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (item.pointsRemoved > 0) Icons.Outlined.Report else notificationIcon(item.type),
                contentDescription = null,
                tint = if (isNew) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val extra = when {
                item.pointsRemoved > 0 -> stringResource(R.string.notifications_points_removed, item.pointsRemoved)
                else -> item.note
            }
            if (!extra.isNullOrBlank()) {
                Text(
                    extra,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.pointsRemoved > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onDetails != null) ExpertLine("${item.type.ifBlank { "-" }} · #${item.id}")
        }
        if (onDetails != null) {
            IconButton(onClick = { onDetails(item) }) {
                Icon(Icons.Outlined.Code, contentDescription = stringResource(R.string.expert_details), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                notificationTime(item.time),
                style = MaterialTheme.typography.labelSmall,
                color = if (isNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (isNew) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/** Like the site: the time for today, "yesterday", or the date. */
@Composable
private fun notificationTime(time: Instant?): String {
    if (time == null) return ""
    val zone = ZoneId.systemDefault()
    val day = time.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        today -> time.atZone(zone).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        today.minusDays(1) -> stringResource(R.string.time_yesterday)
        else -> day.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
    }
}

/** Stips' notification types are words like "new_ask_ans_registered"; the icon follows the main word. */
private fun notificationIcon(type: String): ImageVector {
    val t = type.lowercase()
    return when {
        "flower" in t -> Icons.Outlined.LocalFlorist
        "thank" in t -> Icons.Outlined.VolunteerActivism
        "ans" in t -> Icons.Outlined.QuestionAnswer
        "ask" in t || "question" in t -> Icons.AutoMirrored.Outlined.HelpOutline
        "msg" in t || "message" in t -> Icons.AutoMirrored.Outlined.Chat
        "badge" in t || "rank" in t -> Icons.Outlined.WorkspacePremium
        else -> Icons.Outlined.Notifications
    }
}

/**
 * Turns phone notifications on (asking for Android's permission first, on Android 13 and later) or off. When the
 * permission is refused, [snackbar] explains and offers Android's notification settings. Once they're on, Android's
 * own question about running in the background follows, unless it already allows that (v0.11).
 */
@Composable
fun rememberPhoneNotificationsToggle(snackbar: SnackbarHostState): (Boolean) -> Unit {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.notifications_permission_denied)
    val settingsText = stringResource(R.string.action_open_settings)
    val refusedText = stringResource(R.string.notifications_job_refused)
    fun setEnabled(on: Boolean) {
        // Schedule first: only a job Android accepted counts as "on" (#1, #2).
        val minutes = container.settings.settings.value.checkMinutes
        val scheduled = Notifier.schedule(context.applicationContext, on, minutes)
        container.settings.update { it.copy(backgroundNotifications = on && scheduled) }
        if (on && !scheduled) scope.launch { snackbar.showSnackbar(refusedText) }
        // While Android saves battery on Stips+, it holds the checks back for hours.
        if (on && scheduled && !Notifier.runsInBackground(context)) Notifier.askToRunInBackground(context)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setEnabled(true)
        } else {
            scope.launch {
                val result = snackbar.showSnackbar(deniedText, actionLabel = settingsText, withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) Notifier.openNotificationSettings(context)
            }
        }
    }
    return { on ->
        if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setEnabled(on)
        }
    }
}

/** Without a check for this long, Android is holding Stips+ back (the job's period is 15 minutes). */
private const val LATE_MS = 60 * 60_000L

/**
 * How phone notifications are doing (Settings, v0.11): the last check; what in Android holds them back (notifications
 * turned off, restricted background use, battery saving), each with a way to fix it; "Check now" and a test.
 */
@Composable
fun PhoneNotificationsStatus(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val status by remember { Notifier.status(context.applicationContext) }.collectAsStateWithLifecycle()
    // Android's settings change while the user is away (its battery question, app info): read them again on return.
    var returns by remember { mutableIntStateOf(0) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { returns++ }
    }
    val background = remember(returns) { Notifier.runsInBackground(context) }
    val restricted = remember(returns) { Notifier.backgroundRestricted(context) }
    val blocked = remember(returns) { Notifier.blockedInAndroid(context) }
    val lastRun = remember(returns, status) { Notifier.lastRunAt(context) }
    var checking by remember { mutableStateOf(false) }
    val resources = context.resources
    val nothingText = stringResource(R.string.notify_checked_nothing)
    val failedText = stringResource(R.string.notify_checked_failed)
    val testText = stringResource(R.string.notify_test_sent)
    val deniedText = stringResource(R.string.notifications_permission_denied)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(statusLine(status), style = MaterialTheme.typography.bodyMedium)
        if (blocked) {
            StatusWarning(Icons.Outlined.NotificationsOff, null, stringResource(R.string.notify_blocked), stringResource(R.string.action_open_settings)) {
                Notifier.openNotificationSettings(context)
            }
        }
        if (restricted) {
            StatusWarning(Icons.Outlined.Block, null, stringResource(R.string.notify_restricted), stringResource(R.string.action_open_settings)) {
                Notifier.openAppSettings(context)
            }
        }
        if (!background) {
            StatusWarning(
                Icons.Outlined.BatteryAlert,
                stringResource(R.string.notify_battery_title),
                stringResource(R.string.notify_battery_body),
                stringResource(R.string.notify_battery_allow),
            ) { Notifier.askToRunInBackground(context) }
        } else if (lastRun > 0 && System.currentTimeMillis() - lastRun > LATE_MS) {
            StatusWarning(
                Icons.Outlined.Schedule,
                null,
                stringResource(R.string.notify_status_late, checkTime(lastRun)),
                stringResource(R.string.action_open_settings),
            ) { Notifier.openAppSettings(context) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                enabled = !checking,
                onClick = {
                    checking = true
                    scope.launch {
                        val result = Notifier.run(context.applicationContext, container, Notifier.Trigger.User)
                        checking = false
                        val message = when (result?.outcome) {
                            NotifyRules.Outcome.Posted -> {
                                val count = result?.posted ?: 0
                                resources.getQuantityString(R.plurals.notify_checked_posted, count, count)
                            }
                            NotifyRules.Outcome.Nothing -> nothingText
                            else -> failedText
                        }
                        snackbar.showSnackbar(message)
                    }
                },
            ) { Text(stringResource(R.string.notify_check_now)) }
            TextButton(
                onClick = {
                    val sent = Notifier.postTest(context)
                    scope.launch { snackbar.showSnackbar(if (sent) testText else deniedText) }
                },
            ) { Text(stringResource(R.string.notify_send_test)) }
        }
        if (settings.expertMode) {
            val bucket = remember(returns) { Notifier.standbyBucket(context) ?: "?" }
            val ran = if (lastRun > 0) checkTime(lastRun) else "never"
            ExpertLine("standby: $bucket · last run: $ran · every ${settings.checkMinutes} min · background: $background")
        }
    }
}

@Composable
private fun statusLine(status: NotifyRules.Status?): String {
    if (status == null) return stringResource(R.string.notify_status_never)
    val time = checkTime(status.at)
    return when (status.outcome) {
        NotifyRules.Outcome.Posted ->
            LocalContext.current.resources.getQuantityString(R.plurals.notify_status_posted, status.posted, time, status.posted)
        NotifyRules.Outcome.Nothing -> stringResource(R.string.notify_status_nothing, time)
        NotifyRules.Outcome.Failed -> stringResource(R.string.notify_status_failed, time, status.error ?: "?")
    }
}

/** "14:32" today, or a short date and time before that. */
private fun checkTime(at: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(at).atZone(zone)
    val format = if (time.toLocalDate() == LocalDate.now(zone)) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
    return time.format(format)
}

@Composable
private fun StatusWarning(icon: ImageVector, title: String?, text: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/**
 * Once, for a signed-in user (v0.11): turn phone notifications on, or, when they're on but Android saves battery on
 * Stips+, let it run in the background. Either answer ends it; Settings has both.
 */
@Composable
fun NotificationsPrompt(active: Boolean, snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    // Before any early return: the permission's answer comes back to this launcher.
    val toggle = rememberPhoneNotificationsToggle(snackbar)
    if (!active || !session.signedIn || settings.notificationsPrompted) return
    val on = settings.backgroundNotifications && Notifier.canPost(context)
    val background = remember { Notifier.runsInBackground(context) }
    val done = { container.settings.update { it.copy(notificationsPrompted = true) } }
    if (on && background) {
        LaunchedEffect(Unit) { done() }
        return
    }
    AlertDialog(
        onDismissRequest = done,
        icon = { Icon(if (on) Icons.Outlined.BatteryAlert else Icons.Outlined.NotificationsActive, contentDescription = null) },
        title = { Text(stringResource(if (on) R.string.notify_prompt_battery_title else R.string.notify_prompt_title)) },
        text = { Text(stringResource(if (on) R.string.notify_prompt_battery_body else R.string.notify_prompt_body)) },
        confirmButton = {
            TextButton(
                onClick = {
                    done()
                    if (on) Notifier.askToRunInBackground(context) else toggle(true)
                },
            ) { Text(stringResource(if (on) R.string.notify_battery_allow else R.string.notifications_phone_turn_on)) }
        },
        dismissButton = { TextButton(onClick = done) { Text(stringResource(R.string.notify_prompt_later)) } },
    )
}
