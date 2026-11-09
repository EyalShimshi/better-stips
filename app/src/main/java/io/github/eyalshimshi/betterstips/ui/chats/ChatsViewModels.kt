package io.github.eyalshimshi.betterstips.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.ChatReadMarks
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.UnreadMark
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.AutoRefresh
import io.github.eyalshimshi.betterstips.util.ReplyRefs
import io.github.eyalshimshi.betterstips.util.StipsEmoji
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ChatsUiState(
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: ApiException? = null,
    /** The last page of `messages.list` read (v0.17, build 30): Stips sends the chats a page at a time. 0 before the first. */
    val page: Int = 0,
    /** No more pages: one came back empty, or with no one new. */
    val end: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
)

class ChatsViewModel(private val repository: StipsRepository, session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state.asStateFlow()
    private var job: Job? = null
    private var moreJob: Job? = null

    /** Changes when the list starts again from page 1, so a later page asked for before can't land on the new list. */
    private var generation = 0

    /** When the list last came from Stips, for coming back to the app on Chats (v0.16). */
    private var loadedAt = 0L

    init {
        viewModelScope.launch {
            session.session.map { it.userId }.distinctUntilChanged().collect { userId ->
                if (userId == null) {
                    job?.cancel()
                    moreJob?.cancel()
                    generation++
                    _state.value = ChatsUiState()
                } else {
                    load(refreshing = false)
                }
            }
        }
    }

    fun refresh() = load(refreshing = true)

    fun retry() = load(refreshing = false)

    /** Coming back to the app on Chats (v0.16): the list again, quietly, unless it came in moments ago. */
    fun refreshIfStale() {
        if (AutoRefresh.due(loadedAt, System.currentTimeMillis())) refreshQuietly()
    }

    /**
     * A message came in or was read (the unread count changed): page 1 again, without the spinner (#6), over the chats
     * already loaded from later pages, which stay (v0.17, build 30).
     */
    fun refreshQuietly() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            try {
                val list = repository.conversations(page = 1)
                loadedAt = System.currentTimeMillis()
                _state.update { st ->
                    st.copy(
                        conversations = ChatRules.withFirstPage(st.conversations, list),
                        error = null,
                        page = maxOf(st.page, 1),
                        end = if (st.page == 0) list.isEmpty() else st.end,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The list on screen stays; the next change tries again.
            }
        }
    }

    /** The next page of chats, asked for as the list's end comes near (v0.17, build 30), as the site's list does. */
    fun loadMore() {
        val s = _state.value
        if (s.page == 0 || s.end || s.loadingMore || s.loading || s.refreshing) return
        val gen = generation
        val next = s.page + 1
        _state.update { it.copy(loadingMore = true, moreFailed = false) }
        moreJob = viewModelScope.launch {
            try {
                val list = repository.conversations(page = next)
                if (gen != generation) return@launch
                _state.update { st ->
                    val added = ChatRules.newOnPage(st.conversations, list)
                    st.copy(conversations = st.conversations + added, page = next, end = added.isEmpty(), loadingMore = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(loadingMore = false, moreFailed = true) }
            }
        }
    }

    private fun load(refreshing: Boolean) {
        job?.cancel()
        moreJob?.cancel()
        generation++
        job = viewModelScope.launch {
            _state.update { it.copy(loading = !refreshing, refreshing = refreshing, error = null, loadingMore = false, moreFailed = false) }
            try {
                // Pulling to refresh also makes sure the session is alive: a lost one answers with no chats (#9).
                val list = repository.conversations(page = 1, verify = refreshing)
                loadedAt = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        conversations = list.distinctBy { c -> c.other.userId },
                        loading = false,
                        refreshing = false,
                        page = 1,
                        end = list.isEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.asApiException()) }
            }
        }
    }
}

data class ConversationUiState(
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = true,
    val loadingEarlier: Boolean = false,
    val canLoadEarlier: Boolean = true,
    /** Loading earlier messages failed; the start of the list offers to try again. */
    val earlierFailed: Boolean = false,
    val error: ApiException? = null,
    val draft: String = "",
    /** Bumped when the app (not the user's typing) replaces the draft, so the field moves its cursor to the end. */
    val draftVersion: Int = 0,
    /** Easy reply (v0.12): the message this one replies to; the draft opens with the carets pointing at it. */
    val replyTo: ChatMessage? = null,
    /** Bumped to put the cursor in the box and open the keyboard (a swipe started a reply). */
    val focusComposer: Int = 0,
    val sending: Boolean = false,
    val sendError: ApiException? = null,
    /** The other person: photo and online status for the top bar, and the summary at the start (#6). */
    val profile: UserProfile? = null,
    /** Their profile page (answers, flowers, time on Stips), read once the start of the chat is reached. */
    val page: ProfilePage? = null,
    /** Bubbles showing their time. A new visit starts with all of them folded (#6). */
    val expanded: Set<Long> = emptySet(),
    /** You blocked messages from them: Stips' `userBlocked` on the first page, or the menu. The composer gives way. */
    val blocked: Boolean = false,
    val blockBusy: Boolean = false,
    /**
     * The "unread" bar (v0.13): above the first message that was unread when the chat opened, with how many of theirs
     * follow. Only for this visit; gone once the user writes.
     */
    val unread: UnreadMark? = null,
    /** A one-time message for the snackbar. */
    val notice: ChatNotice? = null,
)

enum class ChatNotice { Reported, Failed }

class ConversationViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val readMarks: ChatReadMarks,
    private val userId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()
    private var pageRequested = false

    /** Stips' count of unread messages here when the chat opened, from the last chats list; null when unknown (v0.13). */
    private val unreadOnOpen: Int? = repository.unreadInChat(userId)

    /** The newest message shown here on the last visit, read before this visit marks its own (v0.13). */
    private val seenBefore: Long? = readMarks.newestSeen(userId)
    private var unreadMarked = false

    val myUserId: Long? get() = session.session.value.userId

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            launch { loadProfile() }
            try {
                val first = repository.thread(userId, beforeId = null)
                val unread = if (unreadMarked) null else ChatRules.firstUnread(first.messages, myUserId, unreadOnOpen, seenBefore)
                unreadMarked = true
                _state.update {
                    it.copy(
                        messages = first.messages,
                        blocked = first.blocked,
                        loading = false,
                        canLoadEarlier = first.messages.isNotEmpty(),
                        unread = unread ?: it.unread,
                    )
                }
                // Reading the thread is what marks it read on Stips.
                repository.chatRead(userId)
                markSeen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.asApiException()) }
            }
        }
    }

    /** The newest message shown is the mark for the next visit. */
    private fun markSeen() {
        _state.value.messages.lastOrNull()?.let { readMarks.markSeen(userId, it.id) }
    }

    /** Who they are and whether they're online. Asked on opening, then every minute while the chat is on screen. */
    suspend fun loadProfile() {
        try {
            val profile = repository.userProfile(userId)
            _state.update { it.copy(profile = profile) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The top bar keeps the name it was opened with.
        }
    }

    /** Messages newer than the last one shown. Returns false when the check failed, so the caller can slow down. */
    suspend fun pollNew(): Boolean {
        // Nothing comes from someone you blocked.
        if (_state.value.loading || _state.value.blocked) return true
        val last = _state.value.messages.lastOrNull()?.id ?: 0L
        return try {
            val fresh = repository.newMessages(userId, afterId = last)
            if (fresh.isNotEmpty()) {
                _state.update { it.copy(messages = ChatRules.merge(it.messages, fresh)) }
                markSeen()
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** Called as the start of the list comes into view: loads earlier messages until there are none (#6). */
    fun loadEarlier() {
        val current = _state.value
        val oldest = current.messages.firstOrNull()?.id ?: return
        if (current.loadingEarlier || !current.canLoadEarlier) return
        viewModelScope.launch {
            _state.update { it.copy(loadingEarlier = true, earlierFailed = false) }
            try {
                val older = repository.thread(userId, beforeId = oldest).messages
                _state.update { s ->
                    val merged = ChatRules.merge(s.messages, older)
                    s.copy(messages = merged, loadingEarlier = false, canLoadEarlier = merged.size > s.messages.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingEarlier = false, earlierFailed = true) }
            }
        }
    }

    /** The start of the chat is on screen: its summary needs their profile page, once. */
    fun ensurePage() {
        if (pageRequested) return
        pageRequested = true
        viewModelScope.launch {
            try {
                val page = repository.profilePage(userId)
                _state.update { it.copy(page = page) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pageRequested = false
            }
        }
    }

    fun toggleTime(messageId: Long) {
        _state.update { s -> s.copy(expanded = if (messageId in s.expanded) s.expanded - messageId else s.expanded + messageId) }
    }

    /** Reports them to Stips' team (`reports.send`, `usermsgs`) with one of the site's reason ids and optional details. */
    fun report(reasonId: Int, details: String) {
        viewModelScope.launch {
            val notice = try {
                repository.reportUser(userId, reasonId, details.trim())
                ChatNotice.Reported
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChatNotice.Failed
            }
            _state.update { it.copy(notice = notice) }
        }
    }

    /** Blocks or unblocks messages from them (`messages.block_user`), as the site's chat menu does. */
    fun setBlocked(blocked: Boolean) {
        if (_state.value.blockBusy) return
        viewModelScope.launch {
            _state.update { it.copy(blockBusy = true) }
            try {
                repository.setMessagesBlocked(userId, blocked)
                _state.update { it.copy(blocked = blocked, blockBusy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(blockBusy = false, notice = ChatNotice.Failed) }
            }
        }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    fun setDraft(text: String) {
        _state.update {
            // Deleting the carets by hand ends the reply.
            val replyTo = if (it.replyTo != null && ReplyRefs.leadingCount(text) == null) null else it.replyTo
            it.copy(draft = text, replyTo = replyTo, sendError = null)
        }
    }

    /**
     * Easy reply (v0.12), as on answers: the draft opens with Stips' carets pointing at [message], counted from where a
     * new message lands (the bottom); a pointer the draft already opens with is replaced.
     */
    fun replyTo(message: ChatMessage) {
        val s = _state.value
        val count = ChatRules.replyCount(s.messages, message.id) ?: return
        _state.update {
            it.copy(
                replyTo = message,
                draft = ReplyRefs.withReply(it.draft, count),
                draftVersion = it.draftVersion + 1,
                focusComposer = it.focusComposer + 1,
                sendError = null,
            )
        }
    }

    fun cancelReply() {
        _state.update { it.copy(replyTo = null, draft = ReplyRefs.withoutReply(it.draft), draftVersion = it.draftVersion + 1) }
    }

    fun send() {
        val current = _state.value
        if (current.draft.isBlank() || current.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, sendError = null) }
            try {
                var text = current.draft.trim()
                val replyTo = current.replyTo
                if (replyTo != null) {
                    // Stips can't edit a sent message, so the carets are counted again just before sending, with the
                    // newest messages in: ones that came while the user wrote land in between.
                    pollNew()
                    ChatRules.replyCount(_state.value.messages, replyTo.id)?.let { text = ReplyRefs.withReply(text, it) }
                }
                val id = repository.sendMessage(userId, text)
                if (id == null) {
                    // Stips always answers with the new id (the site treats a missing one as a failure); if it ever
                    // doesn't, fetch the message rather than guess an id the next check would duplicate.
                    _state.update { it.copy(draft = "", draftVersion = it.draftVersion + 1, replyTo = null, sending = false, unread = null) }
                    pollNew()
                    return@launch
                }
                // As Stips will show it back: a :code: typed by hand is its emoji (#22).
                val sent = ChatMessage(id = id, text = StipsEmoji.inChat(text), time = java.time.Instant.now(), fromUserId = myUserId, toUserId = userId)
                _state.update { s ->
                    s.copy(
                        messages = ChatRules.merge(s.messages, listOf(sent)),
                        draft = "",
                        draftVersion = s.draftVersion + 1,
                        replyTo = null,
                        sending = false,
                        // Writing back reads the chat, as in WhatsApp.
                        unread = null,
                    )
                }
                markSeen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, sendError = e.asApiException()) }
            }
        }
    }
}

/** Pure rules for the chat screen. */
object ChatRules {
    /**
     * How many messages up [targetId] is from a message sent now, which lands at the bottom: the count of its carets
     * (v0.12). Null when that message isn't loaded.
     */
    fun replyCount(messages: List<ChatMessage>, targetId: Long): Int? {
        val index = messages.indexOfFirst { it.id == targetId }
        return if (index < 0) null else messages.size - index
    }

    /** Every message once, oldest first, whatever order pages and checks arrive in. */
    fun merge(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
        (current + incoming).distinctBy { it.id }.sortedBy { it.id }

    /** Whether [message] follows one from the same sender, so the two sit close together. */
    fun continuesRun(message: ChatMessage, previous: ChatMessage?): Boolean =
        previous != null && previous.fromUserId == message.fromUserId

    /**
     * Their messages after [seenUpTo], the newest one the reader has seen at the bottom: the count on the jump button.
     * Nothing counts before the first page has been shown ([seenUpTo] 0).
     */
    fun unseen(messages: List<ChatMessage>, seenUpTo: Long, myId: Long?): Int =
        if (seenUpTo <= 0L) 0 else messages.count { it.id > seenUpTo && it.fromUserId != myId }

    /**
     * Where the "unread" bar goes when a chat opens (v0.13): above the first of their messages that Stips counted as
     * unread ([unreadCount], from the chats list), or, without that count, the first of theirs after [lastSeenId] (the
     * newest one shown on the last visit). With how many of theirs it starts. Null when nothing is unread or known.
     */
    fun firstUnread(messages: List<ChatMessage>, myId: Long?, unreadCount: Int?, lastSeenId: Long?): UnreadMark? {
        if (myId == null) return null
        val theirs = messages.filter { it.fromUserId != myId }
        val first = when {
            unreadCount != null -> if (unreadCount > 0) theirs.takeLast(unreadCount).firstOrNull() else null
            lastSeenId != null -> theirs.firstOrNull { it.id > lastSeenId }
            else -> null
        } ?: return null
        return UnreadMark(first.id, theirs.count { it.id >= first.id })
    }

    /** Notes to self (#20): the chat with the account itself, which Stips keeps like any other. */
    fun isNotes(userId: Long?, myId: Long?): Boolean = myId != null && userId == myId

    /**
     * The chats list after page 1 came again (v0.17, build 30): its chats on top, in Stips' order, then the ones already
     * loaded from later pages that aren't on it, as they were. Each person once.
     */
    fun withFirstPage(current: List<Conversation>, page: List<Conversation>): List<Conversation> {
        val onPage: Set<Long?> = page.map { it.other.userId }.toSet()
        return (page + current.filter { it.other.userId !in onPage }).distinctBy { it.other.userId }
    }

    /**
     * What a later page adds (v0.17, build 30): people not on the list yet, each once. Pages can repeat a chat when a
     * new message moved others down meanwhile, as the site's own list allows for. Nothing new means the list is whole.
     */
    fun newOnPage(current: List<Conversation>, page: List<Conversation>): List<Conversation> {
        val listed: Set<Long?> = current.map { it.other.userId }.toSet()
        return page.filter { it.other.userId !in listed }.distinctBy { it.other.userId }
    }

    /**
     * The shelf on top of Chats (#20), as on Instagram: the people of recent chats who are online now, the most
     * recent first (at the start, the right in Hebrew). Notes to self come before them, on their own.
     */
    fun onlineShelf(conversations: List<Conversation>, myId: Long?): List<Author> =
        conversations.map { it.other }
            .filter { it.online && it.userId != null && !isNotes(it.userId, myId) }
            .distinctBy { it.userId }

    /** The chats the search on top of Chats finds (#20): by the person's name, and notes to self by [notesName]. */
    fun matching(conversations: List<Conversation>, query: String, myId: Long?, notesName: String): List<Conversation> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return conversations.filter { c ->
            c.other.nickname.contains(q, ignoreCase = true) || (isNotes(c.other.userId, myId) && notesName.contains(q, ignoreCase = true))
        }
    }

    /** What a day header in a chat says (v0.17, as in WhatsApp): today, yesterday, the weekday, the date, or with the year. */
    enum class DayKind { Today, Yesterday, ThisWeek, ThisYear, Older }

    /** Whether a day header goes above [message]: it's the oldest shown, or [older], the one before it, is from another day. */
    fun startsDay(message: ChatMessage, older: ChatMessage?, zone: ZoneId): Boolean {
        val day = message.time?.atZone(zone)?.toLocalDate() ?: return false
        if (older == null) return true
        val before = older.time?.atZone(zone)?.toLocalDate() ?: return false
        return day != before
    }

    fun dayKind(date: LocalDate, today: LocalDate): DayKind = when {
        !date.isBefore(today) -> DayKind.Today
        date == today.minusDays(1) -> DayKind.Yesterday
        date.isAfter(today.minusDays(7)) -> DayKind.ThisWeek
        date.year == today.year -> DayKind.ThisYear
        else -> DayKind.Older
    }
}
