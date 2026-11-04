package io.github.eyalshimshi.betterstips.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val items: List<Question> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: ApiException? = null,
)

/** One paged list of questions: Home, a topic, or the user's own lists. */
class FeedViewModel(
    private val repository: StipsRepository,
    initial: FeedQuery,
    private val useCache: Boolean,
) : ViewModel() {
    private val _query = MutableStateFlow(initial.normalized())
    val query: StateFlow<FeedQuery> = _query.asStateFlow()

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private var page = 0
    private var job: Job? = null

    init {
        load(reset = true, refreshing = false, showCache = useCache)
    }

    fun setQuery(query: FeedQuery) {
        val next = query.normalized()
        if (next == _query.value) return
        _query.value = next
        load(reset = true, refreshing = false, showCache = useCache)
    }

    fun refresh() {
        load(reset = true, refreshing = true, showCache = false)
    }

    fun retry() {
        if (_state.value.items.isEmpty()) load(reset = true, refreshing = false, showCache = false) else loadMore(force = true)
    }

    fun loadMore(force: Boolean = false) {
        val s = _state.value
        if (s.loading || s.refreshing || s.loadingMore || s.endReached || s.items.isEmpty()) return
        if (s.error != null && !force) return
        load(reset = false, refreshing = false, showCache = false)
    }

    /** Live: fetches page 1 and puts questions we don't have yet on top. Returns how many, or -1 when the check failed. */
    suspend fun pollNew(): Int {
        val s = _state.value
        if (s.loading || s.refreshing || s.items.isEmpty()) return 0
        val query = _query.value
        val fresh = try {
            repository.feedPage(query, 1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return -1
        }
        if (query != _query.value) return 0
        var added = 0
        _state.update { current ->
            val known = current.items.mapTo(HashSet()) { it.id }
            val newOnes = fresh.filter { it.id !in known }
            added = newOnes.size
            if (newOnes.isEmpty()) current else current.copy(items = newOnes + current.items)
        }
        return added
    }

    private fun load(reset: Boolean, refreshing: Boolean, showCache: Boolean) {
        job?.cancel()
        val query = _query.value
        job = viewModelScope.launch {
            if (reset) {
                _state.update {
                    it.copy(
                        loading = !refreshing,
                        refreshing = refreshing,
                        loadingMore = false,
                        endReached = false,
                        error = null,
                        items = if (refreshing) it.items else emptyList(),
                    )
                }
                if (showCache) {
                    val cached = repository.cachedFirstPage(query)
                    if (cached.isNotEmpty()) _state.update { it.copy(items = cached) }
                }
            } else {
                _state.update { it.copy(loadingMore = true, error = null) }
            }
            try {
                val next = if (reset) 1 else page + 1
                val list = repository.feedPage(query, next)
                page = next
                _state.update { s ->
                    s.copy(
                        items = if (reset) list else (s.items + list).distinctBy { it.id },
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        endReached = list.isEmpty(),
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, loadingMore = false, error = e.asApiException()) }
            }
        }
    }
}
