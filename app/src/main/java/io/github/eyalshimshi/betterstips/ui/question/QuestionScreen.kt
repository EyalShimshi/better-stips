package io.github.eyalshimshi.betterstips.ui.question

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.FooterNote
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.answersText
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.components.channelLabel
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.shareText
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.SeededTheme
import io.github.eyalshimshi.betterstips.util.AnswerRules
import java.util.Locale

@Composable
fun QuestionScreen(
    questionId: Long,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm = appViewModel { QuestionViewModel(it.repository, questionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val saved by container.saved.items.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val question = state.question
    val channel = question?.let { q -> channels.firstOrNull { it.id == q.channelId } }
    val context = LocalContext.current

    SeededTheme(seed = channel?.seedColor) {
        val snackbar = remember { SnackbarHostState() }
        val postedText = stringResource(R.string.answer_posted)
        val voteFailedText = stringResource(R.string.vote_failed)
        LaunchedEffect(state.message) {
            val message = state.message ?: return@LaunchedEffect
            snackbar.showSnackbar(if (message == QuestionMessage.AnswerPosted) postedText else voteFailedText)
            vm.consumeMessage()
        }
        val isSaved = question != null && saved.any { it.id == question.id }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        if (question != null) {
                            IconButton(onClick = { container.saved.toggle(question) }) {
                                Icon(
                                    if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = stringResource(if (isSaved) R.string.action_unsave else R.string.action_save),
                                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { shareText(context, question.title + "\n" + question.shareUrl) }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                            }
                            IconButton(onClick = { openInBrowser(context, question.shareUrl) }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.action_open_site))
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (question != null && state.error?.code != ApiException.NOT_FOUND) {
                    Composer(
                        signedIn = session.signedIn,
                        state = state,
                        onDraft = vm::setDraft,
                        onAnonymous = vm::setAnonymous,
                        onSend = vm::post,
                        onSignIn = onSignIn,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val error = state.error
            when {
                error?.code == ApiException.NOT_FOUND -> Box(Modifier.padding(padding)) {
                    MessageCard(stringResource(R.string.error_not_found), null, stringResource(R.string.action_go_back), onBack)
                }
                question == null && error != null -> Box(Modifier.padding(padding)) { ErrorCard(error, onRetry = vm::load) }
                question == null -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                else -> QuestionContent(
                    question = question,
                    channel = channel,
                    state = state,
                    signedIn = session.signedIn,
                    onOpenTopic = onOpenTopic,
                    onVote = vm::vote,
                    onRetry = vm::load,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun QuestionContent(
    question: Question,
    channel: Channel?,
    state: QuestionUiState,
    signedIn: Boolean,
    onOpenTopic: (String) -> Unit,
    onVote: (Answer, Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    LazyColumn(modifier.fillMaxSize()) {
        val cover = question.cover
        if (cover != null && showImages) {
            item(key = "cover") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    AsyncImage(
                        model = cover.largeUrl,
                        contentDescription = null,
                        imageLoader = loader,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    if (cover is Cover.Unsplash && cover.creatorName != null) {
                        Text(
                            stringResource(R.string.photo_credit, cover.creatorName),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item(key = "title") {
            Text(
                question.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }
        if (question.body.isNotBlank()) {
            item(key = "body") {
                LinkifiedText(
                    question.body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
        }
        if (question.topics.isNotEmpty()) {
            item(key = "topics") {
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    question.topics.forEach { topic ->
                        AssistChip(onClick = { onOpenTopic(topic) }, label = { Text("#$topic") })
                    }
                }
            }
        }
        item(key = "meta") {
            val meta = listOfNotNull(
                authorName(question.author),
                agoText(question.postedAt).takeIf { it.isNotBlank() },
                channel?.let { channelLabel(it) },
            ).joinToString(" · ")
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(question.author, 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "answers-header") {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    answersText(maxOf(question.answerCount, state.answers.size)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        val answersError = state.answersError
        if (answersError != null && state.answers.isEmpty()) {
            item(key = "answers-error") { ErrorCard(answersError, onRetry) }
        }
        items(state.answers, key = { it.id }) { answer ->
            AnswerCard(answer, signedIn, onVote = { direction -> onVote(answer, direction) })
        }
        state.loadedInMs?.let { ms ->
            item(key = "timing") {
                FooterNote(stringResource(R.string.loaded_in, String.format(Locale.getDefault(), "%.2f", ms / 1000.0)))
            }
        }
    }
}

@Composable
private fun AnswerCard(answer: Answer, signedIn: Boolean, onVote: (Int) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(answer.author, 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    listOf(authorName(answer.author), agoText(answer.postedAt)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (answer.flowered) {
                    Icon(
                        Icons.Filled.LocalFlorist,
                        contentDescription = stringResource(R.string.flower_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            LinkifiedText(
                answer.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
            )
            answer.links.forEach { link ->
                Text(
                    link.title ?: link.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { uriHandler.openUri(link.url) },
                )
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
private fun Composer(
    signedIn: Boolean,
    state: QuestionUiState,
    onDraft: (String) -> Unit,
    onAnonymous: (Boolean) -> Unit,
    onSend: () -> Unit,
    onSignIn: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (!signedIn) {
                FilledTonalButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sign_in_to_answer))
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = state.anonymous, onValueChange = onAnonymous, role = Role.Switch)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.composer_anonymous),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.anonymous, onCheckedChange = null)
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
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
