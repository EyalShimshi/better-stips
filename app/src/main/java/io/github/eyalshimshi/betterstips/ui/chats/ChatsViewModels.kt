package io.github.eyalshimshi.betterstips.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsUiState(
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: ApiException? = null,
)

class ChatsViewModel(private val repository: StipsRepository, session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            session.session.map { it.userId }.distinctUntilChanged().collect { userId ->
                if (userId == null) {
                    job?.cancel()
                    _state.value = ChatsUiState()
                } else {
                    load(refreshing = false)
                }
            }
        }
    }

    fun refresh() = load(refreshing = true)

    fun retry() = load(refreshing = false)

    /** A message came in or was read (the unread count changed): update the list without the spinner (#6). */
    fun refreshQuietly() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            try {
                val list = repository.conversations(page = 1)
                _state.update { it.copy(conversations = list, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The list on screen stays; the next change tries again.
            }
        }
    }

    private fun load(refreshing: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = !refreshing, refreshing = refreshing, error = null) }
            try {
                val list = repository.conversations(page = 1)
                _state.update { it.copy(conversations = list, loading = false, refreshing = false) }
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
    val sending: Boolean = false,
    val sendError: ApiException? = null,
    /** The other person: photo and online status for the top bar, and the summary at the start (#6). */
    val profile: UserProfile? = null,
    /** Their profile page (answers, flowers, time on Stips), read once the start of the chat is reached. */
    val page: ProfilePage? = null,
    /** Bubbles showing their time. A new visit starts with all of them folded (#6). */
    val expanded: Set<Long> = emptySet(),
)

class ConversationViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val userId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()
    private var pageRequested = false

    val myUserId: Long? get() = session.session.value.userId

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            launch { loadProfile() }
            try {
                val list = repository.thread(userId, beforeId = null)
                _state.update { it.copy(messages = list, loading = false, canLoadEarlier = list.isNotEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.asApiException()) }
            }
        }
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
        if (_state.value.loading) return true
        val last = _state.value.messages.lastOrNull()?.id ?: 0L
        return try {
            val fresh = repository.newMessages(userId, afterId = last)
            if (fresh.isNotEmpty()) _state.update { it.copy(messages = ChatRules.merge(it.messages, fresh)) }
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
                val older = repository.thread(userId, beforeId = oldest)
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

    fun setDraft(text: String) {
        _state.update { it.copy(draft = text, sendError = null) }
    }

    fun send() {
        val current = _state.value
        val text = current.draft.trim()
        if (text.isEmpty() || current.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, sendError = null) }
            try {
                val id = repository.sendMessage(userId, text)
                if (id == null) {
                    // Stips always answers with the new id (the site treats a missing one as a failure); if it ever
                    // doesn't, fetch the message rather than guess an id the next check would duplicate.
                    _state.update { it.copy(draft = "", sending = false) }
                    pollNew()
                    return@launch
                }
                val sent = ChatMessage(id = id, text = text, time = java.time.Instant.now(), fromUserId = myUserId, toUserId = userId)
                _state.update { s -> s.copy(messages = ChatRules.merge(s.messages, listOf(sent)), draft = "", sending = false) }
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
    /** Every message once, oldest first, whatever order pages and checks arrive in. */
    fun merge(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
        (current + incoming).distinctBy { it.id }.sortedBy { it.id }

    /** Whether [message] follows one from the same sender, so the two sit close together. */
    fun continuesRun(message: ChatMessage, previous: ChatMessage?): Boolean =
        previous != null && previous.fromUserId == message.fromUserId
}
