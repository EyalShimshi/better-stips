package io.github.eyalshimshi.betterstips.ui.question

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.model.ModeratorRank
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.ui.components.AuthorName
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.ExpertFields
import io.github.eyalshimshi.betterstips.ui.components.ExpertLine
import io.github.eyalshimshi.betterstips.ui.components.FooterNote
import io.github.eyalshimshi.betterstips.ui.components.ImageViewerDialog
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.QuestionMetaRow
import io.github.eyalshimshi.betterstips.ui.components.ServerDetailsDialog
import io.github.eyalshimshi.betterstips.ui.components.TextTarget
import io.github.eyalshimshi.betterstips.ui.components.VerifiedBlue
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.answersText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.components.channelLabel
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.rememberAnonymousModeOn
import io.github.eyalshimshi.betterstips.ui.components.shareText
import io.github.eyalshimshi.betterstips.ui.components.specialNameColor
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.SeededTheme
import io.github.eyalshimshi.betterstips.util.AnswerDisplay
import io.github.eyalshimshi.betterstips.util.AnswerRules
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.ReplyRefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

/** What a dialog on the question screen is about. */
private sealed interface PendingAction {
    data class Report(val objType: String, val id: Long) : PendingAction

    data class HideUser(val objType: String, val id: Long, val author: Author) : PendingAction

    data class DeleteAnswer(val answer: Answer) : PendingAction

    data class DetachAnswer(val answer: Answer) : PendingAction

    data object DeleteQuestion : PendingAction

    data object DetachQuestion : PendingAction

    data object EditQuestion : PendingAction

    /** Expert mode (#8): what Stips sent about the question, or about one answer. */
    data object QuestionDetails : PendingAction

    data class AnswerDetails(val answer: Answer) : PendingAction
}

/** One row of a ⋮ menu. */
private data class MenuEntry(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun QuestionScreen(
    questionId: Long,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onSignIn: () -> Unit,
    focus: QuestionFocus? = null,
) {
    val container = LocalAppContainer.current
    val vm = appViewModel {
        QuestionViewModel(it.repository, it.session, it.settings, it.anonymousAnswers, it.readMarks, it.replyTargets, questionId, focus)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val blockVersion by container.blocks.version.collectAsStateWithLifecycle()
    val question = state.question
    val channel = question?.let { q -> channels.firstOrNull { it.id == q.channelId } }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val anonymousMode = rememberAnonymousModeOn(settings.anonymousUntil)

    LaunchedEffect(state.closed) {
        if (state.closed) onBack()
    }

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
            QuestionMessage.Reported to stringResource(R.string.report_sent),
            QuestionMessage.Hidden to stringResource(R.string.content_hidden),
            QuestionMessage.AnswerDeleted to stringResource(R.string.answer_deleted),
            QuestionMessage.QuestionEdited to stringResource(R.string.question_edited),
            QuestionMessage.QuestionDetached to stringResource(R.string.question_detached),
            QuestionMessage.FlowerSent to stringResource(R.string.flower_sent),
            QuestionMessage.ReplyFixed to stringResource(R.string.reply_fixed),
        )
        LaunchedEffect(state.message) {
            val message = state.message ?: return@LaunchedEffect
            snackbar.showSnackbar(texts[message].orEmpty())
            vm.consumeMessage()
        }
        var showLinkDialog by remember { mutableStateOf(false) }
        var pending by remember { mutableStateOf<PendingAction?>(null) }
        var viewerOpen by remember { mutableStateOf(false) }
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
        val hidden = question != null && remember(question.id, question.author.userId, blockVersion) {
            container.blocks.isBlocked("ask", question.id, question.author.userId)
        }
        val answers = remember(state.answers, blockVersion, settings.answersOrder) {
            AnswerDisplay.ordered(state.answers.filterNot { container.blocks.isBlocked("ans", it.id, it.author.userId) }, settings.answersOrder)
        }
        val signedIn = session.signedIn
        val me = session.userId
        val expert = settings.expertMode
        // Easy reply (v0.10): signed in, writing a new answer (not editing one).
        val canReply = signedIn && settings.easyReply && state.editing == null
        val badgesVersion by container.userBadges.version.collectAsStateWithLifecycle()
        val moderatorOf: (Answer) -> ModeratorBadge? = { answer ->
            val userId = answer.author.userId
            // Reading the version makes the cards draw marks found after they appeared.
            val known = badgesVersion >= 0 && settings.moderatorBadges
            val entry = if (known && userId != null && !answer.author.anonymous) container.userBadges.entry(userId) else null
            entry?.let { e -> e.moderator?.let { ModeratorBadge(it, e.female == true) } }
        }
        val isHidden: (Answer) -> Boolean = { container.blocks.isBlocked("ans", it.id, it.author.userId) }

        fun requireSignIn(action: () -> Unit) {
            if (signedIn) action() else onSignIn()
        }

        // The ⋮ menu of an answer: reply; your own answers can be edited, detached or deleted; others reported or hidden.
        val replyLabel = stringResource(R.string.action_reply)
        val editLabel = stringResource(R.string.edit_answer)
        val detachLabel = stringResource(R.string.detach_answer)
        val deleteLabel = stringResource(R.string.delete_answer)
        val reportAnswerLabel = stringResource(R.string.report_answer)
        val hideAnswerLabel = stringResource(R.string.hide_answer)
        val hideUserLabel = stringResource(R.string.hide_user_content)
        val detailsLabel = stringResource(R.string.expert_details)
        val answerMenu: (Answer) -> List<MenuEntry> = { answer ->
            buildList {
                if (canReply) add(MenuEntry(replyLabel, Icons.AutoMirrored.Filled.Reply) { vm.replyTo(answer) })
                if (answer.isOwner) {
                    if (answer.canEdit) add(MenuEntry(editLabel, Icons.Filled.Edit) { vm.startEdit(answer) })
                    if (!answer.author.anonymous) add(MenuEntry(detachLabel, Icons.Outlined.PersonOff) { pending = PendingAction.DetachAnswer(answer) })
                    if (answer.canDelete) add(MenuEntry(deleteLabel, Icons.Outlined.Delete, destructive = true) { pending = PendingAction.DeleteAnswer(answer) })
                } else {
                    add(MenuEntry(reportAnswerLabel, Icons.Outlined.Flag) { requireSignIn { pending = PendingAction.Report("ans", answer.id) } })
                    add(MenuEntry(hideAnswerLabel, Icons.Outlined.VisibilityOff) { vm.block("ans", answer.id, null) })
                    val authorId = answer.author.userId
                    if (authorId != null && authorId != me) {
                        add(MenuEntry(hideUserLabel, Icons.Outlined.Block) { pending = PendingAction.HideUser("ans", answer.id, answer.author) })
                    }
                }
                if (expert) add(MenuEntry(detailsLabel, Icons.Outlined.Code) { pending = PendingAction.AnswerDetails(answer) })
            }
        }
        val floweredCount = answers.count { it.flowered }

        Scaffold(
            bottomBar = {
                if (question != null && !notFound && !hidden) {
                    Composer(
                        signedIn = signedIn,
                        state = state,
                        anonymousMode = anonymousMode,
                        onDraft = vm::setDraft,
                        onAnonymous = vm::setAnonymous,
                        onFollowOnAnswer = vm::setFollowOnAnswer,
                        onAddLink = { showLinkDialog = true },
                        onRemoveLink = vm::removeLink,
                        onCancelEdit = vm::cancelEdit,
                        onCancelReply = vm::cancelReply,
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
                    hidden -> Column(
                        Modifier
                            .statusBarsPadding()
                            .padding(top = 56.dp),
                    ) {
                        MessageCard(
                            stringResource(R.string.question_hidden_title),
                            stringResource(R.string.question_hidden_body),
                            stringResource(R.string.action_go_back),
                            onBack,
                        )
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
                        answers = answers,
                        signedIn = signedIn,
                        expert = expert,
                        listState = listState,
                        collapse = collapse,
                        topBarPx = barPx,
                        onHeroHeight = { heroHeight = it },
                        onOpenTopic = onOpenTopic,
                        onVote = { answer, direction ->
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            vm.vote(answer, direction)
                        },
                        onRetry = vm::load,
                        onRefresh = vm::refresh,
                        answerMenu = answerMenu,
                        canFlower = { answer -> canSendFlower(question, answer, floweredCount) },
                        onFlower = { answer ->
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            vm.sendFlower(answer)
                        },
                        replyQuotes = settings.replyQuotes,
                        onReply = if (canReply) vm::replyTo else null,
                        isHidden = isHidden,
                        moderatorOf = moderatorOf,
                        onVisibleAuthors = vm::wantBadges,
                        onJumpHandled = vm::consumeJump,
                    )
                }
                QuestionTopBar(
                    collapse = if (question == null || hidden) 1f else collapse,
                    title = question?.title.orEmpty(),
                    onBack = onBack,
                ) {
                    if (question != null && !notFound && !hidden) {
                        PinAction(
                            pinned = state.pinned,
                            count = state.pinCount,
                            collapse = collapse,
                            onClick = {
                                requireSignIn {
                                    haptics.performHapticFeedback(if (state.pinned) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    vm.togglePin()
                                }
                            },
                        )
                        if (signedIn) {
                            BarIconButton(
                                collapse = collapse,
                                onClick = {
                                    haptics.performHapticFeedback(if (state.following == true) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    vm.toggleFollow()
                                },
                            ) {
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
                        val showImages = LocalAppSettings.current.showImages
                        val entries = buildList {
                            add(MenuEntry(stringResource(R.string.action_open_site), Icons.Outlined.OpenInBrowser) { openInBrowser(context, question.shareUrl) })
                            if (question.cover != null && showImages) {
                                add(MenuEntry(stringResource(R.string.view_image), Icons.Outlined.Image) { viewerOpen = true })
                            }
                            if (creditText != null) {
                                add(MenuEntry(stringResource(R.string.photo_credit_menu), Icons.Outlined.Info) { creditShown = true })
                            }
                            if (question.isOwner) {
                                if (question.canEdit) add(MenuEntry(stringResource(R.string.edit_question), Icons.Filled.Edit) { pending = PendingAction.EditQuestion })
                                if (!question.author.anonymous) {
                                    add(MenuEntry(stringResource(R.string.detach_question), Icons.Outlined.PersonOff) { pending = PendingAction.DetachQuestion })
                                }
                                if (question.canDelete) {
                                    add(MenuEntry(stringResource(R.string.delete_question), Icons.Outlined.Delete, destructive = true) { pending = PendingAction.DeleteQuestion })
                                }
                            } else {
                                add(MenuEntry(stringResource(R.string.report_question), Icons.Outlined.Flag) { requireSignIn { pending = PendingAction.Report("ask", question.id) } })
                                add(MenuEntry(stringResource(R.string.hide_question), Icons.Outlined.VisibilityOff) { vm.block("ask", question.id, null) })
                                val authorId = question.author.userId
                                if (authorId != null && authorId != me) {
                                    add(MenuEntry(hideUserLabel, Icons.Outlined.Block) { pending = PendingAction.HideUser("ask", question.id, question.author) })
                                }
                            }
                            if (expert) add(MenuEntry(detailsLabel, Icons.Outlined.Code) { pending = PendingAction.QuestionDetails })
                        }
                        ActionMenuButton(entries = entries, description = stringResource(R.string.more_actions), collapse = collapse)
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
        val cover = question?.cover
        if (viewerOpen && cover != null) {
            ImageViewerDialog(fullUrl = cover.fullUrl, previewUrl = cover.largeUrl, caption = creditText, onDismiss = { viewerOpen = false })
        }
        when (val action = pending) {
            null -> Unit
            is PendingAction.Report -> ReportDialog(
                objType = action.objType,
                onDismiss = { pending = null },
                onSend = { reason ->
                    pending = null
                    vm.report(action.objType, action.id, reason)
                },
            )
            is PendingAction.HideUser -> ConfirmDialog(
                title = stringResource(R.string.hide_user_title, action.author.nickname),
                body = stringResource(R.string.hide_user_body),
                confirm = stringResource(R.string.hide_user_confirm),
                onConfirm = {
                    pending = null
                    vm.block(action.objType, action.id, action.author.userId)
                },
                onDismiss = { pending = null },
            )
            is PendingAction.DeleteAnswer -> ConfirmDialog(
                title = stringResource(R.string.delete_answer_title),
                body = null,
                confirm = stringResource(R.string.action_delete),
                onConfirm = {
                    pending = null
                    vm.deleteAnswer(action.answer)
                },
                onDismiss = { pending = null },
            )
            is PendingAction.DetachAnswer -> ConfirmDialog(
                title = stringResource(R.string.detach_title),
                body = stringResource(R.string.detach_body),
                confirm = stringResource(R.string.detach_confirm),
                onConfirm = {
                    pending = null
                    vm.detach(action.answer)
                },
                onDismiss = { pending = null },
            )
            PendingAction.DeleteQuestion -> ConfirmDialog(
                title = stringResource(R.string.delete_question_title),
                body = null,
                confirm = stringResource(R.string.action_delete),
                onConfirm = {
                    pending = null
                    vm.deleteQuestion()
                },
                onDismiss = { pending = null },
            )
            PendingAction.DetachQuestion -> ConfirmDialog(
                title = stringResource(R.string.detach_question_title),
                body = stringResource(R.string.detach_question_body),
                confirm = stringResource(R.string.detach_confirm),
                onConfirm = {
                    pending = null
                    vm.detachQuestion()
                },
                onDismiss = { pending = null },
            )
            PendingAction.EditQuestion -> if (question != null) {
                EditQuestionDialog(
                    question = question,
                    onDismiss = { pending = null },
                    onSave = { title, body, link ->
                        pending = null
                        vm.editQuestion(title, body, link)
                    },
                )
            }
            PendingAction.QuestionDetails -> if (question != null) {
                ServerDetailsDialog(
                    title = stringResource(R.string.expert_details),
                    fields = ExpertFields.question(question, state.answers.size),
                    raw = listOf("omniobj ask" to question.raw),
                    onDismiss = { pending = null },
                )
            }
            is PendingAction.AnswerDetails -> ServerDetailsDialog(
                title = stringResource(R.string.expert_details),
                fields = ExpertFields.answer(action.answer),
                raw = listOf("ans.for_item" to action.answer.raw),
                onDismiss = { pending = null },
            )
        }
    }
}

/** Only the asker gives flowers: to someone else's answer, up to 14 days old, 5 per question (as on the site). */
private fun canSendFlower(question: Question, answer: Answer, floweredCount: Int): Boolean {
    if (!question.isOwner || answer.isOwner || answer.flowered || answer.byAsker || floweredCount >= MAX_FLOWERS) return false
    val posted = answer.postedAt ?: return false
    return Duration.between(posted, Instant.now()).toDays() <= FLOWER_DAYS
}

private const val MAX_FLOWERS = 5
private const val FLOWER_DAYS = 14L

/** A card to light up after a jump; [nonce] tells repeated jumps to the same answer apart. */
private data class Highlight(val answerId: Long, val nonce: Long)

/** The mark by a moderator's name on their answers (v0.10). */
private data class ModeratorBadge(val rank: ModeratorRank, val female: Boolean)

/** How long a jumped-to answer glows (v0.10). */
private const val HIGHLIGHT_MS = 2_000L

/** Answers just below the screen whose authors are looked up too, so marks are ready when they scroll in. */
private const val LOOKAHEAD = 3

@Composable
private fun QuestionContent(
    question: Question,
    channel: Channel?,
    state: QuestionUiState,
    answers: List<Answer>,
    signedIn: Boolean,
    expert: Boolean,
    listState: LazyListState,
    collapse: Float,
    topBarPx: Float,
    onHeroHeight: (Int) -> Unit,
    onOpenTopic: (String) -> Unit,
    onVote: (Answer, Int) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    answerMenu: (Answer) -> List<MenuEntry>,
    canFlower: (Answer) -> Boolean,
    onFlower: (Answer) -> Unit,
    replyQuotes: Boolean,
    onReply: ((Answer) -> Unit)?,
    isHidden: (Answer) -> Boolean,
    moderatorOf: (Answer) -> ModeratorBadge?,
    onVisibleAuthors: (Collection<Long>) -> Unit,
    onJumpHandled: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gapPx = with(LocalDensity.current) { 8.dp.toPx() }
    var highlight by remember { mutableStateOf<Highlight?>(null) }
    // Answers show as written; the reader may hide the opening carets that the quotes stand for (v0.11).
    val hideCarets = LocalAppSettings.current.hideReplyCarets
    // Carets count in Stips' order (oldest first), whatever order the answers are shown in (v0.10).
    val stipsOrder = state.answers
    val positions = remember(stipsOrder) { stipsOrder.withIndex().associate { (i, a) -> a.id to i } }
    val answersError = state.answersError
    val headerKeys = buildList {
        add(KEY_HERO)
        if (expert) add(KEY_EXPERT)
        if (question.body.isNotBlank()) add(KEY_BODY)
        add(KEY_ANSWERS_HEADER)
        if (answersError != null && state.answers.isEmpty()) add(KEY_ANSWERS_ERROR)
    }

    /** Scrolls [answerId] to just below the folded top bar, and lights it up for a moment (v0.10). */
    fun jumpTo(answerId: Long) {
        val index = answers.indexOfFirst { it.id == answerId }
        if (index < 0) return
        scope.launch {
            listState.animateScrollToItem(headerKeys.size + index, -(topBarPx + gapPx).roundToInt())
            highlight = Highlight(answerId, System.nanoTime())
        }
    }
    LaunchedEffect(state.jump) {
        val id = state.jump ?: return@LaunchedEffect
        jumpTo(id)
        onJumpHandled()
    }
    LaunchedEffect(highlight) {
        if (highlight == null) return@LaunchedEffect
        delay(HIGHLIGHT_MS + 200)
        highlight = null
    }
    // Moderator marks (v0.10): the authors of the answers on screen, and of the next few, are looked up when unknown.
    val currentAnswers by rememberUpdatedState(answers)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long } }
            .distinctUntilChanged()
            .collect { visible ->
                val list = currentAnswers
                val last = visible.lastOrNull()?.let { id -> list.indexOfFirst { it.id == id } } ?: -1
                val ahead = if (last >= 0) list.subList(last + 1, minOf(list.size, last + 1 + LOOKAHEAD)).map { it.id } else emptyList()
                val authors = (visible + ahead).mapNotNull { id ->
                    list.firstOrNull { it.id == id }?.author?.takeUnless { it.anonymous }?.userId
                }
                if (authors.isNotEmpty()) onVisibleAuthors(authors)
            }
    }

    val pullState = rememberPullToRefreshState()
    // The hero runs under the see-through top bar, so the spinner comes down below the bar, not behind it (#8).
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.refreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 48.dp),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            headerKeys.forEach { key ->
                item(key = key) {
                    when (key) {
                        KEY_HERO -> QuestionHero(
                            question = question,
                            channel = channel,
                            answerCount = maxOf(question.answerCount, state.answers.size),
                            pinCount = state.pinCount,
                            collapse = collapse,
                            onOpenTopic = onOpenTopic,
                            modifier = Modifier.onSizeChanged { onHeroHeight(it.height) },
                        )
                        KEY_EXPERT -> ExpertLine(
                            ExpertFields.questionLine(question),
                            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                        KEY_BODY -> QuestionBody(question.body)
                        KEY_ANSWERS_HEADER -> Row(
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                answersText(maxOf(question.answerCount, state.answers.size)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.loading && !state.refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        KEY_ANSWERS_ERROR -> answersError?.let { ErrorCard(it, onRetry) }
                        else -> Unit
                    }
                }
            }
            items(answers, key = { it.id }) { answer ->
                val position = positions[answer.id]
                val reply = if (replyQuotes && position != null) {
                    remember(answer.text, position, hideCarets) { ReplyRefs.resolve(answer.text, position, hideCarets) }
                } else {
                    null
                }
                AnswerCard(
                    answer = answer,
                    signedIn = signedIn,
                    expert = expert,
                    menu = answerMenu(answer),
                    showFlowerButton = canFlower(answer),
                    flowering = answer.id in state.flowering,
                    onVote = { direction -> onVote(answer, direction) },
                    onFlower = { onFlower(answer) },
                    reply = reply,
                    stipsOrder = stipsOrder,
                    isHidden = isHidden,
                    onJump = { jumpTo(it) },
                    onSwipeReply = onReply?.let { replyTo -> { replyTo(answer) } },
                    highlightKey = highlight?.takeIf { it.answerId == answer.id }?.nonce,
                    moderator = moderatorOf(answer),
                    modifier = Modifier.animateItem(),
                )
            }
            // Load times are a diagnostic, so they only show in expert mode (#8).
            if (expert) {
                state.loadedInMs?.let { ms ->
                    item(key = "timing") {
                        FooterNote(stringResource(R.string.loaded_in, String.format(Locale.getDefault(), "%.2f", ms / 1000.0)))
                    }
                }
            }
        }
    }
}

private const val KEY_HERO = "hero"
private const val KEY_EXPERT = "expert"
private const val KEY_BODY = "body"
private const val KEY_ANSWERS_HEADER = "answers-header"
private const val KEY_ANSWERS_ERROR = "answers-error"

/**
 * The cover as a background that fades into the page; the title sits on the fade. The photo drifts slower than the
 * page (parallax) but stays inside the header over a solid surface, so it never shows past the header's edge (#4).
 */
@Composable
private fun QuestionHero(
    question: Question,
    channel: Channel?,
    answerCount: Int,
    pinCount: Int,
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
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(surface),
    ) {
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
                        0.85f to surface,
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
            Spacer(Modifier.height(8.dp))
            QuestionMetaRow(
                question = question,
                answerCount = answerCount,
                pinCount = pinCount,
                avatarSize = 22.dp,
                clickableAuthor = true,
            )
            if (question.topics.isNotEmpty() || channel != null) {
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (channel != null) {
                        Text(
                            channelLabel(channel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
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

/** A ⋮ button with its menu; in the top bar it fades with the header ([collapse]). */
@Composable
private fun ActionMenuButton(entries: List<MenuEntry>, description: String, collapse: Float? = null) {
    if (entries.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        if (collapse != null) {
            BarIconButton(collapse = collapse, onClick = { open = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = description)
            }
        } else {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = description)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { entry ->
                val color = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    text = { Text(entry.label, color = color) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null, tint = color) },
                    onClick = {
                        open = false
                        entry.onClick()
                    },
                )
            }
        }
    }
}

/** How far an answer is dragged to reply to it (easy reply, v0.10), and how far it can go. */
private val SWIPE_THRESHOLD = 64.dp
private val SWIPE_MAX = 104.dp

@Composable
private fun AnswerCard(
    answer: Answer,
    signedIn: Boolean,
    expert: Boolean,
    menu: List<MenuEntry>,
    showFlowerButton: Boolean,
    flowering: Boolean,
    onVote: (Int) -> Unit,
    onFlower: () -> Unit,
    reply: ReplyRefs.Resolved?,
    stipsOrder: List<Answer>,
    isHidden: (Answer) -> Boolean,
    onJump: (Long) -> Unit,
    onSwipeReply: (() -> Unit)?,
    highlightKey: Long?,
    moderator: ModeratorBadge?,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val animations = LocalAppSettings.current.animations
    val density = LocalDensity.current
    val threshold = with(density) { SWIPE_THRESHOLD.toPx() }
    val maxDrag = with(density) { SWIPE_MAX.toPx() }

    // A jump lights the card up: two slow breaths over two seconds, then back to normal (v0.10).
    val glow = remember { Animatable(0f) }
    LaunchedEffect(highlightKey) {
        if (highlightKey == null) return@LaunchedEffect
        if (animations) {
            glow.snapTo(0f)
            glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            glow.animateTo(0.35f, tween(450, easing = FastOutSlowInEasing))
            glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            glow.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
        } else {
            glow.snapTo(1f)
            delay(HIGHLIGHT_MS)
            glow.snapTo(0f)
        }
    }

    // Easy reply (v0.10): dragging the card from left to right (in Hebrew too) past the line replies to it.
    var dragX by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    val currentOnSwipeReply by rememberUpdatedState(onSwipeReply)
    val dragState = rememberDraggableState { delta ->
        val resisted = if (dragX >= threshold) delta * 0.3f else delta
        dragX = (dragX + resisted).coerceIn(0f, maxDrag)
        val nowArmed = dragX >= threshold
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
    val replyLabel = stringResource(R.string.action_reply)

    // Organizations and Stips' own system answers get a thin blue outline next to their blue checkmark (#8).
    val border = if (answer.author.verified) BorderStroke(1.dp, VerifiedBlue.copy(alpha = 0.5f)) else null
    val container = lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primaryContainer, glow.value)
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = onSwipeReply != null,
                onDragStopped = {
                    if (dragX >= threshold) currentOnSwipeReply?.invoke()
                    armed = false
                    animate(dragX, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> dragX = value }
                },
            ),
    ) {
        if (onSwipeReply != null) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .absolutePadding(left = 12.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        val progress = (dragX / threshold).coerceIn(0f, 1f)
                        alpha = progress
                        scaleX = 0.6f + 0.4f * progress
                        scaleY = 0.6f + 0.4f * progress
                    },
            )
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = container),
            border = border,
            modifier = Modifier
                .fillMaxWidth()
                .absoluteOffset { IntOffset(dragX.roundToInt(), 0) }
                .semantics {
                    val action = onSwipeReply
                    if (action != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(replyLabel) {
                                action()
                                true
                            },
                        )
                    }
                },
        ) {
            Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(answer.author, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AuthorName(answer.author, MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
                            if (moderator != null) {
                                Spacer(Modifier.width(6.dp))
                                ModeratorPill(moderator)
                            }
                            if (answer.byAsker) {
                                Spacer(Modifier.width(6.dp))
                                AskerLabel(female = AnswerDisplay.askerIsFemale(answer))
                            }
                        }
                        val ago = agoText(answer.postedAt)
                        if (ago.isNotBlank()) {
                            Text(ago, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (expert) ExpertLine(ExpertFields.answerLine(answer))
                    }
                    if (answer.flowered) {
                        Icon(
                            Icons.Filled.LocalFlorist,
                            contentDescription = stringResource(R.string.flower_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                    ActionMenuButton(entries = menu, description = stringResource(R.string.answer_actions))
                }
                // What this answer replies to, like a quoted message (v0.10). A tap goes there.
                val quoted = reply?.targets?.mapNotNull { stipsOrder.getOrNull(it) }.orEmpty()
                if (quoted.isNotEmpty()) {
                    Column(
                        Modifier.padding(top = 8.dp, end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        quoted.forEach { target -> ReplyQuote(target, hidden = isHidden(target), onClick = { onJump(target.id) }) }
                    }
                }
                val inBody = remember(reply, stipsOrder) {
                    reply?.inBody?.mapNotNull { (range, index) -> stipsOrder.getOrNull(index)?.let { TextTarget(range, it.id) } }.orEmpty()
                }
                LinkifiedText(
                    reply?.body ?: answer.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 6.dp, end = 10.dp),
                    targets = inBody,
                    onTarget = onJump,
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
                    Spacer(Modifier.weight(1f))
                    if (showFlowerButton) {
                        TextButton(onClick = onFlower, enabled = !flowering) {
                            if (flowering) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.LocalFlorist, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.send_flower))
                        }
                    }
                }
            }
        }
    }
}

/** The answer a reply points at, as a quoted message: its author and its first lines; a tap goes there (v0.10). */
@Composable
private fun ReplyQuote(target: Answer, hidden: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val jumpLabel = stringResource(R.string.reply_jump)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
            .clickable(enabled = !hidden, onClickLabel = jumpLabel, onClick = onClick),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(
                if (hidden) stringResource(R.string.reply_hidden) else authorName(target.author),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!hidden) {
                Text(
                    ReplyRefs.snippet(target.text, hideLeading = LocalAppSettings.current.hideReplyCarets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "נאמן סטיפס" by a moderator's name, as on their profile: a shield in Stips' purple (v0.10). */
@Composable
private fun ModeratorPill(badge: ModeratorBadge) {
    val purple = specialNameColor()
    val label = stringResource(
        when (badge.rank) {
            ModeratorRank.Moderator -> if (badge.female) R.string.badge_moderator_f else R.string.badge_moderator_m
            ModeratorRank.Senior -> if (badge.female) R.string.badge_senior_moderator_f else R.string.badge_senior_moderator_m
        },
    )
    Surface(shape = RoundedCornerShape(50), color = purple.copy(alpha = 0.14f), contentColor = purple) {
        Row(
            Modifier.padding(start = 6.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** "שואל השאלה" by an answer the asker wrote, also when they asked anonymously (#8). */
@Composable
private fun AskerLabel(female: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            stringResource(if (female) R.string.asker_label_female else R.string.asker_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}

/** Above the composer while replying (v0.10): who to, their first line, and ✕ to stop replying. */
@Composable
private fun ReplyBar(replyTo: Answer, onCancel: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                stringResource(R.string.reply_to, authorName(replyTo.author)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                ReplyRefs.snippet(replyTo.text, hideLeading = LocalAppSettings.current.hideReplyCarets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.reply_cancel), modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Writing an answer. The options fit in one compact row (no sideways scrolling, #4); editing puts the cursor at
 * the end of your text and opens the keyboard.
 */
@Composable
private fun Composer(
    signedIn: Boolean,
    state: QuestionUiState,
    anonymousMode: Boolean,
    onDraft: (String) -> Unit,
    onAnonymous: (Boolean) -> Unit,
    onFollowOnAnswer: (Boolean) -> Unit,
    onAddLink: () -> Unit,
    onRemoveLink: () -> Unit,
    onCancelEdit: () -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    onSignIn: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
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
                var field by remember { mutableStateOf(TextFieldValue(state.draft, TextRange(state.draft.length))) }
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(state.draftVersion) {
                    // The app replaced the text (editing an answer, cancel, sent): cursor goes to the end.
                    if (field.text != state.draft) field = TextFieldValue(state.draft, TextRange(state.draft.length))
                }
                LaunchedEffect(editing?.id) {
                    if (editing != null) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                // Easy reply (v0.10): a swipe starts a reply, so the cursor goes to the text and the keyboard opens.
                LaunchedEffect(state.focusComposer) {
                    if (state.focusComposer > 0) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                val replyTo = state.replyTo
                if (replyTo != null && editing == null) ReplyBar(replyTo, onCancelReply)
                if (editing != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.editing_answer), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = onCancelEdit) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editing == null) {
                        val anonymousOn = state.anonymous || anonymousMode
                        FilterChip(
                            selected = anonymousOn,
                            enabled = !anonymousMode,
                            onClick = {
                                haptics.performHapticFeedback(if (state.anonymous) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                onAnonymous(!state.anonymous)
                            },
                            label = {
                                Text(stringResource(if (anonymousMode) R.string.chip_anonymous_mode else R.string.chip_anonymous), maxLines = 1)
                            },
                            leadingIcon = {
                                Icon(
                                    if (anonymousOn) Icons.Outlined.PersonOff else Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                    val link = state.draftLink
                    if (link == null) {
                        AssistChip(
                            onClick = onAddLink,
                            label = { Text(stringResource(R.string.chip_link), maxLines = 1) },
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
                                    modifier = Modifier.widthIn(max = 120.dp),
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
                            onClick = {
                                haptics.performHapticFeedback(if (state.followOnAnswer) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                onFollowOnAnswer(!state.followOnAnswer)
                            },
                            label = { Text(stringResource(R.string.chip_updates), maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    if (state.followOnAnswer) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = field,
                        onValueChange = { value ->
                            field = value
                            if (value.text != state.draft) onDraft(value.text)
                        },
                        placeholder = { Text(stringResource(R.string.composer_hint)) },
                        maxLines = 6,
                        isError = state.problem != null || state.postError != null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSend()
                        },
                        enabled = !state.posting && state.draft.isNotBlank(),
                    ) {
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

/** Reporting a question or an answer: Stips wants a few words on what's wrong (5 to 500 characters). */
@Composable
private fun ReportDialog(objType: String, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()
    val valid = trimmed.length in REPORT_MIN..REPORT_MAX
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (objType == "ask") R.string.report_question else R.string.report_answer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.report_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(REPORT_MAX) },
                    placeholder = { Text(stringResource(if (objType == "ask") R.string.report_hint_question else R.string.report_hint_answer)) },
                    supportingText = { Text("${trimmed.length} / $REPORT_MAX") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSend(trimmed) }, enabled = valid) { Text(stringResource(R.string.report_send)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val REPORT_MIN = 5
private const val REPORT_MAX = 500

@Composable
private fun ConfirmDialog(title: String, body: String?, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = if (body != null) {
            { Text(body) }
        } else {
            null
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Editing your own question: title (10–120 characters), details (up to 2000) and the link; tags stay as they are. */
@Composable
private fun EditQuestionDialog(question: Question, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var title by rememberSaveable { mutableStateOf(question.title) }
    var body by rememberSaveable { mutableStateOf(question.body) }
    var link by rememberSaveable { mutableStateOf(question.link.orEmpty()) }
    val titleOk = title.trim().length in 10..120 && !AnswerRules.hasRepeatedLetters(title)
    val bodyOk = body.length <= 2000 && !AnswerRules.hasRepeatedLetters(body)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_question)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text(stringResource(R.string.ask_title_label)) },
                    supportingText = { Text("${title.trim().length} / 120") },
                    isError = !titleOk,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(2000) },
                    label = { Text(stringResource(R.string.ask_body_label)) },
                    minLines = 2,
                    maxLines = 6,
                    isError = !bodyOk,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(stringResource(R.string.ask_link_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, body, link) }, enabled = titleOk && bodyOk) { Text(stringResource(R.string.action_save_changes)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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
