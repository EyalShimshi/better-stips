package io.github.eyalshimshi.betterstips.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.util.CredRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The signed-in user's credibility points when known (v0.13): from Stips' account (`user.get_app_user`), else from
 * the saved profile. Null when signed out or not known yet; never guessed as 0.
 */
@Composable
fun knownCredibility(): Int? {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val account by container.session.account.collectAsStateWithLifecycle()
    if (!session.signedIn) return null
    return account?.points ?: session.profile?.points?.takeIf { it > 0 }
}

/** Stips' name for a rating ("מושלם" … "מזיק מאוד"). */
@Composable
fun ratingName(rating: CredRules.Rating): String = stringResource(
    when (rating) {
        CredRules.Rating.Perfect -> R.string.cred_rating_perfect
        CredRules.Rating.VeryReliable -> R.string.cred_rating_very_reliable
        CredRules.Rating.Reliable -> R.string.cred_rating_reliable
        CredRules.Rating.Okay -> R.string.cred_rating_okay
        CredRules.Rating.Problematic -> R.string.cred_rating_problematic
        CredRules.Rating.Harmful -> R.string.cred_rating_harmful
        CredRules.Rating.VeryHarmful -> R.string.cred_rating_very_harmful
    },
)

/** How fast points rise at [rating]: "a point a day", "every 2 days", "every 3–7 days", or not at all. */
@Composable
fun riseText(rating: CredRules.Rating): String {
    val min = rating.minDays
    val max = rating.maxDays
    return when {
        min == null || max == null -> stringResource(R.string.cred_rises_never)
        min == 1 && max == 1 -> stringResource(R.string.cred_rises_daily)
        min == max -> stringResource(R.string.cred_rises_every, min)
        else -> stringResource(R.string.cred_rises_range, min, max)
    }
}

/** A day, the way the phone writes dates. */
fun dayText(day: LocalDate): String = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * Stips blocks the account (v0.13): the screen the site shows as a dialog on every action, here once per visit and
 * again when Stips refuses an action. Stips' reason and end date when it sends them; otherwise what the points mean:
 * 20–29 blocks activity for 30 − points days (a point a day), below 20 for good. Reading stays open, as on the site.
 */
@Composable
fun BanScreen(state: CredRules.BanState, points: Int?, onContinue: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onContinue)
    val forGood = state.permanent && points != null && points < CredRules.RISES_FROM
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(44.dp))
            }
            Text(
                stringResource(
                    when {
                        !state.permanent -> R.string.ban_title_temporary
                        forGood -> R.string.ban_title_permanent
                        else -> R.string.ban_title_blocked
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            val reason = state.reason
            if (!reason.isNullOrBlank()) {
                Card(
                    Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.ban_reason), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(reason, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            val ends = state.endsText
            val days = state.daysLeft
            when {
                !ends.isNullOrBlank() -> Text(
                    stringResource(R.string.ban_ends, ends),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                days != null -> {
                    Text(
                        context.resources.getQuantityString(R.plurals.ban_days_left, days, days),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.ban_around, dayText(LocalDate.now().plusDays(days.toLong()))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (points != null) {
                Text(
                    stringResource(R.string.ban_points, points),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            val rule = when {
                forGood -> R.string.ban_rule_permanent
                days != null -> R.string.ban_rule_temporary
                else -> null
            }
            if (rule != null) {
                Text(
                    stringResource(rule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                stringResource(R.string.ban_reading) + " " + stringResource(R.string.ban_appeal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            ) { Text(stringResource(R.string.ban_continue)) }
            OutlinedButton(
                onClick = { openInBrowser(context, "$SITE_URL/contact-us") },
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_contact)) }
            TextButton(onClick = { openInBrowser(context, "$SITE_URL/rules") }) { Text(stringResource(R.string.settings_rules)) }
            TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
        }
    }
}
