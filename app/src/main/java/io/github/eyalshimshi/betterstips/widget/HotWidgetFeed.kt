package io.github.eyalshimshi.betterstips.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.diag.NetLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the "Hot right now" widget shows (D-51): the first page of Hot, the list behind Home's big cards. Whenever a
 * screen loads that list, the widget shows the same copy at no cost. By itself it asks Stips only when Android wakes
 * it (hourly), the saved copy is over [STALE_MS] old and the request budget allows (D-46).
 */
class HotWidgetFeed(
    private val context: Context,
    private val repository: StipsRepository,
    private val scope: CoroutineScope,
) {
    data class State(
        val items: List<Question> = emptyList(),
        /** The saved copy has been read (it may have been empty). */
        val loaded: Boolean = false,
        val refreshing: Boolean = false,
        /** The last refresh failed and there's nothing to show. */
        val failed: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val oneAtATime = Mutex()

    /** Shows the last saved copy, once per process. */
    suspend fun ensureLoaded() {
        if (_state.value.loaded) return
        val cached = repository.cachedFirstPage(QUERY)
        _state.update { if (it.loaded) it else it.copy(items = select(cached), loaded = true) }
    }

    /** Stips sent this list to some screen (or to the widget): show it, and redraw the widgets. */
    fun publish(list: List<Question>) {
        _state.update { it.copy(items = select(list), loaded = true, failed = false) }
        scope.launch { updateWidgets() }
    }

    /**
     * Asks Stips again when the saved copy is stale and the budget allows, or always when [byUser] (the refresh
     * button). One at a time: while one runs, another call returns at once, and the running one redraws when done.
     */
    suspend fun refresh(byUser: Boolean) {
        if (!oneAtATime.tryLock()) return
        try {
            ensureLoaded()
            if (byUser) {
                _state.update { it.copy(refreshing = true) }
                updateWidgets()
            } else {
                val savedAt = repository.firstPageSavedAt(QUERY) ?: 0L
                if (!isStale(savedAt, System.currentTimeMillis()) || !RateGuard.allowBackground()) return
            }
            // A successful load reaches publish() through the repository's onFirstPage.
            val loaded = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                try {
                    repository.feedPage(QUERY, 1)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    NetLog.event("widget", "refresh failed: ${e.javaClass.simpleName}")
                    false
                }
            } ?: false
            if (!loaded) _state.update { it.copy(failed = it.items.isEmpty()) }
        } finally {
            val wasRefreshing = _state.value.refreshing
            _state.update { it.copy(refreshing = false) }
            oneAtATime.unlock()
            if (wasRefreshing) updateWidgets()
        }
    }

    private suspend fun updateWidgets() {
        try {
            HotWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetLog.event("widget", "redraw failed: ${e.javaClass.simpleName}")
        }
    }

    private fun select(list: List<Question>): List<Question> =
        list.filterNot { repository.blocks.isBlocked("ask", it.id, it.author.userId) }.take(MAX_ITEMS)

    companion object {
        /** Home's top cards: Hot, right now, all channels. */
        val QUERY = FeedQuery(sort = FeedSort.Hot)
        const val MAX_ITEMS = 10
        const val STALE_MS = 30 * 60_000L
        private const val FETCH_TIMEOUT_MS = 15_000L

        /** Whether a copy saved at [savedAt] should be asked for again at [now]. Pure. */
        fun isStale(savedAt: Long, now: Long): Boolean = now - savedAt >= STALE_MS
    }
}
