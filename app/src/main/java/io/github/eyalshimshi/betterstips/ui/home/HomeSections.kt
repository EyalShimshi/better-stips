package io.github.eyalshimshi.betterstips.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.HomeSection
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.FooterNote
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.question.SwipeableQuestionRow
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.ui.components.SkeletonRows
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeSectionsState(
    val sections: List<HomeSection> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: ApiException? = null,
)

/** Pure rules for paging the website's home sections (#8). */
object HomeSectionsMerge {
    /** Page [more] after [existing]: a heading seen before gets the new questions under it, without repeats. */
    fun append(existing: List<HomeSection>, more: List<HomeSection>): List<HomeSection> {
        if (more.isEmpty()) return existing
        val merged = LinkedHashMap<String, HomeSection>()
        existing.forEach { merged[it.title] = it }
        more.forEach { section ->
            val before = merged[section.title]
            merged[section.title] = if (before == null) {
                section
            } else {
                val known = before.questions.mapTo(HashSet()) { it.id }
                before.copy(questions = before.questions + section.questions.filter { it.id !in known }, topic = before.topic ?: section.topic)
            }
        }
        return merged.values.toList()
    }
}

/**
 * Home's website-like layout (#8): `feed.get` sections, page after page. Signed in it starts from the account's own
 * mix and, like the site, uses everyone's when that's empty. One visit keeps the site's order number for every page.
 */
class HomeSectionsViewModel(private val repository: StipsRepository, private val session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(HomeSectionsState())
    val state: StateFlow<HomeSectionsState> = _state.asStateFlow()

    private var page = 0
    private var order = 0
    private var personal = false
    private var started = false
    private var job: Job? = null

    fun ensureLoaded() {
        if (!started) load(reset = true, refreshing = false)
    }

    fun refresh() = load(reset = true, refreshing = true)

    fun retry() {
        if (_state.value.sections.isEmpty()) load(reset = true, refreshing = false) else loadMore(force = true)
    }

    fun loadMore(force: Boolean = false) {
        val s = _state.value
        if (!started || s.loading || s.refreshing || s.loadingMore || s.endReached || s.sections.isEmpty()) return
        if (s.error != null && !force) return
        load(reset = false, refreshing = false)
    }

    private fun load(reset: Boolean, refreshing: Boolean) {
        job?.cancel()
        started = true
        job = viewModelScope.launch {
            if (reset) {
                order = StipsRepository.homeFeedOrderNow()
                personal = session.session.value.signedIn
                _state.update {
                    it.copy(
                        loading = !refreshing,
                        refreshing = refreshing,
                        loadingMore = false,
                        endReached = false,
                        error = null,
                        sections = if (refreshing) it.sections else emptyList(),
                    )
                }
            } else {
                _state.update { it.copy(loadingMore = true, error = null) }
            }
            try {
                val next = if (reset) 1 else page + 1
                var list = repository.homeSections(next, order, personal)
                if (reset && personal && list.isEmpty()) {
                    personal = false
                    list = repository.homeSections(next, order, personal = false)
                }
                page = next
                _state.update { s ->
                    s.copy(
                        sections = if (reset) list else HomeSectionsMerge.append(s.sections, list),
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

/** The website-like Home (#8): [header] (chips and hot questions), then today's sections; a topic heading opens it. */
@Composable
fun HomeSectionsList(
    state: HomeSectionsState,
    channels: List<Channel>,
    onOpen: (Question) -> Unit,
    onOpenTopic: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    header: LazyListScope.() -> Unit = {},
) {
    val error = state.error
    // Questions hidden with a swipe leave the list at once (#18).
    val container = LocalAppContainer.current
    val blockVersion by container.blocks.version.collectAsStateWithLifecycle()
    val sections = remember(state.sections, blockVersion) {
        state.sections
            .map { section -> section.copy(questions = section.questions.filterNot { container.blocks.isBlocked("ask", it.id, it.author.userId) }) }
            .filter { it.questions.isNotEmpty() }
    }
    val moving = LocalAppSettings.current.motion.elements
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            header()
            if (state.loading && sections.isNotEmpty()) {
                item(key = "sections-progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            if (sections.isEmpty()) {
                when {
                    state.loading -> item(key = "sections-skeleton") { SkeletonRows() }
                    error != null -> item(key = "sections-error") { ErrorCard(error, onRetry) }
                    else -> item(key = "sections-empty") { MessageCard(stringResource(R.string.empty_feed), null, null, null) }
                }
            } else {
                sections.forEach { section ->
                    item(key = "section|${section.title}") {
                        SectionHeading(section, onOpenTopic, if (moving) Modifier.animateItem() else Modifier)
                    }
                    items(section.questions, key = { "section|${section.title}|${it.id}" }) { question ->
                        SwipeableQuestionRow(
                            question,
                            channels.firstOrNull { it.id == question.channelId },
                            onClick = { onOpen(question) },
                            modifier = if (moving) Modifier.animateItem() else Modifier,
                        )
                    }
                }
                when {
                    error != null -> item(key = "sections-error-more") { ErrorCard(error, onRetry) }
                    state.loadingMore -> item(key = "sections-more") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    state.endReached -> item(key = "sections-end") { FooterNote(stringResource(R.string.end_of_list)) }
                }
            }
        }
    }
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, sections.size) {
        if (nearEnd) onLoadMore()
    }
}

/** A section's heading with a thin rule, like the site's; a topic's heading opens the topic. */
@Composable
private fun SectionHeading(section: HomeSection, onOpenTopic: (String) -> Unit, modifier: Modifier = Modifier) {
    val topic = section.topic
    Row(
        modifier
            .fillMaxWidth()
            .then(if (topic != null) Modifier.clickable(role = Role.Button) { onOpenTopic(topic) } else Modifier)
            .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 4.dp)
            .semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (topic != null) "#$topic" else section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (topic != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (topic != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
