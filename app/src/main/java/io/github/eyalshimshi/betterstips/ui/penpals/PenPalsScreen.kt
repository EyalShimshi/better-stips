package io.github.eyalshimshi.betterstips.ui.penpals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.PenPalPost
import io.github.eyalshimshi.betterstips.ui.components.AuthorName
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.PresenceAvatar
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.util.AskRules

/** חברים לעט (#4): short posts from people looking for someone to talk to. Tapping one opens a private chat. */
@Composable
fun PenPalsScreen(onOpenConversation: (Long, String) -> Unit, onSignIn: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { PenPalsViewModel(it.repository, it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val blockVersion by container.blocks.version.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var reportTarget by remember { mutableStateOf<PenPalPost?>(null) }
    val texts = mapOf(
        PenPalMessage.Posted to stringResource(R.string.penpals_posted),
        PenPalMessage.Deleted to stringResource(R.string.penpals_deleted),
        PenPalMessage.Reported to stringResource(R.string.report_sent),
        PenPalMessage.Hidden to stringResource(R.string.content_hidden),
        PenPalMessage.Failed to stringResource(R.string.action_failed),
    )
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(texts[message].orEmpty())
        vm.consumeMessage()
    }
    val visible = remember(state.items, blockVersion) {
        state.items.filterNot { container.blocks.isBlocked("penfriendsitem", it.id, it.author.userId) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.penpals_title)) }) },
        floatingActionButton = {
            if (!state.closedForAge) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.penpals_new)) },
                    icon = { Icon(Icons.Outlined.AddComment, contentDescription = null) },
                    onClick = vm::openComposer,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (state.closedForAge) {
            Column(Modifier.padding(padding)) {
                MessageCard(stringResource(R.string.penpals_closed_title), stringResource(R.string.penpals_closed_body), null, null)
            }
        } else {
            PenPalsList(state, visible, session.signedIn, session.userId, padding, vm, onOpenConversation, onSignIn) { reportTarget = it }
        }
    }
    PenPalsDialogs(state, vm, reportTarget, onSignIn) { reportTarget = null }
}

@Composable
private fun PenPalsList(
    state: PenPalsState,
    visible: List<PenPalPost>,
    signedIn: Boolean,
    myUserId: Long?,
    padding: PaddingValues,
    vm: PenPalsViewModel,
    onOpenConversation: (Long, String) -> Unit,
    onSignIn: () -> Unit,
    onReport: (PenPalPost) -> Unit,
) {
        PullToRefreshBox(
            isRefreshing = state.refreshing && state.page > 0,
            onRefresh = vm::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
                item(key = "intro") {
                    Text(
                        stringResource(R.string.penpals_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                val error = state.error
                when {
                    state.page == 0 && error != null -> item(key = "error") { ErrorCard(error, onRetry = vm::refresh) }
                    state.page == 0 -> item(key = "loading") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    visible.isEmpty() -> item(key = "empty") {
                        Text(
                            stringResource(R.string.penpals_empty),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                        )
                    }
                    else -> {
                        items(visible, key = { it.id }) { post ->
                            PenPalCard(
                                post = post,
                                isMine = post.author.userId != null && post.author.userId == myUserId,
                                onOpen = {
                                    val userId = post.author.userId
                                    when {
                                        !signedIn -> onSignIn()
                                        userId != null && userId != myUserId -> onOpenConversation(userId, post.author.nickname)
                                        else -> Unit
                                    }
                                },
                                onReport = { if (signedIn) onReport(post) else onSignIn() },
                                onHide = { vm.hide(post, wholeUser = false) },
                                onHideUser = { vm.hide(post, wholeUser = true) },
                                onDelete = { vm.delete(post) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(key = "more") {
                            when {
                                state.loadingMore -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }
                                state.moreFailed -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) { TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.action_retry)) } }
                                !state.end -> LaunchedEffect(state.page) { vm.loadMore() }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun PenPalsDialogs(state: PenPalsState, vm: PenPalsViewModel, reportTarget: PenPalPost?, onSignIn: () -> Unit, onReportDone: () -> Unit) {
    if (state.composerOpen) {
        PenPalComposer(state = state, vm = vm, onSignIn = {
            vm.closeComposer()
            onSignIn()
        })
    }
    if (reportTarget != null) {
        PenPalReportDialog(
            onDismiss = onReportDone,
            onSend = { reason ->
                onReportDone()
                vm.report(reportTarget, reason)
            },
        )
    }
}

@Composable
private fun PenPalCard(
    post: PenPalPost,
    isMine: Boolean,
    onOpen: () -> Unit,
    onReport: () -> Unit,
    onHide: () -> Unit,
    onHideUser: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PresenceAvatar(post.author, 36.dp, ring = MaterialTheme.colorScheme.surfaceContainerLow)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    AuthorName(post.author, MaterialTheme.typography.labelLarge)
                    Text(agoText(post.time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_actions))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (isMine) {
                            if (post.canDelete) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.penpals_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menu = false
                                        onDelete()
                                    },
                                )
                            }
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.penpals_report)) },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    onReport()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.penpals_hide)) },
                                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    onHide()
                                },
                            )
                            if (post.author.userId != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.hide_user_content)) },
                                    leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        onHideUser()
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                post.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, end = 10.dp),
            )
        }
    }
}

@Composable
private fun PenPalComposer(state: PenPalsState, vm: PenPalsViewModel, onSignIn: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest = vm::closeComposer) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.penpals_new), style = MaterialTheme.typography.titleLarge)
            val block = state.block
            when {
                state.checking -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                block is PostBlock.SignedOut -> {
                    Text(stringResource(R.string.penpals_sign_in), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onSignIn) { Text(stringResource(R.string.action_sign_in)) }
                }
                block is PostBlock.NeedFlowers -> Text(
                    stringResource(R.string.penpals_need_flowers, block.have),
                    style = MaterialTheme.typography.bodyLarge,
                )
                block is PostBlock.Cooldown -> Text(
                    if (block.nextInMinutes != null) {
                        stringResource(R.string.penpals_cooldown, block.nextInMinutes)
                    } else {
                        stringResource(R.string.penpals_cooldown_unknown)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> {
                    val problem = AskRules.penPalProblem(state.draft)
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = vm::setDraft,
                        placeholder = { Text(stringResource(R.string.penpals_hint)) },
                        supportingText = {
                            Text(
                                if (problem == AskRules.Problem.RepeatedLetters) {
                                    stringResource(R.string.answer_problem_repeated)
                                } else {
                                    "${state.draft.trim().length} / ${AskRules.PEN_PAL_MAX}"
                                },
                            )
                        },
                        isError = problem == AskRules.Problem.RepeatedLetters,
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val error = state.postError
                    if (error != null) {
                        Text(errorMessage(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = vm::useLastMessage) { Text(stringResource(R.string.penpals_last_message)) }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                vm.post()
                            },
                            enabled = problem == null && !state.posting,
                        ) {
                            if (state.posting) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(stringResource(R.string.action_send))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun PenPalReportDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.penpals_report)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(500) },
                placeholder = { Text(stringResource(R.string.report_hint_penpal)) },
                supportingText = { Text("${trimmed.length} / 500") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSend(trimmed) }, enabled = trimmed.length in 5..500) { Text(stringResource(R.string.report_send)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
