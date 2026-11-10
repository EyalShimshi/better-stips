package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.util.ChatLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the cards for Stips links in chats show (v0.18): a question's title, photo, asker and answers; a person's photo,
 * name, credibility and counts. Each is read from Stips once per app session, one at a time, only for cards on screen
 * (a card that scrolls away stops waiting), and only while the request budget has room. A question a list already
 * showed costs nothing.
 */
class LinkPreviews(private val repository: StipsRepository) {
    sealed interface Preview {
        data class OfQuestion(val question: Question) : Preview

        /** [page] is null when only the profile could be read. */
        data class OfPerson(val profile: UserProfile, val page: ProfilePage?) : Preview

        /** Stips has nothing there: deleted, or never was. */
        data object Missing : Preview
    }

    private val cache = HashMap<String, Preview>()
    private val lock = Mutex()

    /** What's known without asking Stips. */
    fun cached(target: ChatLinks.Target): Preview? {
        synchronized(cache) { cache[key(target)] }?.let { return it }
        if (target is ChatLinks.Target.Question) repository.cachedQuestion(target.id)?.let { return Preview.OfQuestion(it) }
        return null
    }

    /** Reads [target] from Stips when it isn't known yet; null when it couldn't now (offline, or Stips busy). */
    suspend fun load(target: ChatLinks.Target): Preview? {
        cached(target)?.let { return it }
        return lock.withLock {
            cached(target)?.let { return@withLock it }
            while (!RateGuard.allowBackground()) delay(BUDGET_WAIT_MS)
            val preview = try {
                when (target) {
                    is ChatLinks.Target.Question -> Preview.OfQuestion(repository.question(target.id))
                    is ChatLinks.Target.Profile -> {
                        val profile = repository.userProfile(target.id)
                        val page = try {
                            repository.profilePage(target.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                        Preview.OfPerson(profile, page)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.code == ApiException.NOT_FOUND || e.code == ApiException.BAD_RESPONSE) Preview.Missing else null
            } catch (e: Exception) {
                null
            }
            if (preview != null) synchronized(cache) { cache[key(target)] = preview }
            preview
        }
    }

    private fun key(target: ChatLinks.Target): String = when (target) {
        is ChatLinks.Target.Question -> "q${target.id}"
        is ChatLinks.Target.Profile -> "p${target.id}"
    }

    private companion object {
        /** How long a card waits before asking the request budget again. */
        const val BUDGET_WAIT_MS = 3_000L
    }
}
