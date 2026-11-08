package io.github.eyalshimshi.betterstips.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import io.github.eyalshimshi.betterstips.ui.components.QuestionIcon
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.ui.account.dayText
import io.github.eyalshimshi.betterstips.ui.account.ratingName
import io.github.eyalshimshi.betterstips.ui.account.riseText
import io.github.eyalshimshi.betterstips.util.CredRules
import java.time.LocalDate
import kotlin.math.roundToInt

/** The site's four gauge colors: below 30, 30–49, 50–79, 80 and up. */
internal fun credibilitySeed(points: Int): Int = when {
    points >= 80 -> 0xFF09C286
    points >= 50 -> 0xFF3F51B5
    points >= 30 -> 0xFFFDD835
    else -> 0xFFF50057
}.toInt()

/**
 * What a profile's credibility points mean (v0.13), by Stips' rule book: the rating and how fast it rises, what the
 * points allow, when they'd reach a chosen number if none are taken away, the milestones ahead, and the table. The
 * user's own sheet adds the goals the rule book names: the senior answerer badge and what Stips asks of moderators.
 */
@Composable
fun CredibilitySheet(points: Int, page: ProfilePage?, isMe: Boolean, onDismiss: () -> Unit) {
    val value = points.coerceIn(0, CredRules.MAX)
    val rating = CredRules.rating(value)
    val today = LocalDate.now()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = bandColor(credibilitySeed(value)),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.profile_credibility) + " · " + ratingName(rating), style = MaterialTheme.typography.titleLarge)
                    Text(riseText(rating), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            when (val block = CredRules.block(value)) {
                is CredRules.Block.Temporary -> StatusLine(
                    Icons.Outlined.Block,
                    LocalContext.current.resources.getQuantityString(R.plurals.cred_now_blocked, block.days, block.days),
                    MaterialTheme.colorScheme.error,
                )
                CredRules.Block.Permanent -> StatusLine(Icons.Outlined.Block, stringResource(R.string.cred_now_blocked_forever), MaterialTheme.colorScheme.error)
                CredRules.Block.None -> Unit
            }
            if (CredRules.canEdit(value)) {
                StatusLine(Icons.Outlined.Edit, stringResource(R.string.cred_now_edit_ok), MaterialTheme.colorScheme.primary)
            } else {
                StatusLine(Icons.Outlined.Edit, stringResource(R.string.cred_now_no_edit), MaterialTheme.colorScheme.error)
            }

            when {
                value >= CredRules.MAX -> StatusLine(Icons.AutoMirrored.Outlined.TrendingUp, stringResource(R.string.cred_projection_top), MaterialTheme.colorScheme.primary)
                !rating.rises -> StatusLine(Icons.Outlined.Info, stringResource(R.string.cred_projection_none), MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Projection(value, today)
                    Milestones(value, today)
                }
            }

            SheetLabel(stringResource(R.string.cred_table_title))
            RateTable(rating)

            if (isMe) Goals(value, page)

            Text(
                stringResource(R.string.cred_rules_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "When will I get to N?" (v0.13): a slider over the points ahead, and the day (or days) they'd be reached. */
@Composable
private fun Projection(from: Int, today: LocalDate) {
    val first = from + 1
    var target by rememberSaveable(from) { mutableIntStateOf(CredRules.milestones(from).firstOrNull() ?: CredRules.MAX) }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.cred_projection_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.cred_projection_target, target), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (first < CredRules.MAX) {
                Slider(
                    value = target.toFloat(),
                    onValueChange = { target = it.roundToInt().coerceIn(first, CredRules.MAX) },
                    valueRange = first.toFloat()..CredRules.MAX.toFloat(),
                    steps = (CredRules.MAX - first - 1).coerceAtLeast(0),
                )
            }
            Text(reachText(from, target, today), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.cred_projection_assumes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Around 3 Mar 2027, in 12 days", or "Between … and …" where Stips adds a point every 3 to 7 days. */
@Composable
private fun reachText(from: Int, target: Int, today: LocalDate): String {
    val days = CredRules.daysToReach(from, target) ?: return stringResource(R.string.cred_projection_none)
    return if (days.first == days.last) {
        val inDays = LocalContext.current.resources.getQuantityString(R.plurals.cred_in_days, days.first, days.first)
        stringResource(R.string.cred_projection_date, dayText(today.plusDays(days.first.toLong())), inDays)
    } else {
        stringResource(R.string.cred_projection_range, dayText(today.plusDays(days.first.toLong())), dayText(today.plusDays(days.last.toLong())))
    }
}

@Composable
private fun Milestones(from: Int, today: LocalDate) {
    val milestones = CredRules.milestones(from)
    if (milestones.isEmpty()) return
    SheetLabel(stringResource(R.string.cred_milestones))
    milestones.forEach { points ->
        val label = when (points) {
            CredRules.ACTIVE_FROM -> stringResource(R.string.cred_milestone_active)
            CredRules.EDIT_FROM -> stringResource(R.string.cred_milestone_edit)
            CredRules.MAX -> stringResource(R.string.cred_milestone_top)
            else -> ratingName(CredRules.rating(points))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                points.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = bandColor(credibilitySeed(points)),
                modifier = Modifier.width(44.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(reachText(from, points, today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Stips' table: each rating's points and how often a point is added; the current one stands out. */
@Composable
private fun RateTable(current: CredRules.Rating) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            CredRules.Rating.entries.forEach { rating ->
                val here = rating == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (here) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${rating.from}–${rating.to}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(
                        ratingName(rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (here) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(riseText(rating), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The rule book's goals (v0.13): the senior answerer badge at 500 flowers, and what Stips asks of moderators. */
@Composable
private fun Goals(points: Int, page: ProfilePage?) {
    val flowers = page?.flowers
    SheetLabel(stringResource(R.string.cred_goals))
    if (flowers != null) {
        Text(stringResource(R.string.cred_goal_senior_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        LinearProgressIndicator(
            progress = { (flowers.toFloat() / CredRules.SENIOR_ANSWERER_FLOWERS).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (flowers >= CredRules.SENIOR_ANSWERER_FLOWERS) {
                stringResource(R.string.cred_goal_senior_done, flowers)
            } else {
                stringResource(R.string.cred_goal_senior_body, flowers)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        stringResource(R.string.cred_goal_mod_title),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 4.dp),
    )
    val age = if (page != null && !page.hideAge) page.age else null
    val months = CredRules.monthsOnStips(page?.activeSince)
    Requirement(stringResource(R.string.cred_goal_mod_age), age?.let { it >= CredRules.MODERATOR_AGE })
    Requirement(stringResource(R.string.cred_goal_mod_points), points >= CredRules.MAX)
    Requirement(
        stringResource(R.string.cred_goal_mod_flowers, flowers ?: 0),
        flowers?.let { it >= CredRules.MODERATOR_FLOWERS },
    )
    Requirement(stringResource(R.string.cred_goal_mod_time), months?.let { it >= CredRules.MODERATOR_MONTHS })
    Requirement(stringResource(R.string.cred_goal_mod_block), null)
    Text(
        stringResource(R.string.cred_goal_mod_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One of Stips' requirements: met, not yet, or not something the app can tell (null). */
@Composable
private fun Requirement(text: String, met: Boolean?) {
    val (icon, tint) = when (met) {
        true -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.primary
        false -> Icons.Outlined.Cancel to MaterialTheme.colorScheme.error
        null -> QuestionIcon to MaterialTheme.colorScheme.onSurfaceVariant
    }
    StatusLine(icon, text, tint)
}
