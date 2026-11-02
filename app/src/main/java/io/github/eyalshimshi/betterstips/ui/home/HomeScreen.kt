package io.github.eyalshimshi.betterstips.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.answersText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.ui.components.pinsText
import io.github.eyalshimshi.betterstips.ui.feed.FeedFilters
import io.github.eyalshimshi.betterstips.ui.feed.FeedViewModel
import io.github.eyalshimshi.betterstips.ui.feed.QuestionList
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val AUTO_REFRESH_MS = 30_000L

data class HotStripState(
    val items: List<Question> = emptyList(),
    val loading: Boolean = true,
    val error: ApiException? = null,
)

/** The top 5 hot questions shown as big cards above the New feed. */
class HotStripViewModel(private val repository: StipsRepository) : ViewModel() {
    private val _state = MutableStateFlow(HotStripState())
    val state: StateFlow<HotStripState> = _state.asStateFlow()
    private var loadedFor: String? = null
    private var loadedOnce = false
    private var job: Job? = null

    fun load(channelName: String?, force: Boolean = false) {
        if (!force && loadedOnce && loadedFor == channelName) return
        loadedFor = channelName
        loadedOnce = true
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val query = FeedQuery(sort = FeedSort.Hot, channelName = channelName).normalized()
                val list = repository.feedPage(query, 1).take(5)
                _state.value = HotStripState(items = list, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.asApiException()) }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenQuestion: (Long) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenHot: () -> Unit,
) {
    val container = LocalAppContainer.current
    val feedVm = appViewModel { FeedViewModel(it.repository, FeedQuery(), useCache = true) }
    val hotVm = appViewModel { HotStripViewModel(it.repository) }
    val state by feedVm.state.collectAsStateWithLifecycle()
    val query by feedVm.query.collectAsStateWithLifecycle()
    val hot by hotVm.state.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(query.channelName) { hotVm.load(query.channelName) }

    // Live: while Home is on screen, new questions slide in on top every 30 seconds.
    LaunchedEffect(settings.autoRefreshNew, query) {
        if (!settings.autoRefreshNew) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(AUTO_REFRESH_MS)
                val atTop = listState.firstVisibleItemIndex <= 3
                val added = feedVm.pollNew()
                if (added > 0 && atTop) listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = { SearchBarRow(onSearch = onOpenTopic, onOpenNotifications = onOpenNotifications) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        QuestionList(
            state = state,
            channels = channels,
            emptyTitle = stringResource(R.string.empty_feed),
            onOpen = { onOpenQuestion(it.id) },
            onRefresh = {
                feedVm.refresh()
                hotVm.load(query.channelName, force = true)
            },
            onLoadMore = { feedVm.loadMore() },
            onRetry = { feedVm.retry() },
            modifier = Modifier.padding(padding),
            listState = listState,
            header = {
                item(key = "channels") {
                    FeedFilters(
                        query,
                        channels,
                        showChannels = true,
                        showSort = false,
                        onChange = {
                            feedVm.setQuery(it)
                            scope.launch { listState.scrollToItem(0) }
                        },
                    )
                }
                item(key = "hot") {
                    HotSection(
                        state = hot,
                        channels = channels,
                        onOpen = { onOpenQuestion(it.id) },
                        onSeeAll = onOpenHot,
                        onRetry = { hotVm.load(query.channelName, force = true) },
                    )
                }
                item(key = "new-header") {
                    NewHeader(
                        live = settings.autoRefreshNew,
                        onToggleLive = { container.settings.update { s -> s.copy(autoRefreshNew = !s.autoRefreshNew) } },
                    )
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun HotSection(
    state: HotStripState,
    channels: List<Channel>,
    onOpen: (Question) -> Unit,
    onSeeAll: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(R.string.hot_now), Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.see_all)) }
        }
        val error = state.error
        when {
            state.items.isEmpty() && state.loading -> Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(196.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
            state.items.isEmpty() && error != null -> TextButton(onClick = onRetry, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(R.string.hot_failed))
            }
            else -> {
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    flingBehavior = rememberSnapFlingBehavior(rowState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.items, key = { _, question -> question.id }) { index, question ->
                        HotCard(
                            question = question,
                            rank = index + 1,
                            seed = channels.firstOrNull { it.id == question.channelId }?.seedColor,
                            onClick = { onOpen(question) },
                        )
                    }
                }
            }
        }
    }
}

/** A big card: the cover fills it and the title sits on a dark fade at the bottom. */
@Composable
private fun HotCard(question: Question, rank: Int, seed: Int?, onClick: () -> Unit) {
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    val cover = question.cover
    val withImage = cover != null && showImages
    val tinted = withImage || seed != null
    val plain = if (seed != null) Color(seed) else MaterialTheme.colorScheme.primaryContainer
    val textColor = if (tinted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = plain),
        modifier = Modifier
            .width(280.dp)
            .height(196.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (cover != null && showImages) {
                AsyncImage(
                    model = cover.largeUrl,
                    contentDescription = null,
                    imageLoader = loader,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (tinted) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.05f),
                                0.45f to Color.Black.copy(alpha = 0.2f),
                                1f to Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                )
            }
            Text(
                "#$rank",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    question.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val meta = listOf(
                    answersText(question.answerCount),
                    if (question.pinCount > 0) pinsText(question.pinCount) else "",
                ).filter { it.isNotBlank() }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.85f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun NewHeader(live: Boolean, onToggleLive: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(stringResource(R.string.new_questions), Modifier.weight(1f))
        FilterChip(
            selected = live,
            onClick = onToggleLive,
            label = { Text(stringResource(R.string.live)) },
            leadingIcon = { LiveDot(active = live) },
        )
    }
}

@Composable
private fun LiveDot(active: Boolean) {
    val animate = LocalAppSettings.current.animations
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer { alpha = if (active && animate) pulse else 1f }
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
    )
}

/** The pill at the top of Home. Stips has no full-text search, so it opens a topic (D-15). */
@Composable
private fun SearchBarRow(onSearch: (String) -> Unit, onOpenNotifications: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val unread = LocalUnreadCounts.current.notifications
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            placeholder = { Text(stringResource(R.string.search_topic_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val topic = text.trim().removePrefix("#").trim()
                    if (topic.isNotEmpty()) {
                        keyboard?.hide()
                        onSearch(topic)
                    }
                },
            ),
        )
        IconButton(onClick = onOpenNotifications) {
            BadgedBox(
                badge = {
                    if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                },
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications_title))
            }
        }
    }
}
