package io.github.eyalshimshi.betterstips.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.eyalshimshi.betterstips.MainActivity
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.StipsApp
import io.github.eyalshimshi.betterstips.util.Shortcuts
import java.time.ZoneId

/** The receiver Android talks to; Glance does the rest. */
class ChatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChatsWidget()
}

/** Asks the launcher to place the chats widget (Settings). False when it can't, so the caller can explain. */
fun requestPinChatsWidget(context: Context): Boolean = try {
    val manager = context.getSystemService(AppWidgetManager::class.java)
    manager != null && manager.isRequestPinAppWidgetSupported &&
        manager.requestPinAppWidget(ComponentName(context, ChatsWidgetReceiver::class.java), null, null)
} catch (e: RuntimeException) {
    false
}

/**
 * Your chats on the home screen (#8): who wrote, the last message, and how many are unread. Tap a chat to open it,
 * the title to open Chats, ⟳ to refresh. Colors as the "Hot right now" widget.
 */
class ChatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as StipsApp).container
        val feed = container.chatsWidget
        feed.ensureLoaded()
        feed.refresh(byUser = false)
        provideContent {
            val state by feed.state.collectAsState()
            val session by container.session.session.collectAsState()
            val settings by container.settings.settings.collectAsState()
            val accent = remember(settings.accent) { accentColors(settings.accent) }
            val dynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            GlanceTheme(colors = if (dynamic) GlanceTheme.colors else accent) {
                ChatsWidgetContent(state, session.signedIn)
            }
        }
    }
}

/** The ⟳ button, and "Tap to try again". */
class RefreshChatsWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as StipsApp).container.chatsWidget.refresh(byUser = true)
    }
}

@Composable
private fun ChatsWidgetContent(state: ChatsWidgetFeed.State, signedIn: Boolean) {
    val context = LocalContext.current
    val refresh = actionRunCallback<RefreshChatsWidget>()
    val openChatsAction = actionStartActivity(openChats(context))
    val unread = if (signedIn) state.unread else 0
    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(R.drawable.ic_widget_chats),
                title = if (unread > 0) context.getString(R.string.widget_chats_title_unread, unread) else context.getString(R.string.tab_chats),
                iconColor = GlanceTheme.colors.primary,
                modifier = GlanceModifier.clickable(openChatsAction),
                actions = {
                    if (signedIn) {
                        if (state.refreshing) {
                            Box(GlanceModifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(GlanceModifier.size(24.dp), color = GlanceTheme.colors.primary)
                            }
                        } else {
                            CircleIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
                                contentDescription = context.getString(R.string.widget_refresh),
                                onClick = refresh,
                                backgroundColor = null,
                            )
                        }
                    }
                },
            )
        },
    ) {
        when {
            !signedIn -> WidgetMessage(context.getString(R.string.widget_chats_signed_out), onClick = openChatsAction)
            state.chats.isNotEmpty() -> ChatList(state.chats)
            !state.loaded || state.refreshing -> WidgetMessage(context.getString(R.string.widget_loading), onClick = null)
            state.failed -> WidgetMessage(context.getString(R.string.widget_chats_failed), onClick = refresh)
            else -> WidgetMessage(context.getString(R.string.widget_chats_empty), onClick = refresh)
        }
    }
}

@Composable
private fun ChatList(chats: List<ChatsWidgetFeed.Chat>) {
    val now = remember(chats) { System.currentTimeMillis() }
    LazyColumn(GlanceModifier.fillMaxSize()) {
        items(chats, itemId = { it.userId }) { chat ->
            ChatRow(chat, now)
        }
    }
}

/** The name and time, then the last message and the unread count. Unread chats are bold. */
@Composable
private fun ChatRow(chat: ChatsWidgetFeed.Chat, now: Long) {
    val context = LocalContext.current
    val unread = chat.unread > 0
    val locale = context.resources.configuration.locales[0]
    Column(
        GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(actionStartActivity(openChat(context, chat))),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = chat.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            val time = chat.time
            if (time != null) {
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = ChatsWidgetFeed.timeLabel(time, now, ZoneId.systemDefault(), locale),
                    style = TextStyle(
                        color = if (unread) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.height(2.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (chat.fromMe) context.getString(R.string.widget_chat_you, chat.text) else chat.text,
                style = TextStyle(
                    color = if (unread) GlanceTheme.colors.onSurface else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (unread) {
                Spacer(GlanceModifier.width(6.dp))
                Box(
                    GlanceModifier
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (chat.unread > 99) "99+" else chat.unread.toString(),
                        style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Opens the Chats tab, bringing the running app forward like a phone notification does. */
private fun openChats(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Shortcuts.ACTION_CHATS
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

/** Opens one chat. Each chat has its own address, so Android keeps the rows' taps apart. */
private fun openChat(context: Context, chat: ChatsWidgetFeed.Chat): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Shortcuts.ACTION_CHAT
        data = Uri.parse(Shortcuts.chatUri(chat.userId, chat.name))
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
