package io.github.eyalshimshi.betterstips.ui.question

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoNotTouch
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.QuestionSwipe
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.QuestionRow
import io.github.eyalshimshi.betterstips.ui.components.SwipeActionsBox
import io.github.eyalshimshi.betterstips.ui.components.SwipeSpec
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.shareText
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.AnswerDisplay
import io.github.eyalshimshi.betterstips.util.SwipeRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A thread being exported from a list (#18): its answers once they've loaded. */
internal data class ExportJob(val question: Question, val answers: List<Answer>? = null, val hidden: Set<Long> = emptySet())

/**
 * What swiping a question in a list can do (#18), from any list: pin, get updates, share, copy its link, open it on
 * Stips, export it, report or hide it. Dialogs (the report's words, hiding someone, the export) and messages show
 * over the whole app. Provided by [QuestionActionsHost].
 */
@Stable
class QuestionActions internal constructor(
    private val container: AppContainer,
    private val context: Context,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
) {
    internal var onSignIn: () -> Unit = {}
    internal var reportFor by mutableStateOf<Question?>(null)
    internal var hideUserFor by mutableStateOf<Question?>(null)
    internal var export by mutableStateOf<ExportJob?>(null)
    private var exportWork: Job? = null

    /** Pins changed by a swipe, until the list reloads with Stips' own. */
    private val pinned = mutableStateMapOf<Long, Boolean>()

    private val signedIn: Boolean get() = container.session.session.value.signedIn

    fun isPinned(question: Question): Boolean = pinned[question.id] ?: question.userPinned

    fun available(question: Question, action: QuestionSwipe): Boolean = SwipeRules.questionAvailable(
        action = action,
        isOwner = question.isOwner,
        authorId = question.author.userId,
        anonymous = question.author.anonymous,
        me = container.session.session.value.userId,
    )

    fun run(question: Question, action: QuestionSwipe) {
        when (action) {
            QuestionSwipe.None -> Unit
            QuestionSwipe.Pin -> signedInOnly { togglePin(question) }
            QuestionSwipe.Follow -> signedInOnly { follow(question) }
            QuestionSwipe.Share -> shareText(context, question.title + "\n" + question.shareUrl)
            QuestionSwipe.CopyLink -> copyLink(question)
            QuestionSwipe.OpenOnSite -> openInBrowser(context, question.shareUrl)
            QuestionSwipe.Export -> startExport(question)
            QuestionSwipe.Report -> signedInOnly { reportFor = question }
            QuestionSwipe.Hide -> hide(question, null)
            QuestionSwipe.HideUser -> hideUserFor = question
        }
    }

    private fun signedInOnly(action: () -> Unit) {
        if (signedIn) action() else onSignIn()
    }

    private fun message(text: Int) {
        scope.launch { snackbar.showSnackbar(context.getString(text)) }
    }

    private fun togglePin(question: Question) {
        val pin = !isPinned(question)
        pinned[question.id] = pin
        scope.launch {
            try {
                container.repository.setPinned(question.id, pin)
                container.repository.updateCachedQuestion(question.id) { it.copy(userPinned = pin) }
                snackbar.showSnackbar(context.getString(if (pin) R.string.swipe_pinned else R.string.swipe_unpinned))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pinned[question.id] = !pin
                snackbar.showSnackbar(context.getString(R.string.action_failed))
            }
        }
    }

    /** Turns updates on; when they already are, offers to turn them off. */
    private fun follow(question: Question) {
        scope.launch {
            try {
                val following = container.repository.isFollowing(question.id) == true
                if (following) {
                    val result = snackbar.showSnackbar(
                        context.getString(R.string.swipe_already_following),
                        actionLabel = context.getString(R.string.stop_updates),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        container.repository.setFollowing(question.id, false)
                        snackbar.showSnackbar(context.getString(R.string.following_off))
                    }
                } else {
                    container.repository.setFollowing(question.id, true)
                    val result = snackbar.showSnackbar(
                        context.getString(R.string.following_on),
                        actionLabel = context.getString(R.string.swipe_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) container.repository.setFollowing(question.id, false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(context.getString(R.string.action_failed))
            }
        }
    }

    private fun copyLink(question: Question) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Stips", question.shareUrl))
        message(R.string.swipe_link_copied)
    }

    internal fun sendReport(question: Question, reason: String) {
        scope.launch {
            try {
                container.repository.report("ask", question.id, reason)
                snackbar.showSnackbar(context.getString(R.string.report_sent))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(context.getString(R.string.action_failed))
            }
        }
    }

    /**
     * Hides the question (and everything by [authorId] when given) on this phone at once, with *Undo* on its message,
     * as in Gmail: a swipe is easy to make by mistake. Stips is told once the message goes without it.
     */
    internal fun hide(question: Question, authorId: Long?) {
        container.blocks.block("ask", question.id, authorId)
        scope.launch {
            var undo = false
            try {
                undo = snackbar.showSnackbar(
                    context.getString(R.string.content_hidden),
                    actionLabel = context.getString(R.string.swipe_undo),
                    duration = SnackbarDuration.Short,
                ) == SnackbarResult.ActionPerformed
            } finally {
                // Also when the message is cut short (the phone turned): the hide then stays.
                if (undo) {
                    container.blocks.unblock("ask", question.id, authorId)
                } else {
                    container.repository.tellStipsHiddenLater(container.appScope, "ask", question.id, authorId)
                }
            }
        }
    }

    /** Reads the answers, then shows the export (#16) as the question's ⋮ menu does. */
    private fun startExport(question: Question) {
        exportWork?.cancel()
        export = ExportJob(question)
        exportWork = scope.launch {
            try {
                val answers = AnswerDisplay.withAskerNickname(question, container.repository.answers(question.id))
                val hidden = answers.filter { container.blocks.isBlocked("ans", it.id, it.author.userId) }.map { it.id }.toSet()
                if (export?.question?.id == question.id) export = ExportJob(question, answers, hidden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                export = null
                snackbar.showSnackbar(context.getString(R.string.action_failed))
            }
        }
    }

    internal fun cancelExport() {
        exportWork?.cancel()
        export = null
    }

    internal fun channelName(question: Question): String? =
        container.channels.channels.value.firstOrNull { it.id == question.channelId }?.name
}

/** The app's [QuestionActions] (#18); null where none is provided (previews, tests). */
val LocalQuestionActions = staticCompositionLocalOf<QuestionActions?> { null }

/**
 * Tells Stips about a hide made on this phone once its *Undo* has passed (#18). Runs on [scope], the app's, so
 * leaving the screen or turning the phone doesn't stop it.
 */
internal fun StipsRepository.tellStipsHiddenLater(scope: CoroutineScope, objType: String, id: Long, authorId: Long?) {
    scope.launch {
        try {
            tellStipsHidden(objType, id, authorId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // It's hidden here already; Stips hearing of it is a bonus.
        }
    }
}

/** Provides [QuestionActions] to everything inside, and shows their dialogs. [onSignIn] opens the sign-in screen. */
@Composable
fun QuestionActionsHost(snackbar: SnackbarHostState, onSignIn: () -> Unit, content: @Composable () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = remember(container, context, scope, snackbar) { QuestionActions(container, context, scope, snackbar) }
    val currentOnSignIn by rememberUpdatedState(onSignIn)
    actions.onSignIn = { currentOnSignIn() }
    CompositionLocalProvider(LocalQuestionActions provides actions) { content() }

    actions.reportFor?.let { question ->
        ReportDialog(
            objType = "ask",
            onDismiss = { actions.reportFor = null },
            onSend = { reason ->
                actions.reportFor = null
                actions.sendReport(question, reason)
            },
        )
    }
    actions.hideUserFor?.let { question ->
        ConfirmDialog(
            title = stringResource(R.string.hide_user_title, question.author.nickname),
            body = stringResource(R.string.hide_user_body),
            confirm = stringResource(R.string.hide_user_confirm),
            onConfirm = {
                actions.hideUserFor = null
                actions.hide(question, question.author.userId)
            },
            onDismiss = { actions.hideUserFor = null },
        )
    }
    actions.export?.let { job ->
        val answers = job.answers
        if (answers == null) {
            AlertDialog(
                onDismissRequest = actions::cancelExport,
                title = { Text(stringResource(R.string.export_title)) },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text(stringResource(R.string.swipe_export_loading))
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = actions::cancelExport) { Text(stringResource(R.string.action_cancel)) } },
            )
        } else {
            ExportThreadDialog(
                question = job.question,
                answers = answers,
                hidden = job.hidden,
                channel = actions.channelName(job.question),
                onDismiss = actions::cancelExport,
            )
        }
    }
}

/** What a swipe on [question] does with [action] (#18), or null when it doesn't apply to this question. */
@Composable
fun QuestionActions.swipeSpec(question: Question, action: QuestionSwipe): SwipeSpec? {
    if (!available(question, action)) return null
    val pinned = isPinned(question)
    val label = when (action) {
        QuestionSwipe.Pin -> stringResource(if (pinned) R.string.unpin else R.string.pin)
        else -> stringResource(questionSwipeLabel(action))
    }
    val destructive = action == QuestionSwipe.Report || action == QuestionSwipe.Hide || action == QuestionSwipe.HideUser
    return SwipeSpec(label, questionSwipeIcon(action, pinned), destructive) { run(question, action) }
}

/** The icon for a question's swipe action (#18); [pinned]: the question is pinned, so the swipe unpins it. */
fun questionSwipeIcon(action: QuestionSwipe, pinned: Boolean = false): ImageVector = when (action) {
    QuestionSwipe.Pin -> if (pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin
    QuestionSwipe.Follow -> Icons.Filled.NotificationsActive
    QuestionSwipe.Share -> Icons.Filled.Share
    QuestionSwipe.CopyLink -> Icons.Outlined.Link
    QuestionSwipe.OpenOnSite -> Icons.Outlined.OpenInBrowser
    QuestionSwipe.Export -> Icons.Outlined.ContentCopy
    QuestionSwipe.Report -> Icons.Outlined.Flag
    QuestionSwipe.Hide -> Icons.Outlined.VisibilityOff
    QuestionSwipe.HideUser -> Icons.Outlined.Block
    QuestionSwipe.None -> Icons.Outlined.DoNotTouch
}

/** The icon for an answer's left-swipe action, as its ⋮ menu has it (v0.16, for the pictures in Settings and the intro). */
fun answerSwipeIcon(action: AnswerSwipe): ImageVector = when (action) {
    AnswerSwipe.None -> Icons.Outlined.DoNotTouch
    AnswerSwipe.Edit -> Icons.Filled.Edit
    AnswerSwipe.Detach -> Icons.Outlined.PersonOff
    AnswerSwipe.Delete -> Icons.Outlined.Delete
    AnswerSwipe.History -> Icons.Outlined.History
    AnswerSwipe.Report -> Icons.Outlined.Flag
    AnswerSwipe.Hide -> Icons.Outlined.VisibilityOff
    AnswerSwipe.HideUser -> Icons.Outlined.Block
}

/** The words for an answer's left-swipe action, as Settings lists them (#18). */
fun answerSwipeLabel(action: AnswerSwipe): Int = when (action) {
    AnswerSwipe.None -> R.string.swipe_none
    AnswerSwipe.Edit -> R.string.edit_answer
    AnswerSwipe.Detach -> R.string.detach_answer
    AnswerSwipe.Delete -> R.string.delete_answer
    AnswerSwipe.History -> R.string.edit_history
    AnswerSwipe.Report -> R.string.report_answer
    AnswerSwipe.Hide -> R.string.hide_answer
    AnswerSwipe.HideUser -> R.string.hide_user_content
}

/** The words for a question's swipe action, as Settings lists them (#18). */
fun questionSwipeLabel(action: QuestionSwipe): Int = when (action) {
    QuestionSwipe.None -> R.string.swipe_none
    QuestionSwipe.Pin -> R.string.swipe_pin
    QuestionSwipe.Follow -> R.string.get_updates
    QuestionSwipe.Share -> R.string.action_share
    QuestionSwipe.CopyLink -> R.string.swipe_copy_link
    QuestionSwipe.OpenOnSite -> R.string.action_open_site
    QuestionSwipe.Export -> R.string.export_thread
    QuestionSwipe.Report -> R.string.report_question
    QuestionSwipe.Hide -> R.string.hide_question
    QuestionSwipe.HideUser -> R.string.hide_user_content
}

/** A question in a list, with the swipes picked in Settings (#18). */
@Composable
fun SwipeableQuestionRow(question: Question, channel: Channel?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val actions = LocalQuestionActions.current
    val settings = LocalAppSettings.current
    val right = actions?.swipeSpec(question, settings.swipeRight)
    val left = actions?.swipeSpec(question, settings.swipeLeft)
    SwipeActionsBox(right, left, modifier) { QuestionRow(question, channel, onClick) }
}
