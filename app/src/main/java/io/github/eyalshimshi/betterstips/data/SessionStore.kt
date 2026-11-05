package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.SessionCookieJar
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.AppUser
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.diag.NetLog
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

    private val _account = MutableStateFlow<AppUser?>(null)

    /**
     * The signed-in account as `user.get_app_user` last described it (v0.13): credibility points, flowers, a block by
     * Stips, moderator. Null until read in this process (on start and after signing in), and when signed out.
     */
    val account: StateFlow<AppUser?> = _account.asStateFlow()

    private val _bannedSignals = MutableStateFlow(0)

    /** Counts the actions Stips refused because the account is blocked (`API_ERROR_BANNED`); the block screen shows again on each. */
    val bannedSignals: StateFlow<Int> = _bannedSignals.asStateFlow()

    @Volatile
    private var bannedCheckedAt = 0L

    /** The token whose server session is live in this process. */
    @Volatile
    private var liveToken: String? = null

    /** When the server session was last known to be alive, and when we last signed in with the token. */
    @Volatile
    private var confirmedAt = 0L

    @Volatile
    private var loggedInAt = 0L

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
        confirmedAt = System.currentTimeMillis()
        loggedInAt = confirmedAt
        NetLog.event("session", "signed in")
        save(Session(token = token, userId = userId, profile = null, safeFilter = result.safeFilter ?: true))
        refreshProfile()
        // A blocked account sees why right away (v0.13).
        refreshAccount()
    }

    /**
     * Makes sure Stips knows this session. After a restart, or when the session may have expired on the server
     * (Stips quietly answers account calls with empty data then), it checks, and signs in again with the token.
     * Only a clear "this token is not valid" ({"logged": false}) signs the user out; errors never do (#3).
     * [verify] checks with Stips even when the session was confirmed in the last [FRESH_MS] (#9: an empty list, or the
     * user pulling to refresh). Returns true when it had to sign in again.
     */
    suspend fun ensureSession(force: Boolean = false, verify: Boolean = false): Boolean {
        val token = _session.value.token ?: throw ApiException(ApiException.NOT_SIGNED_IN)
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val live = liveToken == token
            if (!force && !verify && live && now - confirmedAt < FRESH_MS) return false
            if (force && live && now - loggedInAt < RELOGIN_COOLDOWN_MS) return false
            if (!force && live && stillSignedIn()) {
                confirmedAt = now
                return false
            }
            val data = try {
                api.call("user.login", buildJsonObject { put("auth_token", token) }, forcePost = true)
            } catch (e: ApiException) {
                NetLog.event("session", "signing in again with the saved token failed: ${e.code}; keeping the session")
                throw e
            }
            if (Parsers.loginRejected(data)) {
                NetLog.event("session", "Stips rejected the saved sign-in (logged: false); signing out")
                expire()
                throw ApiException(ApiException.NOT_SIGNED_IN)
            }
            val result = Parsers.login(data)
            if (result?.userId == null) {
                NetLog.event("session", "unexpected answer to signing in again; keeping the session")
                throw ApiException(ApiException.BAD_RESPONSE)
            }
            val newToken = result.token ?: token
            liveToken = newToken
            confirmedAt = now
            loggedInAt = now
            NetLog.event("session", if (force) "signed in again (Stips said we were signed out)" else "session restored with the saved token")
            save(_session.value.copy(token = newToken, safeFilter = result.safeFilter ?: _session.value.safeFilter))
            true
        }
    }

    /** `user.get_app_user` is the one call that fails when the server session is gone; the others return empty data. */
    private suspend fun stillSignedIn(): Boolean = try {
        val user = Parsers.appUser(api.call("user.get_app_user"))
        val same = user != null && user.id == _session.value.userId
        if (same) _account.value = user
        same
    } catch (e: ApiException) {
        if (e.isTransient) throw e
        NetLog.event("session", "the server session expired (${e.code})")
        false
    }

    /**
     * Reads the account again (v0.13): its credibility, flowers and any block. Needs a live server session (Stips
     * fails this call without one), so it runs after the session is restored or right after signing in. Never throws.
     */
    suspend fun refreshAccount(): AppUser? {
        val userId = _session.value.userId ?: return null
        val user = try {
            Parsers.appUser(api.call("user.get_app_user"))
        } catch (e: ApiException) {
            NetLog.event("session", "account not read: ${e.code}")
            null
        }
        if (user == null || user.id != userId || _session.value.userId != userId) return null
        _account.value = user
        user.ban?.let { NetLog.event("session", "Stips blocks this account" + if (it.temporary) " until ${it.endsText}" else "") }
        return user
    }

    /**
     * Stips refused a call because the account is blocked (`API_ERROR_BANNED`). As the site does, the account is read
     * again (at most once a minute) for the block's reason and end. A refused action ([write]) shows the block screen
     * again; a refused read (a background check) doesn't, so it can't keep coming back (v0.13).
     */
    suspend fun onBanned(write: Boolean) {
        if (!_session.value.signedIn) return
        val now = System.currentTimeMillis()
        if (now - bannedCheckedAt >= BANNED_RECHECK_MS) {
            bannedCheckedAt = now
            refreshAccount()
        }
        if (write) _bannedSignals.value += 1
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
        NetLog.event("session", "signed out by the user")
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
        confirmedAt = 0L
        loggedInAt = 0L
        bannedCheckedAt = 0L
        _account.value = null
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

    private companion object {
        /** Stips (classic ASP) drops idle sessions after about 20 minutes; check sooner than that. */
        const val FRESH_MS = 10 * 60_000L

        /** Never sign in again more than once in this window, whatever the server says (no sign-in storms). */
        const val RELOGIN_COOLDOWN_MS = 30_000L

        /** A blocked account's refused actions read the account again at most this often. */
        const val BANNED_RECHECK_MS = 60_000L
    }
}
