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
import io.github.eyalshimshi.betterstips.util.UpdateRules
import java.time.Instant

/**
 * Home's card for a newer Stips+ (#25): *Update* opens the new APK in the browser, which downloads it (v0.20, D-145);
 * *What's new* opens the release's page, *Later* waits for the next build.
 */
@Composable
fun UpdateCard(state: AppUpdates.State, onLater: () -> Unit, modifier: Modifier = Modifier) {
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
            Text(stringResource(R.string.update_body), style = MaterialTheme.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { openInBrowser(context, release.pageUrl) }) { Text(stringResource(R.string.update_whats_new)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
                Spacer(Modifier.width(4.dp))
                Button(onClick = { openInBrowser(context, UpdateRules.downloadUrl(release)) }) {
                    Text(stringResource(R.string.update_now))
                }
            }
        }
    }
}

/** Settings → About's update row: its title. */
@Composable
fun updateRowTitle(state: AppUpdates.State): String {
    val release = state.available
    return if (release == null) stringResource(R.string.settings_update_check) else stringResource(R.string.settings_update_to, release.versionName)
}

/** Settings → About's update row: what it says under its title. */
@Composable
fun updateRowSummary(state: AppUpdates.State): String = when {
    state.checking -> stringResource(R.string.settings_update_checking)
    state.available != null -> stringResource(R.string.update_body)
    state.checkFailed -> stringResource(R.string.settings_update_failed)
    state.checkedAt > 0L -> stringResource(R.string.settings_update_latest, agoText(Instant.ofEpochMilli(state.checkedAt)))
    else -> stringResource(R.string.settings_update_never)
}