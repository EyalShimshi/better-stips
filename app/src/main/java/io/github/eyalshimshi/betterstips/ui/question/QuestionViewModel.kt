package io.github.eyalshimshi.betterstips.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.AnswerRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuestionMessage { AnswerPosted, VoteFailed }

data class QuestionUiState(
    val question: Question? = null,
    val answers: List<Answer> = emptyList(),
    val loading: Boolean = true,
    val error: ApiException? = null,
    val answersError: ApiException? = null,
    val loadedInMs: Long? = null,
    val draft: String = "",
    val anonymous: Boolean = false,
    val posting: Boolean = false,
    val problem: AnswerRules.Problem? = null,
    val postError: ApiException? = null,
    val message: QuestionMessage? = null,
)

class QuestionViewModel(private val repository: StipsRepository, private val questionId: Long) : ViewModel() {
    private val _state = MutableStateFlow(QuestionUiState(question = repository.cachedQuestion(questionId)))
    val state: StateFlow<QuestionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val started = System.nanoTime()
            _state.update { it.copy(loading = true, error = null, answersError = null) }
            val question = async { attempt { repository.question(questionId) } }
            val answers = async { attempt { repository.answers(questionId) } }
            val q = question.await()
            val a = answers.await()
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (q.isSuccess) repository.timings.record(Timings.QUESTION, elapsed)
            _state.update { s ->
                s.copy(
                    question = q.getOrNull() ?: s.question,
                    error = q.exceptionOrNull()?.asApiException(),
                    answers = a.getOrNull() ?: s.answers,
                    answersError = a.exceptionOrNull()?.asApiException(),
                    loading = false,
                    loadedInMs = if (q.isSuccess) elapsed else s.loadedInMs,
                )
            }
        }
    }

    fun setDraft(text: String) {
        _state.update { it.copy(draft = text, problem = null, postError = null) }
    }

    fun setAnonymous(anonymous: Boolean) {
        _state.update { it.copy(anonymous = anonymous) }
    }

    fun post() {
        val current = _state.value
        if (current.posting) return
        val text = current.draft.trim()
        val problem = AnswerRules.check(text)
        if (problem != null) {
            _state.update { it.copy(problem = problem) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(posting = true, problem = null, postError = null) }
            try {
                repository.postAnswer(questionId, text, current.anonymous)
                _state.update { it.copy(posting = false, draft = "", message = QuestionMessage.AnswerPosted) }
                attempt { repository.answers(questionId) }.onSuccess { list ->
                    _state.update { s -> s.copy(answers = list, question = s.question?.copy(answerCount = list.size)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(posting = false, postError = e.asApiException()) }
            }
        }
    }

    /** Like the site: tapping your current vote again clears it. */
    fun vote(answer: Answer, direction: Int) {
        val newVote = if (answer.userVote == direction) 0 else direction
        val delta = newVote - answer.userVote
        updateAnswer(answer.id) { it.copy(userVote = newVote, score = it.score + delta) }
        viewModelScope.launch {
            try {
                repository.vote(answer.id, newVote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateAnswer(answer.id) { it.copy(userVote = answer.userVote, score = answer.score) }
                _state.update { it.copy(message = QuestionMessage.VoteFailed) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateAnswer(id: Long, change: (Answer) -> Answer) {
        _state.update { s -> s.copy(answers = s.answers.map { if (it.id == id) change(it) else it }) }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
