package io.github.eyalshimshi.betterstips.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.SettingsStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.AskAllowance
import io.github.eyalshimshi.betterstips.data.model.AskDraft
import io.github.eyalshimshi.betterstips.data.model.GalleryPhoto
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.AskRules
import io.github.eyalshimshi.betterstips.util.EmojiFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AskStep { Question, Tags, Photo }

data class AskUiState(
    val step: AskStep = AskStep.Question,
    val title: String = "",
    val body: String = "",
    val link: String = "",
    val showLink: Boolean = false,
    val anonymous: Boolean = false,
    val nickname: String = "",
    val channelId: Int? = null,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val suggested: List<String> = emptyList(),
    val suggestedFor: String? = null,
    val completions: List<String> = emptyList(),
    val photoQuery: String = "",
    val photos: List<GalleryPhoto> = emptyList(),
    val photosLoading: Boolean = false,
    val photoId: String? = null,
    val allowance: AskAllowance? = null,
    /** Show field errors only after the user tried to continue. */
    val showErrors: Boolean = false,
    val posting: Boolean = false,
    val error: ApiException? = null,
    val postedId: Long? = null,
    /** Emojis were left out of what the user typed: Stips keeps none in questions (#12). */
    val emojisRemoved: Boolean = false,
)

/** Asking a question, in the site's three steps: the question, its tags, its photo (#4). */
class AskViewModel(
    private val repository: StipsRepository,
    private val session: SessionStore,
    settings: SettingsStore,
    channelId: Int?,
) : ViewModel() {
    private val _state = MutableStateFlow(
        AskUiState(
            channelId = channelId,
            // Anonymous mode makes every question anonymous while it's on.
            anonymous = settings.settings.value.anonymousModeOn(),
            nickname = defaultNickname(),
        ),
    )
    val state: StateFlow<AskUiState> = _state.asStateFlow()
    private var completionJob: Job? = null
    private var photoJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val allowance = repository.askAllowance()
                _state.update { it.copy(allowance = allowance) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The server checks again when posting.
            }
        }
    }

    private fun defaultNickname(): String =
        if (session.session.value.profile?.female == true) StipsRepository.ANONYMOUS_FEMALE else StipsRepository.ANONYMOUS_MALE

    fun setTitle(value: String) = withoutEmojis(value) { text, removed ->
        _state.update { it.copy(title = text.take(AskRules.TITLE_MAX), emojisRemoved = it.emojisRemoved || removed) }
    }

    fun setBody(value: String) = withoutEmojis(value) { text, removed ->
        _state.update { it.copy(body = text.take(AskRules.BODY_MAX), emojisRemoved = it.emojisRemoved || removed) }
    }

    /** Stips cuts a question at its first emoji, so they're left out as they're typed (#12). */
    private inline fun withoutEmojis(value: String, apply: (String, Boolean) -> Unit) {
        val clean = EmojiFilter.strip(value)
        apply(clean, clean != value)
    }

    fun setLink(value: String) = _state.update { it.copy(link = value) }

    fun showLink() = _state.update { it.copy(showLink = true) }

    fun removeLink() = _state.update { it.copy(link = "", showLink = false) }

    fun setAnonymous(on: Boolean) = _state.update { it.copy(anonymous = on, nickname = if (on) it.nickname else defaultNickname()) }

    fun setNickname(value: String) = withoutEmojis(value) { text, removed ->
        _state.update { it.copy(nickname = text.take(AskRules.NICKNAME_MAX), emojisRemoved = it.emojisRemoved || removed) }
    }

    fun leaveChannel() = _state.update { it.copy(channelId = null) }

    fun questionValid(s: AskUiState = _state.value): Boolean =
        AskRules.titleProblem(s.title) == null && AskRules.bodyProblem(s.body) == null &&
            (!s.anonymous || AskRules.nicknameOk(s.nickname)) &&
            (s.link.isBlank() || AskRules.normalizeLink(s.link) != null)

    /** The photo step is skipped for a YouTube link, as on the site. */
    val photoNeeded: Boolean get() = !AskRules.isYouTube(AskRules.normalizeLink(_state.value.link))

    fun next() {
        val s = _state.value
        when (s.step) {
            AskStep.Question -> {
                if (!questionValid(s)) {
                    _state.update { it.copy(showErrors = true) }
                    return
                }
                _state.update { it.copy(step = AskStep.Tags, showErrors = false) }
                loadSuggestions()
            }
            AskStep.Tags -> {
                if (s.tags.isEmpty()) {
                    _state.update { it.copy(showErrors = true) }
                    return
                }
                if (!photoNeeded) {
                    submit()
                    return
                }
                _state.update { it.copy(step = AskStep.Photo, showErrors = false, photoQuery = s.tags.first()) }
                searchPhotos(s.tags.first())
            }
            AskStep.Photo -> submit()
        }
    }

    /** Returns false when there is no earlier step (the screen then closes). */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            AskStep.Question -> return false
            AskStep.Tags -> AskStep.Question
            AskStep.Photo -> AskStep.Tags
        }
        _state.update { it.copy(step = previous, showErrors = false) }
        return true
    }

    private fun loadSuggestions() {
        val title = _state.value.title.trim()
        if (_state.value.suggestedFor == title) return
        viewModelScope.launch {
            val tags = try {
                repository.suggestedTags(title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(suggested = tags.take(20), suggestedFor = title) }
        }
    }

    fun toggleTag(tag: String) {
        _state.update { s ->
            when {
                tag in s.tags -> s.copy(tags = s.tags - tag)
                s.tags.size >= AskRules.TAGS_MAX -> s
                else -> s.copy(tags = s.tags + tag)
            }
        }
    }

    fun setTagInput(input: String) {
        val value = EmojiFilter.strip(input)
        _state.update { it.copy(tagInput = value) }
        completionJob?.cancel()
        val prefix = AskRules.cleanTag(value)
        if (prefix.length < 2) {
            _state.update { it.copy(completions = emptyList()) }
            return
        }
        completionJob = viewModelScope.launch {
            delay(300)
            val list = try {
                repository.completeTag(prefix)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(completions = list.take(8)) }
        }
    }

    /** Adds what the user typed (Stips accepts new tags). */
    fun addTypedTag(raw: String = _state.value.tagInput) {
        val tag = AskRules.cleanTag(raw)
        if (tag.isEmpty()) return
        _state.update { s ->
            if (tag in s.tags || s.tags.size >= AskRules.TAGS_MAX) {
                s.copy(tagInput = "", completions = emptyList())
            } else {
                s.copy(tags = s.tags + tag, tagInput = "", completions = emptyList())
            }
        }
    }

    fun setPhotoQuery(value: String) = _state.update { it.copy(photoQuery = value) }

    fun searchPhotos(query: String = _state.value.photoQuery) {
        val term = AskRules.cleanTag(query)
        if (term.isEmpty()) return
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            _state.update { it.copy(photosLoading = true) }
            val photos = try {
                repository.galleryPhotos(listOf(term)).ifEmpty { repository.galleryPhotos(listOf(FALLBACK_TAG)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(photos = photos, photosLoading = false) }
        }
    }

    fun selectPhoto(id: String) = _state.update { it.copy(photoId = if (it.photoId == id) null else id) }

    fun submit() {
        val s = _state.value
        if (s.posting || !questionValid(s) || s.tags.isEmpty()) return
        val needPhoto = photoNeeded
        if (needPhoto && s.photoId == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        val draft = AskDraft(
            title = s.title.trim(),
            body = s.body.trim(),
            tags = s.tags,
            photoId = if (needPhoto) s.photoId else null,
            link = AskRules.normalizeLink(s.link),
            anonymous = s.anonymous,
            nickname = if (s.anonymous) s.nickname.trim() else null,
            channelId = s.channelId,
        )
        viewModelScope.launch {
            _state.update { it.copy(posting = true, error = null) }
            try {
                val id = repository.askQuestion(draft)
                _state.update { it.copy(posting = false, postedId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(posting = false, error = e.asApiException()) }
            }
        }
    }

    private companion object {
        /** What the site falls back to when a tag has no photos: "abstract backgrounds". */
        const val FALLBACK_TAG = "\u05e8\u05e7\u05e2\u05d9 \u05e4\u05e8\u05d9\u05d8\u05d9\u05dd"
    }
}
