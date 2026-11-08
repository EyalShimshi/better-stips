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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.ServerClock
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.ui.theme.ColorMath
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.ui.theme.LocalIsDark
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.StipsTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Opens a user's profile from anywhere (author names, avatars, thanks wall). Provided by AppRoot. */
val LocalOpenProfile = staticCompositionLocalOf<(Long) -> Unit> { {} }

/** Unread chats and notifications, refreshed by AppRoot while the app is open. */
val LocalUnreadCounts = compositionLocalOf { UnreadCounts() }

/**
 * The time "how long ago" is measured against. AppRoot ticks it every second while "Count times live" is on (#8);
 * null means the moment the text is drawn.
 */
val LocalNow = compositionLocalOf<Instant?> { null }

/** Taps on the bottom bar's tab that's already open, by its route (v0.12). Provided by AppRoot. */
val LocalTabReselects = staticCompositionLocalOf<SharedFlow<String>> { MutableSharedFlow() }

/** Shows the first-run intro again (Settings → About, #12). Provided by AppRoot. */
val LocalShowIntro = staticCompositionLocalOf<() -> Unit> { {} }

/** Farther down than this, going back to the top jumps instead of scrolling through everything. */
private const val RESELECT_ANIMATE_LIMIT = 30

/**
 * Tapping [route]'s tab while it's open brings [listState] back to the top, as in other apps (v0.12), and reloads it
 * at the same time ([onRefresh]), as if entering the page again (v0.13, #10).
 */
@Composable
fun ScrollToTopOnReselect(route: String, listState: LazyListState, onRefresh: (() -> Unit)? = null) {
    val reselects = LocalTabReselects.current
    val animate by rememberUpdatedState(LocalAppSettings.current.motion.scrolls)
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(reselects, listState, route) {
        reselects.collect { tapped ->
            if (tapped != route) return@collect
            refresh?.invoke()
            if (animate && listState.firstVisibleItemIndex <= RESELECT_ANIMATE_LIMIT) {
                listState.animateScrollToItem(0)
            } else {
                listState.scrollToItem(0)
            }
        }
    }
}

/** An answer's score: green above zero, red below, as usual at zero (v0.13, #10). */
@Composable
fun voteScoreColor(score: Int): Color = when {
    score > 0 -> if (LocalIsDark.current) Color(0xFF81C995) else Color(0xFF188038)
    score < 0 -> MaterialTheme.colorScheme.error
    else -> LocalContentColor.current
}

/** Now, as Stips' servers see it (a wrong phone clock is corrected, [ServerClock]). */
@Composable
fun currentTime(): Instant = LocalNow.current ?: ServerClock.now()

/**
 * The thin "unread" band above the first unread answer or message, as in WhatsApp (v0.13). The screen drops it on the
 * next visit or refresh.
 */
@Composable
fun UnreadBar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
fun Avatar(author: Author, size: Dp, modifier: Modifier = Modifier, large: Boolean = false, clickable: Boolean = true) {
    val showImages = LocalAppSettings.current.showImages
    val loader = LocalAppContainer.current.imageLoader
    val openProfile = LocalOpenProfile.current
    val fontSize = with(LocalDensity.current) { (size * 0.45f).toSp() }
    val userId = author.userId
    val canOpen = clickable && userId != null && !author.anonymous
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .then(if (canOpen && userId != null) Modifier.clickable { openProfile(userId) } else Modifier),
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

/** "12 s" under a minute (#8), then "לפני 5 דק׳", "לפני 3 שעות", or a date. */
@Composable
fun agoText(instant: Instant?): String {
    if (instant == null) return ""
    val resources = LocalContext.current.resources
    val now = currentTime()
    val seconds = StipsTime.secondsAgo(instant, now)
    if (seconds != null) {
        return if (seconds == 0) {
            stringResource(R.string.time_just_now)
        } else {
            resources.getQuantityString(R.plurals.time_seconds, seconds, seconds)
        }
    }
    return when (val ago = StipsTime.ago(instant, now)) {
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
fun pinsText(count: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.pins_count, count, count)

/** An author's name that opens their profile (unless anonymous), with the verified mark for organizations (#8). */
@Composable
fun AuthorName(author: Author, style: TextStyle, modifier: Modifier = Modifier) {
    val openProfile = LocalOpenProfile.current
    val userId = author.userId
    NameWithMark(
        author = author,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.then(
            if (userId != null && !author.anonymous) Modifier.clickable { openProfile(userId) } else Modifier,
        ),
    )
}

/** The name in Stips' special color when it gives one, then the blue check for organizations and Stips itself. */
@Composable
fun NameWithMark(author: Author, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            authorName(author),
            style = style,
            color = if (author.highlightName) specialNameColor() else color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (author.verified) {
            Spacer(Modifier.width(3.dp))
            val markSize = if (style.fontSize.isSp) with(LocalDensity.current) { style.fontSize.toDp() } + 2.dp else 16.dp
            VerifiedMark(author, markSize)
        }
    }
}

/** The blue check by organizations working inside Stips, and by Stips' own system account (#8). */
@Composable
fun VerifiedMark(author: Author, size: Dp = 16.dp) {
    if (!author.verified) return
    Icon(
        Icons.Filled.Verified,
        contentDescription = stringResource(if (author.platform) R.string.verified_stips else R.string.verified_organization),
        tint = VerifiedBlue,
        modifier = Modifier.size(size),
    )
}

/** The verified mark's blue, the same in light and dark themes so it reads as the familiar check. */
val VerifiedBlue = Color(0xFF1D9BF0)

/** Stips' purple for names it highlights (the site animates them toward #9069d7), at a tone that suits the theme. */
@Composable
fun specialNameColor(): Color {
    val dark = LocalIsDark.current
    return remember(dark) {
        val lch = ColorMath.toLch(SPECIAL_NAME_SEED)
        Color(SchemeGenerator.TonalPalette(lch.h, minOf(lch.c, 80.0)).tone(if (dark) 78 else 42))
    }
}

private const val SPECIAL_NAME_SEED = 0xFF9069D7.toInt()

@Composable
fun QuestionRow(question: Question, channel: Channel?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = listOf(
        question.title,
        answersText(question.answerCount),
        pinsText(question.pinCount),
        authorName(question.author),
        agoText(question.postedAt),
    ).filter { it.isNotBlank() }.joinToString(", ")
    ListItem(
        headlineContent = {
            Text(question.title, maxLines = 3, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        supportingContent = { QuestionMetaRow(question, Modifier.padding(top = 6.dp)) },
        leadingContent = { CoverArt(question, channel?.seedColor, 56.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/**
 * Under a question: the asker (photo and name, cut with "…" when long) at the start; the number of answers and pins
 * and the time, as symbols, at the end (#4).
 */
@Composable
fun QuestionMetaRow(
    question: Question,
    modifier: Modifier = Modifier,
    answerCount: Int = question.answerCount,
    pinCount: Int = question.pinCount,
    avatarSize: Dp = 18.dp,
    clickableAuthor: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    special: SpecialAccounts.Entry? = null,
) {
    val style = MaterialTheme.typography.labelMedium
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Avatar(question.author, avatarSize, clickable = clickableAuthor)
            Spacer(Modifier.width(6.dp))
            if (clickableAuthor) {
                AuthorName(question.author, style, Modifier.weight(1f, fill = false))
            } else {
                NameWithMark(question.author, style, color, Modifier.weight(1f, fill = false))
            }
            if (special != null) {
                Spacer(Modifier.width(6.dp))
                SpecialBadgePill(special)
            }
        }
        Spacer(Modifier.width(8.dp))
        MetaItem(Icons.Outlined.ChatBubbleOutline, answerCount.toString(), color)
        Spacer(Modifier.width(10.dp))
        MetaItem(Icons.Outlined.PushPin, pinCount.toString(), color)
        val ago = shortAgoText(question.postedAt)
        if (ago.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            MetaItem(Icons.Outlined.Schedule, ago, color)
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** "5m", "3h", "2d" (Hebrew: "5 דק׳"…), or a short date: for tight rows. Under a minute, in seconds (#8). */
@Composable
fun shortAgoText(instant: Instant?): String {
    if (instant == null) return ""
    val now = currentTime()
    val seconds = StipsTime.secondsAgo(instant, now)
    if (seconds != null) {
        return if (seconds == 0) stringResource(R.string.time_short_now) else stringResource(R.string.time_short_seconds, seconds)
    }
    return when (val ago = StipsTime.ago(instant, now)) {
        StipsTime.Ago.JustNow -> stringResource(R.string.time_short_now)
        is StipsTime.Ago.Minutes -> stringResource(R.string.time_short_minutes, ago.count.toInt())
        is StipsTime.Ago.Hours -> stringResource(R.string.time_short_hours, ago.count.toInt())
        is StipsTime.Ago.Days -> stringResource(R.string.time_short_days, ago.count.toInt())
        is StipsTime.Ago.OnDate -> ago.date.format(DateTimeFormatter.ofPattern("d.M"))
    }
}

/** A span of text that leads to another item (a reply's caret pointer, v0.10): [id] is what [LinkifiedText] reports. */
data class TextTarget(val range: IntRange, val id: Long)

/** Text with tappable links; long-press selects and copies (F-06). [targets] are tappable spans reported to [onTarget]. */
@Composable
fun LinkifiedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onOverflow: ((Boolean) -> Unit)? = null,
    /** False where taps and long-presses belong to the container (chat bubbles); links still open. */
    selectable: Boolean = true,
    targets: List<TextTarget> = emptyList(),
    onTarget: (Long) -> Unit = {},
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val currentOnTarget by rememberUpdatedState(onTarget)
    val annotated = remember(text, linkColor, targets) {
        val urls = Links.find(text)
        // Pointers inside a link stay part of the link.
        val kept = targets.filter { t ->
            t.range.first >= 0 && t.range.last < text.length && urls.none { it.first <= t.range.last && t.range.first <= it.last }
        }
        val spans: List<Pair<IntRange, TextTarget?>> =
            (urls.map { range -> Pair<IntRange, TextTarget?>(range, null) } + kept.map { Pair<IntRange, TextTarget?>(it.range, it) })
                .sortedBy { it.first.first }
        buildAnnotatedString {
            var last = 0
            for ((range, target) in spans) {
                if (range.first < last) continue
                append(text.substring(last, range.first))
                val piece = text.substring(range)
                if (target == null) {
                    withLink(LinkAnnotation.Url(piece, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
                        append(piece)
                    }
                } else {
                    val link = LinkAnnotation.Clickable(
                        tag = "target:" + target.id,
                        styles = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)),
                        linkInteractionListener = { currentOnTarget(target.id) },
                    )
                    withLink(link) { append(piece) }
                }
                last = range.last + 1
            }
            append(text.substring(last))
        }
    }
    if (!selectable) {
        Text(
            annotated,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> onOverflow?.invoke(result.hasVisualOverflow) },
            modifier = modifier,
        )
        return
    }
    SelectionContainer(modifier) {
        Text(
            annotated,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> onOverflow?.invoke(result.hasVisualOverflow) },
        )
    }
}

@Composable
fun errorMessage(error: ApiException): String = when {
    error.isNetwork -> stringResource(R.string.error_offline)
    error.code == ApiException.RATE_LIMITED -> stringResource(R.string.error_rate_limited)
    error.code == ApiException.THROTTLED -> stringResource(R.string.error_throttled)
    error.code == ApiException.BLOCKED -> stringResource(R.string.error_blocked)
    error.code == ApiException.SERVER -> stringResource(R.string.error_server_down, error.serverMessage.orEmpty())
    error.code == ApiException.HTTP -> stringResource(R.string.error_http, error.serverMessage.orEmpty())
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
