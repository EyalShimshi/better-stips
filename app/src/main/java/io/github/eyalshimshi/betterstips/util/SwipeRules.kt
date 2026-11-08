package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.QuestionSwipe
import kotlin.math.abs

/** Which side a swipe goes to (v0.16): to the right is left to right on the screen, in Hebrew too. */
enum class SwipeSide { Right, Left }

/** Which way a snackbar swiped away leaves (v0.16). */
enum class SnackbarExit { Left, Right, Down }

/** Which swipe actions (#18) apply where, and how a swipe ends (v0.16). Pure. */
object SwipeRules {
    /** A snackbar dragged this share of its width to a side goes, as Android's own do with half. */
    const val SNACKBAR_SIDE_SHARE = 0.35f

    /** A snackbar dragged down this share of its height goes. */
    const val SNACKBAR_DOWN_SHARE = 0.5f

    /** The choices for swiping a question in a list, in the order Settings lists them. */
    val QUESTION_CHOICES: List<QuestionSwipe> = QuestionSwipe.entries

    /** The choices for swiping someone else's answer to the left: what its ⋮ menu offers. */
    val OTHERS_ANSWER_CHOICES: List<AnswerSwipe> =
        listOf(AnswerSwipe.None, AnswerSwipe.Report, AnswerSwipe.Hide, AnswerSwipe.HideUser, AnswerSwipe.History)

    /** The choices for swiping your own answer to the left. */
    val MY_ANSWER_CHOICES: List<AnswerSwipe> =
        listOf(AnswerSwipe.None, AnswerSwipe.Edit, AnswerSwipe.Detach, AnswerSwipe.Delete, AnswerSwipe.History)

    /**
     * Whether [action] applies to a question: reporting and hiding are for other people's questions, and hiding
     * everything from someone needs to know who asked (not anonymous, and not [me]).
     */
    fun questionAvailable(action: QuestionSwipe, isOwner: Boolean, authorId: Long?, anonymous: Boolean, me: Long?): Boolean =
        when (action) {
            QuestionSwipe.None -> false
            QuestionSwipe.Report, QuestionSwipe.Hide -> !isOwner
            QuestionSwipe.HideUser -> !isOwner && !anonymous && authorId != null && authorId != me
            else -> true
        }

    /**
     * Where a swipe let go at [offset] px from rest, moving at [velocity] px/s (positive: to the right), leads (v0.16),
     * as Material's swipe-to-dismiss and Android's list swipes decide: past [trigger] it runs, and so does a flick
     * toward that side faster than [fling] once it has moved [flingMin]. A flick back cancels, even past the line.
     * Null: it springs back.
     */
    fun release(offset: Float, velocity: Float, trigger: Float, fling: Float, flingMin: Float): SwipeSide? {
        if (offset == 0f) return null
        val side = if (offset > 0f) SwipeSide.Right else SwipeSide.Left
        // Speed away from rest: negative when the finger is heading back.
        val outward = if (side == SwipeSide.Right) velocity else -velocity
        val distance = abs(offset)
        return when {
            outward <= -fling -> null
            distance >= trigger -> side
            distance >= flingMin && outward >= fling -> side
            else -> null
        }
    }

    /**
     * Where a snackbar let go at ([dx], [dy]) px, moving at ([vx], [vy]) px/s, goes (v0.16): to the side it was mostly
     * dragged, or down, once it went far enough or was flicked that way faster than [fling]. Null: back in place.
     */
    fun snackbarExit(dx: Float, dy: Float, vx: Float, vy: Float, width: Float, height: Float, fling: Float): SnackbarExit? =
        if (abs(dx) >= abs(dy)) {
            val side = if (dx > 0f) SnackbarExit.Right else SnackbarExit.Left
            when {
                dx == 0f -> null
                abs(dx) >= width * SNACKBAR_SIDE_SHARE -> side
                abs(vx) >= fling && (vx > 0f) == (dx > 0f) && abs(vx) > abs(vy) -> side
                else -> null
            }
        } else {
            when {
                dy <= 0f -> null
                dy >= height * SNACKBAR_DOWN_SHARE -> SnackbarExit.Down
                vy >= fling && vy > abs(vx) -> SnackbarExit.Down
                else -> null
            }
        }
}
