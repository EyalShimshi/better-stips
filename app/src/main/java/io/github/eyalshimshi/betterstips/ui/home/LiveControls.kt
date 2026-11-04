package io.github.eyalshimshi.betterstips.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AppSettings
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Live's switch and its refresh rate (5–30 s), with what it costs in data. The same controls appear in Settings
 * and when you long-press the Live chip on Home (#4).
 */
@Composable
fun LiveControls(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var seconds by remember(settings.liveIntervalSeconds) { mutableFloatStateOf(settings.liveIntervalSeconds.toFloat()) }
    val shown = seconds.roundToInt()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_live_new), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.live_what),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.autoRefreshNew,
                onCheckedChange = { on ->
                    haptics.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onChange { it.copy(autoRefreshNew = on) }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.live_every, shown),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Slider(
            value = seconds,
            onValueChange = { value ->
                if (value.roundToInt() != seconds.roundToInt()) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                seconds = value
            },
            onValueChangeFinished = {
                val chosen = seconds.roundToInt().coerceIn(AppSettings.LIVE_MIN_SECONDS, AppSettings.LIVE_MAX_SECONDS)
                onChange { it.copy(liveIntervalSeconds = chosen) }
            },
            valueRange = AppSettings.LIVE_MIN_SECONDS.toFloat()..AppSettings.LIVE_MAX_SECONDS.toFloat(),
            steps = AppSettings.LIVE_MAX_SECONDS - AppSettings.LIVE_MIN_SECONDS - 1,
            modifier = Modifier.fillMaxWidth(),
        )
        val megabytes = String.format(Locale.getDefault(), "%.1f", AppSettings.liveMegabytesPerHour(shown))
        Text(
            stringResource(R.string.live_cost, megabytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (shown < FAST_SECONDS) {
            Text(
                stringResource(R.string.live_fast_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/** Below this, Live costs noticeably more data and battery, and loads Stips more. */
private const val FAST_SECONDS = 10

/** Long-press on the Live chip: the same controls in a sheet. */
@Composable
fun LiveSheet(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.live_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LiveControls(
            settings = settings,
            onChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        )
    }
}
