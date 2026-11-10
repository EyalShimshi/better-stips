package io.github.eyalshimshi.betterstips.ui.chats

import io.github.eyalshimshi.betterstips.ui.components.FlowerIcon
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Tune
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
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.ui.Routes
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalOpenProfile
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.PresenceAvatar
import io.github.eyalshimshi.betterstips.ui.components.RefreshOnEnter
import io.github.eyalshimshi.betterstips.ui.components.ScrollToTopOnReselect
import io.github.eyalshimshi.betterstips.ui.components.TextTarget
import io.github.eyalshimshi.betterstips.ui.components.UnreadBar
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.memberForText
import io.github.eyalshimshi.betterstips.ui.components.onlineColor
import io.github.eyalshimshi.betterstips.ui.components.reveal
import io.github.eyalshimshi.betterstips.ui.components.rememberSwipeDrag
import io.github.eyalshimshi.betterstips.ui.components.swipeToAct
import io.github.eyalshimshi.betterstips.ui.components.SwipeHint
import io.github.eyalshimshi.betterstips.ui.components.SwipeHintEffect
import io.github.eyalshimshi.betterstips.ui.components.TipPill
import io.github.eyalshimshi.betterstips.ui.components.TipState
import io.github.eyalshimshi.betterstips.ui.components.rememberTipState
import io.github.eyalshimshi.betterstips.ui.components.LocalTipsAllowed
import io.github.eyalshimshi.betterstips.ui.components.TIP_DELAY_MS
import io.github.eyalshimshi.betterstips.ui.components.TIP_SHOW_MS
import io.github.eyalshimshi.betterstips.ui.components.PEEK_TOTAL_MS
import io.github.eyalshimshi.betterstips.ui.components.glowOnce
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.ReplyRefs
import io.github.eyalshimshi.betterstips.util.ChatLinks
import io.github.eyalshimshi.betterstips.util.SwipeSide
import io.github.eyalshimshi.betterstips.util.TutorialRules
import io.github.eyalshimshi.betterstips.data.Tutorials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/** How often an open chat asks for new messages (the site's own chat asks every 1.6 s). */
private const val CHAT_POLL_MS = 3_000L

/** Their online status is asked again every this many checks: about once a minute. */
private const val PROFILE_EVERY_POLLS = 20

/** Farther than this from the newest message, the jump button skips the animation. */
private const val JUMP_ANIMATE_LIMIT = 30

private const val REPORT_DETAILS_MAX = 500

/** Room above the "unread" bar when a chat opens at it (v0.13). */
private val UNREAD_TOP_MARGIN = 48.dp

/** Stips opens messages to users with this many flowers; moderators always (the site's MIN_FLOWERS_TO_SEND_MESSAGE). */
private const val MIN_FLOWERS_TO_MESSAGE = 5

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
    // Coming back to the app on Chats brings the list up to date too, unless it was just read (v0.16).
    RefreshOnEnter(Routes.CHATS) {
        if (session.signedIn) vm.refreshIfStale()
    }
    val listState = rememberLazyListState()
    // Tapping Chats again scrolls back to the top (v0.12) and reloads the list (v0.13, #10).
    ScrollToTopOnReselect(Routes.CHATS, listState) {
        if (session.signedIn) vm.refresh()
    }
    // Stips sends the chats a page at a time (v0.17, build 30): the next one is asked for while about a screen of them is
    // still below, so scrolling doesn't stop at the end of a page.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - PREFETCH_ROWS
        }
    }
    var dmSettings by rememberSaveable { mutableStateOf(false) }
    // People to write to (#20): the search on top finds them on all of Stips, as Home's does; a tap opens the chat.
    var query by rememberSaveable { mutableStateOf("") }
    val people = rememberPeopleSearch(query)
    BackHandler(enabled = query.isNotEmpty()) { query = "" }
    val searching = query.isNotBlank()
    LaunchedEffect(nearEnd, state.page, state.end, searching, session.signedIn) {
        if (nearEnd && !state.end && !searching && session.signedIn) vm.loadMore()
    }
    val myId = session.userId
    // The account itself, for notes to self (#20).
    val me = session.profile?.avatar ?: myId?.let { Author("", it, null, anonymous = false, online = false) }
    val notesName = stringResource(R.string.notes_title)
    // A tip (#28): the first circle on the shelf is the chat with yourself. Once the shelf is on screen.
    val tipsAllowed = LocalTipsAllowed.current
    val notesTip = rememberTipState()
    val notesTipText = stringResource(R.string.tip_notes_shelf)
    LaunchedEffect(tipsAllowed, me != null, searching, session.signedIn) {
        if (!tipsAllowed || me == null || searching || !session.signedIn) return@LaunchedEffect
        if (!container.tutorials.due(Tutorials.Tip.NotesShelf)) return@LaunchedEffect
        delay(TIP_DELAY_MS)
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == "shelf" }) return@LaunchedEffect
        if (!container.tutorials.claim(Tutorials.Tip.NotesShelf)) return@LaunchedEffect
        try {
            notesTip.show(notesTipText)
            delay(TIP_SHOW_MS)
        } finally {
            notesTip.hide()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chats_title)) },
                actions = { DmSettingsButton { dmSettings = true } },
            )
        },
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
        val open: (Conversation) -> Unit = { conversation ->
            val id = conversation.other.userId
            if (id != null) onOpenConversation(id, conversation.other.nickname)
        }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "search") { ChatsSearchField(query, onQuery = { query = it }) }
                if (query.isNotBlank()) {
                    val chats = ChatRules.matching(state.conversations, query, myId, notesName)
                    val inChats = chats.mapNotNull { it.other.userId }.toSet()
                    val others = people.results.filter { it.userId != null && it.userId !in inChats }.distinctBy { it.userId }
                    if (chats.isNotEmpty()) {
                        item(key = "label-chats") { SearchLabel(stringResource(R.string.chats_search_chats)) }
                        items(chats, key = { "c${it.other.userId}" }) { conversation ->
                            ConversationRow(conversation, notes = ChatRules.isNotes(conversation.other.userId, myId), me = me) { open(conversation) }
                        }
                    }
                    if (others.isNotEmpty()) {
                        item(key = "label-people") { SearchLabel(stringResource(R.string.search_people)) }
                        items(others, key = { "p${it.userId}" }) { person ->
                            PersonRow(person, notes = ChatRules.isNotes(person.userId, myId), me = me) {
                                person.userId?.let { onOpenConversation(it, person.nickname) }
                            }
                        }
                    }
                    if (chats.isEmpty() && others.isEmpty()) {
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
                if (me != null) {
                    item(key = "shelf") {
                        OnlineShelf(me, state.conversations, notesTip) { id, name ->
                            if (id == myId) container.tutorials.done(Tutorials.Tip.NotesShelf)
                            onOpenConversation(id, name)
                        }
                    }
                }
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
                    ConversationRow(conversation, notes = ChatRules.isNotes(conversation.other.userId, myId), me = me) { open(conversation) }
                }
                // Older chats are on the next page, which loads by itself on the way here (v0.17, build 30).
                if (state.conversations.isNotEmpty() && !state.end) {
                    item(key = "more") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.moreFailed) {
                                TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.action_retry)) }
                            } else {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
    if (dmSettings) ChatSettingsSheet(onDismiss = { dmSettings = false })
}

/** How many rows before the end of the chats list the next page is asked for (about a screen). */
private const val PREFETCH_ROWS = 10

/** What the search on top of Chats found on Stips (#20), and whether it's still looking. */
private class PeopleSearch(val results: List<Author>, val searching: Boolean)

/** People whose nickname matches [query], as Home's search finds them (the site's `users/Search`), after a pause in typing. */
@Composable
private fun rememberPeopleSearch(query: String): PeopleSearch {
    val repository = LocalAppContainer.current.repository
    var results by remember { mutableStateOf<List<Author>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(400)
        results = try {
            repository.searchUsers(q).take(PEOPLE_RESULTS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        searching = false
    }
    return PeopleSearch(results, searching)
}

/** How many people the search on top of Chats shows. */
private const val PEOPLE_RESULTS = 8

/** The search field on top of Chats (#20), shaped as Home's. */
@Composable
private fun ChatsSearchField(query: String, onQuery: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 52.dp),
        placeholder = { Text(stringResource(R.string.chats_search_hint), maxLines = 1) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun SearchLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** Someone the search found: a tap opens the chat with them, not their profile (#20). */
@Composable
private fun PersonRow(person: Author, notes: Boolean, me: Author?, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(if (notes) stringResource(R.string.notes_title) else authorName(person), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            if (notes && me != null) NotesAvatar(me, 40.dp) else PresenceAvatar(person, 40.dp, clickable = false)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * The shelf on top of Chats (#20), as on Instagram: notes to self first, then the people of recent chats who are
 * online now, the most recent first (at the right in Hebrew). It scrolls sideways when they don't fit.
 */
@Composable
private fun OnlineShelf(me: Author, conversations: List<Conversation>, tip: TipState, onOpen: (Long, String) -> Unit) {
    val myId = me.userId ?: return
    val online = remember(conversations, myId) { ChatRules.onlineShelf(conversations, myId) }
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "notes") {
                ShelfItem(stringResource(R.string.notes_short), onClick = { onOpen(myId, me.nickname) }) {
                    NotesAvatar(me, SHELF_AVATAR)
                }
            }
            items(online, key = { it.userId ?: 0L }) { person ->
                ShelfItem(authorName(person), onClick = { person.userId?.let { onOpen(it, person.nickname) } }) {
                    PresenceAvatar(person, SHELF_AVATAR, clickable = false)
                }
            }
        }
        // A tip (#28) about the first circle: the chat with yourself.
        TipPill(tip, Modifier.padding(start = 4.dp), expand = true)
    }
}

private val SHELF_AVATAR = 58.dp

@Composable
private fun ShelfItem(label: String, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        avatar()
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/**
 * Notes to self (#20): the user's own photo with a bookmark, wherever the chat with themselves shows, so it doesn't
 * look like a chat with someone. [ring] is the surface it sits on.
 */
@Composable
private fun NotesAvatar(me: Author, size: Dp, ring: Color = MaterialTheme.colorScheme.surface) {
    Box {
        Avatar(me, size, clickable = false)
        val mark = (size * 0.38f).coerceAtLeast(16.dp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(mark)
                .clip(CircleShape)
                .background(ring)
                .padding(mark * 0.1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(mark * 0.55f))
        }
    }
}

/** The way into DM settings, at the top of the chats screens (v0.12). */
@Composable
private fun DmSettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.dm_settings))
    }
}

/** A chat in the list. Notes to self (#20) say so, with the bookmark, and never count as unread. */
@Composable
private fun ConversationRow(conversation: Conversation, notes: Boolean = false, me: Author? = null, onClick: () -> Unit) {
    // The message itself, not the carets it opens with (v0.17, build 30).
    val last = remember(conversation.lastMessage) { ReplyRefs.preview(conversation.lastMessage) }
    val preview = if (conversation.lastFromMe && !notes) {
        stringResource(R.string.you_prefix, last)
    } else {
        last
    }
    val time = agoText(conversation.time).ifBlank { conversation.timeText.orEmpty() }
    ListItem(
        headlineContent = {
            Text(if (notes) stringResource(R.string.notes_title) else authorName(conversation.other), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            if (notes) NotesAvatar(me ?: conversation.other, 48.dp) else PresenceAvatar(conversation.other, 48.dp)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conversation.unread > 0 && !notes) {
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
fun ConversationScreen(userId: Long, name: String, onBack: () -> Unit, about: Long? = null) {
    val vm = appViewModel { ConversationViewModel(it.repository, it.session, it.chatReadMarks, userId, about) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val openProfile = LocalOpenProfile.current
    val context = LocalContext.current
    val motion = LocalAppSettings.current.motion
    val snackbar = remember { SnackbarHostState() }
    val profile = state.profile
    val other = profile?.avatar ?: Author(name, userId, null, anonymous = false, online = false)
    // Notes to self (#20): the chat with yourself reads as notes, not as a chat with someone.
    val notes = ChatRules.isNotes(userId, vm.myUserId)
    val title = if (notes) {
        stringResource(R.string.notes_title)
    } else {
        profile?.nickname?.takeIf { it.isNotBlank() } ?: name.ifBlank { stringResource(R.string.chats_title) }
    }
    val onlineText = stringResource(if (profile?.female == true) R.string.profile_online_female else R.string.profile_online_male)
    val copiedText = stringResource(R.string.message_copied)
    val reportedText = stringResource(R.string.report_sent)
    val failedText = stringResource(R.string.action_failed)
    val myId = vm.myUserId
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    var confirmingBlock by rememberSaveable { mutableStateOf(false) }
    var dmSettings by rememberSaveable { mutableStateOf(false) }
    val settings = LocalAppSettings.current
    val youText = stringResource(R.string.chat_you)
    // Easy reply and reply quotes in chats (v0.12): carets count messages in Stips' order, oldest first.
    val canReply = settings.chatEasyReply && !state.blocked
    // Below 5 flowers Stips doesn't send messages from non-moderators; the site explains instead of sending (v0.13).
    val account by LocalAppContainer.current.session.account.collectAsStateWithLifecycle()
    val needFlowers = account?.let { a -> a.flowers?.takeIf { it < MIN_FLOWERS_TO_MESSAGE && !a.isModerator } }
    val positions = remember(state.messages) { state.messages.withIndex().associate { (i, m) -> m.id to i } }
    var highlight by remember { mutableStateOf<ChatHighlight?>(null) }

    // A tip (#28): the reply swipe on their newest message once it's on screen, also one that comes while here. Only
    // where a reply can go (easy reply on, not blocked, enough flowers), and never in notes to self.
    val tutorials = LocalAppContainer.current.tutorials
    val tipsAllowed = LocalTipsAllowed.current
    val chatTipText = stringResource(R.string.tip_swipe_reply)
    var bubbleHint by remember { mutableStateOf<Pair<Long, SwipeHint>?>(null) }
    val tipTarget = remember(state.messages, myId) { TutorialRules.chatTipTarget(state.messages, myId) }
    LaunchedEffect(tipTarget?.id, canReply, needFlowers == null, tipsAllowed) {
        val target = tipTarget ?: return@LaunchedEffect
        if (notes || !canReply || needFlowers != null || !tipsAllowed || !tutorials.due(Tutorials.Tip.ChatReply)) return@LaunchedEffect
        delay(TIP_DELAY_MS)
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == target.id }) return@LaunchedEffect
        if (!tutorials.claim(Tutorials.Tip.ChatReply)) return@LaunchedEffect
        bubbleHint = target.id to SwipeHint(SwipeSide.Right, chatTipText)
        try {
            delay(PEEK_TOTAL_MS)
        } finally {
            bubbleHint = null
        }
    }

    /**
     * Shows a message a reply points at and lights it up for a moment: no scroll when it's already in sight whole,
     * otherwise the least scroll that shows it, with some room above it (#12).
     */
    fun jumpTo(messageId: Long) {
        val position = positions[messageId] ?: return
        // The list is reversed: the newest message is item 0.
        val item = state.messages.size - 1 - position
        scope.launch {
            listState.reveal(item, motion.jumps, quick = motion.quick)
            highlight = ChatHighlight(messageId, System.nanoTime())
        }
    }
    LaunchedEffect(highlight) {
        if (highlight == null) return@LaunchedEffect
        delay(CHAT_HIGHLIGHT_MS + 200)
        highlight = null
    }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        snackbar.showSnackbar(if (notice == ChatNotice.Reported) reportedText else failedText)
        vm.consumeNotice()
    }
    // Open, so this chat's phone notification has done its job (v0.11).
    LaunchedEffect(userId) { Notifier.clearChat(context, userId) }

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
    // Opening with unread messages (v0.13): the chat opens with their "unread" bar near the top when they fill more
    // than the screen, and at the newest message otherwise. Once per visit.
    var placedUnread by rememberSaveable { mutableStateOf(false) }
    val barMarginPx = with(LocalDensity.current) { UNREAD_TOP_MARGIN.toPx() }
    // The list is reversed: the newest message is item 0, at the bottom. Follow a new one when the reader is already
    // there or it's their own; otherwise leave the reading position alone, and the jump button counts it.
    val newest = state.messages.lastOrNull()
    LaunchedEffect(newest?.id) {
        if (newest == null) return@LaunchedEffect
        val unread = vm.state.value.unread
        if (!placedUnread && unread != null) {
            placedUnread = true
            // Their unread messages count on the jump button until the reader reaches the newest.
            seenUpTo = maxOf(seenUpTo, unread.id - 1)
            val position = positions[unread.id]
            if (position != null) {
                val item = state.messages.size - 1 - position
                // The bar's message at the bottom, then back toward the newest until it's near the top. Scrolling
                // back stops at the newest message, so a few unread ones simply show at the bottom.
                listState.scrollToItem(item)
                val size = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == item }?.size ?: 0
                val viewport = listState.layoutInfo.viewportSize.height
                listState.scrollBy(-(viewport - size - barMarginPx))
                return@LaunchedEffect
            }
        }
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
                            .then(
                                if (notes) {
                                    Modifier
                                } else {
                                    Modifier.clickable(onClickLabel = stringResource(R.string.chat_view_profile)) { openProfile(userId) }
                                },
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        if (notes) NotesAvatar(other, 36.dp) else PresenceAvatar(other, 36.dp, clickable = false)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (other.online && !notes) {
                                Text(onlineText, style = MaterialTheme.typography.labelSmall, color = onlineColor())
                            }
                        }
                    }
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    DmSettingsButton { dmSettings = true }
                    // Nobody to report or block in notes to self (#20).
                    if (!notes) Box {
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
            } else if (needFlowers != null) {
                NeedFlowersBar(needFlowers)
            } else {
                val replyTo = state.replyTo
                MessageComposer(
                    draft = state.draft,
                    draftVersion = state.draftVersion,
                    focusComposer = state.focusComposer,
                    replyTo = replyTo,
                    // Their name, or null for a reply to the user's own message.
                    replyName = if (replyTo != null && replyTo.fromUserId != null && replyTo.fromUserId == myId) null else title,
                    about = state.about,
                    sending = state.sending,
                    error = state.sendError?.let { errorMessage(it) },
                    onDraft = vm::setDraft,
                    onCancelReply = vm::cancelReply,
                    onRemoveAbout = vm::removeAbout,
                    onSend = vm::send,
                    hint = stringResource(if (notes) R.string.notes_hint else R.string.message_hint),
                )
            }
        },
        snackbarHost = { SwipeAwaySnackbarHost(snackbar) },
    ) { padding ->
        val newestFirst = state.messages.asReversed()
        val zone = remember { ZoneId.systemDefault() }
        // Read again as messages come in, not every second as times tick.
        val today = remember(state.messages) { LocalDate.now(zone) }
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
                    val position = positions[message.id]
                    val reply = if (settings.chatReplyQuotes && position != null) {
                        remember(message.text, position, settings.chatHideReplyCarets) {
                            ReplyRefs.resolve(message.text, position, settings.chatHideReplyCarets)
                        }
                    } else {
                        null
                    }
                    // The first message that was unread on opening sits under a thin "unread" bar (v0.13).
                    val unread = state.unread?.takeIf { it.id == message.id }
                    val older = newestFirst.getOrNull(index + 1)
                    // Each day starts with its date, as in WhatsApp (v0.17).
                    val day = if (ChatRules.startsDay(message, older, zone)) message.time?.atZone(zone)?.toLocalDate() else null
                    Column {
                        if (day != null) DayHeader(day, today)
                        AnimatedVisibility(visible = unread != null, enter = EnterTransition.None, exit = shrinkVertically() + fadeOut()) {
                            val count = remember { unread?.count ?: 0 }
                            UnreadBar(
                                LocalContext.current.resources.getQuantityString(R.plurals.chat_unread, count, count),
                                Modifier.padding(top = 10.dp),
                            )
                        }
                        Bubble(
                            message = message,
                            mine = message.fromUserId != null && message.fromUserId == myId,
                            expanded = message.id in state.expanded,
                            onClick = { vm.toggleTime(message.id) },
                            onLongClick = { copyMessage(context, message.text, copiedText) },
                            reply = reply,
                            messages = state.messages,
                            authorOf = { m -> if (m.fromUserId != null && m.fromUserId == myId) youText else title },
                            onJump = { jumpTo(it) },
                            onSwipeReply = if (canReply) {
                                {
                                    tutorials.done(Tutorials.Tip.ChatReply)
                                    vm.replyTo(message)
                                }
                            } else {
                                null
                            },
                            highlightKey = highlight?.takeIf { it.messageId == message.id }?.nonce,
                            hint = bubbleHint?.takeIf { it.first == message.id }?.second,
                            // The message drawn above this one is the next item: close together while one person writes.
                            modifier = Modifier.padding(
                                top = if (unread == null && day == null && ChatRules.continuesRun(message, older)) 2.dp else 10.dp,
                            ),
                        )
                    }
                }
                // The first page's spinner has a key of its own: the list keeps its place by the key of the item at the
                // bottom, and anchored to "start" it would open at the oldest message (and keep loading older ones).
                item(key = if (state.loading && state.messages.isEmpty()) "loading" else "start") {
                    ConversationStart(
                        state = state,
                        other = other,
                        name = title,
                        onlineText = onlineText,
                        notes = notes,
                        onRetry = vm::load,
                        onLoadEarlier = vm::loadEarlier,
                        onNeedPage = vm::ensurePage,
                        onOpenProfile = { openProfile(userId) },
                    )
                }
            }
            AnimatedVisibility(
                visible = awayFromNewest,
                enter = if (motion.elements) fadeIn() + scaleIn() else EnterTransition.None,
                exit = if (motion.elements) fadeOut() + scaleOut() else ExitTransition.None,
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
    if (dmSettings) ChatSettingsSheet(onDismiss = { dmSettings = false })
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

/**
 * In place of the composer below 5 flowers (v0.13): Stips opens messages to users with 5 flowers or more, and says so
 * the way the site does.
 */
@Composable
private fun NeedFlowersBar(have: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            FlowerIcon(size = 20.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.chat_need_flowers, have),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
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
 * #6), and once there are no more, a short summary of the other person, as messaging apps show; in notes to self, a
 * line about them instead (#20).
 */
@Composable
private fun ConversationStart(
    state: ConversationUiState,
    other: Author,
    name: String,
    onlineText: String,
    notes: Boolean,
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
        else -> if (notes) {
            NotesIntro(other)
        } else {
            LaunchedEffect(Unit) { onNeedPage() }
            ChatSummary(other, name, state, onlineText, onOpenProfile)
        }
    }
}

/** The top of notes to self (#20): what this chat is for, in place of a person's summary. Not a card: just a few words. */
@Composable
private fun NotesIntro(me: Author) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NotesAvatar(me, 64.dp)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.notes_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The date above a day's first message (v0.17, as in WhatsApp): today, yesterday, the weekday within a week, then the
 * date, with the year when it isn't this one.
 */
@Composable
private fun DayHeader(date: LocalDate, today: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    val text = when (ChatRules.dayKind(date, today)) {
        ChatRules.DayKind.Today -> stringResource(R.string.notif_today)
        ChatRules.DayKind.Yesterday -> stringResource(R.string.notif_yesterday)
        ChatRules.DayKind.ThisWeek -> date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        ChatRules.DayKind.ThisYear -> date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
        ChatRules.DayKind.Older -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
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

/** How far a bubble is dragged to reply to it (easy reply in chats, v0.12), and how far it can go; a flick works too (v0.16). */
private val BUBBLE_SWIPE_THRESHOLD = 56.dp
private val BUBBLE_SWIPE_MAX = 88.dp

/** How long a message glows after a jump to it (v0.12, as answers). */
private const val CHAT_HIGHLIGHT_MS = 2_000L

/** A message to light up after a jump; [nonce] tells repeated jumps to the same one apart. */
private data class ChatHighlight(val messageId: Long, val nonce: Long)

/**
 * A compact bubble with the text only: a tap shows its time, a long-press copies it (#6). Easy reply (v0.12): dragging
 * it from left to right (in Hebrew too) replies to it. A message that points up with carets shows the one it replies
 * to as a quote above its text, which stays as written.
 */
@Composable
private fun Bubble(
    message: ChatMessage,
    mine: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    reply: ReplyRefs.Resolved?,
    messages: List<ChatMessage>,
    authorOf: (ChatMessage) -> String,
    onJump: (Long) -> Unit,
    onSwipeReply: (() -> Unit)?,
    highlightKey: Long?,
    hint: SwipeHint? = null,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val motion = settings.motion
    val base = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    // A jump lights the bubble up: two slow breaths over two seconds, then back to normal.
    val glow = remember { Animatable(0f) }
    LaunchedEffect(highlightKey) {
        if (highlightKey == null) return@LaunchedEffect
        glow.glowOnce(motion, CHAT_HIGHLIGHT_MS)
    }
    val container = lerp(base, MaterialTheme.colorScheme.tertiaryContainer, glow.value)

    val drag = rememberSwipeDrag(trigger = BUBBLE_SWIPE_THRESHOLD, max = BUBBLE_SWIPE_MAX)
    // A tip slides the bubble a little to show the reply swipe (#28).
    val tip = rememberTipState()
    SwipeHintEffect(hint, drag, tip)
    val replyLabel = stringResource(R.string.action_reply)
    val quoted = reply?.targets?.mapNotNull { messages.getOrNull(it) }.orEmpty()
    val inBody = remember(reply, messages) {
        reply?.inBody?.mapNotNull { (range, index) -> messages.getOrNull(index)?.let { TextTarget(range, it.id) } }.orEmpty()
    }
    // A Stips question or profile link shows as a card; on a line of its own, the card takes its place (v0.18).
    val shown = remember(reply, message.text) { ChatLinks.read(reply?.body ?: message.text) }
    val bodyTargets = remember(inBody, shown) {
        inBody.mapNotNull { target -> ChatLinks.shift(target.range, shown.removed)?.let { TextTarget(it, target.id) } }
    }

    Box(
        modifier
            .fillMaxWidth()
            .swipeToAct(drag, onRight = onSwipeReply, onLeft = null),
    ) {
        if (onSwipeReply != null) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .absolutePadding(left = 4.dp)
                    .size(22.dp)
                    .graphicsLayer {
                        val progress = drag.rightProgress
                        alpha = progress
                        scaleX = 0.6f + 0.4f * progress
                        scaleY = 0.6f + 0.4f * progress
                    },
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .absoluteOffset { IntOffset(drag.offset.roundToInt(), 0) },
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(container)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .semantics {
                        val action = onSwipeReply
                        if (action != null) {
                            customActions = listOf(
                                CustomAccessibilityAction(replyLabel) {
                                    action()
                                    true
                                },
                            )
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                // The quotes stretch to the text's width, and the text to theirs.
                Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    quoted.forEach { target ->
                        ChatQuote(
                            author = authorOf(target),
                            text = ReplyRefs.snippet(ChatLinks.withoutLink(target.text), hideLeading = settings.chatHideReplyCarets),
                            content = content,
                            onClick = { onJump(target.id) },
                        )
                    }
                    shown.target?.let { ChatLinkCard(it, content, onLongClick) }
                    if (shown.body.isNotBlank() || shown.target == null) {
                        LinkifiedText(
                            shown.body,
                            style = MaterialTheme.typography.bodyLarge.copy(color = content),
                            selectable = false,
                            targets = bodyTargets,
                            onTarget = onJump,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = if (motion.elements) expandVertically() + fadeIn() else EnterTransition.None,
                exit = if (motion.elements) shrinkVertically() + fadeOut() else ExitTransition.None,
            ) {
                Text(
                    bubbleTime(message.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        TipPill(tip, Modifier.align(Alignment.Center))
    }
}

/** The message a reply points at, inside its bubble: who wrote it and its first lines; a tap goes there (v0.12). */
@Composable
private fun ChatQuote(author: String, text: String, content: Color, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(content.copy(alpha = 0.07f))
            .clickable(onClickLabel = stringResource(R.string.chat_reply_jump), onClick = onClick),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                author,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    draftVersion: Int,
    focusComposer: Int,
    replyTo: ChatMessage?,
    replyName: String?,
    about: Long?,
    sending: Boolean,
    error: String?,
    onDraft: (String) -> Unit,
    onCancelReply: () -> Unit,
    onRemoveAbout: () -> Unit,
    onSend: () -> Unit,
    hint: String = stringResource(R.string.message_hint),
) {
    var field by remember { mutableStateOf(TextFieldValue(draft, TextRange(draft.length))) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(draftVersion) {
        // The app replaced the text (a reply started or was cancelled, a message went out): cursor at the end.
        if (field.text != draft) field = TextFieldValue(draft, TextRange(draft.length))
    }
    // Easy reply (v0.12): a swipe starts a reply, so the cursor goes to the text and the keyboard opens.
    LaunchedEffect(focusComposer) {
        if (focusComposer > 0) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (replyTo != null) ChatReplyBar(replyTo, replyName, onCancelReply)
            // Reply in private (v0.18): the question's link goes first, so the message can be sent without words too.
            if (about != null) ChatAboutBar(about, onRemoveAbout)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = field,
                    onValueChange = {
                        field = it
                        onDraft(it.text)
                    },
                    placeholder = { Text(hint) },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    isError = error != null,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSend, enabled = !sending && (draft.isNotBlank() || about != null)) {
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

/** Above the box while replying (v0.12): to whom ([name], or null for the user's own message), its first line, ✕. */
@Composable
private fun ChatReplyBar(replyTo: ChatMessage, name: String?, onCancel: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                if (name != null) stringResource(R.string.reply_to, name) else stringResource(R.string.reply_to_self),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                ReplyRefs.snippet(ChatLinks.withoutLink(replyTo.text), hideLeading = LocalAppSettings.current.chatHideReplyCarets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.reply_cancel), modifier = Modifier.size(20.dp))
        }
    }
}
