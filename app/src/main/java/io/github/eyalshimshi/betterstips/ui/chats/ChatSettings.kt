package io.github.eyalshimshi.betterstips.ui.chats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.notifications.rememberPhoneNotificationsToggle
import io.github.eyalshimshi.betterstips.ui.profile.GroupLabel
import io.github.eyalshimshi.betterstips.ui.profile.GroupRow
import io.github.eyalshimshi.betterstips.ui.settings.ChatPreview
import io.github.eyalshimshi.betterstips.widget.requestPinChatsWidget
import kotlinx.coroutines.launch

/**
 * DM settings (v0.12): everything about private chats in one place. The same rows open from the top of the chats
 * screens (a sheet) and in Settings → DM settings. Chats have their own reply switches, apart from answers'.
 */
@Composable
fun ChatSettingsContent(snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = container.settings
    val settings by store.settings.collectAsStateWithLifecycle()
    val toggleNotifications = rememberPhoneNotificationsToggle(snackbar)
    val phoneOn = settings.backgroundNotifications && Notifier.canPost(context)
    val widgetByHandText = stringResource(R.string.settings_widget_by_hand)

    // How messages look with these switches (#12): it changes as they do.
    ChatPreview(
        easyReply = settings.chatEasyReply,
        quotes = settings.chatReplyQuotes,
        hideCarets = settings.chatHideReplyCarets,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    GroupLabel(stringResource(R.string.settings_chat_replies))
    val replies = if (settings.chatReplyQuotes) 3 else 2
    GroupRow(
        title = stringResource(R.string.settings_easy_reply),
        summary = stringResource(R.string.settings_chat_easy_reply_summary),
        index = 0,
        count = replies,
        onClick = { store.update { it.copy(chatEasyReply = !it.chatEasyReply) } },
        trailing = { Switch(checked = settings.chatEasyReply, onCheckedChange = null) },
    )
    GroupRow(
        title = stringResource(R.string.settings_chat_reply_quotes),
        summary = stringResource(R.string.settings_chat_reply_quotes_summary),
        index = 1,
        count = replies,
        onClick = { store.update { it.copy(chatReplyQuotes = !it.chatReplyQuotes) } },
        trailing = { Switch(checked = settings.chatReplyQuotes, onCheckedChange = null) },
    )
    if (settings.chatReplyQuotes) {
        GroupRow(
            title = stringResource(R.string.settings_hide_reply_carets),
            summary = stringResource(R.string.settings_chat_hide_carets_summary),
            index = 2,
            count = replies,
            onClick = { store.update { it.copy(chatHideReplyCarets = !it.chatHideReplyCarets) } },
            trailing = { Switch(checked = settings.chatHideReplyCarets, onCheckedChange = null) },
        )
    }

    GroupLabel(stringResource(R.string.notifications_title))
    // Messages ride on phone notifications: their own switch and Android's sound settings show once those are on.
    val alerts = if (phoneOn) 3 else 1
    GroupRow(
        title = stringResource(R.string.settings_phone_notifications),
        summary = stringResource(R.string.settings_phone_notifications_summary),
        index = 0,
        count = alerts,
        onClick = { toggleNotifications(!phoneOn) },
        trailing = { Switch(checked = phoneOn, onCheckedChange = null) },
    )
    if (phoneOn) {
        GroupRow(
            title = stringResource(R.string.settings_chat_notifications),
            summary = stringResource(R.string.settings_chat_notifications_summary),
            index = 1,
            count = alerts,
            onClick = { store.update { it.copy(chatNotifications = !it.chatNotifications) } },
            trailing = { Switch(checked = settings.chatNotifications, onCheckedChange = null) },
        )
        GroupRow(
            title = stringResource(R.string.settings_chat_sound),
            summary = stringResource(R.string.settings_chat_sound_summary),
            index = 2,
            count = alerts,
            onClick = { Notifier.openMessageSettings(context) },
            trailing = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
        )
    }

    GroupLabel(stringResource(R.string.settings_widgets))
    GroupRow(
        title = stringResource(R.string.settings_widget_chats),
        summary = stringResource(R.string.settings_widget_chats_summary),
        index = 0,
        count = 1,
        onClick = { if (!requestPinChatsWidget(context)) scope.launch { snackbar.showSnackbar(widgetByHandText) } },
    )
}

/** The DM settings as a sheet over the chats screens (v0.12), with its own messages so the sheet doesn't hide them. */
@Composable
fun ChatSettingsSheet(onDismiss: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.dm_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ChatSettingsContent(snackbar)
            SwipeAwaySnackbarHost(snackbar, Modifier.padding(horizontal = 16.dp))
        }
    }
}
