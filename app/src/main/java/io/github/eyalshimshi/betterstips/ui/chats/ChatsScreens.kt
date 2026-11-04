package io.github.eyalshimshi.betterstips.ui.chats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalOpenProfile
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.PresenceAvatar
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.memberForText
import io.github.eyalshimshi.betterstips.ui.components.onlineColor
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How often an open chat asks for new messages (the site's own chat asks every 1.6 s). */
private const val CHAT_POLL_MS = 3_000L

/** Their online status is asked again every this many checks: about once a minute. */
private const val PROFILE_EVERY_POLLS = 20

/** Farther than this from the newest message, the jump button skips the animation. */
private const val JUMP_ANIMATE_LIMIT = 30

private const val REPORT_DETAILS_MAX = 500

/** The site's reasons for reporting someone from a chat (`reports.send`, `itemtype: "usermsgs"`, `reason_id`). */
private val reportReasons = listOf(
    2 to R.string.chat_report_reason_spam,
    4 to R.string.chat_report_reason_sexual,
    8 to R.string.chat_report_reason_abuse,
    16 to R.string.chat_report_reason_details,
    1024 to R.string.chat_report_reason_other,
)

@Composable
fun ChatsScreen(onOpenConversation: (Long, String) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val vm = appViewModel { ChatsViewModel(it.repository, it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    // New or read messages change the unread count (checked every 10 s), and coming back from a chat reads some:
    // either way the list updates by itself (#6).
    val unread = LocalUnreadCounts.current.messages
    LaunchedEffect(unread, session.signedIn) {
        if (session.signedIn) vm.refreshQuietly()
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.chats_title)) }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (!session.signedIn) {
            Box(Modifier.padding(padding)) {
                MessageCard(
                    stringResource(R.string.chats_signed_out_title),
                    stringResource(R.string.chats_signed_out_body),
                    stringResource(R.string.action_sign_in),
                    onSignIn,
                )
            }
            return@Scaffold
        }
        val error = state.error
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                when {
                    state.loading && state.conversations.isEmpty() -> item(key = "loading") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    error != null && state.conversations.isEmpty() -> item(key = "error") { ErrorCard(error, onRetry = { vm.retry() }) }
                    state.conversations.isEmpty() -> item(key = "empty") {
                        MessageCard(stringResource(R.string.chats_empty), stringResource(R.string.chats_empty_body), null, null)
                    }
                }
                items(state.conversations, key = { it.other.userId ?: 0L }) { conversation ->
                    ConversationRow(conversation) {
                        val id = conversation.other.userId
                        if (id != null) onOpenConversation(id, conversation.other.nickname)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val preview = if (conversation.lastFromMe) {
        stringResource(R.string.you_prefix, conversation.lastMessage)
    } else {
        conversation.lastMessage
    }
    val time = agoText(conversation.time).ifBlank { conversation.timeText.orEmpty() }
    ListItem(
        headlineContent = { Text(authorName(conversation.other), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { PresenceAvatar(conversation.other, 48.dp) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conversation.unread > 0) {
                    Spacer(Modifier.size(4.dp))
                    Badge { Text(conversation.unread.toString()) }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun ConversationScreen(userId: Long, name: String, onBack: () -> Unit) {
    val vm = appViewModel { ConversationViewModel(it.repository, it.session, userId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val openProfile = LocalOpenProfile.current
    val context = LocalContext.current
    val animate = LocalAppSettings.current.animations
    val snackbar = remember { SnackbarHostState() }
    val profile = state.profile
    val other = profile?.avatar ?: Author(name, userId, null, anonymous = false, online = false)
    val title = profile?.nickname?.takeIf { it.isNotBlank() } ?: name.ifBlank { stringResource(R.string.chats_title) }
    val onlineText = stringResource(if (profile?.female == true) R.string.profile_online_female else R.string.profile_online_male)
    val copiedText = stringResource(R.string.message_copied)
    val reportedText = stringResource(R.string.report_sent)
    val failedText = stringResource(R.string.action_failed)
    val myId = vm.myUserId
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    var confirmingBlock by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        snackbar.showSnackbar(if (notice == ChatNotice.Reported) reportedText else failedText)
        vm.consumeNotice()
    }

    // New messages every 3 s while the chat is on screen, slower after failures; their status about once a minute (#6).
    LaunchedEffect(vm, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var misses = 0
            var polls = 0
            while (true) {
                delay(CHAT_POLL_MS shl misses.coerceAtMost(3))
                if (!RateGuard.allowBackground()) continue
                misses = if (vm.pollNew()) 0 else misses + 1
                if (++polls % PROFILE_EVERY_POLLS == 0) vm.loadProfile()
            }
        }
    }

    // The newest message the reader has seen at the bottom. Their later ones count on the jump button.
    var seenUpTo by rememberSaveable { mutableLongStateOf(0L) }
    // The list is reversed: the newest message is item 0, at the bottom. Follow a new one when the reader is already
    // there or it's their own; otherwise leave the reading position alone, and the jump button counts it.
    val newest = state.messages.lastOrNull()
    LaunchedEffect(newest?.id) {
        if (newest == null) return@LaunchedEffect
        if (newest.fromUserId == myId || listState.firstVisibleItemIndex <= 1) {
            seenUpTo = maxOf(seenUpTo, newest.id)
            listState.animateScrollToItem(0)
        }
    }
    // Scrolling back down to the newest message reads it too.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 }.collect { atNewest ->
            val last = vm.state.value.messages.lastOrNull()
            if (atNewest && last != null) seenUpTo = maxOf(seenUpTo, last.id)
        }
    }
    val unseen = remember(state.messages, seenUpTo, myId) { ChatRules.unseen(state.messages, seenUpTo, myId) }
    val awayFromNewest by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClickLabel = stringResource(R.string.chat_view_profile)) { openProfile(userId) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        PresenceAvatar(other, 36.dp, clickable = false)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (other.online) {
                                Text(onlineText, style = MaterialTheme.typography.labelSmall, color = onlineColor())
                            }
                        }
                    }
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_actions))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_report, title)) },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    reporting = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(if (state.blocked) R.string.chat_unblock else R.string.chat_block)) },
                                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                enabled = !state.blockBusy,
                                onClick = {
                                    menuOpen = false
                                    if (state.blocked) vm.setBlocked(false) else confirmingBlock = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.blocked) {
                BlockedBar(name = title, busy = state.blockBusy, onUnblock = { vm.setBlocked(false) })
            } else {
                MessageComposer(
                    draft = state.draft,
                    sending = state.sending,
                    error = state.sendError?.let { errorMessage(it) },
                    onDraft = vm::setDraft,
                    onSend = vm::send,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val newestFirst = state.messages.asReversed()
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                itemsIndexed(newestFirst, key = { _, message -> message.id }) { index, message ->
                    Bubble(
                        message = message,
                        mine = message.fromUserId != null && message.fromUserId == myId,
                        expanded = message.id in state.expanded,
                        onClick = { vm.toggleTime(message.id) },
                        onLongClick = { copyMessage(context, message.text, copiedText) },
                        // The message drawn above this one is the next item: close together while one person writes.
                        modifier = Modifier.padding(top = if (ChatRules.continuesRun(message, newestFirst.getOrNull(index + 1))) 2.dp else 10.dp),
                    )
                }
                // The first page's spinner has a key of its own: the list keeps its place by the key of the item at the
                // bottom, and anchored to "start" it would open at the oldest message (and keep loading older ones).
                item(key = if (state.loading && state.messages.isEmpty()) "loading" else "start") {
                    ConversationStart(
                        state = state,
                        other = other,
                        name = title,
                        onlineText = onlineText,
                        onRetry = vm::load,
                        onLoadEarlier = vm::loadEarlier,
                        onNeedPage = vm::ensurePage,
                        onOpenProfile = { openProfile(userId) },
                    )
                }
            }
            AnimatedVisibility(
                visible = awayFromNewest,
                enter = if (animate) fadeIn() + scaleIn() else EnterTransition.None,
                exit = if (animate) fadeOut() + scaleOut() else ExitTransition.None,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                JumpToNewest(unseen) {
                    newest?.let { seenUpTo = maxOf(seenUpTo, it.id) }
                    scope.launch {
                        if (listState.firstVisibleItemIndex > JUMP_ANIMATE_LIMIT) listState.scrollToItem(0) else listState.animateScrollToItem(0)
                    }
                }
            }
        }
    }

    if (reporting) {
        ReportUserDialog(
            name = title,
            onDismiss = { reporting = false },
            onSend = { reasonId, details ->
                reporting = false
                vm.report(reasonId, details)
            },
        )
    }
    if (confirmingBlock) {
        AlertDialog(
            onDismissRequest = { confirmingBlock = false },
            title = { Text(stringResource(R.string.chat_block_title, title)) },
            text = { Text(stringResource(R.string.chat_block_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingBlock = false
                        vm.setBlocked(true)
                    },
                ) { Text(stringResource(R.string.chat_block_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmingBlock = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The round ↓ button while reading earlier messages, with how many of theirs arrived meanwhile. */
@Composable
private fun JumpToNewest(unseen: Int, onClick: () -> Unit) {
    val description = if (unseen > 0) {
        LocalContext.current.resources.getQuantityString(R.plurals.chat_new_messages, unseen, unseen)
    } else {
        stringResource(R.string.chat_jump_newest)
    }
    BadgedBox(
        badge = {
            if (unseen > 0) Badge { Text(if (unseen > 99) "99+" else unseen.toString()) }
        },
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = description)
        }
    }
}

/** In place of the composer while you block them: what's going on, and the way back. */
@Composable
private fun BlockedBar(name: String, busy: Boolean, onUnblock: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                Icons.Outlined.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.chat_blocked_bar, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUnblock, enabled = !busy) { Text(stringResource(R.string.chat_unblock_action)) }
        }
    }
}

/** Reporting someone from a chat, as on the site: one of its reasons, then optional details. */
@Composable
private fun ReportUserDialog(name: String, onDismiss: () -> Unit, onSend: (Int, String) -> Unit) {
    var reasonId by rememberSaveable { mutableIntStateOf(0) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_report_title, name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.chat_report_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup()) {
                    reportReasons.forEach { (id, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = reasonId == id, role = Role.RadioButton, onClick = { reasonId = id }),
                        ) {
                            RadioButton(selected = reasonId == id, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (reasonId != 0) {
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it.take(REPORT_DETAILS_MAX) },
                        placeholder = { Text(stringResource(R.string.chat_report_hint)) },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(reasonId, details) }, enabled = reasonId != 0) { Text(stringResource(R.string.report_send)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * The top of the chat: loading, an error, earlier messages loading by themselves as it comes into view (no button,
 * #6), and once there are no more, a short summary of the other person, as messaging apps show.
 */
@Composable
private fun ConversationStart(
    state: ConversationUiState,
    other: Author,
    name: String,
    onlineText: String,
    onRetry: () -> Unit,
    onLoadEarlier: () -> Unit,
    onNeedPage: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val error = state.error
    when {
        state.loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        error != null && state.messages.isEmpty() -> ErrorCard(error, onRetry = onRetry)
        state.earlierFailed -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onLoadEarlier) { Text(stringResource(R.string.chat_earlier_failed), textAlign = TextAlign.Center) }
        }
        state.canLoadEarlier && state.messages.isNotEmpty() -> {
            // Being drawn means the reader scrolled up to here: fetch the next older page, and again after it.
            LaunchedEffect(state.messages.firstOrNull()?.id) { onLoadEarlier() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        }
        else -> {
            LaunchedEffect(Unit) { onNeedPage() }
            ChatSummary(other, name, state, onlineText, onOpenProfile)
        }
    }
}

/** Their photo, name, status and a line about them, above the first message. Tapping it opens their profile. */
@Composable
private fun ChatSummary(other: Author, name: String, state: ConversationUiState, onlineText: String, onOpenProfile: () -> Unit) {
    val resources = LocalContext.current.resources
    val profile = state.profile
    val page = state.page
    val credibility = if (profile != null) stringResource(R.string.chat_credibility, profile.points) else null
    val answers = page?.let { resources.getQuantityString(R.plurals.answers_count, it.answers, it.answers) }
    val flowers = page?.let { resources.getQuantityString(R.plurals.chat_flowers, it.flowers, it.flowers) }
    val stats = listOfNotNull(credibility, answers, flowers).joinToString(" · ")
    val memberFor = memberForText(page?.activeSince)
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(onClick = onOpenProfile, shape = RoundedCornerShape(24.dp), color = card) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PresenceAvatar(other, 72.dp, large = true, clickable = false, ring = card)
                Spacer(Modifier.height(8.dp))
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (other.online) {
                    Text(onlineText, style = MaterialTheme.typography.labelMedium, color = onlineColor())
                }
                if (stats.isNotEmpty()) {
                    Text(
                        stats,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (memberFor != null) {
                    Text(memberFor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.chat_view_profile), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.chat_start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A compact bubble with the text only: a tap shows its time, a long-press copies it (#6). */
@Composable
private fun Bubble(
    message: ChatMessage,
    mine: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animate = LocalAppSettings.current.animations
    val container = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(container)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            LinkifiedText(message.text, style = MaterialTheme.typography.bodyLarge.copy(color = content), selectable = false)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = if (animate) expandVertically() + fadeIn() else EnterTransition.None,
            exit = if (animate) shrinkVertically() + fadeOut() else ExitTransition.None,
        ) {
            Text(
                bubbleTime(message.time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** "14:05" today, "Yesterday, 14:05", or the date with the time. */
@Composable
private fun bubbleTime(time: Instant?): String {
    if (time == null) return ""
    val zone = ZoneId.systemDefault()
    val local = time.atZone(zone)
    val clock = local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val today = LocalDate.now(zone)
    return when (local.toLocalDate()) {
        today -> clock
        today.minusDays(1) -> stringResource(R.string.chat_yesterday_at, clock)
        else -> local.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }
}

private fun copyMessage(context: Context, text: String, copied: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(copied, text))
    // Android 13 and later confirm a copy by themselves.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
}

@Composable
private fun MessageComposer(
    draft: String,
    sending: Boolean,
    error: String?,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraft,
                    placeholder = { Text(stringResource(R.string.message_hint)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    isError = error != null,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSend, enabled = !sending && draft.isNotBlank()) {
                    if (sending) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                    }
                }
            }
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
