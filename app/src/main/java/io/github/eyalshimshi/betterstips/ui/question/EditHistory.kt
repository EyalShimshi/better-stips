package io.github.eyalshimshi.betterstips.ui.question

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerRevision
import io.github.eyalshimshi.betterstips.ui.components.ErrorCard
import io.github.eyalshimshi.betterstips.ui.components.LinkifiedText
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.EditHistory
import io.github.eyalshimshi.betterstips.util.Links
import kotlinx.coroutines.CancellationException

/**
 * An edited answer's history (v0.13, #10), as the site's "גרסאות קודמות": every version, newest first, with who made the
 * edit that produced it ("נערך על ידי העונה", "… נאמן") and when. Read from Stips when the sheet opens.
 */
@Composable
internal fun EditHistorySheet(answer: Answer, onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    var attempt by remember { mutableIntStateOf(0) }
    val result by produceState<Result<List<AnswerRevision>>?>(initialValue = null, answer.id, attempt) {
        value = null
        value = try {
            Result.success(container.repository.answerRevisions(answer.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.edit_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            val loaded = result
            when {
                loaded == null -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                loaded.isFailure -> ErrorCard(
                    (loaded.exceptionOrNull() ?: IllegalStateException()).asApiException(),
                    onRetry = { attempt++ },
                )
                else -> {
                    val revisions = loaded.getOrDefault(emptyList())
                    if (revisions.isEmpty()) {
                        Text(
                            stringResource(R.string.edit_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    EditHistory.versions(answer.text, answer.links, revisions).forEach { version ->
                        VersionCard(version, answer)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionCard(version: EditHistory.Version, answer: Answer) {
    val uriHandler = LocalUriHandler.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (version.current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    when {
                        version.current -> R.string.edit_history_now
                        version.original -> R.string.edit_history_original
                        else -> R.string.edit_history_earlier
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            val by = version.editedBy
            val parts = if (by == null) {
                listOf(stringResource(R.string.edit_history_posted), agoText(answer.postedAt))
            } else {
                listOf(
                    stringResource(
                        when (by) {
                            EditHistory.Editor.Owner -> R.string.edit_history_by_owner
                            EditHistory.Editor.Moderator -> R.string.edit_history_by_moderator
                            EditHistory.Editor.Stips -> R.string.edit_history_by_stips
                            EditHistory.Editor.Unknown -> R.string.edit_history_by_unknown
                        },
                    ),
                    version.editedAt?.let { agoText(it) } ?: version.editedAtText.orEmpty(),
                )
            }
            val line = parts.filter { it.isNotBlank() }.joinToString(" · ")
            Text(line, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkifiedText(version.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
            version.links.forEach { link ->
                Row(
                    Modifier
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
        }
    }
}
