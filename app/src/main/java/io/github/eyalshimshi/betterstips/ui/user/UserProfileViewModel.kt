package io.github.eyalshimshi.betterstips.ui.user

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.ChartAction
import io.github.eyalshimshi.betterstips.data.model.ChartPoint
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.ThanksMessage
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.util.ImagePrep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The thanks wall comes first and opens by default (#4). */
enum class ProfileSection { Thanks, Activity }

enum class ProfileMessage { BioSaved, PhotoUpdated, ThanksDeleted, ThanksSent, Failed }

/** A list read page by page (thanks wall, flowered answers). */
data class Paged<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val end: Boolean = false,
    val loading: Boolean = false,
    /** The last page failed; the list waits for a retry instead of asking again by itself. */
    val failed: Boolean = false,
)

data class ProfileUiState(
    val profile: UserProfile? = null,
    val page: ProfilePage? = null,
    val loading: Boolean = true,
    val error: ApiException? = null,
    val section: ProfileSection = ProfileSection.Thanks,
    val chartAction: ChartAction = ChartAction.Answers,
    val charts: Map<ChartAction, List<ChartPoint>> = emptyMap(),
    /** Graphs being fetched; each loads only when shown (#5: fewer requests). */
    val chartsLoading: Set<ChartAction> = emptySet(),
    val flowers: Paged<Answer> = Paged(),
    val thanks: Paged<ThanksMessage> = Paged(),
    val busy: Boolean = false,
    val sendingThanks: Boolean = false,
    val message: ProfileMessage? = null,
)

/** One profile page, the same for the signed-in user and for everyone else (owner-only actions aside). */
class UserProfileViewModel(
    private val repository: StipsRepository,
    private val appContext: Context,
    val userId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // Bumped on refresh, so a page that arrives late can't mix into the fresh list.
    private var generation = 0

    // Bumped when the thanks wall alone starts over (after writing on it), without dropping graphs on their way.
    private var thanksGeneration = 0

    init {
        load()
    }

    /**
     * The profile and its page; the thanks wall and the flowered answers load as their lists come into view, and
     * each graph only when it's shown. A profile costs 3 requests instead of 8 (#5).
     */
    fun load() {
        generation++
        _state.update {
            it.copy(loading = true, error = null, flowers = Paged(), thanks = Paged(), charts = emptyMap(), chartsLoading = emptySet())
        }
        viewModelScope.launch {
            val profile = async { attempt { repository.userProfile(userId) } }
            val page = async { attempt { repository.profilePage(userId) } }
            val p = profile.await()
            val pg = page.await()
            _state.update { s ->
                s.copy(
                    profile = p.getOrNull() ?: s.profile,
                    page = pg.getOrNull() ?: s.page,
                    error = (p.exceptionOrNull() ?: pg.exceptionOrNull())?.asApiException(),
                    loading = false,
                )
            }
            if (_state.value.section == ProfileSection.Activity) ensureChart(_state.value.chartAction)
        }
    }

    fun selectSection(section: ProfileSection) {
        _state.update { it.copy(section = section) }
        if (section == ProfileSection.Activity) ensureChart(_state.value.chartAction)
    }

    fun selectChart(action: ChartAction) {
        _state.update { it.copy(chartAction = action) }
        if (_state.value.section == ProfileSection.Activity) ensureChart(action)
    }

    private fun ensureChart(action: ChartAction) {
        val s = _state.value
        if (s.charts.containsKey(action) || action in s.chartsLoading) return
        val gen = generation
        _state.update { it.copy(chartsLoading = it.chartsLoading + action) }
        viewModelScope.launch {
            val result = attempt { repository.chart(userId, action) }
            if (gen != generation) return@launch
            _state.update { st ->
                val points = result.getOrNull()
                st.copy(chartsLoading = st.chartsLoading - action, charts = if (points != null) st.charts + (action to points) else st.charts)
            }
        }
    }

    fun loadMoreFlowers() {
        val current = _state.value.flowers
        if (current.loading || current.end) return
        val gen = generation
        val next = current.page + 1
        _state.update { it.copy(flowers = it.flowers.copy(loading = true, failed = false)) }
        viewModelScope.launch {
            val result = attempt { repository.floweredAnswers(userId, next) }
            if (gen != generation) return@launch
            _state.update { s -> s.copy(flowers = s.flowers.appended(result, next) { it.id }) }
        }
    }

    fun loadMoreThanks() {
        val current = _state.value.thanks
        if (current.loading || current.end) return
        val gen = generation
        val thanksGen = thanksGeneration
        val next = current.page + 1
        _state.update { it.copy(thanks = it.thanks.copy(loading = true, failed = false)) }
        viewModelScope.launch {
            val result = attempt { repository.thanksWall(userId, next) }
            if (gen != generation || thanksGen != thanksGeneration) return@launch
            _state.update { s -> s.copy(thanks = s.thanks.appended(result, next) { it.id }) }
        }
    }

    fun deleteThanks(message: ThanksMessage) {
        viewModelScope.launch {
            try {
                repository.deleteThanks(message.id)
                _state.update { s ->
                    s.copy(thanks = s.thanks.copy(items = s.thanks.items.filterNot { it.id == message.id }), message = ProfileMessage.ThanksDeleted)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = ProfileMessage.Failed) }
            }
        }
    }

    /** Writes on this user's thanks wall, then shows the wall from the top. Returns at once; the result comes as a message. */
    fun writeThanks(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.sendingThanks) return
        viewModelScope.launch {
            _state.update { it.copy(sendingThanks = true) }
            try {
                repository.writeThanks(userId, message)
                thanksGeneration++
                _state.update { it.copy(sendingThanks = false, thanks = Paged(), message = ProfileMessage.ThanksSent) }
                loadMoreThanks()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(sendingThanks = false, message = ProfileMessage.Failed) }
            }
        }
    }

    fun saveBio(text: String) {
        val bio = text.trim()
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repository.updateBio(bio)
                val page = attempt { repository.profilePage(userId) }.getOrNull()
                _state.update { s ->
                    s.copy(busy = false, page = page ?: s.page?.copy(bio = bio.ifEmpty { null }), message = ProfileMessage.BioSaved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = ProfileMessage.Failed) }
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val (jpeg, size) = withContext(Dispatchers.IO) { ImagePrep.squareJpeg(appContext, uri) }
                repository.uploadProfilePhoto(jpeg, size)
                val profile = attempt { repository.userProfile(userId) }.getOrNull()
                _state.update { s -> s.copy(busy = false, profile = profile ?: s.profile, message = ProfileMessage.PhotoUpdated) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = ProfileMessage.Failed) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun <T> Paged<T>.appended(result: Result<List<T>>, page: Int, id: (T) -> Long): Paged<T> {
        val list = result.getOrNull() ?: return copy(loading = false, failed = true)
        return Paged(items = (items + list).distinctBy(id), page = page, end = list.isEmpty(), loading = false)
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
