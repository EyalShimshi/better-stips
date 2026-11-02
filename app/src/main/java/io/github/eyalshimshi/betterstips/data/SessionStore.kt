package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.SessionCookieJar
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

data class Session(
    val token: String? = null,
    val userId: Long? = null,
    val profile: UserProfile? = null,
    /** The account's "safe content" setting; signed-out readers always get the safe filter, as on the site. */
    val safeFilter: Boolean = true,
) {
    val signedIn: Boolean get() = token != null && userId != null
}

/**
 * Signs in with email and password (D-04). The password is sent once and never stored: only Stips'
 * session token is kept, and it re-creates the server session after a restart (SPEC-03 §5).
 */
class SessionStore(context: Context, private val api: StipsApi, private val cookies: SessionCookieJar) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(read())
    val session: StateFlow<Session> = _session.asStateFlow()

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when Stips no longer accepts the saved token. */
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    private val mutex = Mutex()

    /** The token whose server session is live in this process. */
    @Volatile
    private var liveToken: String? = null

    suspend fun signIn(email: String, password: String) {
        val data = api.call(
            "user.login",
            buildJsonObject {
                put("email", email.trim())
                put("password", password)
                put("auth_token", "")
            },
            forcePost = true,
        )
        val result = Parsers.login(data) ?: throw ApiException(ApiException.GENERAL)
        val userId = result.userId ?: throw ApiException(ApiException.BAD_RESPONSE)
        val token = result.token ?: throw ApiException(ApiException.BAD_RESPONSE)
        liveToken = token
        save(Session(token = token, userId = userId, profile = null, safeFilter = result.safeFilter ?: true))
        refreshProfile()
    }

    /** Makes sure Stips knows this session; after a restart or an expired cookie it signs in again with the token. */
    suspend fun ensureSession(force: Boolean = false) {
        val token = _session.value.token ?: throw ApiException(ApiException.NOT_SIGNED_IN)
        mutex.withLock {
            if (!force && liveToken == token) return
            val data = try {
                api.call("user.login", buildJsonObject { put("auth_token", token) }, forcePost = true)
            } catch (e: ApiException) {
                if (e.isNetwork) throw e
                expire()
                throw ApiException(ApiException.NOT_SIGNED_IN, e.serverMessage)
            }
            val result = Parsers.login(data)
            if (result?.userId == null) {
                expire()
                throw ApiException(ApiException.NOT_SIGNED_IN)
            }
            val newToken = result.token ?: token
            liveToken = newToken
            save(_session.value.copy(token = newToken, safeFilter = result.safeFilter ?: _session.value.safeFilter))
        }
    }

    suspend fun refreshProfile() {
        val userId = _session.value.userId ?: return
        try {
            val data = api.call(
                "omniobj",
                restAction = "GET",
                omniobj = buildJsonObject {
                    put("objType", "user")
                    putJsonObject("data") { put("id", userId) }
                },
            )
            val profile = Parsers.profile(data) ?: return
            if (_session.value.userId == userId) save(_session.value.copy(profile = profile))
        } catch (e: ApiException) {
            // Keep the profile we had; it's only for display and the anonymous nickname.
        }
    }

    /** Keeps the local copy of the account's safe-mode setting in step after changing it on Stips. */
    fun setSafeFilter(on: Boolean) {
        val current = _session.value
        if (current.signedIn) save(current.copy(safeFilter = on))
    }

    suspend fun signOut() {
        try {
            withTimeout(5_000) { api.call("user.logout") }
        } catch (e: Exception) {
            // Signing out locally is what matters.
        }
        clearLocal()
    }

    private fun expire() {
        clearLocal()
        _expired.tryEmit(Unit)
    }

    private fun clearLocal() {
        liveToken = null
        cookies.clear()
        prefs.edit().clear().apply()
        _session.value = Session()
    }

    private fun save(session: Session) {
        _session.value = session
        val profile = session.profile
        prefs.edit()
            .putString("token", session.token)
            .putLong("userId", session.userId ?: 0L)
            .putBoolean("safeFilter", session.safeFilter)
            .putString("nickname", profile?.nickname)
            .putInt("points", profile?.points ?: 0)
            .putBoolean("female", profile?.female ?: false)
            .putString("photoStamp", profile?.photoStamp)
            .apply()
    }

    private fun read(): Session {
        val token = prefs.getString("token", null) ?: return Session()
        val userId = prefs.getLong("userId", 0L).takeIf { it > 0 } ?: return Session()
        val nickname = prefs.getString("nickname", null)
        val profile = nickname?.let {
            UserProfile(
                userId = userId,
                nickname = it,
                points = prefs.getInt("points", 0),
                female = prefs.getBoolean("female", false),
                photoStamp = prefs.getString("photoStamp", null),
            )
        }
        return Session(token, userId, profile, prefs.getBoolean("safeFilter", true))
    }
}
