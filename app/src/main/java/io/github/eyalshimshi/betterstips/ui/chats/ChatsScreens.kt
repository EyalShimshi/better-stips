package io.github.eyalshimshi.betterstips.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.components.errorMessage

@Composable
fun ChatsScreen(onOpenConversation: (Long, String) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val vm = appViewModel { ChatsViewModel(it.repository, it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
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
        leadingContent = { Avatar(conversation.other, 48.dp) },
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
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name.ifBlank { stringResource(R.string.chats_title) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            MessageComposer(
                draft = state.draft,
                sending = state.sending,
                error = state.sendError?.let { errorMessage(it) },
                onDraft = vm::setDraft,
                onSend = vm::send,
            )
        },
    ) { padding ->
        val error = state.error
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "top") {
                when {
                    state.loading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    error != null && state.messages.isEmpty() -> ErrorCard(error, onRetry = { vm.load() })
                    state.canLoadEarlier && state.messages.isNotEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { vm.loadEarlier() }, enabled = !state.loadingEarlier) {
                            Text(stringResource(R.string.load_earlier))
                        }
                    }
                }
            }
            items(state.messages, key = { it.id }) { message ->
                Bubble(message, mine = message.fromUserId != null && message.fromUserId == vm.myUserId)
            }
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage, mine: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                LinkifiedText(message.text, style = MaterialTheme.typography.bodyLarge)
                val time = agoText(message.time)
                if (time.isNotBlank()) {
                    Text(time, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
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
