package io.github.eyalshimshi.betterstips.ui.chats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.FlowerIcon
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.PresenceAvatar
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.util.ReplyRefs
import io.github.eyalshimshi.betterstips.util.ShareRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Picking up to five chats for one message (v0.19), and sending it to each, one after another. */
class SendToViewModel(private val repository: StipsRepository) : ViewModel() {
    data class State(
        /** In the order they were picked. */
        val selected: List<Author> = emptyList(),
        /** While it goes, and after it went until the screen closes, so a second tap can't send it twice. */
        val sending: Boolean = false,
        /** Bumped when a pick was refused: as many as [ShareRules.MAX_RECIPIENTS] are picked already. */
        val full: Int = 0,
        /** Bumped when the message couldn't go to anyone; the picks stay, to try again. */
        val failed: Int = 0,
        /** Sent, to whom it went and to whom it didn't. */
        val done: Done? = null,
    )

    data class Done(val sent: List<Author>, val failed: List<Author>)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun toggle(person: Author) {
        val id = person.userId ?: return
        _state.update { s ->
            if (s.sending) return@update s
            val ids = ShareRules.toggle(s.selected.mapNotNull { it.userId }, id)
            when {
                ids == null -> s.copy(full = s.full + 1)
                ids.size < s.selected.size -> s.copy(selected = s.selected.filter { it.userId in ids })
                else -> s.copy(selected = s.selected + person)
            }
        }
    }

    fun send(text: String) {
        val current = _state.value
        if (current.sending || current.done != null || current.selected.isEmpty() || text.isBlank()) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            val sent = mutableListOf<Author>()
            val failed = mutableListOf<Author>()
            for (person in current.selected) {
                val id = person.userId ?: continue
                try {
                    repository.sendMessage(id, text)
                    sent += person
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed += person
                }
            }
            _state.update {
                if (sent.isEmpty()) it.copy(sending = false, failed = it.failed + 1) else it.copy(done = Done(sent, failed))
            }
        }
    }
}

/**
 * Where text shared from another app, or a message forwarded from a chat, goes (v0.19), as in WhatsApp: the message on
 * top, then a search (chats by name, and anyone on Stips), notes to self, then recent chats, loading more as the list
 * nears its end. Up to [ShareRules.MAX_RECIPIENTS] can be picked; they show at the bottom with the send button.
 * [onSent] gets who it went to, and who it couldn't go to.
 */
@Composable
fun SendToScreen(text: String, onClose: () -> Unit, onSent: (List<Author>, List<Author>) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val account by container.session.account.collectAsStateWithLifecycle()
    val chatsVm = appViewModel { ChatsViewModel(it.repository, it.session) }
    val chats by chatsVm.state.collectAsStateWithLifecycle()
    val vm = appViewModel { SendToViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    val people = rememberPeopleSearch(query)
    BackHandler(enabled = query.isNotEmpty()) { query = "" }
    val searching = query.isNotBlank()
    val myId = session.userId
    val me = session.profile?.avatar ?: myId?.let { Author("", it, null, anonymous = false, online = false) }
    val notesName = stringResource(R.string.notes_title)
    // Below 5 flowers Stips doesn't send messages from non-moderators (v0.13).
    val needFlowers = account?.let { a -> a.flowers?.takeIf { it < MIN_FLOWERS_TO_MESSAGE && !a.isModerator } }
    val max = ShareRules.MAX_RECIPIENTS
    val fullText = stringResource(R.string.send_to_full, max)
    val failedText = stringResource(R.string.send_to_failed)
    LaunchedEffect(state.full) {
        if (state.full > 0) snackbar.showSnackbar(fullText)
    }
    LaunchedEffect(state.failed) {
        if (state.failed > 0) snackbar.showSnackbar(failedText)
    }
    val currentOnSent by rememberUpdatedState(onSent)
    val done = state.done
    LaunchedEffect(done) {
        if (done != null) currentOnSent(done.sent, done.failed)
    }
    // More chats as the list nears its end, as on Chats (v0.17, build 30).
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - 10
        }
    }
    LaunchedEffect(nearEnd, chats.page, chats.end, searching, session.signedIn) {
        if (nearEnd && !chats.end && !searching && session.signedIn) chatsVm.loadMore()
    }
    val selectedIds = state.selected.mapNotNull { it.userId }.toSet()
    val full = selectedIds.size >= max
    val pick: (Author) -> Unit = { vm.toggle(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.send_to_title), maxLines = 1)
                        Text(
                            if (selectedIds.isEmpty()) stringResource(R.string.send_to_up_to, max) else stringResource(R.string.send_to_count, selectedIds.size, max),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
        bottomBar = {
            if (session.signedIn && state.selected.isNotEmpty()) {
                SendBar(
                    selected = state.selected,
                    me = me,
                    notesName = notesName,
                    sending = state.sending,
                    canSend = needFlowers == null,
                    onRemove = pick,
                    onSend = { vm.send(text) },
                )
            }
        },
        snackbarHost = { SwipeAwaySnackbarHost(snackbar) },
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
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "message") { MessagePreview(text) }
            item(key = "search") { ChatsSearchField(query, onQuery = { query = it }) }
            if (needFlowers != null) {
                item(key = "flowers") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        FlowerIcon(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.chat_need_flowers, needFlowers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (searching) {
                val found = ChatRules.matching(chats.conversations, query, myId, notesName)
                val inChats = found.mapNotNull { it.other.userId }.toSet()
                val others = people.results.filter { it.userId != null && it.userId !in inChats }.distinctBy { it.userId }
                if (found.isNotEmpty()) {
                    item(key = "label-chats") { SearchLabel(stringResource(R.string.chats_search_chats)) }
                    items(found, key = { "c${it.other.userId}" }) { conversation ->
                        val person = conversation.other
                        val notes = ChatRules.isNotes(person.userId, myId)
                        PickRow(
                            person = if (notes) me ?: person else person,
                            notes = notes,
                            me = me,
                            supporting = if (notes) null else ReplyRefs.preview(conversation.lastMessage),
                            selected = person.userId in selectedIds,
                            dimmed = full,
                            onClick = pick,
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "label-people") { SearchLabel(stringResource(R.string.search_people)) }
                    items(others, key = { "p${it.userId}" }) { person ->
                        val notes = ChatRules.isNotes(person.userId, myId)
                        PickRow(person, notes, me, null, person.userId in selectedIds, full, pick)
                    }
                }
                if (found.isEmpty() && others.isEmpty()) {
                    item(key = "no-results") {
                        if (people.searching) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }
                        } else {
                            Text(
                                stringResource(R.string.chats_search_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 24.dp),
                            )
                        }
                    }
                }
                return@LazyColumn
            }
            // Notes to self, always first.
            if (me != null) {
                item(key = "notes") {
                    PickRow(me, notes = true, me = me, supporting = null, selected = me.userId in selectedIds, dimmed = full, onClick = pick)
                }
            }
            val recent = chats.conversations.filterNot { ChatRules.isNotes(it.other.userId, myId) }
            items(recent, key = { "r${it.other.userId}" }) { conversation ->
                PickRow(
                    person = conversation.other,
                    notes = false,
                    me = me,
                    supporting = ReplyRefs.preview(conversation.lastMessage),
                    selected = conversation.other.userId in selectedIds,
                    dimmed = full,
                    onClick = pick,
                )
            }
            val error = chats.error
            when {
                chats.loading && chats.conversations.isEmpty() -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                error != null && chats.conversations.isEmpty() -> item(key = "error") { ErrorCard(error, onRetry = { chatsVm.retry() }) }
                chats.conversations.isNotEmpty() && !chats.end -> item(key = "more") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (chats.moreFailed) {
                            TextButton(onClick = { chatsVm.loadMore() }) { Text(stringResource(R.string.action_retry)) }
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

/** The message that will go, as it will go: a few lines of it. */
@Composable
private fun MessagePreview(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            stringResource(R.string.send_to_message),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A chat or a person to send to, with a check box; [dimmed] when as many as allowed are picked already. */
@Composable
private fun PickRow(
    person: Author,
    notes: Boolean,
    me: Author?,
    supporting: String?,
    selected: Boolean,
    dimmed: Boolean,
    onClick: (Author) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = {
            Text(if (notes) stringResource(R.string.notes_title) else authorName(person), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = supporting?.takeIf { it.isNotBlank() }?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        leadingContent = {
            if (notes && me != null) NotesAvatar(me, 44.dp) else PresenceAvatar(person, 44.dp, clickable = false)
        },
        trailingContent = { Checkbox(checked = selected, onCheckedChange = null, enabled = selected || !dimmed) },
        colors = ListItemDefaults.colors(containerColor = if (selected) colors.secondaryContainer.copy(alpha = 0.45f) else Color.Transparent),
        modifier = Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick(person) }),
    )
}

/** Who it goes to, as chips (a tap lets one go), and the send button. */
@Composable
private fun SendBar(
    selected: List<Author>,
    me: Author?,
    notesName: String,
    sending: Boolean,
    canSend: Boolean,
    onRemove: (Author) -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            LazyRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(selected, key = { it.userId ?: 0L }) { person ->
                    val notes = me != null && person.userId == me.userId
                    InputChip(
                        selected = true,
                        onClick = { onRemove(person) },
                        enabled = !sending,
                        label = { Text(if (notes) notesName else authorName(person), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        avatar = {
                            if (notes) {
                                NotesAvatar(me ?: person, InputChipDefaults.AvatarSize)
                            } else {
                                Avatar(person, InputChipDefaults.AvatarSize, clickable = false)
                            }
                        },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(InputChipDefaults.IconSize))
                        },
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = canSend && !sending,
                modifier = Modifier.size(56.dp),
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                }
            }
        }
    }
}
