package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AppSettings
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Whether anonymous mode is on right now; flips back on screen by itself when its time is up. */
@Composable
fun rememberAnonymousModeOn(until: Long): Boolean {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(until) {
        while (true) {
            now = System.currentTimeMillis()
            if (until == AppSettings.ANONYMOUS_FOREVER || until <= now) break
            delay((until - now).coerceIn(1_000L, 60_000L))
        }
    }
    return until > now
}

/** The masks button on Home: lit while anonymous mode is on. */
@Composable
fun AnonymousModeButton(until: Long, onClick: () -> Unit) {
    val on = rememberAnonymousModeOn(until)
    IconButton(onClick = onClick) {
        Icon(
            if (on) Icons.Filled.TheaterComedy else Icons.Outlined.TheaterComedy,
            contentDescription = stringResource(if (on) R.string.anon_mode_on_cd else R.string.anon_mode_title),
            tint = if (on) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The choices, in minutes; the last one means "until I turn it off". */
private val DURATIONS = listOf(30L to R.string.anon_for_30m, 60L to R.string.anon_for_1h, 120L to R.string.anon_for_2h,
    240L to R.string.anon_for_4h, 480L to R.string.anon_for_8h, 1440L to R.string.anon_for_24h)

/**
 * Anonymous mode (#4): while it's on, every answer and question goes out anonymously. Asks for how long, from
 * 30 minutes to "until I turn it off".
 */
@Composable
fun AnonymousModeDialog(until: Long, onSet: (Long) -> Unit, onDismiss: () -> Unit) {
    val on = rememberAnonymousModeOn(until)
    val haptics = LocalHapticFeedback.current
    fun pick(value: Long) {
        haptics.performHapticFeedback(if (value == 0L) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
        onSet(value)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.TheaterComedy, contentDescription = null) },
        title = { Text(stringResource(R.string.anon_mode_title)) },
        text = {
            Column {
                Text(
                    when {
                        !on -> stringResource(R.string.anon_mode_explain)
                        until == AppSettings.ANONYMOUS_FOREVER -> stringResource(R.string.anon_mode_on_forever)
                        else -> stringResource(R.string.anon_mode_on_until, untilText(until))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DURATIONS.forEach { (minutes, label) ->
                    ChoiceRow(stringResource(label)) { pick(System.currentTimeMillis() + minutes * 60_000L) }
                }
                ChoiceRow(stringResource(R.string.anon_forever)) { pick(AppSettings.ANONYMOUS_FOREVER) }
            }
        },
        confirmButton = {
            if (on) {
                TextButton(onClick = { pick(0L) }) { Text(stringResource(R.string.anon_mode_turn_off)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
        dismissButton = if (on) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
        } else {
            null
        },
    )
}

@Composable
private fun ChoiceRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

/** "14:30", or a date and time when it isn't today. */
@Composable
private fun untilText(until: Long): String {
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(until).atZone(zone)
    val formatter = if (at.toLocalDate() == LocalDate.now(zone)) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
    return at.format(formatter)
}
