package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.ui.theme.ColorMath
import io.github.eyalshimshi.betterstips.ui.theme.LocalIsDark
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import io.github.eyalshimshi.betterstips.util.TextUtils

/** Stips' "online" green, at a tone that reads well on the current light or dark surface. */
@Composable
fun onlineColor(): Color {
    val dark = LocalIsDark.current
    return remember(dark) {
        val lch = ColorMath.toLch(ONLINE_SEED)
        Color(SchemeGenerator.TonalPalette(lch.h, minOf(lch.c, 72.0)).tone(if (dark) 78 else 50))
    }
}

private const val ONLINE_SEED = 0xFF09C286.toInt()

/**
 * An avatar with the green "online" dot at its lower end corner, the same mark as on profiles (#6). [ring] is the
 * color around the dot: the surface the avatar sits on.
 */
@Composable
fun PresenceAvatar(
    author: Author,
    size: Dp,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    clickable: Boolean = true,
    ring: Color = MaterialTheme.colorScheme.surface,
) {
    Box(modifier) {
        Avatar(author, size, large = large, clickable = clickable)
        if (author.online) {
            val online = stringResource(R.string.presence_online)
            val dot = (size * 0.26f).coerceAtLeast(10.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot)
                    .clip(CircleShape)
                    .background(ring)
                    .padding(dot * 0.18f)
                    .clip(CircleShape)
                    .background(onlineColor())
                    .semantics { contentDescription = online },
            )
        }
    }
}

/** "On Stips for 6 years, 9 months". Stips sends the span as a Hebrew phrase, shown as is in Hebrew. */
@Composable
fun memberForText(raw: String?): String? {
    if (raw == null) return null
    val resources = LocalContext.current.resources
    val hebrewUi = resources.configuration.locales[0].language.let { it == "iw" || it == "he" }
    val span = if (hebrewUi) null else TextUtils.parseHebrewSpan(raw)
    val duration = when {
        hebrewUi -> raw
        span == null -> return null
        span.years > 0 && span.months > 0 ->
            resources.getQuantityString(R.plurals.span_years, span.years, span.years) + ", " +
                resources.getQuantityString(R.plurals.span_months, span.months, span.months)
        span.years > 0 -> resources.getQuantityString(R.plurals.span_years, span.years, span.years)
        else -> resources.getQuantityString(R.plurals.span_months, span.months, span.months)
    }
    return stringResource(R.string.profile_member_for, duration)
}
