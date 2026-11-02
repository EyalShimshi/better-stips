package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.LocalIsDark
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.StipsTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** The first letter of the first word, for monograms. */
fun monogram(text: String): String =
    text.trim().split(' ').firstOrNull { it.any(Char::isLetterOrDigit) }?.firstOrNull(Char::isLetterOrDigit)?.uppercase() ?: "?"

/** A question's cover: a squircle with a monogram, and the photo on top when there is one (DESIGN-PHILOSOPHY §2.8). */
@Composable
fun CoverArt(question: Question, seed: Int?, size: Dp, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    val fallback = MaterialTheme.colorScheme.secondaryContainer
    val onFallback = MaterialTheme.colorScheme.onSecondaryContainer
    val colors = remember(seed, dark, fallback, onFallback) {
        if (seed == null) {
            fallback to onFallback
        } else {
            val palette = SchemeGenerator.palettes(seed).primary
            Color(palette.tone(if (dark) 30 else 90)) to Color(palette.tone(if (dark) 90 else 10))
        }
    }
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 28))
            .background(colors.first),
        contentAlignment = Alignment.Center,
    ) {
        Text(monogram(question.title), color = colors.second, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
        val cover = question.cover
        if (cover != null && showImages) {
            AsyncImage(
                model = cover.thumbnailUrl,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun Avatar(author: Author, size: Dp, modifier: Modifier = Modifier, large: Boolean = false) {
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    val fontSize = with(LocalDensity.current) { (size * 0.45f).toSp() }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (author.anonymous || author.nickname.isBlank()) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(size * 0.6f),
            )
        } else {
            Text(monogram(author.nickname), color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = fontSize, maxLines = 1)
        }
        val url = author.avatarUrl(large)
        if (url != null && showImages) {
            AsyncImage(
                model = url,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun authorName(author: Author): String = author.nickname.ifBlank { stringResource(R.string.anonymous) }

@Composable
fun channelLabel(channel: Channel): String {
    val hebrewUi = LocalContext.current.resources.configuration.locales[0].language.let { it == "iw" || it == "he" }
    return if (hebrewUi) channel.name else channel.nameEn ?: channel.name
}

/** "5 min", "לפני 3 שעות", or a date. */
@Composable
fun agoText(instant: Instant?): String {
    if (instant == null) return ""
    val resources = LocalContext.current.resources
    return when (val ago = StipsTime.ago(instant, Instant.now())) {
        StipsTime.Ago.JustNow -> stringResource(R.string.time_just_now)
        is StipsTime.Ago.Minutes -> resources.getQuantityString(R.plurals.time_minutes, ago.count.toInt(), ago.count.toInt())
        is StipsTime.Ago.Hours -> resources.getQuantityString(R.plurals.time_hours, ago.count.toInt(), ago.count.toInt())
        is StipsTime.Ago.Days -> resources.getQuantityString(R.plurals.time_days, ago.count.toInt(), ago.count.toInt())
        is StipsTime.Ago.OnDate -> ago.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }
}

@Composable
fun answersText(count: Int): String =
    if (count == 0) stringResource(R.string.no_answers) else LocalContext.current.resources.getQuantityString(R.plurals.answers_count, count, count)

@Composable
fun QuestionRow(question: Question, channel: Channel?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val meta = listOf(answersText(question.answerCount), agoText(question.postedAt), authorName(question.author))
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    ListItem(
        headlineContent = {
            Text(question.title, maxLines = 3, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        supportingContent = { Text(meta, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingContent = { CoverArt(question, channel?.seedColor, 56.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = question.title + ", " + meta },
    )
}

/** Text with tappable links; long-press selects and copies (F-06). */
@Composable
fun LinkifiedText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            var last = 0
            for (range in Links.find(text)) {
                append(text.substring(last, range.first))
                val url = text.substring(range)
                withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
                    append(url)
                }
                last = range.last + 1
            }
            append(text.substring(last))
        }
    }
    SelectionContainer(modifier) { Text(annotated, style = style) }
}

@Composable
fun errorMessage(error: ApiException): String = when {
    error.isNetwork -> stringResource(R.string.error_offline)
    error.code == ApiException.NOT_FOUND -> stringResource(R.string.error_not_found)
    error.code == ApiException.BANNED -> stringResource(R.string.error_banned)
    error.code == ApiException.NOT_SIGNED_IN -> stringResource(R.string.error_signed_out)
    error.code == ApiException.BAD_RESPONSE -> stringResource(R.string.error_bad_response)
    !error.serverMessage.isNullOrBlank() -> stringResource(R.string.error_server_message, error.serverMessage.orEmpty())
    else -> stringResource(R.string.error_server)
}

fun Throwable.asApiException(): ApiException = this as? ApiException ?: ApiException(ApiException.BAD_RESPONSE, message)

/** A friendly card: what happened, and the button that helps (DESIGN-PHILOSOPHY §6). */
@Composable
fun MessageCard(
    title: String,
    body: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!body.isNullOrBlank()) Text(body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun ErrorCard(error: ApiException, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageCard(
        title = errorMessage(error),
        body = null,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
        modifier = modifier,
        isError = true,
    )
}

/** Placeholder rows, only when there is truly nothing to show yet. */
@Composable
fun SkeletonRows(count: Int = 6) {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(percent = 28))
                        .background(block),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(block),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(block),
                    )
                }
            }
        }
    }
}

@Composable
fun FooterNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    )
}
