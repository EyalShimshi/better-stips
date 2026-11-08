package io.github.eyalshimshi.betterstips.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import java.util.Locale
import kotlin.math.roundToInt

/** Where Live brings new things in by itself (#4): Home's new questions, or Pen pals' new posts (v0.16). */
enum class LivePlace { Home, PenPals }

/**
 * Live's switch for [place] and its rate (5–30 s, one rate for both places), with what it costs in data. The same
 * controls appear in Settings and when you long-press a Live chip (#4).
 */
@Composable
fun LiveControls(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
    place: LivePlace = LivePlace.Home,
) {
    val haptics = LocalHapticFeedback.current
    var seconds by remember(settings.liveIntervalSeconds) { mutableFloatStateOf(settings.liveIntervalSeconds.toFloat()) }
    val shown = seconds.roundToInt()
    val on = when (place) {
        LivePlace.Home -> settings.autoRefreshNew
        LivePlace.PenPals -> settings.penPalsLive
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (place == LivePlace.Home) R.string.settings_live_new else R.string.live_penpals),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(if (place == LivePlace.Home) R.string.live_what else R.string.live_penpals_what),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = on,
                onCheckedChange = { checked ->
                    haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onChange {
                        when (place) {
                            LivePlace.Home -> it.copy(autoRefreshNew = checked)
                            LivePlace.PenPals -> it.copy(penPalsLive = checked)
                        }
                    }
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
        val cost = when (place) {
            LivePlace.Home -> stringResource(
                R.string.live_cost,
                String.format(Locale.getDefault(), "%.1f", AppSettings.liveMegabytesPerHour(shown)),
            )
            LivePlace.PenPals -> stringResource(R.string.live_penpals_cost)
        }
        Text(cost, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.live_shared_rate),
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

/** Long-press on a Live chip: the same controls in a sheet. */
@Composable
fun LiveSheet(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit,
    place: LivePlace = LivePlace.Home,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(if (place == LivePlace.Home) R.string.live_sheet_title else R.string.live_penpals),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LiveControls(
            settings = settings,
            onChange = onChange,
            place = place,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        )
    }
}

/** The Live chip (Home's new questions, Pen pals): a tap turns Live on or off, a long press sets how often it checks. */
@Composable
fun LiveChip(live: Boolean, seconds: Int, onToggle: () -> Unit, onOptions: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val label = if (live) stringResource(R.string.live_with_rate, seconds) else stringResource(R.string.live)
    val optionsLabel = stringResource(R.string.live_options)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (live) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (live) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (live) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                role = Role.Switch,
                onLongClickLabel = optionsLabel,
                onLongClick = onOptions,
                onClick = {
                    haptics.performHapticFeedback(if (live) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    onToggle()
                },
            )
            .semantics { stateDescription = label },
    ) {
        Row(
            Modifier
                .heightIn(min = 32.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveDot(active = live)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LiveDot(active: Boolean) {
    val animate = LocalAppSettings.current.motion.elements
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer { alpha = if (active && animate) pulse else 1f }
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
    )
}