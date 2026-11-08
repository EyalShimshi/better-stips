package io.github.eyalshimshi.betterstips.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.HomeLayout

/*
 * Small pictures of the app for Settings and the intro (#12): drawn with the app's own colors and the current
 * settings, so a change shows at once. They're decoration: TalkBack reads the switches next to them instead.
 */

/** A frame for a preview: the app's background, a hairline, round corners. */
@Composable
fun PreviewFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
    ) {
        Box(Modifier.padding(12.dp)) { content() }
    }
}

/** A hot question's card, a list below it, and the tab bar: how colors and the theme look (Look). */
@Composable
fun LookPreview(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    PreviewFrame(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.preview_question_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(colors.primary, 16.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.preview_name_a), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        Text("12", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
            ListLine(0.9f)
            ListLine(0.7f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceContainer)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabIcon(Icons.Outlined.Home, selected = true)
                TabIcon(Icons.Outlined.Edit, selected = false)
                TabIcon(Icons.AutoMirrored.Outlined.Chat, selected = false)
                TabIcon(Icons.Outlined.Person, selected = false)
            }
        }
    }
}

@Composable
private fun TabIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 3.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A gray line standing for a row of text. */
@Composable
private fun ListLine(fraction: Float, thickness: Dp = 10.dp, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(thickness)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

@Composable
private fun Dot(color: Color, diameter: Dp) {
    Box(
        Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(color),
    )
}

/** One of Home's layouts, drawn small, to pick from (Home, intro). */
@Composable
fun HomeLayoutThumbnail(layout: HomeLayout, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = label
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.background,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (layout != HomeLayout.NewOnly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HotBlock(Modifier.weight(1f))
                        HotBlock(Modifier.weight(0.55f))
                    }
                }
                when (layout) {
                    HomeLayout.HotAndNew -> repeat(4) { ListLine(if (it % 2 == 0) 1f else 0.8f, thickness = 7.dp) }
                    HomeLayout.LikeWebsite -> {
                        ListLine(0.4f, thickness = 6.dp, color = colors.tertiary)
                        ListLine(1f, thickness = 7.dp)
                        ListLine(0.8f, thickness = 7.dp)
                        ListLine(0.4f, thickness = 6.dp, color = colors.tertiary)
                        ListLine(0.9f, thickness = 7.dp)
                    }
                    HomeLayout.NewOnly -> repeat(8) { ListLine(if (it % 3 == 2) 0.75f else 1f, thickness = 7.dp) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun HotBlock(modifier: Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
    )
}

/** The three layouts side by side; picking one calls [onSelect]. */
@Composable
fun HomeLayoutPicker(selected: HomeLayout, onSelect: (HomeLayout) -> Unit, modifier: Modifier = Modifier) {
    val layouts = listOf(
        HomeLayout.HotAndNew to R.string.layout_hot_new,
        HomeLayout.LikeWebsite to R.string.layout_website,
        HomeLayout.NewOnly to R.string.layout_new_only,
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        layouts.forEach { (layout, label) ->
            HomeLayoutThumbnail(
                layout = layout,
                label = stringResource(label),
                selected = selected == layout,
                onClick = { onSelect(layout) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Two answers, the second replying to the first (Questions and answers, intro): its quote when [quotes] is on,
 * its "^" unless [hideCarets] leaves it out, the moderator mark when [moderatorBadges] is on, and the swipe that
 * starts a reply when [easyReply] is on.
 */
@Composable
fun RepliesPreview(easyReply: Boolean, quotes: Boolean, hideCarets: Boolean, moderatorBadges: Boolean, modifier: Modifier = Modifier) {
    val nameA = stringResource(R.string.preview_name_a)
    val answerA = stringResource(R.string.preview_answer_a)
    PreviewFrame(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAnswer(name = nameA, color = MaterialTheme.colorScheme.tertiary) {
                Text(answerA, style = MaterialTheme.typography.bodyMedium)
            }
            MiniAnswer(
                name = stringResource(R.string.preview_name_b),
                color = MaterialTheme.colorScheme.primary,
                badge = { if (moderatorBadges) ModeratorPill() },
            ) {
                if (quotes) Quote(nameA, answerA)
                if (!(quotes && hideCarets)) Caret()
                Text(stringResource(R.string.preview_answer_b), style = MaterialTheme.typography.bodyMedium)
            }
            if (easyReply) SwipeHint(stringResource(R.string.preview_swipe_answer))
        }
    }
}

@Composable
private fun MiniAnswer(name: String, color: Color, badge: @Composable RowScope.() -> Unit = {}, body: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color, 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(name, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(6.dp))
                badge()
            }
            body()
        }
    }
}

@Composable
private fun ModeratorPill() {
    val color = MaterialTheme.colorScheme.secondary
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Shield, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(stringResource(R.string.badge_moderator_m), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** A quoted message: a bar in the accent color, the name, and the start of the words. */
@Composable
private fun Quote(name: String, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(38.dp)
                .background(colors.primary),
        )
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Caret() {
    Text("^", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun SwipeHint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A short chat: a message, and a reply to it, as bubbles with the same reply options as chats have (DM settings). */
@Composable
fun ChatPreview(easyReply: Boolean, quotes: Boolean, hideCarets: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val incoming = stringResource(R.string.preview_chat_in)
    val name = stringResource(R.string.preview_name_a)
    PreviewFrame(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Bubble(mine = false) { Text(incoming, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface) }
            Bubble(mine = true) {
                if (quotes) Quote(name, incoming)
                if (!(quotes && hideCarets)) Caret()
                Text(stringResource(R.string.preview_chat_out), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
            }
            if (easyReply) SwipeHint(stringResource(R.string.preview_swipe_message))
        }
    }
}

@Composable
private fun Bubble(mine: Boolean, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mine) 18.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 18.dp,
            ),
            color = if (mine) colors.primaryContainer else colors.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
        }
    }
}

/** What a phone notification from Stips+ looks like (Notifications, intro). */
@Composable
fun NotificationPreview(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    PreviewFrame(modifier) {
        Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerHigh) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(12.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.LocalFlorist, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.app_name) + " · " + stringResource(R.string.time_short_now),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.preview_notification_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.preview_name_a) + ": " + stringResource(R.string.preview_answer_a),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Swiping a question (#18): a row slid to the right over its action, and one slid to the left over the other. */
@Composable
fun SwipePreview(
    right: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>,
    left: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>,
    modifier: Modifier = Modifier,
) {
    PreviewFrame(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwipedRow(right, toRight = true)
            SwipedRow(left, toRight = false)
        }
    }
}

@Composable
private fun SwipedRow(action: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>, toRight: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.secondaryContainer),
    ) {
        Row(
            Modifier
                .align(if (toRight) androidx.compose.ui.AbsoluteAlignment.CenterLeft else androidx.compose.ui.AbsoluteAlignment.CenterRight)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(action.second, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(action.first),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        }
        // The question's row, slid aside.
        Row(
            Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp)
                .align(if (toRight) androidx.compose.ui.AbsoluteAlignment.CenterRight else androidx.compose.ui.AbsoluteAlignment.CenterLeft)
                .background(colors.surfaceContainerLow)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryContainer),
            )
            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ListLine(1f, thickness = 7.dp)
                ListLine(0.6f, thickness = 6.dp)
            }
        }
    }
}

/**
 * Swiping an answer to the left (#18, v0.16): someone's answer and the reader's own, under [myName], each slid aside
 * over what that swipe does. One that does nothing stays in place.
 */
@Composable
fun AnswerSwipePreview(
    others: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>?,
    mine: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>?,
    myName: String,
    modifier: Modifier = Modifier,
) {
    PreviewFrame(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwipedAnswer(
                name = stringResource(R.string.preview_name_a),
                color = MaterialTheme.colorScheme.tertiary,
                text = stringResource(R.string.preview_answer_a),
                action = others,
            )
            SwipedAnswer(
                name = myName,
                color = MaterialTheme.colorScheme.primary,
                text = stringResource(R.string.preview_answer_mine),
                action = mine,
            )
        }
    }
}

@Composable
private fun SwipedAnswer(
    name: String,
    color: Color,
    text: String,
    action: Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>?,
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        if (action != null) {
            // What the swipe does, where the answer slid away from: the icon at the edge, as on a real answer.
            Row(
                Modifier
                    .align(androidx.compose.ui.AbsoluteAlignment.CenterRight)
                    .fillMaxWidth(0.3f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(action.second, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(action.first),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier
                .align(androidx.compose.ui.AbsoluteAlignment.CenterLeft)
                .fillMaxWidth(if (action != null) 0.7f else 1f),
        ) {
            MiniAnswer(name = name, color = color) {
                Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}