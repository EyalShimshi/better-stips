package io.github.eyalshimshi.betterstips.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.AnonymousAnswers
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.SettingsStore
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

enum class QuestionMessage {
    AnswerPosted, AnswerEdited, AnswerDetached, VoteFailed, ActionFailed, Following, NotFollowing,
    Reported, Hidden, AnswerDeleted, QuestionEdited, QuestionDetached, FlowerSent,
}

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
    /** Bumped when the app (not the user's typing) replaces the draft, so the field moves its cursor to the end. */
    val draftVersion: Int = 0,
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
    /** The question was deleted: the screen closes. */
    val closed: Boolean = false,
    /** Answers that just got a flower from the asker here (the list refreshes in the background). */
    val flowering: Set<Long> = emptySet(),
)

class QuestionViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val settings: SettingsStore,
    private val anonymousAnswers: AnonymousAnswers,
    private val questionId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<QuestionUiState> = _state.asStateFlow()

    /** Once the user flips the anonymous switch, their choice wins over the automatic default. */
    private var anonymousTouched = false

    private fun initialState(): QuestionUiState {
        val cached = repository.cachedQuestion(questionId)
        return QuestionUiState(
            question = cached,
            pinned = cached?.userPinned ?: false,
            pinCount = cached?.pinCount ?: 0,
            // Answered anonymously here before? Then the next answer starts anonymous too (#4).
            anonymous = anonymousAnswers.contains(questionId),
        )
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val started = System.nanoTime()
            _state.update { it.copy(loading = true, error = null, answersError = null) }
            // A copy from a list shown in the last 3 minutes saves a request (#5).
            val recent = repository.recentQuestion(questionId, RECENT_MS)
            val question = async { if (recent != null) Result.success(recent) else attempt { repository.question(questionId) } }
            val answers = async { attempt { repository.answers(questionId) } }
            val following = async { if (session.session.value.signedIn) attempt { repository.isFollowing(questionId) } else null }
            val q = question.await()
            val a = answers.await()
            val f = following.await()?.getOrNull()
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (q.isSuccess) repository.timings.record(Timings.QUESTION, elapsed)
            _state.update { s ->
                val loaded = q.getOrNull()
                val answers = a.getOrNull() ?: s.answers
                s.copy(
                    question = loaded ?: s.question,
                    pinned = loaded?.userPinned ?: s.pinned,
                    pinCount = loaded?.pinCount ?: s.pinCount,
                    following = f ?: s.following,
                    error = q.exceptionOrNull()?.asApiException(),
                    answers = answers,
                    answersError = a.exceptionOrNull()?.asApiException(),
                    loading = false,
                    loadedInMs = if (q.isSuccess) elapsed else s.loadedInMs,
                    anonymous = s.anonymous || (!anonymousTouched && answeredAnonymously(answers)),
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
                repository.updateCachedQuestion(questionId) { it.copy(userPinned = pin, pinCount = _state.value.pinCount) }
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
        anonymousTouched = true
        _state.update { it.copy(anonymous = anonymous) }
    }

    /** Stips marks the signed-in user's own answers, anonymous ones included. */
    private fun answeredAnonymously(answers: List<Answer>): Boolean = answers.any { it.isOwner && it.author.anonymous }

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
            it.copy(
                editing = answer,
                draft = answer.text,
                draftVersion = it.draftVersion + 1,
                draftLink = answer.links.firstOrNull(),
                problem = null,
                postError = null,
            )
        }
    }

    fun cancelEdit() {
        _state.update {
            it.copy(editing = null, draft = "", draftVersion = it.draftVersion + 1, draftLink = null, problem = null, postError = null)
        }
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
                    // Anonymous mode (#4) makes every answer anonymous while it's on.
                    val anonymous = current.anonymous || settings.settings.value.anonymousModeOn()
                    repository.postAnswer(questionId, text, anonymous, current.draftLink)
                    if (anonymous) anonymousAnswers.remember(questionId)
                    if (current.followOnAnswer) {
                        attempt { repository.setFollowing(questionId, true) }.onSuccess { _state.update { s -> s.copy(following = true) } }
                    }
                }
                _state.update {
                    it.copy(
                        posting = false,
                        draft = "",
                        draftVersion = it.draftVersion + 1,
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

    /** Sends the user's words to Stips' moderators. */
    fun report(objType: String, id: Long, reason: String) {
        runAction(QuestionMessage.Reported) { repository.report(objType, id, reason) }
    }

    /** Hides an item (and everything by [authorId] when given) on this phone, and tells Stips. */
    fun block(objType: String, id: Long, authorId: Long?) {
        viewModelScope.launch {
            repository.blockContent(objType, id, authorId)
            _state.update { it.copy(message = QuestionMessage.Hidden) }
        }
    }

    fun deleteAnswer(answer: Answer) {
        runAction(QuestionMessage.AnswerDeleted) {
            repository.deleteItem("ans", answer.id)
            _state.update { s -> s.copy(answers = s.answers.filterNot { it.id == answer.id }) }
            reloadAnswers()
        }
    }

    fun deleteQuestion() {
        viewModelScope.launch {
            try {
                repository.deleteItem("ask", questionId)
                _state.update { it.copy(closed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = QuestionMessage.ActionFailed) }
            }
        }
    }

    fun detachQuestion() {
        runAction(QuestionMessage.QuestionDetached) {
            repository.detachQuestion(questionId)
            attempt { repository.question(questionId) }.onSuccess { q -> _state.update { it.copy(question = q) } }
        }
    }

    fun editQuestion(title: String, body: String, link: String?) {
        val question = _state.value.question ?: return
        runAction(QuestionMessage.QuestionEdited) {
            repository.editQuestion(questionId, title.trim(), body.trim(), question.topics, link?.trim()?.takeIf { it.isNotEmpty() })
            attempt { repository.question(questionId) }.onSuccess { q -> _state.update { it.copy(question = q) } }
        }
    }

    /** The asker's flower for a good answer (Stips allows 5 per question). */
    fun sendFlower(answer: Answer) {
        _state.update { it.copy(flowering = it.flowering + answer.id) }
        viewModelScope.launch {
            try {
                repository.sendFlower(answer.id)
                updateAnswer(answer.id) { it.copy(flowered = true) }
                _state.update { it.copy(message = QuestionMessage.FlowerSent, flowering = it.flowering - answer.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = QuestionMessage.ActionFailed, flowering = it.flowering - answer.id) }
            }
        }
    }

    private fun runAction(success: QuestionMessage, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(message = success) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = QuestionMessage.ActionFailed) }
            }
        }
    }

    private suspend fun reloadAnswers() {
        attempt { repository.answers(questionId) }.onSuccess { list ->
            _state.update { s -> s.copy(answers = list, question = s.question?.copy(answerCount = list.size)) }
        }
    }

    private fun updateAnswer(id: Long, change: (Answer) -> Answer) {
        _state.update { s -> s.copy(answers = s.answers.map { if (it.id == id) change(it) else it }) }
    }

    private companion object {
        const val RECENT_MS = 3 * 60_000L
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
