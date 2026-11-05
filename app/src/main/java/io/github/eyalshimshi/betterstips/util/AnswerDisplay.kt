package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswersOrder
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.model.Answer

/** Pure rules for how a question's answers are shown (#8). */
object AnswerDisplay {
    /** The answers in the reader's chosen order. Ties keep Stips' order. */
    fun ordered(answers: List<Answer>, order: AnswersOrder): List<Answer> = when (order) {
        AnswersOrder.AsOnStips -> answers
        AnswersOrder.Newest -> answers.sortedWith(compareByDescending<Answer> { it.postedAt }.thenByDescending { it.id })
        AnswersOrder.Oldest -> answers.sortedWith(compareBy<Answer> { it.postedAt }.thenBy { it.id })
        AnswersOrder.TopVoted -> answers.sortedByDescending { it.score }
    }

    /** The site says "שואלת השאלה" rather than "שואל השאלה" only when the asker's answer is by an anonymous "אנונימית". */
    fun askerIsFemale(answer: Answer): Boolean = answer.author.nickname.trim() == StipsRepository.ANONYMOUS_FEMALE
}
