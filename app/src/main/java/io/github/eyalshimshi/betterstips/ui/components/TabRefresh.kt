package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Entering Home, Pen pals or Chats from the bottom bar, or coming back to the app on one of them, refreshes it
 * (v0.16). AppRoot marks the tab and the tab's screen takes the mark when it shows, so going back to a tab from a
 * question, or turning the phone, doesn't refresh it.
 */
@Stable
class TabRefreshes {
    private val pending = HashSet<String>()
    private val _version = MutableStateFlow(0)

    /** Changes with every mark, so a screen that's already showing hears of it. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun request(route: String) {
        synchronized(pending) { pending += route }
        _version.update { it + 1 }
    }

    /** Whether [route] was marked; the mark is taken. */
    fun take(route: String): Boolean = synchronized(pending) { pending.remove(route) }
}

/** The app's [TabRefreshes]; provided by AppRoot. */
val LocalTabRefreshes = staticCompositionLocalOf { TabRefreshes() }

/** Runs [onRefresh] when [route]'s tab is entered from the bottom bar, or the app comes back to it (v0.16). */
@Composable
fun RefreshOnEnter(route: String, onRefresh: suspend () -> Unit) {
    val refreshes = LocalTabRefreshes.current
    val current by rememberUpdatedState(onRefresh)
    LaunchedEffect(refreshes, route) {
        refreshes.version.collect {
            if (refreshes.take(route)) current()
        }
    }
}
