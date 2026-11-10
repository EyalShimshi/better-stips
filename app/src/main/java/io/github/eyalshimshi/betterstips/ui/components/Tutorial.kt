package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.ui.theme.LocalAppSettings
import io.github.eyalshimshi.betterstips.util.SwipeSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive

/** A tip's swipe hint (#28): which way the item slides, and the words shown over it; [nonce] tells repeats apart. */
@Immutable
data class SwipeHint(val side: SwipeSide, val text: String, val nonce: Long = System.nanoTime())

/** The question row Home's tip slides now (#28), by the question's id. */
@Immutable
data class QuestionRowHint(val questionId: Long, val hint: SwipeHint)

/** Whether tips may show now (#28): not under the intro or the blocked-account screen. Provided by AppRoot. */
val LocalTipsAllowed = compositionLocalOf { false }

/** The question row a tip slides now (#28). Provided by Home. */
val LocalQuestionRowHint = compositionLocalOf<QuestionRowHint?> { null }

/** How long a tip waits after its screen shows, so it doesn't start while things still move. */
const val TIP_DELAY_MS = 900L

/** How long a tip waits for what it shows to be on screen (a list loading). */
const val TIP_WAIT_MS = 15_000L

/** How long a tip that doesn't slide anything stays. */
const val TIP_SHOW_MS = 4_500L

/** A tip's words on screen (#28), and whether they show; the words stay while they fade out. */
@Stable
class TipState {
    var text by mutableStateOf("")
        private set
    var visible by mutableStateOf(false)
        private set

    fun show(words: String) {
        text = words
        visible = true
    }

    fun hide() {
        visible = false
    }
}

@Composable
fun rememberTipState(): TipState = remember { TipState() }

/**
 * Runs each new [hint] on [drag] (#28): the item slides most of the way to the line and back, with the hint's words
 * over it. A finger on the item ends it at once; the hint going away doesn't cut it short.
 */
@Composable
fun SwipeHintEffect(hint: SwipeHint?, drag: SwipeDrag, tip: TipState) {
    val animated = LocalAppSettings.current.motion.elements
    val latest by rememberUpdatedState(hint)
    LaunchedEffect(drag, tip) {
        snapshotFlow { latest }.filterNotNull().distinctUntilChangedBy { it.nonce }.collect { next ->
            tip.show(next.text)
            try {
                drag.peek(next.side, animated)
            } catch (e: CancellationException) {
                // A finger took over: only this hint ends.
                if (!currentCoroutineContext().isActive) throw e
            } finally {
                tip.hide()
            }
        }
    }
}

/**
 * A tip's words in a dark pill with a finger (#28), fading in and out; TalkBack reads them as they show. Over what it
 * explains, or with [expand] in a column, where it makes room for itself.
 */
@Composable
fun TipPill(tip: TipState, modifier: Modifier = Modifier, expand: Boolean = false) {
    AnimatedVisibility(
        visible = tip.visible,
        enter = if (expand) expandVertically() + fadeIn() else fadeIn(),
        exit = if (expand) shrinkVertically() + fadeOut() else fadeOut(),
        modifier = modifier,
    ) {
        val colors = MaterialTheme.colorScheme
        Row(
            Modifier
                .padding(8.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.inverseSurface)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = colors.inverseOnSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(tip.text, style = MaterialTheme.typography.labelLarge, color = colors.inverseOnSurface, textAlign = TextAlign.Center)
        }
    }
}
