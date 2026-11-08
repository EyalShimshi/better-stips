package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.QuestionSwipe

/** Which swipe actions (#18) apply where. Pure. */
object SwipeRules {
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
}
