package io.github.eyalshimshi.betterstips.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.LinkPreviews
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.CoverArt
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.QuestionIcon
import io.github.eyalshimshi.betterstips.ui.components.SpecialBadgePill
import io.github.eyalshimshi.betterstips.ui.components.answersText
import io.github.eyalshimshi.betterstips.ui.components.authorName
import io.github.eyalshimshi.betterstips.ui.user.badgeLabel
import io.github.eyalshimshi.betterstips.util.ChatLinks
import io.github.eyalshimshi.betterstips.util.Links

/**
 * What [target] shows, read once per app session and only once its card is on screen; null until it's known, and when
 * it couldn't be read now ([tried] then turns true).
 */
private class CardPreview(val preview: LinkPreviews.Preview?, val tried: Boolean)

@Composable
private fun rememberPreview(target: ChatLinks.Target): CardPreview {
    val previews = LocalAppContainer.current.linkPreviews
    var preview by remember(target) { mutableStateOf(previews.cached(target)) }
    var tried by remember(target) { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (preview == null) {
            preview = previews.load(target)
            tried = true
        }
    }
    return CardPreview(preview, tried)
}

/**
 * The card for a Stips question or profile link in a chat bubble (v0.18): the question's photo, title, asker and
 * answers, or the person's photo, name, badge, credibility and counts. A tap opens it in the app; a long-press is the
 * bubble's (copying the message).
 */
@Composable
internal fun ChatLinkCard(target: ChatLinks.Target, content: Color, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val card = rememberPreview(target)
    val openLabel = stringResource(if (target is ChatLinks.Target.Question) R.string.link_card_open_question else R.string.link_card_open_profile)
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(content.copy(alpha = 0.07f))
            .combinedClickable(onClickLabel = openLabel, onClick = { uriHandler.openUri(target.link) }, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        when (val preview = card.preview) {
            is LinkPreviews.Preview.OfQuestion -> QuestionCardContent(preview.question, content)
            is LinkPreviews.Preview.OfPerson -> PersonCardContent(preview.profile, preview.page, content)
            LinkPreviews.Preview.Missing -> FallbackCardContent(target, content, loading = false, missing = true)
            null -> FallbackCardContent(target, content, loading = !card.tried, missing = false)
        }
    }
}

@Composable
private fun QuestionCardContent(question: Question, content: Color) {
    val channels by LocalAppContainer.current.channels.channels.collectAsStateWithLifecycle()
    val seed = channels.firstOrNull { it.id == question.channelId }?.seedColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoverArt(question, seed, 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                question.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(authorName(question.author), answersText(question.answerCount)).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PersonCardContent(profile: UserProfile, page: ProfilePage?, content: Color) {
    val resources = LocalContext.current.resources
    val badge = page?.let { p -> p.customBadges.firstOrNull() ?: p.badges.firstNotNullOfOrNull { badgeLabel(it, profile.female) } }
    val counts = if (page == null || page.hideStats) {
        emptyList()
    } else {
        listOf(
            resources.getQuantityString(R.plurals.answers_count, page.answers, page.answers),
            resources.getQuantityString(R.plurals.chat_flowers, page.flowers, page.flowers),
        )
    }
    val line = (listOfNotNull(badge, stringResource(R.string.chat_credibility, profile.points)) + counts).joinToString(" · ")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(profile.avatar, 44.dp, clickable = false)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                SpecialAccounts.badge(profile.userId)?.let { entry ->
                    Spacer(Modifier.width(6.dp))
                    SpecialBadgePill(entry)
                }
            }
            Text(
                line,
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** While the card loads, or when Stips has nothing there: what kind of link it is, and where it leads. */
@Composable
private fun FallbackCardContent(target: ChatLinks.Target, content: Color, loading: Boolean, missing: Boolean) {
    val question = target is ChatLinks.Target.Question
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(percent = if (question) 28 else 50))
                .background(content.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = content.copy(alpha = 0.6f))
            } else {
                Icon(if (question) QuestionIcon else Icons.Outlined.Person, contentDescription = null, tint = content.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when {
                        missing && question -> R.string.link_card_question_missing
                        missing -> R.string.link_card_profile_missing
                        question -> R.string.link_card_question
                        else -> R.string.link_card_profile
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                shortLink(target.link),
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "stips.co.il/ask/123" for "https://www.stips.co.il/ask/123". */
private fun shortLink(link: String): String {
    val host = Links.host(link)
    val path = link.substringAfter("://").substringAfter('/', "")
    return if (path.isEmpty()) host else "$host/$path"
}

/** Above the box while replying in private (v0.18): the question the message is about, and ✕ to leave it out. */
@Composable
internal fun ChatAboutBar(questionId: Long, onRemove: () -> Unit) {
    val target = remember(questionId) { ChatLinks.Target.Question(questionId, ChatLinks.questionLink(questionId)) }
    val card = rememberPreview(target)
    val title = (card.preview as? LinkPreviews.Preview.OfQuestion)?.question?.title ?: shortLink(target.link)
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Icon(
            QuestionIcon,
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
                stringResource(R.string.private_reply_about),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.private_reply_remove), modifier = Modifier.size(20.dp))
        }
    }
}
