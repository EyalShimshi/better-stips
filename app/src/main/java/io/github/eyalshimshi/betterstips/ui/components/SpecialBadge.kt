package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.ui.theme.ColorMath
import io.github.eyalshimshi.betterstips.ui.theme.LocalIsDark
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import io.github.eyalshimshi.betterstips.SpecialAccounts

/** The app shows Hebrew. */
@Composable
fun hebrewUi(): Boolean = LocalContext.current.resources.configuration.locales[0].language.let { it == "iw" || it == "he" }

/** A vivid color from a fixed hue, at a tone that reads well on the current light or dark surface. */
@Composable
fun tonedColor(seed: Int): Color {
    val dark = LocalIsDark.current
    return remember(seed, dark) {
        val lch = ColorMath.toLch(seed)
        Color(SchemeGenerator.TonalPalette(lch.h, minOf(lch.c, 72.0)).tone(if (dark) 78 else 45))
    }
}

/** A special account's badge words in the app's language, or null when it has none (#12). */
@Composable
fun specialBadgeLabel(entry: SpecialAccounts.Entry): String? =
    if (hebrewUi()) entry.badgeHe ?: entry.badgeEn else entry.badgeEn ?: entry.badgeHe

/**
 * A special account's badge by its name (#12): a sparkle and its words, in its own color (the app's tertiary when
 * it has none), on a light tint of that color, like the moderators' mark.
 */
@Composable
fun SpecialBadgePill(entry: SpecialAccounts.Entry, modifier: Modifier = Modifier, large: Boolean = false) {
    val label = specialBadgeLabel(entry) ?: return
    val color = entry.color?.let { tonedColor(it) } ?: MaterialTheme.colorScheme.tertiary
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f), contentColor = color, modifier = modifier) {
        Row(
            Modifier.padding(
                start = if (large) 10.dp else 6.dp,
                end = if (large) 12.dp else 8.dp,
                top = if (large) 5.dp else 1.dp,
                bottom = if (large) 5.dp else 1.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(if (large) 16.dp else 12.dp))
            Spacer(Modifier.width(if (large) 6.dp else 3.dp))
            Text(
                label,
                style = if (large) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
