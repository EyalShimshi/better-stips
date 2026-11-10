package io.github.eyalshimshi.betterstips.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AppUpdates
import io.github.eyalshimshi.betterstips.ui.components.agoText
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Home's card for a newer Stips+ (#25): *Update* downloads it from GitHub and opens Android's installer when it's
 * ready (AppRoot does that), *What's new* opens the release's page, *Later* waits for the next build.
 */
@Composable
fun UpdateCard(state: AppUpdates.State, onUpdate: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val release = state.available ?: return
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.update_title, release.versionName),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            val progress = state.progress
            if (progress != null) {
                Text(stringResource(R.string.update_downloading, (progress * 100).roundToInt()), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
            } else {
                val failed = state.downloadFailed && state.ready == null
                Text(
                    stringResource(
                        when {
                            state.ready != null -> R.string.update_ready_body
                            failed -> R.string.update_failed
                            else -> R.string.update_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { openInBrowser(context, release.pageUrl) }) { Text(stringResource(R.string.update_whats_new)) }
                    TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = onUpdate) {
                        Text(
                            stringResource(
                                when {
                                    state.ready != null -> R.string.update_install
                                    failed -> R.string.action_retry
                                    else -> R.string.update_now
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Settings → About's update row: its title. */
@Composable
fun updateRowTitle(state: AppUpdates.State): String {
    val release = state.available
    return when {
        release == null -> stringResource(R.string.settings_update_check)
        state.ready != null -> stringResource(R.string.settings_update_install, release.versionName)
        else -> stringResource(R.string.settings_update_to, release.versionName)
    }
}

/** Settings → About's update row: what it says under its title. */
@Composable
fun updateRowSummary(state: AppUpdates.State): String {
    val progress = state.progress
    return when {
        progress != null -> stringResource(R.string.update_downloading, (progress * 100).roundToInt())
        state.checking -> stringResource(R.string.settings_update_checking)
        state.available != null && state.ready != null -> stringResource(R.string.update_ready_body)
        state.available != null && state.downloadFailed -> stringResource(R.string.update_failed)
        state.available != null -> stringResource(R.string.update_body)
        state.checkFailed -> stringResource(R.string.settings_update_failed)
        state.checkedAt > 0L -> stringResource(R.string.settings_update_latest, agoText(Instant.ofEpochMilli(state.checkedAt)))
        else -> stringResource(R.string.settings_update_never)
    }
}
