package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AnswersOrder
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Question

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

    /**
     * The asker's anonymous answers on their own anonymous question show the nickname the question was asked under
     * (#12), not "אנונימי": the question already shows that name, so nothing new is revealed. A question asked by name
     * changes nothing, so an anonymous answer there stays anonymous.
     */
    fun withAskerNickname(question: Question?, answers: List<Answer>): List<Answer> {
        val asked = question?.author?.takeIf { it.anonymous } ?: return answers
        val nickname = asked.nickname.trim().takeIf { it.isNotEmpty() } ?: return answers
        return answers.map { answer ->
            if (answer.byAsker && answer.author.anonymous && answer.author.nickname.trim() != nickname) {
                answer.copy(displayName = nickname)
            } else {
                answer
            }
        }
    }
}
