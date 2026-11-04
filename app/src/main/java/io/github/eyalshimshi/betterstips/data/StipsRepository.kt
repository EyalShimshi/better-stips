package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.AppUser
import io.github.eyalshimshi.betterstips.data.model.AskAllowance
import io.github.eyalshimshi.betterstips.data.model.AskDraft
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ChartAction
import io.github.eyalshimshi.betterstips.data.model.ChartPoint
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.ChatThread
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.data.model.FeedSort
import io.github.eyalshimshi.betterstips.data.model.GalleryPhoto
import io.github.eyalshimshi.betterstips.data.model.PenPalAllowance
import io.github.eyalshimshi.betterstips.data.model.PenPalPost
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Everything the screens ask of Stips. Requests happen only when the user acts (D-13). */
class StipsRepository(
    private val api: StipsApi,
    private val session: SessionStore,
    private val feedCache: FeedCache,
    val channels: ChannelsSource,
    val timings: Timings,
    val blocks: BlockStore,
) {
    private val questionCache = object : LinkedHashMap<Long, Question>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Question>?): Boolean = size > 300
    }

    /** When each cached question came from Stips (not from the disk cache). Guarded by [questionCache]. */
    private val cachedAt = HashMap<Long, Long>()

    fun cachedQuestion(id: Long): Question? = synchronized(questionCache) { questionCache[id] }

    /**
     * The question as a list showed it, if that was under [maxAgeMs] ago. List items carry the same fields as a
     * full fetch (link, pin state, permissions; checked), so opening it costs one request less (#5).
     */
    fun recentQuestion(id: Long, maxAgeMs: Long): Question? = synchronized(questionCache) {
        val at = cachedAt[id] ?: return@synchronized null
        if (System.currentTimeMillis() - at > maxAgeMs) null else questionCache[id]
    }

    /** Keeps the cached copy in step after the user changes it (a pin). */
    fun updateCachedQuestion(id: Long, change: (Question) -> Question) {
        synchronized(questionCache) {
            val current = questionCache[id] ?: return
            questionCache[id] = change(current)
        }
    }

    private fun remember(list: List<Question>, fresh: Boolean = true) = synchronized(questionCache) {
        val now = System.currentTimeMillis()
        list.forEach {
            questionCache[it.id] = it
            if (fresh) cachedAt[it.id] = now
        }
        if (cachedAt.size > 600) cachedAt.keys.retainAll(questionCache.keys)
    }

    private fun safeFilter(): Boolean = session.session.value.let { if (it.signedIn) it.safeFilter else true }

    /** Told about every first page Stips sends, from any screen, so the widget can show it without asking again. */
    @Volatile
    var onFirstPage: ((FeedQuery, List<Question>) -> Unit)? = null

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
        if (page == 1 && myList == null) onFirstPage?.invoke(query, list)
        return list
    }

    suspend fun cachedFirstPage(query: FeedQuery): List<Question> = withContext(Dispatchers.IO) {
        val text = runCatching { feedCache.read(query.cacheKey) }.getOrNull() ?: return@withContext emptyList()
        val list = runCatching { Parsers.questions(Json.parseToJsonElement(text)) }.getOrDefault(emptyList())
        remember(list, fresh = false)
        list
    }

    /** When [query]'s first page was last saved from Stips (epoch ms), or null. */
    suspend fun firstPageSavedAt(query: FeedQuery): Long? = withContext(Dispatchers.IO) {
        runCatching { feedCache.savedAt(query.cacheKey) }.getOrNull()
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
    /** A page of the thread, oldest first. The first page also says whether you blocked this person (`userBlocked`). */
    suspend fun thread(userId: Long, beforeId: Long?): ChatThread = withSession {
        val params = buildJsonObject {
            put("userid", userId)
            put("msgid", beforeId ?: 0L)
            if (beforeId == null) put("first_load", true) else put("history", true)
        }
        val page = Parsers.chatThread(api.call("messages.from_user", params))
        page.copy(messages = page.messages.sortedBy { it.id })
    }

    /**
     * Messages in the thread newer than [afterId]: what the site's open chat asks every 1.6 s (`first_load: false`,
     * no `history`). Stips+ asks every 3 s while a chat is on screen (#6).
     */
    suspend fun newMessages(userId: Long, afterId: Long): List<ChatMessage> = withSession {
        val params = buildJsonObject {
            put("userid", userId)
            put("msgid", afterId)
            put("first_load", false)
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

    // ---- Moderation (#4) ----

    /** Reports a question, an answer or a pen-pal post to Stips' moderators, in the user's own words (5–500 characters). */
    suspend fun report(objType: String, id: Long, reason: String) {
        withSession {
            api.call(
                "reports.send",
                buildJsonObject {
                    put("itemid", id)
                    put("itemtype", objType)
                    put("reason", reason)
                    put("reason_id", 0)
                },
                forcePost = true,
            )
        }
    }

    /** Reports a user from a chat (`itemtype: "usermsgs"`), with one of the site's reasons. */
    suspend fun reportUser(userId: Long, reasonId: Int, details: String) {
        withSession {
            api.call(
                "reports.send",
                buildJsonObject {
                    put("itemid", userId)
                    put("itemtype", "usermsgs")
                    put("reason", details)
                    put("reason_id", reasonId)
                },
                forcePost = true,
            )
        }
    }

    /**
     * "Block content": hides the item (and, with [authorId], everything by its author) on this phone, and tells
     * Stips as its app does (`ask/BlockContent`), so it can show less of it. The answer is ignored, like on the site.
     */
    suspend fun blockContent(objType: String, id: Long, authorId: Long?) {
        blocks.block(objType, id, authorId)
        if (!session.session.value.signedIn) return
        try {
            withSession {
                api.call(
                    "smartdata.get",
                    buildJsonObject {
                        put("namespace", "ask")
                        put("unit", "BlockContent")
                        put("id", id)
                        put("objType", objType)
                        if (authorId != null) put("blockUser", true)
                    },
                )
            }
        } catch (e: ApiException) {
            // The hiding already happened here; Stips hearing about it is a bonus.
        }
    }

    /** Blocks (or unblocks) private messages from a user (`messages.block_user`). */
    suspend fun setMessagesBlocked(userId: Long, blocked: Boolean) {
        withSession {
            api.call(
                "messages.block_user",
                buildJsonObject {
                    put("userid", userId)
                    put("action", if (blocked) "add" else "remove")
                },
            )
        }
    }

    // ---- Your own items (#4) ----

    /** Edits your question's title, details, tags and link (`omniobj` POST), when Stips allows it. */
    suspend fun editQuestion(id: Long, title: String, body: String, tags: List<String>, link: String?) {
        withSession {
            val omniobj = buildJsonObject {
                put("objType", "ask")
                putJsonObject("data") {
                    put("id", id)
                    put("q", title)
                    put("text_content", body)
                    putJsonArray("tagslist") { tags.forEach { add(it) } }
                    put("q_link", link.orEmpty())
                }
            }
            api.call("omniobj", restAction = "POST", omniobj = omniobj)
        }
    }

    /** Deletes your question, answer or pen-pal post (`omniobj` DELETE), when Stips allows it. */
    suspend fun deleteItem(objType: String, id: Long) {
        withSession {
            api.call(
                "omniobj",
                restAction = "DELETE",
                omniobj = buildJsonObject {
                    put("objType", objType)
                    putJsonObject("data") { put("id", id) }
                },
            )
        }
    }

    /** "Detach from my account" for a question (answers use [detachAnswer]). */
    suspend fun detachQuestion(id: Long) {
        withSession {
            api.call(
                "item.set_anonymous",
                buildJsonObject {
                    put("objType", "ask")
                    put("id", id)
                },
            )
        }
    }

    /** The asker gives an answer a flower (up to 5 per question, answers up to 14 days old). */
    suspend fun sendFlower(answerId: Long): Boolean = withSession {
        Parsers.flowerSent(api.call("item.send_flower", buildJsonObject { put("id", answerId) }, forcePost = true))
    }

    /** Writes a message on someone's thanks wall (`omniobj` PUT, `objType: "thanksmsg"`). */
    suspend fun writeThanks(userId: Long, text: String) {
        withSession {
            api.call(
                "omniobj",
                restAction = "PUT",
                omniobj = buildJsonObject {
                    put("objType", "thanksmsg")
                    putJsonObject("data") {
                        put("msg", text)
                        put("touserid", userId)
                    }
                },
            )
        }
    }

    /** Users whose nickname matches [query] (the site's header search, `users/Search`). */
    suspend fun searchUsers(query: String): List<Author> = Parsers.userSearch(
        api.call(
            "smartdata.get",
            buildJsonObject {
                put("namespace", "users")
                put("unit", "Search")
                put("q", query)
            },
        ),
    )

    // ---- Asking a question (#4) ----

    suspend fun askAllowance(): AskAllowance = withSession {
        Parsers.askAllowance(
            api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "ask")
                    put("unit", "DailyAskCount")
                },
            ),
        )
    }

    /** Tags Stips suggests for a question's title. */
    suspend fun suggestedTags(title: String): List<String> = Parsers.tags(api.call("item.autotags", buildJsonObject { put("title", title) }))

    /** Tags that start like what the user is typing. */
    suspend fun completeTag(prefix: String): List<String> = Parsers.tags(api.call("item.tag_suggest", buildJsonObject { put("q", prefix) }))

    /** Photos from Stips' own gallery for these tags, the way the site offers covers. */
    suspend fun galleryPhotos(tags: List<String>): List<GalleryPhoto> = Parsers.galleryPhotos(
        api.call("photos.tag_gallery", buildJsonObject { putJsonArray("tags") { tags.forEach { add(it) } } }),
    )

    /** Asks the question; returns its id. Same fields as the site (`ask_type` 11). */
    suspend fun askQuestion(draft: AskDraft): Long = withSession {
        val omniobj = buildJsonObject {
            put("objType", "ask")
            putJsonObject("data") {
                put("q", draft.title)
                put("q_link", draft.link.orEmpty())
                put("text_content", draft.body)
                putJsonArray("tagslist") { draft.tags.forEach { add(it) } }
                if (draft.photoId != null) put("photo", draft.photoId)
                if (draft.anonymous) {
                    put("anonflg", true)
                    put("name", draft.nickname.orEmpty())
                }
                put("ask_type", 11)
                if (draft.channelId != null) put("channel_id", draft.channelId)
            }
        }
        Parsers.newId(api.call("omniobj", restAction = "PUT", omniobj = omniobj)) ?: throw ApiException(ApiException.BAD_RESPONSE)
    }

    // ---- Pen pals, חברים לעט (#4) ----

    suspend fun penPals(page: Int): List<PenPalPost> = Parsers.penPals(
        api.call(
            "objectlist",
            buildJsonObject {
                put("method", "penfriendsitem.new")
                put("page", page)
            },
        ),
    )

    suspend fun penPalAllowance(): PenPalAllowance = withSession {
        Parsers.penPalAllowance(
            api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "penfriends")
                    put("unit", "PostAllowed")
                },
            ),
        )
    }

    suspend fun lastPenPalMessage(): String? = withSession {
        Parsers.lastPenPalMessage(
            api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "penfriends")
                    put("unit", "LastMessageForUser")
                },
            ),
        )
    }

    suspend fun postPenPal(text: String): Long? = withSession {
        Parsers.newId(
            api.call(
                "omniobj",
                restAction = "PUT",
                omniobj = buildJsonObject {
                    put("objType", "penfriendsitem")
                    putJsonObject("data") { put("msg", text) }
                },
            ),
        )
    }

    /**
     * Account calls: make sure the session exists; if Stips says we're signed out, sign in again with the token
     * and retry once. Other errors are passed on untouched (a failed post must not be sent twice).
     */
    private suspend fun <T> withSession(block: suspend () -> T): T {
        session.ensureSession()
        return try {
            block()
        } catch (e: ApiException) {
            if (!e.looksSignedOut) throw e
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
