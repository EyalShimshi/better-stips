package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.AppUser
import io.github.eyalshimshi.betterstips.data.model.ChartAction
import io.github.eyalshimshi.betterstips.data.model.ChartPoint
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.model.ThanksMessage
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.data.model.UserProfile
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
    suspend fun postAnswer(questionId: Long, text: String, anonymous: Boolean, link: AnswerLink? = null): Long? = withSession {
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
                if (link != null) {
                    put("link1", link.url)
                    put("link1name", link.title ?: link.url)
                }
            }
        }
        Parsers.newId(api.call("omniobj", restAction = "PUT", omniobj = omniobj))
    }

    /** Edits an answer (`omniobj` POST with only the changed fields, as the site does). Stips decides if it's still editable. */
    suspend fun editAnswer(answerId: Long, text: String, link: AnswerLink?) {
        withSession {
            val omniobj = buildJsonObject {
                put("objType", "ans")
                putJsonObject("data") {
                    put("id", answerId)
                    put("a", text)
                    put("link1", link?.url.orEmpty())
                    put("link1name", link?.let { it.title ?: it.url }.orEmpty())
                }
            }
            api.call("omniobj", restAction = "POST", omniobj = omniobj)
        }
    }

    /** "Detach from my profile": the answer shows as anonymous from now on. */
    suspend fun detachAnswer(answerId: Long) {
        withSession {
            api.call(
                "item.set_anonymous",
                buildJsonObject {
                    put("objType", "ans")
                    put("id", answerId)
                },
            )
        }
    }

    /** A page's title, used to name an attached link (the site does the same). */
    suspend fun linkTitle(url: String): String? = try {
        Parsers.urlTitle(
            api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "utils")
                    put("unit", "GetUrlMeta")
                    put("url", url)
                },
            ),
        )
    } catch (e: ApiException) {
        null
    }

    /** Pins (or unpins) a question for the signed-in user. */
    suspend fun setPinned(questionId: Long, pinned: Boolean) {
        withSession {
            api.call(
                "item.add_pin",
                buildJsonObject {
                    put("id", questionId)
                    put("minus", !pinned)
                },
            )
        }
    }

    /** Whether the user gets updates on a question. */
    suspend fun isFollowing(questionId: Long): Boolean? = withSession {
        Parsers.followState(
            api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "ask")
                    put("unit", "AskAdditionalData")
                    put("id", questionId)
                },
            ),
        )
    }

    suspend fun setFollowing(questionId: Long, follow: Boolean) {
        withSession {
            api.call(
                "item.notifications_register",
                buildJsonObject {
                    put("objType", "ask")
                    put("id", questionId)
                    put("add", follow)
                },
            )
        }
    }

    suspend fun userProfile(userId: Long): UserProfile {
        val data = api.call(
            "omniobj",
            restAction = "GET",
            omniobj = buildJsonObject {
                put("objType", "user")
                putJsonObject("data") { put("id", userId) }
            },
        )
        return Parsers.profile(data) ?: throw ApiException(ApiException.BAD_RESPONSE)
    }

    suspend fun profilePage(userId: Long): ProfilePage =
        Parsers.profilePage(api.call("profile.page_data", buildJsonObject { put("userid", userId) }))
            ?: throw ApiException(ApiException.BAD_RESPONSE)

    suspend fun chart(userId: Long, action: ChartAction): List<ChartPoint> = Parsers.chart(
        api.call(
            "profile.actions_chart",
            buildJsonObject {
                put("userid", userId)
                put("action", action.apiName)
            },
        ),
    )

    /** Answers of [userId] that got a flower, with their questions' titles. */
    suspend fun floweredAnswers(userId: Long, page: Int): List<Answer> = Parsers.answers(
        api.call(
            "objectlist",
            buildJsonObject {
                put("method", "ans.flower_for_user")
                put("userid", userId)
                put("page", page)
            },
        ),
    )

    suspend fun thanksWall(userId: Long, page: Int): List<ThanksMessage> = Parsers.thanksMessages(
        api.call(
            "objectlist",
            buildJsonObject {
                put("objType", "thanksmsg")
                put("userid", userId)
                put("page", page)
            },
        ),
    )

    /** Deletes a message from the signed-in user's own thanks wall (no reason needed, like on the site). */
    suspend fun deleteThanks(messageId: Long) {
        withSession {
            api.call(
                "omniobj",
                restAction = "DELETE",
                omniobj = buildJsonObject {
                    put("objType", "thanksmsg")
                    putJsonObject("data") { put("id", messageId) }
                },
            )
        }
    }

    suspend fun notifications(page: Int): List<StipsNotification> = withSession {
        Parsers.notifications(api.call("notifications.list", buildJsonObject { put("page", page) }))
    }

    /** Unread chats and notifications. Tiny response; safe to ask every minute while the app is open. */
    suspend fun unreadCounts(): UnreadCounts = withSession {
        Parsers.unreadCounts(api.call("messages.count", buildJsonObject { }))
    }

    /** The account's "safe mode" (hides questions Stips marks unsuitable). Stored on Stips, like on the site. */
    suspend fun setSafeMode(on: Boolean) {
        withSession {
            val userId = session.session.value.userId ?: throw ApiException(ApiException.NOT_SIGNED_IN)
            api.call(
                "user.update.settings",
                buildJsonObject {
                    put("userid", userId)
                    put("settings_name", "safeFilter")
                    put("value", on)
                },
            )
            session.setSafeFilter(on)
        }
    }

    suspend fun currentAppUser(): AppUser? = withSession { Parsers.appUser(api.call("user.get_app_user")) }

    /** Saves the signed-in user's bio ("text_status" on the profile page object). */
    suspend fun updateBio(text: String) {
        withSession {
            val userId = session.session.value.userId ?: throw ApiException(ApiException.NOT_SIGNED_IN)
            api.call(
                "omniobj",
                apiParams = buildJsonObject { put("createIfNotExist", true) },
                restAction = "POST",
                omniobj = buildJsonObject {
                    put("objType", "user_profile_page")
                    putJsonObject("data") {
                        put("userid", userId)
                        put("text_status", text)
                    }
                },
            )
        }
    }

    /** Uploads a square JPEG as the profile photo, then sets the thumbnail to the whole square (the site's two steps). */
    suspend fun uploadProfilePhoto(jpeg: ByteArray, size: Int) {
        withSession {
            val userId = session.session.value.userId ?: throw ApiException(ApiException.NOT_SIGNED_IN)
            val response = api.upload("$SITE_URL/scripts/upload/photo.asp?photo_type=profile_photo", "photo.jpg", jpeg, "image/jpeg")
            val (ok, error) = Parsers.uploadResult(response)
            if (!ok) throw ApiException(ApiException.GENERAL, error)
            api.call(
                "profile.set_thumbnail",
                buildJsonObject {
                    put("userid", userId)
                    put("x0", 0)
                    put("x1", size)
                    put("y0", 0)
                    put("y1", size)
                },
            )
        }
        session.refreshProfile()
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
