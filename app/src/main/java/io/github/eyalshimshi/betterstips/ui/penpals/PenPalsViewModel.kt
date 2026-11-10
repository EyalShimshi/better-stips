package io.github.eyalshimshi.betterstips.ui.penpals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.PenPalPost
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.AskRules
import io.github.eyalshimshi.betterstips.util.AutoRefresh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the user can't post on the board right now. */
sealed interface PostBlock {
    data object SignedOut : PostBlock

    data class NeedFlowers(val have: Int) : PostBlock

    data class Cooldown(val intervalMinutes: Int?, val nextInMinutes: Int?) : PostBlock
}

enum class PenPalMessage { Posted, Deleted, Failed }

data class PenPalsState(
    val items: List<PenPalPost> = emptyList(),
    val page: Int = 0,
    val end: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
    val error: ApiException? = null,
    /** The board was just replaced by a refresh by hand: posts don't slide to their places (v0.18, #30). */
    val swapped: Boolean = false,
    /** Stips opens the board to ages 13–21 (moderators aside); signed-out readers may browse. */
    val closedForAge: Boolean = false,
    val composerOpen: Boolean = false,
    val checking: Boolean = false,
    val block: PostBlock? = null,
    val draft: String = "",
    val posting: Boolean = false,
    val postError: ApiException? = null,
    val message: PenPalMessage? = null,
)

/** חברים לעט (#4): a board of short posts; replies go to private messages. */
class PenPalsViewModel(private val repository: StipsRepository, private val session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(PenPalsState())
    val state: StateFlow<PenPalsState> = _state.asStateFlow()
    private var generation = 0

    /** When the board's first page last came from Stips (a refresh, Live, coming back), v0.16. */
    private var pageOneAt = 0L

    init {
        refresh()
        checkAge()
    }

    private fun checkAge() {
        val me = session.session.value.userId ?: return
        viewModelScope.launch {
            val age = try {
                repository.profilePage(me).age
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (age != null && (age < MIN_AGE || age > MAX_AGE)) _state.update { it.copy(closedForAge = true) }
        }
    }

    fun refresh() {
        val gen = ++generation
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val list = repository.penPals(1)
                if (gen != generation) return@launch
                pageOneAt = System.currentTimeMillis()
                _state.update { it.copy(items = list, page = 1, end = list.isEmpty(), refreshing = false, moreFailed = false, swapped = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(refreshing = false, error = e.asApiException()) }
            }
        }
    }

    /** Whether coming back to the board should look for new posts: not when the first page came in moments ago. */
    fun isStale(now: Long = System.currentTimeMillis()): Boolean = AutoRefresh.due(pageOneAt, now)

    /**
     * Live and coming back (v0.16): the first page again, its new posts on top and the rest as it is, so nothing moves
     * under the reader. Returns how many came, or -1 when the check failed.
     */
    suspend fun pollNew(): Int {
        val s = _state.value
        if (s.refreshing || s.page == 0 || s.closedForAge) return 0
        val gen = generation
        val fresh = try {
            repository.penPals(1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return -1
        }
        if (gen != generation) return 0
        pageOneAt = System.currentTimeMillis()
        var added = 0
        _state.update { current ->
            val known = current.items.mapTo(HashSet()) { it.id }
            val newOnes = fresh.filter { it.id !in known }
            added = newOnes.size
            if (newOnes.isEmpty()) current else current.copy(items = newOnes + current.items, swapped = false)
        }
        return added
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.end || s.refreshing || s.page == 0) return
        val gen = generation
        val next = s.page + 1
        _state.update { it.copy(loadingMore = true, moreFailed = false) }
        viewModelScope.launch {
            try {
                val list = repository.penPals(next)
                if (gen != generation) return@launch
                // Pages shift as new posts arrive, so ids can repeat.
                _state.update { st -> st.copy(items = (st.items + list).distinctBy { it.id }, page = next, end = list.isEmpty(), loadingMore = false, swapped = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(loadingMore = false, moreFailed = true) }
            }
        }
    }

    /** Opens the composer after checking what the site checks: signed in, 5 flowers, and the posting interval. */
    fun openComposer() {
        if (!session.session.value.signedIn) {
            _state.update { it.copy(composerOpen = true, block = PostBlock.SignedOut) }
            return
        }
        _state.update { it.copy(composerOpen = true, checking = true, block = null, postError = null) }
        viewModelScope.launch {
            val block = try {
                val user = repository.currentAppUser()
                val allowance = repository.penPalAllowance()
                // As on the site: 5 flowers, unless the user is a moderator.
                val flowers = user?.flowers
                when {
                    user != null && flowers != null && flowers < MIN_FLOWERS && !user.isModerator -> PostBlock.NeedFlowers(flowers)
                    !allowance.allowed -> PostBlock.Cooldown(allowance.intervalMinutes, allowance.nextInMinutes)
                    else -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Let the server decide when the user posts.
                null
            }
            _state.update { it.copy(checking = false, block = block) }
        }
    }

    fun closeComposer() = _state.update { it.copy(composerOpen = false) }

    fun setDraft(text: String) = _state.update { it.copy(draft = text.take(AskRules.PEN_PAL_MAX), postError = null) }

    /** "My last message", as on the site. */
    fun useLastMessage() {
        viewModelScope.launch {
            val text = try {
                repository.lastPenPalMessage()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (text != null) _state.update { it.copy(draft = text.take(AskRules.PEN_PAL_MAX)) }
        }
    }

    fun post() {
        val s = _state.value
        if (s.posting || AskRules.penPalProblem(s.draft) != null) return
        viewModelScope.launch {
            _state.update { it.copy(posting = true, postError = null) }
            try {
                repository.postPenPal(s.draft.trim())
                _state.update { it.copy(posting = false, draft = "", composerOpen = false, message = PenPalMessage.Posted) }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(posting = false, postError = e.asApiException()) }
            }
        }
    }

    fun delete(post: PenPalPost) = act(PenPalMessage.Deleted) {
        repository.deleteItem("penfriendsitem", post.id)
        _state.update { s -> s.copy(items = s.items.filterNot { it.id == post.id }) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun act(success: PenPalMessage, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(message = success) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = PenPalMessage.Failed) }
            }
        }
    }

    private companion object {
        const val MIN_FLOWERS = 5
        const val MIN_AGE = 13
        const val MAX_AGE = 21
    }
}
