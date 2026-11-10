package io.github.eyalshimshi.betterstips.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.HotWindow
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.FooterNote
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.question.SwipeableQuestionRow
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.components.SkeletonRows
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.channelLabel
import io.github.eyalshimshi.betterstips.ui.components.listItemMotion

@Composable
fun QuestionList(
    state: FeedUiState,
    channels: List<Channel>,
    emptyTitle: String,
    onOpen: (Question) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    header: LazyListScope.() -> Unit = {},
) {
    val error = state.error
    // Questions hidden with a swipe or ⋮ leave the list at once (#18).
    val container = LocalAppContainer.current
    val blockVersion by container.blocks.version.collectAsStateWithLifecycle()
    val questions = remember(state.items, blockVersion) {
        state.items.filterNot { container.blocks.isBlocked("ask", it.id, it.author.userId) }
    }
    val moving = LocalAppSettings.current.motion.elements
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            header()
            if (state.loading && questions.isNotEmpty()) {
                item(key = "progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            if (questions.isEmpty()) {
                when {
                    state.loading -> item(key = "skeleton") { SkeletonRows() }
                    error != null -> item(key = "error") { ErrorCard(error, onRetry) }
                    else -> item(key = "empty") { MessageCard(emptyTitle, null, null, null) }
                }
            } else {
                items(questions, key = { it.id }) { question ->
                    SwipeableQuestionRow(
                        question,
                        channels.firstOrNull { it.id == question.channelId },
                        onClick = { onOpen(question) },
                        modifier = listItemMotion(moving, state.swapped),
                    )
                }
                when {
                    error != null -> item(key = "error-more") { ErrorCard(error, onRetry) }
                    state.loadingMore -> item(key = "more") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    state.endReached -> item(key = "end") { FooterNote(stringResource(R.string.end_of_list)) }
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
    LaunchedEffect(nearEnd, questions.size) {
        if (nearEnd) onLoadMore()
    }
}

@Composable
fun windowLabel(window: HotWindow): String = stringResource(
    when (window) {
        HotWindow.Now -> R.string.window_now
        HotWindow.Today -> R.string.window_today
        HotWindow.Week -> R.string.window_week
        HotWindow.Month -> R.string.window_month
    },
)

/** New / Hot, channel chips, and the hot window (SPEC-02 §5.1). */
@Composable
fun FeedFilters(
    query: FeedQuery,
    channels: List<Channel>,
    showChannels: Boolean,
    onChange: (FeedQuery) -> Unit,
    showSort: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    ) {
        if (showSort) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                val sorts = FeedSort.entries
                sorts.forEachIndexed { index, sort ->
                    SegmentedButton(
                        selected = query.sort == sort,
                        onClick = { onChange(query.copy(sort = sort)) },
                        shape = SegmentedButtonDefaults.itemShape(index, sorts.size),
                    ) {
                        Text(stringResource(if (sort == FeedSort.New) R.string.feed_new else R.string.feed_hot))
                    }
                }
            }
        }
        if (showChannels && channels.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = query.channelName == null,
                        onClick = { onChange(query.copy(channelName = null)) },
                        label = { Text(stringResource(R.string.channel_all)) },
                    )
                }
                items(channels, key = { it.id }) { channel ->
                    FilterChip(
                        selected = query.channelName == channel.name,
                        onClick = { onChange(query.copy(channelName = channel.name)) },
                        label = { Text(channelLabel(channel)) },
                        leadingIcon = { ChannelDot(channel) },
                    )
                }
            }
        }
        if (query.sort == FeedSort.Hot) {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                query.hotWindows.forEach { window ->
                    FilterChip(
                        selected = query.window == window,
                        onClick = { onChange(query.copy(window = window)) },
                        label = { Text(windowLabel(window)) },
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelDot(channel: Channel) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(channel.seedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary),
    )
}

@Composable
fun TopicScreen(topic: String, onBack: () -> Unit, onOpenQuestion: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { FeedViewModel(it.repository, FeedQuery(topic = topic), useCache = false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.topic_title, topic), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        QuestionList(
            state = state,
            channels = channels,
            emptyTitle = stringResource(R.string.empty_topic),
            onOpen = { onOpenQuestion(it.id) },
            onRefresh = { vm.refresh() },
            onLoadMore = { vm.loadMore() },
            onRetry = { vm.retry() },
            modifier = Modifier.padding(padding),
            header = {
                item(key = "filters") { FeedFilters(query, emptyList(), showChannels = false, onChange = { vm.setQuery(it) }) }
            },
        )
    }
}

@Composable
fun HotScreen(onBack: () -> Unit, onOpenQuestion: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { FeedViewModel(it.repository, FeedQuery(sort = FeedSort.Hot), useCache = false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.hot_title)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        QuestionList(
            state = state,
            channels = channels,
            emptyTitle = stringResource(R.string.empty_feed),
            onOpen = { onOpenQuestion(it.id) },
            onRefresh = { vm.refresh() },
            onLoadMore = { vm.loadMore() },
            onRetry = { vm.retry() },
            modifier = Modifier.padding(padding),
            header = {
                item(key = "filters") {
                    FeedFilters(query, channels, showChannels = true, onChange = { vm.setQuery(it) }, showSort = false)
                }
            },
        )
    }
}

@Composable
fun MyListScreen(kind: MyList, onBack: () -> Unit, onOpenQuestion: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { FeedViewModel(it.repository, FeedQuery(myList = kind), useCache = false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val title = stringResource(
        when (kind) {
            MyList.Asked -> R.string.my_questions
            MyList.Answered -> R.string.my_answers
            MyList.Pinned -> R.string.pinned_title
        },
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        QuestionList(
            state = state,
            channels = channels,
            emptyTitle = stringResource(R.string.empty_my_list),
            onOpen = { onOpenQuestion(it.id) },
            onRefresh = { vm.refresh() },
            onLoadMore = { vm.loadMore() },
            onRetry = { vm.retry() },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun SavedScreen(onBack: () -> Unit, onOpenQuestion: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val saved by container.saved.items.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.saved_title)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (saved.isEmpty()) {
                item(key = "empty") {
                    MessageCard(
                        stringResource(R.string.saved_empty_title),
                        stringResource(R.string.saved_empty_body),
                        null,
                        null,
                    )
                }
            }
            items(saved, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        IconButton(onClick = { container.saved.remove(item.id) }) {
                            Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = stringResource(R.string.action_unsave),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenQuestion(item.id) },
                )
            }
        }
    }
}
