package io.github.eyalshimshi.betterstips.ui.user

import io.github.eyalshimshi.betterstips.ui.components.ImageViewerDialog
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.ChartAction
import io.github.eyalshimshi.betterstips.data.model.ChartPoint
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.ThanksMessage
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.ui.account.ratingName
import io.github.eyalshimshi.betterstips.ui.account.riseText
import io.github.eyalshimshi.betterstips.ui.components.AuthorName
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.ExpertFields
import io.github.eyalshimshi.betterstips.ui.components.ExpertLine
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.ScrollToTopOnReselect
import io.github.eyalshimshi.betterstips.ui.Routes
import io.github.eyalshimshi.betterstips.ui.components.ServerDetailsDialog
import io.github.eyalshimshi.betterstips.ui.components.SpecialBadgePill
import io.github.eyalshimshi.betterstips.ui.components.VerifiedMark
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.memberForText
import io.github.eyalshimshi.betterstips.ui.components.onlineColor
import io.github.eyalshimshi.betterstips.ui.components.shareText
import io.github.eyalshimshi.betterstips.ui.components.specialNameColor
import io.github.eyalshimshi.betterstips.ui.profile.GroupLabel
import io.github.eyalshimshi.betterstips.ui.profile.GroupRow
import io.github.eyalshimshi.betterstips.ui.theme.ColorMath
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.LocalIsDark
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import io.github.eyalshimshi.betterstips.util.BioPromo
import io.github.eyalshimshi.betterstips.util.CredRules
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BIO_MAX = BioPromo.BIO_MAX

/**
 * A social-style profile (SPEC-02 §7): photo, counts, bio, the credibility gauge, activity graphs and the thanks
 * wall. The same screen serves the signed-in user (Profile tab) and everyone else; owner-only actions appear only
 * when the profile is yours.
 */
@Composable
fun UserProfileScreen(
    userId: Long,
    onBack: (() -> Unit)?,
    onOpenQuestion: (Long) -> Unit,
    onOpenConversation: (Long, String) -> Unit,
    onOpenMyList: (MyList) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm = appViewModel(key = "user-$userId") { UserProfileViewModel(it.repository, it.appContext, userId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val saved by container.saved.items.collectAsStateWithLifecycle()
    val isMe = session.signedIn && session.userId == userId
    val profile = state.profile
    val listState = rememberLazyListState()
    // As the Profile tab (no back button), tapping the tab again scrolls back to the top (v0.12) and reloads (v0.13, #10).
    if (onBack == null) ScrollToTopOnReselect(Routes.PROFILE, listState, onRefresh = vm::load)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val titleVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var editingBio by rememberSaveable { mutableStateOf(false) }
    var promoOpen by rememberSaveable { mutableStateOf(false) }
    var photoOpen by rememberSaveable { mutableStateOf(false) }
    val showImages = LocalAppSettings.current.showImages
    val expert = LocalAppSettings.current.expertMode
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    var credibilityOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ThanksMessage?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.uploadPhoto(uri)
    }

    val texts = mapOf(
        ProfileMessage.BioSaved to stringResource(R.string.profile_bio_saved),
        ProfileMessage.PhotoUpdated to stringResource(R.string.profile_photo_updated),
        ProfileMessage.ThanksDeleted to stringResource(R.string.thanks_deleted),
        ProfileMessage.ThanksSent to stringResource(R.string.thanks_sent),
        ProfileMessage.Failed to stringResource(R.string.action_failed),
    )
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(texts[message].orEmpty())
        vm.consumeMessage()
    }

    // header, credibility, (my lists), tabs
    val tabsIndex = if (isMe) 3 else 2
    fun showChart(action: ChartAction) {
        vm.selectSection(ProfileSection.Activity)
        vm.selectChart(action)
        scope.launch { listState.animateScrollToItem(tabsIndex) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = titleVisible, enter = fadeIn(), exit = fadeOut()) {
                        Text(profile?.nickname.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (onBack != null) BackButton(onBack)
                },
                actions = {
                    IconButton(onClick = { shareText(context, "$SITE_URL/profile/$userId") }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                    }
                    if (expert && profile != null) {
                        IconButton(onClick = { detailsOpen = true }) {
                            Icon(Icons.Outlined.Code, contentDescription = stringResource(R.string.expert_details))
                        }
                    }
                    if (isMe) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading && profile != null,
            onRefresh = vm::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                if (profile == null) {
                    item(key = "status") {
                        val error = state.error
                        if (error != null && !state.loading) {
                            ErrorCard(error, onRetry = vm::load)
                        } else {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(64.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }
                } else {
                    item(key = "header") {
                        val onMessage: (() -> Unit)? =
                            if (!isMe && session.signedIn) ({ onOpenConversation(userId, profile.nickname) }) else null
                        ProfileHeader(
                            profile = profile,
                            page = state.page,
                            isMe = isMe,
                            busy = state.busy,
                            expert = expert,
                            onChangePhoto = {
                                pickPhoto.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onEditBio = { editingBio = true },
                            onMessage = onMessage,
                            onStat = ::showChart,
                            onOpenPhoto = if (profile.avatar.fullPhotoUrl() != null && showImages) ({ photoOpen = true }) else null,
                            onPromo = if (isMe && state.page != null) ({ promoOpen = true }) else null,
                        )
                    }
                    item(key = "credibility") { CredibilityCard(profile.points, onClick = { credibilityOpen = true }) }
                    if (isMe) {
                        item(key = "mine") {
                            Column {
                                GroupLabel(stringResource(R.string.profile_my_stuff))
                                val count = if (saved.isEmpty()) 3 else 4
                                GroupRow(stringResource(R.string.pinned_title), 0, count, { onOpenMyList(MyList.Pinned) }, icon = Icons.Outlined.PushPin)
                                GroupRow(
                                    stringResource(R.string.my_questions),
                                    1,
                                    count,
                                    { onOpenMyList(MyList.Asked) },
                                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                )
                                GroupRow(
                                    stringResource(R.string.my_answers),
                                    2,
                                    count,
                                    { onOpenMyList(MyList.Answered) },
                                    icon = Icons.Outlined.QuestionAnswer,
                                )
                                if (saved.isNotEmpty()) {
                                    GroupRow(
                                        stringResource(R.string.saved_title),
                                        3,
                                        count,
                                        onOpenSaved,
                                        summary = saved.size.toString(),
                                        icon = Icons.Outlined.Bookmarks,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    stickyHeader(key = "tabs") {
                        PrimaryTabRow(selectedTabIndex = state.section.ordinal) {
                            Tab(
                                selected = state.section == ProfileSection.Thanks,
                                onClick = { vm.selectSection(ProfileSection.Thanks) },
                                text = { Text(stringResource(R.string.profile_tab_thanks)) },
                            )
                            Tab(
                                selected = state.section == ProfileSection.Activity,
                                onClick = { vm.selectSection(ProfileSection.Activity) },
                                text = { Text(stringResource(R.string.profile_tab_activity)) },
                            )
                        }
                    }
                    when (state.section) {
                        ProfileSection.Activity -> {
                            item(key = "chart") {
                                ActivitySection(
                                    action = state.chartAction,
                                    points = state.charts[state.chartAction],
                                    loading = state.loading || state.chartAction in state.chartsLoading,
                                    onSelect = vm::selectChart,
                                )
                            }
                            if (state.chartAction == ChartAction.Flowers) {
                                item(key = "flowers-label") { GroupLabel(stringResource(R.string.flowered_answers_title)) }
                                items(state.flowers.items, key = { "f" + it.id }) { answer ->
                                    FloweredAnswerCard(answer, onClick = { onOpenQuestion(answer.questionId) })
                                }
                                item(key = "flowers-end") {
                                    PagingFooter(state.flowers, stringResource(R.string.flowered_empty), vm::loadMoreFlowers)
                                }
                            }
                        }
                        ProfileSection.Thanks -> {
                            if (session.signedIn && !isMe) {
                                item(key = "thanks-write") {
                                    ThanksComposer(
                                        nickname = profile.nickname,
                                        sending = state.sendingThanks,
                                        onSend = vm::writeThanks,
                                    )
                                }
                            }
                            items(state.thanks.items, key = { "t" + it.id }) { message ->
                                val onDelete: (() -> Unit)? = if (message.canDelete) ({ pendingDelete = message }) else null
                                ThanksRow(message, onDelete)
                            }
                            item(key = "thanks-end") {
                                PagingFooter(state.thanks, stringResource(R.string.thanks_empty), vm::loadMoreThanks)
                            }
                        }
                    }
                }
            }
        }
    }

    val fullPhoto = profile?.avatar?.fullPhotoUrl()
    if (photoOpen && fullPhoto != null) {
        ImageViewerDialog(
            fullUrl = fullPhoto,
            previewUrl = profile?.avatar?.avatarUrl(large = true),
            caption = profile?.nickname,
            onDismiss = { photoOpen = false },
        )
    }
    if (editingBio) {
        BioDialog(
            initial = state.page?.bio.orEmpty(),
            onSave = {
                editingBio = false
                vm.saveBio(it)
            },
            onDismiss = { editingBio = false },
        )
    }
    if (promoOpen && profile != null) {
        val bio = state.page?.bio
        BioPromoDialog(
            bio = bio,
            female = profile.female,
            onConfirm = {
                promoOpen = false
                vm.saveBio(BioPromo.appended(bio, profile.female))
            },
            onDismiss = { promoOpen = false },
        )
    }
    if (detailsOpen && profile != null) {
        ServerDetailsDialog(
            title = stringResource(R.string.expert_details),
            fields = ExpertFields.profile(profile, state.page),
            raw = listOf("item_profile" to profile.raw, "page_data" to state.page?.raw),
            onDismiss = { detailsOpen = false },
        )
    }
    if (credibilityOpen && profile != null) {
        CredibilitySheet(points = profile.points, page = state.page, isMe = isMe, onDismiss = { credibilityOpen = false })
    }
    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.thanks_delete_title)) },
            text = { Text(stringResource(R.string.thanks_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    vm.deleteThanks(target)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    page: ProfilePage?,
    isMe: Boolean,
    busy: Boolean,
    expert: Boolean,
    onChangePhoto: () -> Unit,
    onEditBio: () -> Unit,
    onMessage: (() -> Unit)?,
    onStat: (ChartAction) -> Unit,
    onOpenPhoto: (() -> Unit)?,
    onPromo: (() -> Unit)? = null,
) {
    // Stips gives some accounts a colored profile background (#8): a band behind the photo, as a cover.
    val band = profile.background.takeIf { it.size >= 2 }?.map { Color(it) }
    Box(Modifier.fillMaxWidth()) {
        if (band != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(band)),
            )
        }
        ProfileHeaderBody(
            profile = profile,
            page = page,
            isMe = isMe,
            busy = busy,
            expert = expert,
            onChangePhoto = onChangePhoto,
            onEditBio = onEditBio,
            onMessage = onMessage,
            onStat = onStat,
            onOpenPhoto = onOpenPhoto,
            top = if (band != null) 40.dp else 8.dp,
            onPromo = onPromo,
        )
    }
}

@Composable
private fun ProfileHeaderBody(
    profile: UserProfile,
    page: ProfilePage?,
    isMe: Boolean,
    busy: Boolean,
    expert: Boolean,
    onChangePhoto: () -> Unit,
    onEditBio: () -> Unit,
    onMessage: (() -> Unit)?,
    onStat: (ChartAction) -> Unit,
    onOpenPhoto: (() -> Unit)?,
    top: Dp,
    onPromo: (() -> Unit)? = null,
) {
    val online = onlineColor()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = top, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                val photoLabel = stringResource(R.string.profile_view_photo)
                Avatar(
                    profile.avatar,
                    88.dp,
                    large = true,
                    clickable = false,
                    modifier = if (onOpenPhoto != null) {
                        Modifier
                            .clip(CircleShape)
                            .clickable(onClickLabel = photoLabel, onClick = onOpenPhoto)
                    } else {
                        Modifier
                    },
                )
                if (busy) {
                    CircularProgressIndicator(Modifier.matchParentSize(), strokeWidth = 3.dp)
                }
                if (profile.online && !isMe) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(online),
                    )
                }
                if (isMe) {
                    FilledIconButton(
                        onClick = onChangePhoto,
                        enabled = !busy,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(34.dp),
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(R.string.profile_change_photo),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            // Some accounts hide their numbers on Stips (#8); the app hides them too.
            if (page?.hideStats != true) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat(page?.questions, stringResource(R.string.profile_stat_questions)) { onStat(ChartAction.Questions) }
                    Stat(page?.answers, stringResource(R.string.profile_stat_answers)) { onStat(ChartAction.Answers) }
                    Stat(page?.flowers, stringResource(R.string.profile_stat_flowers)) { onStat(ChartAction.Flowers) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                profile.nickname,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (profile.highlightName) specialNameColor() else Color.Unspecified,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (profile.avatar.verified) {
                Spacer(Modifier.width(6.dp))
                VerifiedMark(profile.avatar, 24.dp)
            }
        }
        if (expert) ExpertLine("user #${profile.userId}")
        if (profile.online && !isMe) {
            Text(
                stringResource(if (profile.female) R.string.profile_online_female else R.string.profile_online_male),
                style = MaterialTheme.typography.labelMedium,
                color = online,
            )
        }
        val badges = page?.badges.orEmpty().mapNotNull { badgeLabel(it, profile.female) } + page?.customBadges.orEmpty()
        val verifiedLabel = when {
            profile.avatar.platform -> stringResource(R.string.verified_stips)
            profile.avatar.organization -> stringResource(R.string.verified_organization)
            else -> null
        }
        val age = if (page?.hideAge == true) null else page?.age
        val memberFor = memberForText(page?.activeSince)
        // Stips+'s special accounts: their badge comes first (#12).
        val special = SpecialAccounts.badge(profile.userId)
        if (badges.isNotEmpty() || verifiedLabel != null || age != null || memberFor != null || special != null) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (special != null) SpecialBadgePill(special, large = true)
                if (verifiedLabel != null) InfoPill(Icons.Outlined.Verified, verifiedLabel, highlighted = true)
                badges.forEach { InfoPill(Icons.Outlined.WorkspacePremium, it, highlighted = true) }
                if (age != null) {
                    InfoPill(
                        Icons.Outlined.Cake,
                        stringResource(if (profile.female) R.string.profile_age_female else R.string.profile_age_male, age),
                    )
                }
                if (memberFor != null) InfoPill(Icons.Outlined.CalendarMonth, memberFor)
            }
        }
        val bio = page?.bio
        if (bio != null) {
            Spacer(Modifier.height(10.dp))
            // Links in a bio open like links in answers: Stips ones in the app, the rest in the browser (#12).
            LinkifiedText(bio, style = MaterialTheme.typography.bodyMedium)
        }
        if (onMessage != null || isMe) {
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (onMessage != null) {
                    FilledTonalButton(onClick = onMessage) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_message))
                    }
                }
                if (isMe) {
                    OutlinedButton(onClick = onEditBio) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (bio == null) R.string.profile_add_bio else R.string.profile_edit_bio))
                    }
                    // "גם אני משתמש בסטיפס+" for the bio, in the special badges' colors (#12).
                    if (onPromo != null && !BioPromo.has(bio)) PromoButton(onPromo)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Stat(value: Int?, label: String, onClick: () -> Unit) {
    val number = if (value == null) "–" else NumberFormat.getIntegerInstance().format(value)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$number $label" },
    ) {
        Text(number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String, highlighted: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/**
 * Stips' "credibility points" (0–100), drawn as a gauge in the site's four bands, tuned to the theme, with Stips'
 * rating and how fast it rises (v0.13). A tap opens what the points mean and when they'd reach more.
 */
@Composable
private fun CredibilityCard(points: Int, onClick: () -> Unit) {
    val animate = LocalAppSettings.current.motion.elements
    val value = points.coerceIn(0, 100)
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(value, animate) {
        if (animate) sweep.animateTo(value / 100f, tween(900, easing = FastOutSlowInEasing)) else sweep.snapTo(value / 100f)
    }
    val color = bandColor(credibilitySeed(value))
    val rating = CredRules.rating(value)
    val level = ratingName(rating)
    val rises = riseText(rating)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val title = stringResource(R.string.profile_credibility)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title $value, $level. $rises" },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(148.dp)
                    .height(84.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val stroke = 14.dp.toPx()
                    val diameter = minOf(size.width - stroke, 2f * (size.height - stroke))
                    val topLeft = Offset((size.width - diameter) / 2f, stroke / 2f)
                    val arc = Size(diameter, diameter)
                    val style = Stroke(width = stroke, cap = StrokeCap.Round)
                    drawArc(track, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = topLeft, size = arc, style = style)
                    val progress = sweep.value
                    if (progress > 0f) {
                        // The gauge fills in reading direction.
                        if (rtl) {
                            drawArc(color, startAngle = 0f, sweepAngle = -180f * progress, useCenter = false, topLeft = topLeft, size = arc, style = style)
                        } else {
                            drawArc(color, startAngle = 180f, sweepAngle = 180f * progress, useCenter = false, topLeft = topLeft, size = arc, style = style)
                        }
                    }
                }
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(level, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
                Text(
                    rises,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.cred_card_more),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ActivitySection(action: ChartAction, points: List<ChartPoint>?, loading: Boolean, onSelect: (ChartAction) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ChartAction.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = entry == action,
                    onClick = { onSelect(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, ChartAction.entries.size),
                    modifier = Modifier.weight(1f),
                    label = { Text(chartLabel(entry), maxLines = 1) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            when {
                points != null -> ActivityChart(points, Modifier.padding(16.dp))
                loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                else -> Text(
                    stringResource(R.string.chart_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun chartLabel(action: ChartAction): String = stringResource(
    when (action) {
        ChartAction.Answers -> R.string.profile_stat_answers
        ChartAction.Questions -> R.string.profile_stat_questions
        ChartAction.Flowers -> R.string.profile_stat_flowers
    },
)

/**
 * Weekly bars for the last half year. Tap a bar for its week. Time runs left to right, the newest week on the right,
 * also in Hebrew, as on a timeline (#12).
 */
@Composable
private fun ActivityChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val animate = LocalAppSettings.current.motion.elements
    var selected by remember(points) { mutableIntStateOf(-1) }
    val grow = remember(points) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(points) {
        if (animate) grow.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val barColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val max = (points.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val total = points.sumOf { it.count }
    val dayFormat = remember { DateTimeFormatter.ofPattern("d MMM") }
    val shown = points.getOrNull(selected)
    val number = NumberFormat.getIntegerInstance().format(shown?.count ?: total)
    val caption = if (shown != null) {
        stringResource(R.string.chart_week_of, shown.time.atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormat))
    } else {
        stringResource(R.string.chart_half_year)
    }
    Column(modifier.fillMaxWidth()) {
        Text(number, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty()) {
                            val slot = size.width.toFloat() / points.size
                            val index = (offset.x / slot).toInt().coerceIn(0, points.lastIndex)
                            selected = if (selected == index) -1 else index
                        }
                    }
                },
        ) {
            if (points.isEmpty()) return@Canvas
            val slot = size.width / points.size
            val gap = slot * 0.3f
            val barWidth = slot - gap
            val minHeight = 3.dp.toPx()
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            points.forEachIndexed { i, point ->
                val position = i
                val height = if (point.count == 0) minHeight else (size.height * point.count / max * grow.value).coerceAtLeast(minHeight)
                val color = when {
                    point.count == 0 -> emptyColor
                    selected == -1 -> barColor
                    selected == i -> selectedColor
                    else -> barColor.copy(alpha = 0.35f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(position * slot + gap / 2f, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // The first week under the left end, this week under the right one, whatever the language.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val first = points.firstOrNull()?.time?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(dayFormat).orEmpty()
                Text(first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.chart_this_week), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FloweredAnswerCard(answer: Answer, onClick: () -> Unit) {
    val flower = bandColor(0xFFF50057.toInt())
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val title = answer.parentTitle
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                answer.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalFlorist, contentDescription = null, tint = flower, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(agoText(answer.postedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** One message on a thanks wall, as a speech bubble from its author. */
@Composable
private fun ThanksRow(message: ThanksMessage, onDelete: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(message.author, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorName(message.author, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
                val ago = agoText(message.time)
                if (ago.isNotEmpty()) {
                    Text(
                        " · $ago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.thanks_delete))
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

/** The end of a paged list: loads the next page when it scrolls into view, or offers a retry. */
@Composable
private fun <T> PagingFooter(paged: Paged<T>, emptyText: String, onLoadMore: () -> Unit) {
    when {
        paged.loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }
        paged.failed -> Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) { TextButton(onClick = onLoadMore) { Text(stringResource(R.string.action_retry)) } }
        paged.end && paged.items.isEmpty() -> Text(
            emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        )
        !paged.end -> LaunchedEffect(paged.page) { onLoadMore() }
    }
}

@Composable
private fun BioDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_bio)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(BIO_MAX) },
                placeholder = { Text(stringResource(R.string.profile_bio_hint)) },
                supportingText = { Text("${text.length} / $BIO_MAX") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.action_save_changes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Stips' badge codes, in the person's grammatical gender. Unknown codes are skipped. */
@Composable
private fun badgeLabel(code: String, female: Boolean): String? {
    val id = when (code) {
        "moderator" -> if (female) R.string.badge_moderator_f else R.string.badge_moderator_m
        "senior_moderator" -> if (female) R.string.badge_senior_moderator_f else R.string.badge_senior_moderator_m
        "old_expert" -> if (female) R.string.badge_expert_f else R.string.badge_expert_m
        "senior_advisor" -> if (female) R.string.badge_senior_advisor_f else R.string.badge_senior_advisor_m
        else -> return null
    }
    return stringResource(id)
}

/** A vivid accent from a fixed hue, at a tone that reads well on the current light or dark surface. */
@Composable
internal fun bandColor(seed: Int): Color {
    val dark = LocalIsDark.current
    return remember(seed, dark) {
        val lch = ColorMath.toLch(seed)
        Color(SchemeGenerator.TonalPalette(lch.h, minOf(lch.c, 72.0)).tone(if (dark) 78 else 50))
    }
}

/** Writing on someone's thanks wall, as on the site ("כתוב משהו ל…"). */
@Composable
private fun ThanksComposer(nickname: String, sending: Boolean, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(THANKS_MAX) },
            placeholder = { Text(stringResource(R.string.thanks_write_hint, nickname), maxLines = 1) },
            maxLines = 4,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSend(text)
                text = ""
            },
            enabled = !sending && text.isNotBlank(),
        ) {
            if (sending) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
            }
        }
    }
}

private const val THANKS_MAX = 500

/** The colors of the Stips+ bio button: Stips green into blue and purple, bright enough on both themes (#12). */
private val PromoColors = listOf(Color(0xFF09A872), Color(0xFF2F7BF6), Color(0xFF8E44EC))

/** The bio button (#12): a colored pill, like a special badge, that offers to add "גם אני משתמש בסטיפס+". */
@Composable
private fun PromoButton(onClick: () -> Unit) {
    // Shaped like the buttons beside it (#18): a 40 dp pill centered in a 48 dp touch target, as Material's are.
    Row(
        Modifier
            .minimumInteractiveComponentSize()
            .height(ButtonDefaults.MinHeight)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(PromoColors))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(ButtonDefaults.ButtonWithIconContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            stringResource(R.string.bio_promo_button),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * What the bio button adds (#12): the exact line, a promise that the bio stays as it is, and when the line doesn't
 * fit Stips' 250 characters, how many it would take.
 */
@Composable
private fun BioPromoDialog(bio: String?, female: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val length = BioPromo.lengthWith(bio, female)
    val fits = length <= BioPromo.BIO_MAX
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        title = { Text(stringResource(R.string.bio_promo_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.bio_promo_body), style = MaterialTheme.typography.bodyMedium)
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(BioPromo.line(female), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
                Text(
                    stringResource(R.string.bio_promo_keep),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!fits) {
                    Text(
                        stringResource(R.string.bio_promo_too_long, length, BioPromo.BIO_MAX),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = fits) { Text(stringResource(R.string.bio_promo_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

