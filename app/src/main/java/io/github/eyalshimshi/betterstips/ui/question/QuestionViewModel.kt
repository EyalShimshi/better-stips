package io.github.eyalshimshi.betterstips.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
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

enum class QuestionMessage { AnswerPosted, AnswerEdited, AnswerDetached, VoteFailed, ActionFailed, Following, NotFollowing }

data class QuestionUiState(
    val question: Question? = null,
    val answers: List<Answer> = emptyList(),
    val loading: Boolean = true,
    val error: ApiException? = null,
    val answersError: ApiException? = null,
    val loadedInMs: Long? = null,
    val pinned: Boolean = false,
    val pinCount: Int = 0,
    /** Whether the user gets updates on this question; null until known (or when signed out). */
    val following: Boolean? = null,
    val draft: String = "",
    val anonymous: Boolean = false,
    /** The one link attached to the answer being written (Stips allows one). */
    val draftLink: AnswerLink? = null,
    val linkLoading: Boolean = false,
    /** Answering also turns on updates for the question, like on the site; the user can untick it. */
    val followOnAnswer: Boolean = true,
    /** When set, the composer edits this answer instead of posting a new one. */
    val editing: Answer? = null,
    val posting: Boolean = false,
    val problem: AnswerRules.Problem? = null,
    val postError: ApiException? = null,
    val message: QuestionMessage? = null,
)

class QuestionViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val questionId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<QuestionUiState> = _state.asStateFlow()

    private fun initialState(): QuestionUiState {
        val cached = repository.cachedQuestion(questionId)
        return QuestionUiState(question = cached, pinned = cached?.userPinned ?: false, pinCount = cached?.pinCount ?: 0)
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val started = System.nanoTime()
            _state.update { it.copy(loading = true, error = null, answersError = null) }
            val question = async { attempt { repository.question(questionId) } }
            val answers = async { attempt { repository.answers(questionId) } }
            val following = async { if (session.session.value.signedIn) attempt { repository.isFollowing(questionId) } else null }
            val q = question.await()
            val a = answers.await()
            val f = following.await()?.getOrNull()
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (q.isSuccess) repository.timings.record(Timings.QUESTION, elapsed)
            _state.update { s ->
                val loaded = q.getOrNull()
                s.copy(
                    question = loaded ?: s.question,
                    pinned = loaded?.userPinned ?: s.pinned,
                    pinCount = loaded?.pinCount ?: s.pinCount,
                    following = f ?: s.following,
                    error = q.exceptionOrNull()?.asApiException(),
                    answers = a.getOrNull() ?: s.answers,
                    answersError = a.exceptionOrNull()?.asApiException(),
                    loading = false,
                    loadedInMs = if (q.isSuccess) elapsed else s.loadedInMs,
                )
            }
        }
    }

    fun togglePin() {
        val before = _state.value
        val pin = !before.pinned
        _state.update { it.copy(pinned = pin, pinCount = (it.pinCount + if (pin) 1 else -1).coerceAtLeast(0)) }
        viewModelScope.launch {
            try {
                repository.setPinned(questionId, pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(pinned = before.pinned, pinCount = before.pinCount, message = QuestionMessage.ActionFailed) }
            }
        }
    }

    fun toggleFollow() {
        val follow = _state.value.following != true
        val before = _state.value.following
        _state.update { it.copy(following = follow) }
        viewModelScope.launch {
            try {
                repository.setFollowing(questionId, follow)
                _state.update { it.copy(message = if (follow) QuestionMessage.Following else QuestionMessage.NotFollowing) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(following = before, message = QuestionMessage.ActionFailed) }
            }
        }
    }

    fun setDraft(text: String) {
        _state.update { it.copy(draft = text, problem = null, postError = null) }
    }

    fun setAnonymous(anonymous: Boolean) {
        _state.update { it.copy(anonymous = anonymous) }
    }

    fun setFollowOnAnswer(follow: Boolean) {
        _state.update { it.copy(followOnAnswer = follow) }
    }

    /** Attaches a link and names it with the page's title, as the site does. */
    fun attachLink(rawUrl: String) {
        val url = rawUrl.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        _state.update { it.copy(draftLink = AnswerLink(url, null), linkLoading = true) }
        viewModelScope.launch {
            val title = repository.linkTitle(url)
            _state.update { s ->
                if (s.draftLink?.url == url) s.copy(draftLink = AnswerLink(url, title), linkLoading = false) else s
            }
        }
    }

    fun removeLink() {
        _state.update { it.copy(draftLink = null, linkLoading = false) }
    }

    fun startEdit(answer: Answer) {
        _state.update {
            it.copy(editing = answer, draft = answer.text, draftLink = answer.links.firstOrNull(), problem = null, postError = null)
        }
    }

    fun cancelEdit() {
        _state.update { it.copy(editing = null, draft = "", draftLink = null, problem = null, postError = null) }
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
                val editing = current.editing
                if (editing != null) {
                    repository.editAnswer(editing.id, text, current.draftLink)
                } else {
                    repository.postAnswer(questionId, text, current.anonymous, current.draftLink)
                    if (current.followOnAnswer) {
                        attempt { repository.setFollowing(questionId, true) }.onSuccess { _state.update { s -> s.copy(following = true) } }
                    }
                }
                _state.update {
                    it.copy(
                        posting = false,
                        draft = "",
                        draftLink = null,
                        editing = null,
                        message = if (editing != null) QuestionMessage.AnswerEdited else QuestionMessage.AnswerPosted,
                    )
                }
                reloadAnswers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(posting = false, postError = e.asApiException()) }
            }
        }
    }

    /** Removes the user's name from their answer: it shows as anonymous from now on. */
    fun detach(answer: Answer) {
        viewModelScope.launch {
            try {
                repository.detachAnswer(answer.id)
                _state.update { it.copy(message = QuestionMessage.AnswerDetached) }
                reloadAnswers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = QuestionMessage.ActionFailed) }
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

    private suspend fun reloadAnswers() {
        attempt { repository.answers(questionId) }.onSuccess { list ->
            _state.update { s -> s.copy(answers = list, question = s.question?.copy(answerCount = list.size)) }
        }
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
