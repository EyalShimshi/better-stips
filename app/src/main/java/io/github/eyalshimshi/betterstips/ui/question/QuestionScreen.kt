package io.github.eyalshimshi.betterstips.ui.question

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.AuthorName
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.FooterNote
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.answersText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.channelLabel
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.shareText
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.SeededTheme
import io.github.eyalshimshi.betterstips.util.AnswerRules
import io.github.eyalshimshi.betterstips.util.Links
import java.util.Locale

@Composable
fun QuestionScreen(
    questionId: Long,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm = appViewModel { QuestionViewModel(it.repository, it.session, questionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val question = state.question
    val channel = question?.let { q -> channels.firstOrNull { it.id == q.channelId } }
    val context = LocalContext.current

    SeededTheme(seed = channel?.seedColor) {
        val snackbar = remember { SnackbarHostState() }
        val texts = mapOf(
            QuestionMessage.AnswerPosted to stringResource(R.string.answer_posted),
            QuestionMessage.AnswerEdited to stringResource(R.string.answer_edited),
            QuestionMessage.AnswerDetached to stringResource(R.string.answer_detached),
            QuestionMessage.VoteFailed to stringResource(R.string.vote_failed),
            QuestionMessage.ActionFailed to stringResource(R.string.action_failed),
            QuestionMessage.Following to stringResource(R.string.following_on),
            QuestionMessage.NotFollowing to stringResource(R.string.following_off),
        )
        LaunchedEffect(state.message) {
            val message = state.message ?: return@LaunchedEffect
            snackbar.showSnackbar(texts[message].orEmpty())
            vm.consumeMessage()
        }
        var showLinkDialog by remember { mutableStateOf(false) }
        var detachTarget by remember { mutableStateOf<Answer?>(null) }
        var creditShown by remember { mutableStateOf(false) }
        val creditText = (question?.cover as? Cover.Unsplash)?.creatorName?.let { stringResource(R.string.photo_credit, it) }
        LaunchedEffect(creditShown) {
            if (creditShown && creditText != null) snackbar.showSnackbar(creditText)
            creditShown = false
        }

        val listState = rememberLazyListState()
        var heroHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val barPx = with(density) { 56.dp.toPx() } + WindowInsets.statusBars.getTop(density)
        val collapse by remember {
            derivedStateOf {
                when {
                    listState.firstVisibleItemIndex > 0 -> 1f
                    heroHeight <= 0 -> 0f
                    else -> (listState.firstVisibleItemScrollOffset / (heroHeight - barPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
                }
            }
        }
        val notFound = state.error?.code == ApiException.NOT_FOUND

        Scaffold(
            bottomBar = {
                if (question != null && !notFound) {
                    Composer(
                        signedIn = session.signedIn,
                        state = state,
                        onDraft = vm::setDraft,
                        onAnonymous = vm::setAnonymous,
                        onFollowOnAnswer = vm::setFollowOnAnswer,
                        onAddLink = { showLinkDialog = true },
                        onRemoveLink = vm::removeLink,
                        onCancelEdit = vm::cancelEdit,
                        onSend = vm::post,
                        onSignIn = onSignIn,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
                val error = state.error
                when {
                    notFound -> Column(
                        Modifier
                            .statusBarsPadding()
                            .padding(top = 56.dp),
                    ) {
                        MessageCard(stringResource(R.string.error_not_found), null, stringResource(R.string.action_go_back), onBack)
                    }
                    question == null && error != null -> Column(
                        Modifier
                            .statusBarsPadding()
                            .padding(top = 56.dp),
                    ) { ErrorCard(error, onRetry = vm::load) }
                    question == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> QuestionContent(
                        question = question,
                        channel = channel,
                        state = state,
                        signedIn = session.signedIn,
                        listState = listState,
                        collapse = collapse,
                        onHeroHeight = { heroHeight = it },
                        onOpenTopic = onOpenTopic,
                        onVote = vm::vote,
                        onRetry = vm::load,
                        onEdit = vm::startEdit,
                        onDetach = { detachTarget = it },
                    )
                }
                QuestionTopBar(
                    collapse = if (question == null) 1f else collapse,
                    title = question?.title.orEmpty(),
                    onBack = onBack,
                ) {
                    if (question != null && !notFound) {
                        PinAction(
                            pinned = state.pinned,
                            count = state.pinCount,
                            collapse = collapse,
                            onClick = { if (session.signedIn) vm.togglePin() else onSignIn() },
                        )
                        if (session.signedIn) {
                            BarIconButton(collapse = collapse, onClick = vm::toggleFollow) {
                                val following = state.following == true
                                Icon(
                                    if (following) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                                    contentDescription = stringResource(if (following) R.string.stop_updates else R.string.get_updates),
                                    tint = if (following) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        BarIconButton(collapse = collapse, onClick = { shareText(context, question.title + "\n" + question.shareUrl) }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                        }
                        MoreMenu(
                            collapse = collapse,
                            hasCredit = creditText != null,
                            onOpenSite = { openInBrowser(context, question.shareUrl) },
                            onCredit = { creditShown = true },
                        )
                    }
                }
            }
        }

        if (showLinkDialog) {
            LinkDialog(
                onDismiss = { showLinkDialog = false },
                onConfirm = { url ->
                    showLinkDialog = false
                    vm.attachLink(url)
                },
            )
        }
        detachTarget?.let { answer ->
            AlertDialog(
                onDismissRequest = { detachTarget = null },
                title = { Text(stringResource(R.string.detach_title)) },
                text = { Text(stringResource(R.string.detach_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        detachTarget = null
                        vm.detach(answer)
                    }) { Text(stringResource(R.string.detach_confirm)) }
                },
                dismissButton = { TextButton(onClick = { detachTarget = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

@Composable
private fun QuestionContent(
    question: Question,
    channel: Channel?,
    state: QuestionUiState,
    signedIn: Boolean,
    listState: LazyListState,
    collapse: Float,
    onHeroHeight: (Int) -> Unit,
    onOpenTopic: (String) -> Unit,
    onVote: (Answer, Int) -> Unit,
    onRetry: () -> Unit,
    onEdit: (Answer) -> Unit,
    onDetach: (Answer) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "hero") {
            QuestionHero(
                question = question,
                channel = channel,
                collapse = collapse,
                onOpenTopic = onOpenTopic,
                modifier = Modifier.onSizeChanged { onHeroHeight(it.height) },
            )
        }
        if (question.body.isNotBlank()) {
            item(key = "body") { QuestionBody(question.body) }
        }
        item(key = "answers-header") {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    answersText(maxOf(question.answerCount, state.answers.size)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
        val answersError = state.answersError
        if (answersError != null && state.answers.isEmpty()) {
            item(key = "answers-error") { ErrorCard(answersError, onRetry) }
        }
        items(state.answers, key = { it.id }) { answer ->
            AnswerCard(
                answer = answer,
                signedIn = signedIn,
                onVote = { direction -> onVote(answer, direction) },
                onEdit = { onEdit(answer) },
                onDetach = { onDetach(answer) },
                modifier = Modifier.animateItem(),
            )
        }
        state.loadedInMs?.let { ms ->
            item(key = "timing") {
                FooterNote(stringResource(R.string.loaded_in, String.format(Locale.getDefault(), "%.2f", ms / 1000.0)))
            }
        }
    }
}

/** The cover as a background that fades into the page; the title sits on the fade. */
@Composable
private fun QuestionHero(
    question: Question,
    channel: Channel?,
    collapse: Float,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    val uriHandler = LocalUriHandler.current
    val surface = MaterialTheme.colorScheme.surface
    val cover = question.cover
    val withImage = cover != null && showImages
    Box(modifier.fillMaxWidth()) {
        if (cover != null && showImages) {
            AsyncImage(
                model = cover.largeUrl,
                contentDescription = null,
                imageLoader = loader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationY = collapse * size.height * 0.35f
                        alpha = 1f - collapse * 0.5f
                    },
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, surface))),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = 0.1f),
                        0.35f to surface.copy(alpha = 0.6f),
                        0.6f to surface.copy(alpha = 0.9f),
                        1f to surface,
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = if (withImage) 96.dp else 60.dp, bottom = 4.dp),
        ) {
            Text(
                question.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(question.author, 22.dp)
                Spacer(Modifier.width(6.dp))
                AuthorName(question.author, MaterialTheme.typography.labelLarge, Modifier.weight(1f, fill = false))
                val rest = listOfNotNull(
                    agoText(question.postedAt).takeIf { it.isNotBlank() },
                    channel?.let { channelLabel(it) },
                ).joinToString(" · ")
                if (rest.isNotBlank()) {
                    Text(
                        " · $rest",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (question.topics.isNotEmpty()) {
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    question.topics.forEach { topic ->
                        Text(
                            "#$topic",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenTopic(topic) }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            val link = question.link
            if (link != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri(link) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        Links.host(link),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The question's text, clamped to 3 lines so the answers start early. */
@Composable
private fun QuestionBody(body: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        LinkifiedText(
            body,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            onOverflow = { if (!expanded) overflowing = it },
        )
        if (overflowing || expanded) {
            Text(
                stringResource(if (expanded) R.string.show_less else R.string.show_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

/** Collapses to a solid bar with just the question's title (DESIGN-PHILOSOPHY §4: motion follows the scroll). */
@Composable
private fun QuestionTopBar(collapse: Float, title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit) {
    val bar = MaterialTheme.colorScheme.surfaceContainer
    Box(
        Modifier
            .fillMaxWidth()
            .background(bar.copy(alpha = collapse)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIconButton(collapse = collapse, onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        alpha = collapse
                        translationY = (1f - collapse) * 16.dp.toPx()
                    },
            )
            actions()
        }
    }
}

/** An icon button that keeps a soft round backdrop while it floats over the cover. */
@Composable
private fun BarIconButton(collapse: Float, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f * (1f - collapse))),
    ) { content() }
}

@Composable
private fun PinAction(pinned: Boolean, count: Int, collapse: Float, onClick: () -> Unit) {
    val description = stringResource(if (pinned) R.string.unpin else R.string.pin) + ", " +
        androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.pins_count, count, count)
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f * (1f - collapse)))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = null,
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(count.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MoreMenu(collapse: Float, hasCredit: Boolean, onOpenSite: () -> Unit, onCredit: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        BarIconButton(collapse = collapse, onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open_site)) },
                onClick = {
                    open = false
                    onOpenSite()
                },
            )
            if (hasCredit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_credit_menu)) },
                    onClick = {
                        open = false
                        onCredit()
                    },
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(
    answer: Answer,
    signedIn: Boolean,
    onVote: (Int) -> Unit,
    onEdit: () -> Unit,
    onDetach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(answer.author, 28.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    AuthorName(answer.author, MaterialTheme.typography.labelLarge)
                    val ago = agoText(answer.postedAt)
                    if (ago.isNotBlank()) {
                        Text(ago, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (answer.flowered) {
                    Icon(
                        Icons.Filled.LocalFlorist,
                        contentDescription = stringResource(R.string.flower_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                if (answer.isOwner && (answer.canEdit || !answer.author.anonymous)) {
                    OwnerMenu(answer, onEdit, onDetach)
                }
            }
            LinkifiedText(
                answer.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp, end = 10.dp),
            )
            answer.links.forEach { link ->
                Row(
                    Modifier
                        .padding(top = 6.dp, end = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri(link.url) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        link.title ?: Links.host(link.url),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onVote(1) }, enabled = signedIn) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.vote_up),
                        tint = if (answer.userVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(answer.score.toString(), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { onVote(-1) }, enabled = signedIn) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.vote_down),
                        tint = if (answer.userVote == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerMenu(answer: Answer, onEdit: () -> Unit, onDetach: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.answer_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (answer.canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_answer)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        open = false
                        onEdit()
                    },
                )
            }
            if (!answer.author.anonymous) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detach_answer)) },
                    leadingIcon = { Icon(Icons.Outlined.PersonOff, contentDescription = null) },
                    onClick = {
                        open = false
                        onDetach()
                    },
                )
            }
        }
    }
}

@Composable
private fun Composer(
    signedIn: Boolean,
    state: QuestionUiState,
    onDraft: (String) -> Unit,
    onAnonymous: (Boolean) -> Unit,
    onFollowOnAnswer: (Boolean) -> Unit,
    onAddLink: () -> Unit,
    onRemoveLink: () -> Unit,
    onCancelEdit: () -> Unit,
    onSend: () -> Unit,
    onSignIn: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            if (!signedIn) {
                FilledTonalButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sign_in_to_answer))
                }
            } else {
                val editing = state.editing
                if (editing != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.editing_answer), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = onCancelEdit) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (editing == null) {
                        FilterChip(
                            selected = state.anonymous,
                            onClick = { onAnonymous(!state.anonymous) },
                            label = { Text(stringResource(R.string.composer_anonymous)) },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    val link = state.draftLink
                    if (link == null) {
                        AssistChip(
                            onClick = onAddLink,
                            label = { Text(stringResource(R.string.add_link)) },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    } else {
                        InputChip(
                            selected = true,
                            onClick = onRemoveLink,
                            label = {
                                Text(
                                    link.title ?: Links.host(link.url),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp),
                                )
                            },
                            leadingIcon = {
                                if (state.linkLoading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_link), modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                    if (editing == null) {
                        FilterChip(
                            selected = state.followOnAnswer,
                            onClick = { onFollowOnAnswer(!state.followOnAnswer) },
                            label = { Text(stringResource(R.string.get_updates)) },
                            leadingIcon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = onDraft,
                        placeholder = { Text(stringResource(R.string.composer_hint)) },
                        maxLines = 6,
                        isError = state.problem != null || state.postError != null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = onSend, enabled = !state.posting && state.draft.isNotBlank()) {
                        if (state.posting) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                        }
                    }
                }
                val problemText = when (state.problem) {
                    AnswerRules.Problem.Empty -> stringResource(R.string.answer_problem_empty)
                    AnswerRules.Problem.TooLong -> stringResource(R.string.answer_problem_long, AnswerRules.MAX_LENGTH)
                    AnswerRules.Problem.RepeatedLetters -> stringResource(R.string.answer_problem_repeated)
                    null -> state.postError?.let { errorMessage(it) }
                }
                if (problemText != null) {
                    Text(
                        problemText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** The site's "add link" sub-menu: one field; the title is fetched afterwards. */
@Composable
private fun LinkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    val trimmed = url.trim()
    val valid = trimmed.contains('.') && !trimmed.contains(' ')
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_link_title)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text(stringResource(R.string.paste_link_here)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(trimmed) }),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(trimmed) }, enabled = valid) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
