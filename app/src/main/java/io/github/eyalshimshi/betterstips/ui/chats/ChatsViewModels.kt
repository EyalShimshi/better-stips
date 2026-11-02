package io.github.eyalshimshi.betterstips.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
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
    val error: ApiException? = null,
    val draft: String = "",
    val sending: Boolean = false,
    val sendError: ApiException? = null,
)

class ConversationViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val userId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()

    val myUserId: Long? get() = session.session.value.userId

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
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

    fun loadEarlier() {
        val current = _state.value
        val oldest = current.messages.firstOrNull()?.id ?: return
        if (current.loadingEarlier || !current.canLoadEarlier) return
        viewModelScope.launch {
            _state.update { it.copy(loadingEarlier = true, error = null) }
            try {
                val older = repository.thread(userId, beforeId = oldest)
                _state.update { s ->
                    val merged = (older + s.messages).distinctBy { it.id }.sortedBy { it.id }
                    s.copy(messages = merged, loadingEarlier = false, canLoadEarlier = merged.size > s.messages.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingEarlier = false, error = e.asApiException()) }
            }
        }
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
                val sent = ChatMessage(
                    id = id ?: ((current.messages.lastOrNull()?.id ?: 0L) + 1),
                    text = text,
                    time = java.time.Instant.now(),
                    fromUserId = myUserId,
                    toUserId = userId,
                )
                _state.update { s -> s.copy(messages = s.messages + sent, draft = "", sending = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, sendError = e.asApiException()) }
            }
        }
    }
}
