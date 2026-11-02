package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Everything the screens ask of Stips. Requests happen only when the user acts (D-13). */
class StipsRepository(
    private val api: StipsApi,
    private val session: SessionStore,
    private val feedCache: FeedCache,
    val channels: ChannelsSource,
    val timings: Timings,
) {
    private val questionCache = object : LinkedHashMap<Long, Question>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Question>?): Boolean = size > 300
    }

    fun cachedQuestion(id: Long): Question? = synchronized(questionCache) { questionCache[id] }

    private fun remember(list: List<Question>) = synchronized(questionCache) {
        list.forEach { questionCache[it.id] = it }
    }

    private fun safeFilter(): Boolean = session.session.value.let { if (it.signedIn) it.safeFilter else true }

    suspend fun feedPage(query: FeedQuery, page: Int): List<Question> {
        val myList = query.myList
        val params = buildJsonObject {
            when {
                myList != null -> {
                    put("method", "ask.user_activity")
                    put("for_current_user", true)
                    put("filter_section_name", myList.apiName)
                }
                query.sort == FeedSort.Hot -> {
                    put("method", "ask.hot")
                    put("filter_section_name", query.window.apiName)
                }
                else -> put("method", "ask.new")
            }
            query.channelName?.let { put("channel", it) }
            query.topic?.let { put("topic", it) }
            put("page", page)
            if (myList == null) put("safe_filter", safeFilter())
        }
        val started = System.nanoTime()
        val data = if (myList != null) withSession { api.call("objectlist", params) } else api.call("objectlist", params)
        val list = Parsers.questions(data)
        timings.record(Timings.FEED, elapsedMs(started))
        remember(list)
        if (page == 1 && myList == null && list.isNotEmpty()) {
            withContext(Dispatchers.IO) { runCatching { feedCache.write(query.cacheKey, data.toString()) } }
        }
        return list
    }

    suspend fun cachedFirstPage(query: FeedQuery): List<Question> = withContext(Dispatchers.IO) {
        val text = runCatching { feedCache.read(query.cacheKey) }.getOrNull() ?: return@withContext emptyList()
        val list = runCatching { Parsers.questions(Json.parseToJsonElement(text)) }.getOrDefault(emptyList())
        remember(list)
        list
    }

    suspend fun question(id: Long): Question {
        val data = api.call(
            "omniobj",
            restAction = "GET",
            omniobj = buildJsonObject {
                put("objType", "ask")
                putJsonObject("data") { put("id", id) }
            },
        )
        val question = Parsers.questionItem(data) ?: throw ApiException(ApiException.BAD_RESPONSE)
        remember(listOf(question))
        return question
    }

    suspend fun answers(questionId: Long): List<Answer> = Parsers.answers(
        api.call(
            "objectlist",
            buildJsonObject {
                put("method", "ans.for_item")
                put("askid", questionId)
            },
        ),
    )

    /** Posts an answer the way the site's form does (SPEC-04 §2.3). Returns the new answer's id if Stips sends it. */
    suspend fun postAnswer(questionId: Long, text: String, anonymous: Boolean): Long? = withSession {
        if (session.session.value.profile == null) session.refreshProfile()
        val profile = session.session.value.profile ?: throw ApiException(ApiException.NOT_SIGNED_IN)
        val nickname = when {
            !anonymous -> profile.nickname
            profile.female -> ANONYMOUS_FEMALE
            else -> ANONYMOUS_MALE
        }
        val omniobj = buildJsonObject {
            put("objType", "ans")
            putJsonObject("data") {
                put("askid", questionId)
                put("name", nickname)
                put("anonflg", anonymous)
                put("a", text)
            }
        }
        Parsers.newId(api.call("omniobj", restAction = "PUT", omniobj = omniobj))
    }

    /** 1 = up, -1 = down, 0 = clear (sending the current vote again clears it, as on the site). */
    suspend fun vote(answerId: Long, vote: Int) {
        withSession {
            api.call(
                "item.vote",
                buildJsonObject {
                    put("objType", "ans")
                    put("vote", vote)
                    put("id", answerId)
                },
            )
        }
    }

    suspend fun conversations(page: Int): List<Conversation> = withSession {
        Parsers.conversations(api.call("messages.list", buildJsonObject { put("page", page) }), session.session.value.userId)
    }

    /** The thread with one person. [beforeId] loads older messages. */
    suspend fun thread(userId: Long, beforeId: Long?): List<ChatMessage> = withSession {
        val params = buildJsonObject {
            put("userid", userId)
            put("msgid", beforeId ?: 0L)
            if (beforeId == null) put("first_load", true) else put("history", true)
        }
        Parsers.messages(api.call("messages.from_user", params)).sortedBy { it.id }
    }

    suspend fun sendMessage(userId: Long, text: String): Long? = withSession {
        Parsers.newId(
            api.call(
                "messages.send",
                buildJsonObject {
                    put("touserid", userId)
                    put("msg", text)
                },
                forcePost = true,
            ),
        )
    }

    /** Account calls: make sure the session exists; if Stips refuses once, sign in again with the token and retry. */
    private suspend fun <T> withSession(block: suspend () -> T): T {
        session.ensureSession()
        return try {
            block()
        } catch (e: ApiException) {
            if (e.isNetwork || e.code == ApiException.NOT_FOUND || e.code == ApiException.BANNED) throw e
            session.ensureSession(force = true)
            block()
        }
    }

    private fun elapsedMs(startedNanos: Long): Long = (System.nanoTime() - startedNanos) / 1_000_000

    companion object {
        const val ANONYMOUS_MALE = "\u05d0\u05e0\u05d5\u05e0\u05d9\u05de\u05d9"
        const val ANONYMOUS_FEMALE = "\u05d0\u05e0\u05d5\u05e0\u05d9\u05de\u05d9\u05ea"
    }
}
