package io.github.eyalshimshi.betterstips.ui.notifications

import io.github.eyalshimshi.betterstips.ui.components.FlowerColors
import io.github.eyalshimshi.betterstips.ui.components.FlowerIcon
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
import io.github.eyalshimshi.betterstips.data.SeenNotifications
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import io.github.eyalshimshi.betterstips.ui.components.QuestionIcon
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import io.github.eyalshimshi.betterstips.ui.components.SwipeAwaySnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.components.currentTime
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.util.NotificationCategories
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.account.knownCredibility
import io.github.eyalshimshi.betterstips.ui.account.ratingName
import io.github.eyalshimshi.betterstips.ui.account.riseText
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.ExpertLine
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.ServerDetailsDialog
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.util.CredRules
import io.github.eyalshimshi.betterstips.util.JumpRules
import io.github.eyalshimshi.betterstips.util.NotificationKinds
import io.github.eyalshimshi.betterstips.util.NotifyRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val items: List<StipsNotification> = emptyList(),
    val page: Int = 0,
    val end: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
    val error: ApiException? = null,
    /** New since the user last looked (kept for this visit, even after a refresh). */
    val newIds: Set<Long> = emptySet(),
    /** Opened or marked read in the app, on this account (#13): Stips' team's notices stay pinned until then. */
    val opened: Set<Long> = emptySet(),
    /** The category chip that's on (#13); null shows all. */
    val filter: NotificationCategories.Category? = null,
    /** Questions' titles and photos for their rows, when that option is on (#13). */
    val glances: Map<Long, QuestionGlance> = emptyMap(),
    /** Older pages in a row that brought none of the chosen category (v0.18): past a few, loading more waits for a tap. */
    val missesInFilter: Int = 0,
)

/** A question's title and photo, for its notifications' rows (#13, an option). */
data class QuestionGlance(val title: String, val thumbnail: String?)

/** Messages from the list's actions (#13). */
sealed interface NotificationsEvent {
    data class UpdatesStopped(val questionId: Long) : NotificationsEvent

    data object Failed : NotificationsEvent
}

class NotificationsViewModel(
    private val repository: StipsRepository,
    private val seen: SeenNotifications,
    private val userId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<NotificationsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<NotificationsEvent> = _events.asSharedFlow()
    private var generation = 0

    /** Opened during this visit: read, whatever Stips' flag says on a refresh. */
    private val openedNow = mutableSetOf<Long>()

    /** Questions whose title and photo were asked for, and the queue that reads them one at a time. */
    private val asked = mutableSetOf<Long>()
    private val glanceQueue = Channel<Long>(Channel.UNLIMITED)

    init {
        userId?.let { me -> _state.update { it.copy(opened = seen.opened(me)) } }
        refresh()
        viewModelScope.launch { readGlances() }
    }

    /** [verify]: the user pulled to refresh, so the session is checked with Stips first (#9: a lost one lists nothing). */
    fun refresh(verify: Boolean = false) {
        val gen = ++generation
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val list = repository.notifications(page = 1, verify = verify)
                if (gen != generation) return@launch
                markNew(list)
                _state.update { it.copy(items = list, page = 1, end = list.isEmpty(), refreshing = false, moreFailed = false, missesInFilter = 0) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(refreshing = false, error = e.asApiException()) }
            }
        }
    }

    /**
     * Remembers which items are new now, then records the newest one as seen. What the checks found new while the
     * bell was closed is new too (v0.17), though reading the list made Stips count it as seen. What was opened
     * already, from the bell or from a phone notification (v0.18), isn't.
     */
    private fun markNew(list: List<StipsNotification>) {
        val me = userId ?: return
        val waiting = seen.waiting(me).keys
        val opened = seen.opened(me)
        val fresh = SeenNotifications.newIds(list.map { it.id to it.viewed }, seen.lastSeenId(me)) +
            list.map { it.id }.filter { it in waiting } - openedNow - opened
        _state.update { it.copy(newIds = it.newIds + fresh - opened, opened = it.opened + opened) }
        list.maxOfOrNull { it.id }?.let { seen.markSeen(me, it) }
    }

    /**
     * The user opened this one, or marked it read (#13): it leaves "new" right away rather than on the next visit
     * (#6), and a notice from Stips' team stops being pinned, from now on.
     */
    fun markRead(id: Long) = markRead(listOf(id))

    /** As above, for a question's updates together (v0.17). */
    fun markRead(ids: Collection<Long>) {
        openedNow += ids
        userId?.let { me -> ids.forEach { seen.markOpened(me, it) } }
        _state.update { it.copy(newIds = it.newIds - ids.toSet(), opened = it.opened + ids) }
    }

    fun setFilter(category: NotificationCategories.Category?) {
        _state.update { it.copy(filter = category, missesInFilter = 0) }
    }

    /** Stops updates on a question from its notification (#13); the message offers to undo it. */
    fun stopUpdates(questionId: Long) {
        viewModelScope.launch {
            try {
                repository.setFollowing(questionId, false)
                _events.emit(NotificationsEvent.UpdatesStopped(questionId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(NotificationsEvent.Failed)
            }
        }
    }

    fun resumeUpdates(questionId: Long) {
        viewModelScope.launch {
            try {
                repository.setFollowing(questionId, true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(NotificationsEvent.Failed)
            }
        }
    }

    /** Rows on screen ask for their question's title and photo (an option, #13). Each question is read once. */
    fun wantGlance(questionId: Long) {
        if (asked.add(questionId)) glanceQueue.trySend(questionId)
    }

    /**
     * Reads the questions rows asked for: from the app's memory when it has them, else from Stips, one every
     * [GLANCE_GAP_MS] at most and only while the request budget has room for background work.
     */
    private suspend fun readGlances() {
        for (id in glanceQueue) {
            val question = repository.cachedQuestion(id) ?: run {
                while (!RateGuard.allowBackground()) delay(GLANCE_GAP_MS)
                val read = try {
                    repository.question(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                delay(GLANCE_GAP_MS)
                read
            } ?: continue
            val glance = QuestionGlance(question.title, question.cover?.thumbnailUrl)
            _state.update { it.copy(glances = it.glances + (id to glance)) }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.end || s.refreshing || s.page == 0) return
        val gen = generation
        val next = s.page + 1
        _state.update { it.copy(loadingMore = true, moreFailed = false) }
        viewModelScope.launch {
            try {
                val list = repository.notifications(page = next)
                if (gen != generation) return@launch
                _state.update { st ->
                    // A page with none of the chosen category counts toward stopping the automatic loading (v0.18).
                    val filter = st.filter
                    val any = filter == null || list.any { NotificationCategories.of(it) == filter }
                    st.copy(
                        items = (st.items + list).distinctBy { it.id },
                        page = next,
                        end = list.isEmpty(),
                        loadingMore = false,
                        missesInFilter = if (any) 0 else st.missesInFilter + 1,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(loadingMore = false, moreFailed = true) }
            }
        }
    }
}

/** At most one question read every this many ms for the richer rows (#13), as for moderator badges (v0.10). */
private const val GLANCE_GAP_MS = 1_500L

/**
 * Stips' notification bell (answers to your questions, flowers, thanks, badges). Opening the list is what marks
 * them seen on Stips, as on the site, so [onSeen] clears the badge once the first page arrives.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenQuestion: (Long, QuestionFocus?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onSignIn: () -> Unit,
    onSeen: () -> Unit,
) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.notifications_title)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SwipeAwaySnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (session.signedIn) {
            NotificationsList(Modifier.padding(padding), snackbar, onOpenQuestion, onOpenProfile, onSeen)
        } else {
            Column(Modifier.padding(padding)) {
                MessageCard(
                    stringResource(R.string.notifications_signed_out_title),
                    stringResource(R.string.notifications_signed_out_body),
                    stringResource(R.string.action_sign_in),
                    onSignIn,
                )
            }
        }
    }
}

@Composable
private fun NotificationsList(
    modifier: Modifier,
    snackbar: SnackbarHostState,
    onOpenQuestion: (Long, QuestionFocus?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onSeen: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm = appViewModel { NotificationsViewModel(it.repository, it.seenNotifications, it.session.session.value.userId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val toggle = rememberPhoneNotificationsToggle(snackbar)
    val currentOnSeen by rememberUpdatedState(onSeen)
    var details by remember { mutableStateOf<StipsNotification?>(null) }
    details?.let { item ->
        ServerDetailsDialog(
            title = stringResource(R.string.expert_details),
            fields = notificationFields(item),
            raw = listOf("notifications.list" to item.raw),
            onDismiss = { details = null },
        )
    }
    var notice by remember { mutableStateOf<StipsNotification?>(null) }
    val points = knownCredibility()
    notice?.let { item ->
        TeamNoticeDialog(
            item = item,
            points = points,
            onOpen = targetAction(item, onOpenQuestion, onOpenProfile)?.let { action ->
                {
                    notice = null
                    action()
                }
            },
            onDismiss = { notice = null },
        )
    }
    val onDetails: ((StipsNotification) -> Unit)? = if (settings.expertMode) ({ details = it }) else null
    LaunchedEffect(state.page, state.refreshing) {
        if (state.page >= 1 && !state.refreshing) {
            currentOnSeen()
            // Seen here, so they leave the phone's notification shade (v0.11).
            Notifier.clearUpdates(context)
        }
    }
    // Stopping updates from a row says so, with a way back (#13).
    val messageScope = rememberCoroutineScope()
    val stoppedText = stringResource(R.string.following_off)
    val undoText = stringResource(R.string.swipe_undo)
    val failedText = stringResource(R.string.action_failed)
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is NotificationsEvent.UpdatesStopped -> messageScope.launch {
                    val result = snackbar.showSnackbar(stoppedText, actionLabel = undoText, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) vm.resumeUpdates(event.questionId)
                }
                NotificationsEvent.Failed -> messageScope.launch { snackbar.showSnackbar(failedText) }
            }
        }
    }
    val markReadLabel = stringResource(R.string.notif_mark_read)
    val stopUpdatesLabel = stringResource(R.string.stop_updates)
    val listState = rememberLazyListState()
    val now = currentTime()
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()
    PullToRefreshBox(isRefreshing = state.refreshing && state.page > 0, onRefresh = { vm.refresh(verify = true) }, modifier = modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            if (!settings.backgroundNotifications || !Notifier.canPost(context)) {
                item(key = "phone") {
                    MessageCard(
                        stringResource(R.string.notifications_phone_title),
                        stringResource(R.string.notifications_phone_body),
                        stringResource(R.string.notifications_phone_turn_on),
                        { toggle(true) },
                    )
                }
            }
            val error = state.error
            when {
                state.page == 0 && error != null -> item(key = "error") { ErrorCard(error, onRetry = { vm.refresh() }) }
                state.page == 0 -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.items.isEmpty() -> item(key = "empty") {
                    Text(
                        stringResource(R.string.notifications_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                    )
                }
                else -> {
                    // Stips' team's notices (a removal, a report's outcome, a note) open in full here; the rest open
                    // where they point (v0.13).
                    val open = { item: StipsNotification ->
                        if (NotificationKinds.fromTeam(NotificationKinds.kind(item))) {
                            {
                                vm.markRead(item.id)
                                notice = item
                            }
                        } else {
                            targetAction(item, onOpenQuestion, onOpenProfile)?.let { action -> { vm.markRead(item.id); action() } }
                        }
                    }
                    // A long press (#13): mark as read, and stop updates on the question an update is about.
                    val rowMenu = { item: StipsNotification, pinned: Boolean ->
                        buildList {
                            if (item.id in state.newIds || pinned) add(RowAction(markReadLabel) { vm.markRead(item.id) })
                            val questionId = NotificationCategories.questionId(item)
                            if (questionId != null && NotificationCategories.of(item) == NotificationCategories.Category.Updates) {
                                add(RowAction(stopUpdatesLabel) { vm.stopUpdates(questionId) })
                            }
                        }
                    }
                    val filter = state.filter
                    val shown = if (filter == null) state.items else state.items.filter { NotificationCategories.of(it) == filter }
                    val pinned = NotificationCategories.pinned(shown, state.opened, now)
                    val pinnedIds = pinned.map { it.id }.toSet()
                    val newItems = state.items.filter { it.id in state.newIds }
                    item(key = "chips") {
                        // The chips count rows as the list shows them: a question's new updates are one (v0.17).
                        val newRows = NotificationCategories.rows(newItems)
                        CategoryChips(filter, newRows.groupingBy { NotificationCategories.of(it.newest) }.eachCount(), vm::setFilter)
                    }
                    if (pinned.isNotEmpty()) {
                        item(key = "pinned-label") { SectionLabel(stringResource(R.string.notif_pinned)) }
                        items(pinned, key = { "pin-${it.id}" }) { item ->
                            NotificationRow(
                                item,
                                isNew = item.id in state.newIds,
                                onClick = open(item),
                                onDetails = onDetails,
                                menu = rowMenu(item, true),
                                pinned = true,
                            )
                        }
                    }
                    val rest = shown.filterNot { it.id in pinnedIds }
                    // One row per question for its updates (v0.17), in the day of the newest of them.
                    val rows = NotificationCategories.rows(rest)
                    NotificationCategories.sectionsBy(rows, today, zone) { it.newest.time }.forEach { (day, list) ->
                        item(key = "day-${day.name}") { SectionLabel(stringResource(dayLabel(day))) }
                        items(list, key = { it.key }) { row ->
                            val item = row.newest
                            // The question's title and photo, read when the row shows (an option, #13).
                            val questionId = if (settings.notificationDetails) NotificationCategories.questionId(item) else null
                            if (questionId != null) LaunchedEffect(questionId) { vm.wantGlance(questionId) }
                            val glance = questionId?.let { state.glances[it] }
                            if (row.items.size == 1) {
                                NotificationRow(
                                    item,
                                    isNew = item.id in state.newIds,
                                    onClick = open(item),
                                    onDetails = onDetails,
                                    menu = rowMenu(item, false),
                                    glance = glance,
                                )
                            } else {
                                // A question's updates in one sentence with all their answers (v0.18, #27).
                                val fresh = row.items.filter { it.id in state.newIds }
                                NotificationRow(
                                    item,
                                    isNew = fresh.isNotEmpty(),
                                    onClick = {
                                        // At the first of the answers its new updates announce; with none new, as the newest.
                                        val focus = NotificationCategories.focusFor(fresh.ifEmpty { listOf(item) })
                                        vm.markRead(row.items.map { it.id })
                                        row.questionId?.let { onOpenQuestion(it, focus) }
                                    },
                                    onDetails = onDetails,
                                    menu = buildList {
                                        if (fresh.isNotEmpty()) add(RowAction(markReadLabel) { vm.markRead(fresh.map { it.id }) })
                                        row.questionId?.let { question -> add(RowAction(stopUpdatesLabel) { vm.stopUpdates(question) }) }
                                    },
                                    glance = glance,
                                    text = NotificationCategories.combinedText(row.items),
                                )
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        item(key = "empty-category") {
                            Text(
                                stringResource(R.string.notif_empty_category),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 24.dp),
                            )
                        }
                    }
                    // Older pages load by themselves before the end comes into view (v0.18, #27), in a category too. A
                    // rare kind could otherwise read every page at once: after a few pages with none of it, a tap asks.
                    if (!state.end) {
                        item(key = "more") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    state.moreFailed -> TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.action_retry)) }
                                    state.missesInFilter >= AUTO_PAGES_WITHOUT_ANY && !state.loadingMore ->
                                        TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.notif_load_older)) }
                                    else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // The next page is asked for while about a screen of rows is still below (v0.18, #27), as in chats.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - PREFETCH_ROWS
        }
    }
    LaunchedEffect(nearEnd, state.page, state.end, state.filter, state.missesInFilter) {
        if (nearEnd && !state.end && state.page > 0 && state.missesInFilter < AUTO_PAGES_WITHOUT_ANY) vm.loadMore()
    }
}

/** How many rows before the end of the list the next page is asked for (about a screen). */
private const val PREFETCH_ROWS = 8

/** In a category, older pages stop loading by themselves after this many in a row brought none of it. */
private const val AUTO_PAGES_WITHOUT_ANY = 4

/** The category chips on top (#13), each with how many of its notifications are new. Tapping the one that's on turns it off. */
@Composable
private fun CategoryChips(
    selected: NotificationCategories.Category?,
    newCounts: Map<NotificationCategories.Category, Int>,
    onSelect: (NotificationCategories.Category?) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.notif_filter_all)) })
        NotificationCategories.Category.entries.forEach { category ->
            val count = newCounts[category] ?: 0
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(stringResource(categoryLabel(category))) },
                trailingIcon = if (count > 0) {
                    { Badge { Text(if (count > 99) "99+" else count.toString()) } }
                } else {
                    null
                },
            )
        }
    }
}

private fun categoryLabel(category: NotificationCategories.Category): Int = when (category) {
    NotificationCategories.Category.Updates -> R.string.notif_category_updates
    NotificationCategories.Category.FlowersAndThanks -> R.string.notif_category_flowers
    NotificationCategories.Category.System -> R.string.notif_category_system
}

private fun dayLabel(day: NotificationCategories.Day): Int = when (day) {
    NotificationCategories.Day.Today -> R.string.notif_today
    NotificationCategories.Day.Yesterday -> R.string.notif_yesterday
    NotificationCategories.Day.ThisWeek -> R.string.notif_this_week
    NotificationCategories.Day.Earlier -> R.string.notifications_earlier
}

private fun targetAction(item: StipsNotification, onOpenQuestion: (Long, QuestionFocus?) -> Unit, onOpenProfile: (Long) -> Unit): (() -> Unit)? =
    when (val target = item.target) {
        // New answers open at the first new one, a flower at the answer that got it (v0.10); the team's notices at
        // the top (v0.13).
        is NotificationTarget.OpenQuestion -> ({ onOpenQuestion(target.id, JumpRules.focusFor(item)) })
        is NotificationTarget.OpenProfile -> ({ onOpenProfile(target.id) })
        null -> null
    }

/** Expert mode's details of a notification: what Stips sent, and where the app opens it (v0.10). */
private fun notificationFields(item: StipsNotification): List<Pair<String, String>> {
    val focus = (item.target as? NotificationTarget.OpenQuestion)?.let { JumpRules.focusFor(item) }
    return listOf(
        "id" to item.id.toString(),
        "type" to item.type.ifBlank { "-" },
        "kind" to NotificationKinds.kind(item).name,
        "target" to when (val target = item.target) {
            is NotificationTarget.OpenQuestion -> "ask ${target.id}"
            is NotificationTarget.OpenProfile -> "profile ${target.id}"
            null -> "-"
        },
        "viewed" to item.viewed.toString(),
        "time" to (item.time?.toString() ?: "-"),
        "opens at" to when (focus) {
            null -> "-"
            QuestionFocus.MyFlower -> "your answer with a flower"
            is QuestionFocus.NewAnswers -> "new answers" + (focus.count?.let { " ($it)" } ?: "")
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** A long-press action on a notification (#13). */
private data class RowAction(val label: String, val onClick: () -> Unit)

/**
 * One notification, the same layout for all, two lines at most, new ones marked with a dot (#4). Stips' team's
 * notices say so, and show the team's words in a box under the text, as the site does, and the credibility points
 * taken (v0.13). [pinned]: a card on top until it's opened (#13). A long press offers [menu] (#13); with the option on,
 * [glance] adds the question's title and photo (#13). [text]: a question's updates together say it in one sentence
 * (v0.18). A flower stands out in Stips' own colors (v0.18, #27).
 */
@Composable
private fun NotificationRow(
    item: StipsNotification,
    isNew: Boolean,
    onClick: (() -> Unit)?,
    onDetails: ((StipsNotification) -> Unit)?,
    menu: List<RowAction> = emptyList(),
    pinned: Boolean = false,
    glance: QuestionGlance? = null,
    text: String = item.text,
) {
    val kind = remember(item) { NotificationKinds.kind(item) }
    val team = NotificationKinds.fromTeam(kind)
    val flower = kind == NotificationKinds.Kind.Flower
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val background = when {
        pinned && kind == NotificationKinds.Kind.Removal -> colors.errorContainer.copy(alpha = 0.55f)
        pinned -> colors.tertiaryContainer.copy(alpha = 0.6f)
        isNew -> colors.primaryContainer.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val press = if (onClick != null || menu.isNotEmpty()) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = if (menu.isNotEmpty()) {
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                }
            } else {
                null
            },
        )
    } else {
        Modifier
    }
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (pinned) {
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(background)
                .then(press)
                .heightIn(min = 72.dp)
                .padding(horizontal = if (pinned) 12.dp else 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            // A flower stands out in Stips' own colors, new or not (v0.18, #27).
                            flower -> if (isNew) FlowerColors.Petal.copy(alpha = 0.28f) else FlowerColors.Tint
                            isNew -> colors.primary
                            kind == NotificationKinds.Kind.Removal -> colors.errorContainer
                            else -> colors.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (flower) {
                    FlowerIcon(size = 24.dp)
                } else {
                    Icon(
                        notificationIcon(kind),
                        contentDescription = null,
                        tint = when {
                            isNew -> colors.onPrimary
                            kind == NotificationKinds.Kind.Removal -> colors.onErrorContainer
                            else -> colors.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (team) {
                    Text(
                        stringResource(teamLabel(kind)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (kind == NotificationKinds.Kind.Removal) colors.error else colors.primary,
                        maxLines = 1,
                    )
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = if (team) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // The question's title, when its sentence doesn't already say it (an option, #13).
                val title = glance?.title?.takeIf { it.isNotBlank() && !text.contains(it) }
                if (title != null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val note = item.note?.takeIf { it.isNotBlank() }
                if (note != null && team) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                } else if (note != null) {
                    Text(
                        note,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.pointsRemoved > 0) {
                    Text(
                        stringResource(R.string.notifications_points_removed, item.pointsRemoved),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.error,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (onDetails != null) ExpertLine("${item.type.ifBlank { "-" }} · ${kind.name} · #${item.id}")
            }
            val thumbnail = glance?.thumbnail
            if (thumbnail != null && LocalAppSettings.current.showImages) {
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    imageLoader = LocalAppContainer.current.imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            if (onDetails != null) {
                IconButton(onClick = { onDetails(item) }) {
                    Icon(Icons.Outlined.Code, contentDescription = stringResource(R.string.expert_details), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    notificationTime(item.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNew) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                if (isNew) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            menu.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        menuOpen = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
/** The time for today and yesterday (the section says which day, #13), or the date. */
@Composable
private fun notificationTime(time: Instant?): String {
    if (time == null) return ""
    val zone = ZoneId.systemDefault()
    val day = time.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        // The section's heading says the day (#13), so today and yesterday show the time.
        today, today.minusDays(1) -> time.atZone(zone).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        else -> day.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
    }
}

/** Each kind of notification has its icon; Stips' team's notices have their own (v0.13). */
private fun notificationIcon(kind: NotificationKinds.Kind): ImageVector = when (kind) {
    NotificationKinds.Kind.Flower -> Icons.Outlined.LocalFlorist
    NotificationKinds.Kind.Thanks -> Icons.Outlined.VolunteerActivism
    NotificationKinds.Kind.Answers -> Icons.Outlined.QuestionAnswer
    NotificationKinds.Kind.Question -> QuestionIcon
    NotificationKinds.Kind.Message -> Icons.AutoMirrored.Outlined.Chat
    NotificationKinds.Kind.Badge -> Icons.Outlined.WorkspacePremium
    NotificationKinds.Kind.Removal -> Icons.Outlined.Gavel
    NotificationKinds.Kind.Report -> Icons.Outlined.Flag
    NotificationKinds.Kind.TeamNote -> Icons.Outlined.Shield
    NotificationKinds.Kind.Other -> Icons.Outlined.Notifications
}

/** Who a team notice is from, and what about. */
private fun teamLabel(kind: NotificationKinds.Kind): Int = when (kind) {
    NotificationKinds.Kind.Removal -> R.string.notif_kind_removal
    NotificationKinds.Kind.Report -> R.string.notif_kind_report
    else -> R.string.notif_kind_note
}

/**
 * A notice from Stips' team in full (v0.13): what happened, the team's reason, the credibility points taken and where
 * that leaves the user, and for a removal, the way to appeal ("צור קשר", or a senior moderator).
 */
@Composable
internal fun TeamNoticeDialog(item: StipsNotification, points: Int?, onOpen: (() -> Unit)?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val kind = NotificationKinds.kind(item)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(notificationIcon(kind), contentDescription = null) },
        title = { Text(stringResource(teamLabel(kind))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.text, style = MaterialTheme.typography.bodyLarge)
                val note = item.note?.takeIf { it.isNotBlank() && it != item.text }
                if (note != null) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.notif_reason), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (item.pointsRemoved > 0) {
                    Text(
                        stringResource(R.string.notifications_points_removed, item.pointsRemoved),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (points != null && kind != NotificationKinds.Kind.Report) {
                    val rating = CredRules.rating(points)
                    Text(
                        stringResource(R.string.notif_points_now, points, ratingName(rating)) + " " + riseText(rating) + ".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (kind == NotificationKinds.Kind.Removal) {
                    Text(
                        stringResource(R.string.notif_appeal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (onOpen != null) {
                TextButton(onClick = onOpen) { Text(stringResource(R.string.action_open)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
        dismissButton = {
            Row {
                if (kind == NotificationKinds.Kind.Removal) {
                    TextButton(onClick = { openInBrowser(context, "$SITE_URL/contact-us") }) { Text(stringResource(R.string.settings_contact)) }
                }
                if (onOpen != null) TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
    )
}

/**
 * Turns phone notifications on (asking for Android's permission first, on Android 13 and later) or off. When the
 * permission is refused, [snackbar] explains and offers Android's notification settings. Once they're on, Android's
 * own question about running in the background follows, unless it already allows that (v0.11).
 */
@Composable
fun rememberPhoneNotificationsToggle(snackbar: SnackbarHostState): (Boolean) -> Unit {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.notifications_permission_denied)
    val settingsText = stringResource(R.string.action_open_settings)
    val refusedText = stringResource(R.string.notifications_job_refused)
    fun setEnabled(on: Boolean) {
        // Schedule first: only a job Android accepted counts as "on" (#1, #2).
        val minutes = container.settings.settings.value.checkMinutes
        val scheduled = Notifier.schedule(context.applicationContext, on, minutes)
        container.settings.update { it.copy(backgroundNotifications = on && scheduled) }
        if (on && !scheduled) scope.launch { snackbar.showSnackbar(refusedText) }
        // While Android saves battery on Stips+, it holds the checks back for hours.
        if (on && scheduled && !Notifier.runsInBackground(context)) Notifier.askToRunInBackground(context)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setEnabled(true)
        } else {
            scope.launch {
                val result = snackbar.showSnackbar(deniedText, actionLabel = settingsText, withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) Notifier.openNotificationSettings(context)
            }
        }
    }
    return { on ->
        if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setEnabled(on)
        }
    }
}

/** Without a check for this long, Android is holding Stips+ back (the job's period is 15 minutes). */
private const val LATE_MS = 60 * 60_000L

/**
 * How phone notifications are doing (Settings, v0.11): the last check; what in Android holds them back (notifications
 * turned off, restricted background use, battery saving), each with a way to fix it; "Check now" and a test.
 */
@Composable
fun PhoneNotificationsStatus(snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val status by remember { Notifier.status(context.applicationContext) }.collectAsStateWithLifecycle()
    // Android's settings change while the user is away (its battery question, app info): read them again on return.
    var returns by remember { mutableIntStateOf(0) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { returns++ }
    }
    val background = remember(returns) { Notifier.runsInBackground(context) }
    val restricted = remember(returns) { Notifier.backgroundRestricted(context) }
    val blocked = remember(returns) { Notifier.blockedInAndroid(context) }
    val lastRun = remember(returns, status) { Notifier.lastRunAt(context) }
    var checking by remember { mutableStateOf(false) }
    val resources = context.resources
    val nothingText = stringResource(R.string.notify_checked_nothing)
    val failedText = stringResource(R.string.notify_checked_failed)
    val testText = stringResource(R.string.notify_test_sent)
    val deniedText = stringResource(R.string.notifications_permission_denied)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(statusLine(status), style = MaterialTheme.typography.bodyMedium)
        if (blocked) {
            StatusWarning(Icons.Outlined.NotificationsOff, null, stringResource(R.string.notify_blocked), stringResource(R.string.action_open_settings)) {
                Notifier.openNotificationSettings(context)
            }
        }
        if (restricted) {
            StatusWarning(Icons.Outlined.Block, null, stringResource(R.string.notify_restricted), stringResource(R.string.action_open_settings)) {
                Notifier.openAppSettings(context)
            }
        }
        if (!background) {
            StatusWarning(
                Icons.Outlined.BatteryAlert,
                stringResource(R.string.notify_battery_title),
                stringResource(R.string.notify_battery_body),
                stringResource(R.string.notify_battery_allow),
            ) { Notifier.askToRunInBackground(context) }
        } else if (lastRun > 0 && System.currentTimeMillis() - lastRun > LATE_MS) {
            StatusWarning(
                Icons.Outlined.Schedule,
                null,
                stringResource(R.string.notify_status_late, checkTime(lastRun)),
                stringResource(R.string.action_open_settings),
            ) { Notifier.openAppSettings(context) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                enabled = !checking,
                onClick = {
                    checking = true
                    scope.launch {
                        val result = Notifier.run(context.applicationContext, container, Notifier.Trigger.User)
                        checking = false
                        val message = when (result?.outcome) {
                            NotifyRules.Outcome.Posted -> {
                                val count = result?.posted ?: 0
                                resources.getQuantityString(R.plurals.notify_checked_posted, count, count)
                            }
                            NotifyRules.Outcome.Nothing -> nothingText
                            else -> failedText
                        }
                        snackbar.showSnackbar(message)
                    }
                },
            ) { Text(stringResource(R.string.notify_check_now)) }
            TextButton(
                onClick = {
                    val sent = Notifier.postTest(context)
                    scope.launch { snackbar.showSnackbar(if (sent) testText else deniedText) }
                },
            ) { Text(stringResource(R.string.notify_send_test)) }
        }
        if (settings.expertMode) {
            val bucket = remember(returns) { Notifier.standbyBucket(context) ?: "?" }
            val ran = if (lastRun > 0) checkTime(lastRun) else "never"
            ExpertLine("standby: $bucket · last run: $ran · every ${settings.checkMinutes} min · background: $background")
        }
    }
}

@Composable
private fun statusLine(status: NotifyRules.Status?): String {
    if (status == null) return stringResource(R.string.notify_status_never)
    val time = checkTime(status.at)
    return when (status.outcome) {
        NotifyRules.Outcome.Posted ->
            LocalContext.current.resources.getQuantityString(R.plurals.notify_status_posted, status.posted, time, status.posted)
        NotifyRules.Outcome.Nothing -> stringResource(R.string.notify_status_nothing, time)
        NotifyRules.Outcome.Failed -> stringResource(R.string.notify_status_failed, time, status.error ?: "?")
    }
}

/** "14:32" today, or a short date and time before that. */
private fun checkTime(at: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(at).atZone(zone)
    val format = if (time.toLocalDate() == LocalDate.now(zone)) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
    return time.format(format)
}

@Composable
private fun StatusWarning(icon: ImageVector, title: String?, text: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/**
 * Once, for a signed-in user (v0.11): turn phone notifications on, or, when they're on but Android saves battery on
 * Stips+, let it run in the background. Either answer ends it; Settings has both.
 */
@Composable
fun NotificationsPrompt(active: Boolean, snackbar: SnackbarHostState) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    // Before any early return: the permission's answer comes back to this launcher.
    val toggle = rememberPhoneNotificationsToggle(snackbar)
    if (!active || !session.signedIn || settings.notificationsPrompted) return
    val on = settings.backgroundNotifications && Notifier.canPost(context)
    val background = remember { Notifier.runsInBackground(context) }
    val done = { container.settings.update { it.copy(notificationsPrompted = true) } }
    if (on && background) {
        LaunchedEffect(Unit) { done() }
        return
    }
    AlertDialog(
        onDismissRequest = done,
        icon = { Icon(if (on) Icons.Outlined.BatteryAlert else Icons.Outlined.NotificationsActive, contentDescription = null) },
        title = { Text(stringResource(if (on) R.string.notify_prompt_battery_title else R.string.notify_prompt_title)) },
        text = { Text(stringResource(if (on) R.string.notify_prompt_battery_body else R.string.notify_prompt_body)) },
        confirmButton = {
            TextButton(
                onClick = {
                    done()
                    if (on) Notifier.askToRunInBackground(context) else toggle(true)
                },
            ) { Text(stringResource(if (on) R.string.notify_battery_allow else R.string.notifications_phone_turn_on)) }
        },
        dismissButton = { TextButton(onClick = done) { Text(stringResource(R.string.notify_prompt_later)) } },
    )
}
