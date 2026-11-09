package io.github.eyalshimshi.betterstips.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.StipsRepository
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.diag.NetLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the chats widget shows (#8): your latest chats, unread ones in bold with their count. Whenever the Chats screen
 * loads that list, the widget shows the same copy at no cost. By itself it asks Stips (one `messages.list` call) only
 * while a chats widget is on the home screen, and only when the unread count Stips reports changes (the app's 10 s
 * check while it's open, the 15-minute background check), when Android wakes it hourly and the copy is over
 * [STALE_MS] old, or on ⟳; never past the request budget (D-46). The copy stays on the phone: backups skip it, and
 * signing out deletes it.
 */
class ChatsWidgetFeed(
    private val context: Context,
    private val repository: StipsRepository,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    /** One chat as the widget shows it. [time] is epoch ms. */
    data class Chat(val userId: Long, val name: String, val text: String, val time: Long?, val unread: Int, val fromMe: Boolean)

    data class State(
        val chats: List<Chat> = emptyList(),
        /** The saved copy has been read (it may have been empty). */
        val loaded: Boolean = false,
        val refreshing: Boolean = false,
        /** The last refresh failed and there's nothing to show. */
        val failed: Boolean = false,
    ) {
        val unread: Int get() = chats.sumOf { it.unread }
    }

    /** The saved copy: whose chats, when, and the unread count Stips reported then. */
    data class Saved(val userId: Long, val savedAt: Long, val reportedUnread: Int?, val chats: List<Chat>)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val oneAtATime = Mutex()
    private val file = File(context.filesDir, FILE_NAME)

    @Volatile private var savedAt = 0L

    @Volatile private var savedReported: Int? = null

    @Volatile private var lastReported: Int? = null

    /** Listens to the repository (lists and counts Stips sends anyway) and to the account. */
    fun start() {
        repository.onConversations = { list -> publish(list) }
        repository.onUnreadCounts = { counts -> onUnread(counts.messages) }
        scope.launch {
            // Signing out, or into another account: the old account's chats leave the phone.
            session.session.map { it.userId }.distinctUntilChanged().drop(1).collect { clear() }
        }
    }

    /** Shows the saved copy, once per process, if it belongs to the signed-in account. */
    suspend fun ensureLoaded() {
        if (_state.value.loaded) return
        val me = session.session.value.userId
        val saved = withContext(Dispatchers.IO) { runCatching { if (file.exists()) decode(file.readText()) else null }.getOrNull() }
        _state.update { current ->
            when {
                current.loaded -> current
                saved != null && me != null && saved.userId == me -> {
                    savedAt = saved.savedAt
                    savedReported = saved.reportedUnread
                    current.copy(chats = saved.chats, loaded = true)
                }
                else -> current.copy(loaded = true)
            }
        }
    }

    /**
     * Asks Stips again when the copy is stale (or [force]) and the budget allows, or always when [byUser] (⟳). One at a
     * time: while one runs, another call returns at once, and the running one redraws when done.
     */
    suspend fun refresh(byUser: Boolean, force: Boolean = false) {
        if (!oneAtATime.tryLock()) return
        var redraw = false
        try {
            ensureLoaded()
            if (!session.session.value.signedIn) return
            if (!byUser) {
                if (!force && !isStale(savedAt, System.currentTimeMillis())) return
                if (!RateGuard.allowBackground()) return
            }
            redraw = byUser
            _state.update { it.copy(refreshing = true) }
            if (redraw) updateWidgets()
            // A successful load reaches publish() through the repository's onConversations.
            val loaded = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                try {
                    repository.conversations(1)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    NetLog.event("widget", "chats refresh failed: ${e.javaClass.simpleName}")
                    false
                }
            } ?: false
            if (!loaded) {
                _state.update { it.copy(failed = it.chats.isEmpty()) }
                redraw = true
            }
        } finally {
            _state.update { it.copy(refreshing = false) }
            oneAtATime.unlock()
            if (redraw) updateWidgets()
        }
    }

    /** Stips sent the first page of chats to some screen (or to the widget): keep it, and redraw if it changed. */
    private fun publish(list: List<Conversation>) {
        val chats = chatsOf(list, session.session.value.userId)
        val before = _state.value
        savedAt = System.currentTimeMillis()
        savedReported = lastReported ?: chats.sumOf { it.unread }
        _state.update { it.copy(chats = chats, loaded = true, failed = false) }
        if (!hasWidgets()) return
        val me = session.session.value.userId ?: return
        val saved = Saved(me, savedAt, savedReported, chats)
        scope.launch(Dispatchers.IO) { runCatching { file.writeText(encode(saved)) } }
        if (before.chats != chats || before.failed) scope.launch { updateWidgets() }
    }

    /** Stips reported the unread count: if it changed and a chats widget is placed, the list changed too. */
    private fun onUnread(count: Int) {
        if (lastReported == count) return
        scope.launch {
            if (!hasWidgets()) {
                lastReported = count
                return@launch
            }
            ensureLoaded()
            val previous = lastReported ?: savedReported
            lastReported = count
            if (previous != count) refresh(byUser = false, force = true)
        }
    }

    private suspend fun clear() {
        savedAt = 0L
        savedReported = null
        lastReported = null
        _state.value = State(loaded = false)
        withContext(Dispatchers.IO) { runCatching { file.delete() } }
        updateWidgets()
    }

    private fun hasWidgets(): Boolean = try {
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ChatsWidgetReceiver::class.java)).isNotEmpty()
    } catch (e: RuntimeException) {
        false
    }

    private suspend fun updateWidgets() {
        try {
            ChatsWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetLog.event("widget", "chats redraw failed: ${e.javaClass.simpleName}")
        }
    }

    companion object {
        const val FILE_NAME = "chats_widget.json"
        const val MAX_CHATS = 10
        const val STALE_MS = 30 * 60_000L
        private const val FETCH_TIMEOUT_MS = 15_000L

        /** Whether a copy saved at [savedAt] should be asked for again at [now]. Pure. */
        fun isStale(savedAt: Long, now: Long): Boolean = now - savedAt >= STALE_MS

        /** The chats the widget shows, in Stips' order (newest first): people only, not notes to self (#20), at most [MAX_CHATS]. Pure. */
        fun chatsOf(list: List<Conversation>, myId: Long? = null): List<Chat> = list.mapNotNull { c ->
            val userId = c.other.userId ?: return@mapNotNull null
            if (userId == myId) return@mapNotNull null
            Chat(userId, c.other.nickname, c.lastMessage, c.time?.toEpochMilli(), c.unread.coerceAtLeast(0), c.lastFromMe)
        }.take(MAX_CHATS)

        /** "14:05" today, "3 May" this year, else "3.5.25". Pure. */
        fun timeLabel(time: Long, now: Long, zone: ZoneId, locale: Locale): String {
            val then = Instant.ofEpochMilli(time).atZone(zone)
            val today = Instant.ofEpochMilli(now).atZone(zone)
            val pattern = when {
                then.toLocalDate() == today.toLocalDate() -> "HH:mm"
                then.year == today.year -> "d MMM"
                else -> "d.M.yy"
            }
            return then.format(DateTimeFormatter.ofPattern(pattern, locale))
        }

        fun encode(saved: Saved): String = buildJsonObject {
            put("userId", saved.userId)
            put("savedAt", saved.savedAt)
            saved.reportedUnread?.let { put("reported", it) }
            putJsonArray("chats") {
                saved.chats.forEach { chat ->
                    addJsonObject {
                        put("id", chat.userId)
                        put("name", chat.name)
                        put("text", chat.text)
                        chat.time?.let { put("time", it) }
                        put("unread", chat.unread)
                        put("me", chat.fromMe)
                    }
                }
            }
        }.toString()

        /** The saved copy, or null when [text] isn't one. Pure. */
        fun decode(text: String): Saved? = try {
            val o = Json.parseToJsonElement(text).jsonObject
            val userId = o["userId"]?.jsonPrimitive?.longOrNull
            if (userId == null) {
                null
            } else {
                val chats = o["chats"]?.jsonArray?.mapNotNull { element ->
                    val c = element as? JsonObject ?: return@mapNotNull null
                    val id = c["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    Chat(
                        userId = id,
                        name = c["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        text = c["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        time = c["time"]?.jsonPrimitive?.longOrNull,
                        unread = c["unread"]?.jsonPrimitive?.intOrNull ?: 0,
                        fromMe = c["me"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                }.orEmpty()
                Saved(userId, o["savedAt"]?.jsonPrimitive?.longOrNull ?: 0L, o["reported"]?.jsonPrimitive?.intOrNull, chats)
            }
        } catch (e: Exception) {
            null
        }
    }
}
