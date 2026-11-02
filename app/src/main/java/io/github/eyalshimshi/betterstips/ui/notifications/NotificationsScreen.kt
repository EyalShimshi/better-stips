package io.github.eyalshimshi.betterstips.ui.notifications

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.asApiException
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
)

class NotificationsViewModel(private val repository: StipsRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()
    private var generation = 0

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
                _state.update { it.copy(items = list, page = 1, end = list.isEmpty(), refreshing = false, moreFailed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(refreshing = false, error = e.asApiException()) }
            }
        }
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
    onOpenQuestion: (Long) -> Unit,
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
    onOpenQuestion: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onSeen: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm = appViewModel { NotificationsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val toggle = rememberPhoneNotificationsToggle(snackbar)
    val currentOnSeen by rememberUpdatedState(onSeen)
    LaunchedEffect(state.page, state.refreshing) {
        if (state.page >= 1 && !state.refreshing) currentOnSeen()
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
                    items(state.items, key = { it.id }) { item ->
                        val onClick: (() -> Unit)? = when (val target = item.target) {
                            is NotificationTarget.OpenQuestion -> ({ onOpenQuestion(target.id) })
                            is NotificationTarget.OpenProfile -> ({ onOpenProfile(target.id) })
                            null -> null
                        }
                        NotificationRow(item, onClick)
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

@Composable
private fun NotificationRow(item: StipsNotification, onClick: (() -> Unit)?) {
    val unread = !item.viewed
    ListItem(
        headlineContent = {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { Text(agoText(item.time)) },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (unread) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    notificationIcon(item.type),
                    contentDescription = null,
                    tint = if (unread) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        trailingContent = if (unread) {
            {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (unread) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        ),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
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
 * permission is refused, [snackbar] explains and offers Android's notification settings.
 */
@Composable
fun rememberPhoneNotificationsToggle(snackbar: SnackbarHostState): (Boolean) -> Unit {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.notifications_permission_denied)
    val settingsText = stringResource(R.string.action_open_settings)
    fun setEnabled(on: Boolean) {
        container.settings.update { it.copy(backgroundNotifications = on) }
        Notifier.schedule(context.applicationContext, on)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setEnabled(true)
        } else {
            scope.launch {
                val result = snackbar.showSnackbar(deniedText, actionLabel = settingsText, withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) openNotificationSettings(context)
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

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Some devices hide this screen; nothing else to offer.
    }
}
