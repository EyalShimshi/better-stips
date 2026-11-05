package io.github.eyalshimshi.betterstips.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Instant

/** Pretty-printed JSON for expert mode; compact input that doesn't parse is shown as it came. */
object ExpertJson {
    private val pretty = Json { prettyPrint = true }

    fun format(raw: String): String = try {
        pretty.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(raw))
    } catch (e: Exception) {
        raw
    }
}

/** The fields the app read from Stips, as key/value rows for the expert details dialog (#8). Pure, for tests. */
object ExpertFields {
    fun questionLine(question: Question): String = buildString {
        append("ask #").append(question.id)
        append(" · channel ").append(question.channelId)
        question.author.userId?.let { append(" · user ").append(it) }
    }

    fun answerLine(answer: Answer): String = buildString {
        append("ans #").append(answer.id)
        answer.author.userId?.let { append(" · user ").append(it) }
    }

    fun question(question: Question, answersLoaded: Int): List<Pair<String, String>> = listOf(
        "id" to question.id.toString(),
        "url" to question.shareUrl,
        "channel" to question.channelId.toString(),
        "posted" to question.postedAt.orDash(),
        "answers" to "${question.answerCount} (loaded $answersLoaded)",
        "pins" to question.pinCount.toString(),
        "topics" to question.topics.joinToString().ifEmpty { "-" },
        "link" to (question.link ?: "-"),
        "cover" to (question.cover?.fullUrl ?: "-"),
        "owner" to question.isOwner.toString(),
        "canEdit" to question.canEdit.toString(),
        "canDelete" to question.canDelete.toString(),
        "pinned" to question.userPinned.toString(),
    ) + author(question.author)

    fun answer(answer: Answer): List<Pair<String, String>> = listOf(
        "id" to answer.id.toString(),
        "question" to answer.questionId.toString(),
        "posted" to answer.postedAt.orDash(),
        "score" to answer.score.toString(),
        "myVote" to answer.userVote.toString(),
        "flowered" to answer.flowered.toString(),
        "byAsker" to answer.byAsker.toString(),
        "links" to answer.links.joinToString { it.url }.ifEmpty { "-" },
        "owner" to answer.isOwner.toString(),
        "canEdit" to answer.canEdit.toString(),
        "canDelete" to answer.canDelete.toString(),
    ) + author(answer.author)

    fun profile(profile: UserProfile, page: ProfilePage?): List<Pair<String, String>> = buildList {
        add("userId" to profile.userId.toString())
        add("url" to "$SITE_URL/profile/${profile.userId}")
        add("nickname" to profile.nickname)
        add("points" to profile.points.toString())
        add("female" to profile.female.toString())
        add("online" to profile.online.toString())
        add("photo" to (profile.photoStamp ?: "-"))
        add("branding" to (profile.branding ?: "-"))
        add("highlightName" to profile.highlightName.toString())
        add("platform" to profile.platform.toString())
        add("organization" to profile.avatar.organization.toString())
        if (page != null) {
            add("questions" to page.questions.toString())
            add("answers" to page.answers.toString())
            add("flowers" to page.flowers.toString())
            add("activeSince" to (page.activeSince ?: "-"))
            add("age" to (page.age?.toString() ?: "-"))
            add("badges" to (page.badges + page.customBadges).joinToString().ifEmpty { "-" })
            add("hideAge" to page.hideAge.toString())
            add("hideStats" to page.hideStats.toString())
        }
    }

    private fun author(author: Author): List<Pair<String, String>> = listOf(
        "author" to author.nickname,
        "authorId" to (author.userId?.toString() ?: "-"),
        "anonymous" to author.anonymous.toString(),
        "verified" to when {
            author.platform -> "stips"
            author.organization -> "organization"
            else -> "-"
        },
        "branding" to (author.branding ?: "-"),
    )

    private fun Instant?.orDash(): String = this?.toString() ?: "-"
}

/** A small monospace line of ids under an item, in expert mode (#8). */
@Composable
fun ExpertLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier,
    )
}

/**
 * Everything Stips sent about an item, for expert mode (#8): the fields the app reads, then each raw object
 * pretty-printed and selectable. *Copy* puts it all on the clipboard.
 */
@Composable
fun ServerDetailsDialog(title: String, fields: List<Pair<String, String>>, raw: List<Pair<String, String?>>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pretty = remember(raw) { raw.mapNotNull { (label, json) -> json?.let { label to ExpertJson.format(it) } } }
    val all = remember(fields, pretty) {
        buildString {
            fields.forEach { (key, value) -> appendLine("$key: $value") }
            pretty.forEach { (label, json) ->
                appendLine()
                appendLine("$label:")
                appendLine(json)
            }
        }
    }
    val copiedLabel = stringResource(R.string.expert_details)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fields.forEach { (key, value) ->
                        Row(Modifier.padding(vertical = 1.dp)) {
                            Text(
                                key,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.widthIn(min = 96.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(value, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                    pretty.forEach { (label, json) ->
                        Spacer(Modifier.height(12.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(json, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { copyText(context, copiedLabel, all) }) { Text(stringResource(R.string.action_copy)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
