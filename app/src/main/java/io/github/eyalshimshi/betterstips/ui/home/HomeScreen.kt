package io.github.eyalshimshi.betterstips.ui.home

import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.AnonymousModeDialog
import io.github.eyalshimshi.betterstips.ui.components.AnonymousModeButton
import io.github.eyalshimshi.betterstips.data.model.Author
import androidx.compose.runtime.remember
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
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
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.HomeLayout
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.HotWindow
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.util.AutoRefresh
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.LocalUnreadCounts
import io.github.eyalshimshi.betterstips.ui.components.QuestionMetaRow
import io.github.eyalshimshi.betterstips.ui.components.ScrollToTopOnReselect
import io.github.eyalshimshi.betterstips.ui.components.RefreshOnEnter
import io.github.eyalshimshi.betterstips.ui.Routes
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

data class HotStripState(
    val items: List<Question> = emptyList(),
    val loading: Boolean = true,
    val error: ApiException? = null,
    /** Counts the lists that arrived; each new one brings the row back to the first card (#12). */
    val version: Int = 0,
)

/** The hottest questions shown as big cards on Home; the screen shows as many as the reader chose (#8). */
class HotStripViewModel(private val repository: StipsRepository) : ViewModel() {
    private val _state = MutableStateFlow(HotStripState())
    val state: StateFlow<HotStripState> = _state.asStateFlow()
    private var loadedFor: FeedQuery? = null
    private var job: Job? = null
    private var loadedAt = 0L

    /**
     * [quiet]: coming back to Home (v0.16) leaves the row where it was unless the hot list changed; a refresh by hand
     * brings it back to the first card (#12).
     */
    fun load(channelName: String?, window: HotWindow, force: Boolean = false, quiet: Boolean = false) {
        val query = FeedQuery(sort = FeedSort.Hot, window = window, channelName = channelName).normalized()
        if (!force && loadedFor == query) return
        loadedFor = query
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val list = repository.feedPage(query, 1)
                loadedAt = System.currentTimeMillis()
                _state.update {
                    val same = quiet && it.items.map(Question::id) == list.map(Question::id)
                    HotStripState(items = list, loading = false, version = if (same) it.version else it.version + 1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.asApiException()) }
            }
        }
    }

    /** Coming back to Home (v0.16): the hot list again, unless it's loading or came in moments ago. */
    fun refreshIfStale(channelName: String?, window: HotWindow) {
        if (_state.value.loading || !AutoRefresh.due(loadedAt, System.currentTimeMillis())) return
        load(channelName, window, force = true, quiet = true)
    }
}

@Composable
fun HomeScreen(
    onOpenQuestion: (Long) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenHot: () -> Unit,
    onAsk: (Int?) -> Unit,
    onOpenProfile: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    // The New list waits until it's shown, so the website-like layout doesn't spend a request on it (#8).
    val feedVm = appViewModel { FeedViewModel(it.repository, FeedQuery(), useCache = true, autoLoad = false) }
    val hotVm = appViewModel { HotStripViewModel(it.repository) }
    val sectionsVm = appViewModel { HomeSectionsViewModel(it.repository, it.session) }
    val state by feedVm.state.collectAsStateWithLifecycle()
    val query by feedVm.query.collectAsStateWithLifecycle()
    val hot by hotVm.state.collectAsStateWithLifecycle()
    val sections by sectionsVm.state.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Home's layout (#8). Picking a channel in the website-like layout shows that channel's hot and new questions.
    val layout = settings.homeLayout
    val showsSections = layout == HomeLayout.LikeWebsite && query.channelName == null
    val showsHot = layout != HomeLayout.NewOnly
    val showsNew = !showsSections
    fun loadsNew(next: FeedQuery) = !(layout == HomeLayout.LikeWebsite && next.channelName == null)

    // Tapping Home again scrolls back to the top (v0.12) and reloads it, like entering it again (v0.13, #10).
    ScrollToTopOnReselect(Routes.HOME, listState) {
        if (showsSections) sectionsVm.refresh() else feedVm.refresh()
        if (showsHot) hotVm.load(query.channelName, settings.hotWindow, force = true)
    }

    // Entering Home from the bottom bar, or coming back to the app on it, brings it up to date (v0.16): the hot
    // questions again, and new questions on top of the list as Live adds them, so what you were reading stays put.
    RefreshOnEnter(Routes.HOME) {
        if (showsHot) hotVm.refreshIfStale(query.channelName, settings.hotWindow)
        if (showsSections) {
            // The sections start over on a refresh, so only from the top.
            if (listState.firstVisibleItemIndex <= 1) sectionsVm.refreshIfStale()
        } else if (feedVm.isStale()) {
            val atTop = listState.firstVisibleItemIndex <= 3
            if (feedVm.pollNew() > 0 && atTop) {
                if (settings.motion.scrolls) listState.animateScrollToItem(0) else listState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(showsNew) { if (showsNew) feedVm.ensureLoaded() }
    LaunchedEffect(showsSections) { if (showsSections) sectionsVm.ensureLoaded() }
    LaunchedEffect(query.channelName, showsHot, settings.hotWindow) {
        if (showsHot) hotVm.load(query.channelName, settings.hotWindow)
    }
    // With the chips hidden there's no way to leave a channel, so hiding them goes back to all of Stips.
    LaunchedEffect(settings.showChannelChips) {
        if (!settings.showChannelChips && query.channelName != null) {
            val all = query.copy(channelName = null)
            feedVm.setQuery(all, loadNow = loadsNew(all))
        }
    }
    var liveSheet by rememberSaveable { mutableStateOf(false) }
    var anonymousDialog by rememberSaveable { mutableStateOf(false) }

    // Live: while Home is on screen, new questions slide in on top at the chosen rate (5–30 s). Failures slow it
    // down, and it skips its turn when the request budget is low or Stips paused the phone (#3, #4, #5).
    LaunchedEffect(settings.autoRefreshNew, settings.liveIntervalSeconds, query, showsNew) {
        if (!settings.autoRefreshNew || !showsNew) return@LaunchedEffect
        val seconds = settings.liveIntervalSeconds
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var misses = 0
            while (true) {
                delay(AppSettings.liveDelayMs(seconds, misses))
                // Live gives way to what the user does, and stops during a pause (#5).
                if (!RateGuard.allowBackground()) continue
                val atTop = listState.firstVisibleItemIndex <= 3
                val added = feedVm.pollNew()
                misses = if (added < 0) misses + 1 else 0
                if (added > 0 && atTop) listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            SearchBarRow(
                onSearch = onOpenTopic,
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = onOpenProfile,
                anonymousUntil = settings.anonymousUntil,
                onAnonymousMode = { anonymousDialog = true },
            )
        },
        floatingActionButton = {
            val askInChannel = channels.firstOrNull { it.name == query.channelName }?.id
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.ask_fab)) },
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { onAsk(askInChannel) },
                expanded = listState.firstVisibleItemIndex == 0,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val header: LazyListScope.() -> Unit = {
            if (settings.showChannelChips) {
                item(key = "channels") {
                    FeedFilters(
                        query,
                        channels,
                        showChannels = true,
                        showSort = false,
                        onChange = {
                            feedVm.setQuery(it, loadNow = loadsNew(it))
                            scope.launch { listState.scrollToItem(0) }
                        },
                    )
                }
            }
            if (showsHot) {
                item(key = "hot") {
                    HotSection(
                        state = hot,
                        count = settings.hotCount,
                        window = FeedQuery(sort = FeedSort.Hot, window = settings.hotWindow, channelName = query.channelName).normalized().window,
                        channels = channels,
                        onOpen = { onOpenQuestion(it.id) },
                        onSeeAll = onOpenHot,
                        onRetry = { hotVm.load(query.channelName, settings.hotWindow, force = true) },
                    )
                }
            }
            if (showsNew) {
                item(key = "new-header") {
                    NewHeader(
                        live = settings.autoRefreshNew,
                        seconds = settings.liveIntervalSeconds,
                        onToggleLive = { container.settings.update { s -> s.copy(autoRefreshNew = !s.autoRefreshNew) } },
                        onLiveOptions = { liveSheet = true },
                    )
                }
            }
        }
        if (showsSections) {
            HomeSectionsList(
                state = sections,
                channels = channels,
                onOpen = { onOpenQuestion(it.id) },
                onOpenTopic = onOpenTopic,
                onRefresh = {
                    sectionsVm.refresh()
                    if (showsHot) hotVm.load(query.channelName, settings.hotWindow, force = true)
                },
                onLoadMore = { sectionsVm.loadMore() },
                onRetry = { sectionsVm.retry() },
                listState = listState,
                modifier = Modifier.padding(padding),
                header = header,
            )
        } else {
            QuestionList(
                state = state,
                channels = channels,
                emptyTitle = stringResource(R.string.empty_feed),
                onOpen = { onOpenQuestion(it.id) },
                onRefresh = {
                    feedVm.refresh()
                    if (showsHot) hotVm.load(query.channelName, settings.hotWindow, force = true)
                },
                onLoadMore = { feedVm.loadMore() },
                onRetry = { feedVm.retry() },
                modifier = Modifier.padding(padding),
                listState = listState,
                header = header,
            )
        }
    }
    if (liveSheet) {
        LiveSheet(settings = settings, onChange = { container.settings.update(it) }, onDismiss = { liveSheet = false })
    }
    if (anonymousDialog) {
        AnonymousModeDialog(
            until = settings.anonymousUntil,
            onSet = { value ->
                container.settings.update { it.copy(anonymousUntil = value) }
                anonymousDialog = false
            },
            onDismiss = { anonymousDialog = false },
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
    count: Int,
    window: HotWindow,
    channels: List<Channel>,
    onOpen: (Question) -> Unit,
    onSeeAll: () -> Unit,
    onRetry: () -> Unit,
) {
    val shown = state.items.take(count)
    val title = stringResource(
        when (window) {
            HotWindow.Now -> R.string.hot_now
            HotWindow.Today -> R.string.hot_today
            HotWindow.Week -> R.string.hot_week
            HotWindow.Month -> R.string.hot_month
        },
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title, Modifier.weight(1f))
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
                // A refresh brings the row back to the hottest question (#12).
                val animate = LocalAppSettings.current.motion.scrolls
                LaunchedEffect(state.version) {
                    if (rowState.firstVisibleItemIndex == 0 && rowState.firstVisibleItemScrollOffset == 0) return@LaunchedEffect
                    if (animate) rowState.animateScrollToItem(0) else rowState.scrollToItem(0)
                }
                LazyRow(
                    state = rowState,
                    flingBehavior = rememberSnapFlingBehavior(rowState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(shown, key = { _, question -> question.id }) { index, question ->
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
                Spacer(Modifier.height(6.dp))
                QuestionMetaRow(question, color = textColor.copy(alpha = 0.9f))
            }
        }
    }
}

/** "New questions" and the Live chip: tap turns Live on or off, long-press sets how often it checks. */
@Composable
private fun NewHeader(live: Boolean, seconds: Int, onToggleLive: () -> Unit, onLiveOptions: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(stringResource(R.string.new_questions), Modifier.weight(1f))
        LiveChip(live = live, seconds = seconds, onToggle = onToggleLive, onOptions = onLiveOptions)
    }
}

/**
 * The pill at the top of Home. Enter opens a topic (Stips has no full-text search, D-15); people whose nickname
 * matches show up below as you type (the site's `users/Search`). Then anonymous mode and the notifications bell.
 */
@Composable
private fun SearchBarRow(
    onSearch: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    anonymousUntil: Long,
    onAnonymousMode: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val unread = LocalUnreadCounts.current.notifications
    val repository = LocalAppContainer.current.repository
    var people by remember { mutableStateOf<List<Author>>(emptyList()) }
    LaunchedEffect(text) {
        val query = text.trim()
        if (query.length < 2 || query.startsWith("#")) {
            people = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        people = try {
            repository.searchUsers(query).take(5)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (text.isNotEmpty()) {
                    {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                } else {
                    null
                },
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
            AnonymousModeButton(until = anonymousUntil, onClick = onAnonymousMode)
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
        if (people.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = container),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.search_people),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                people.forEach { person ->
                    val userId = person.userId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = userId != null) {
                                keyboard?.hide()
                                if (userId != null) onOpenProfile(userId)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(person, 32.dp, clickable = false)
                        Spacer(Modifier.width(12.dp))
                        Text(person.nickname, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
