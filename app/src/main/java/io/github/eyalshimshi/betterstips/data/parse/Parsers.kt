package io.github.eyalshimshi.betterstips.data.parse

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.AppUser
import io.github.eyalshimshi.betterstips.data.model.AskAllowance
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.ChartPoint
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.ChatThread
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.model.GalleryPhoto
import io.github.eyalshimshi.betterstips.data.model.HomeSection
import io.github.eyalshimshi.betterstips.data.model.LoginResult
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.PenPalAllowance
import io.github.eyalshimshi.betterstips.data.model.PenPalPost
import io.github.eyalshimshi.betterstips.data.model.ProfilePage
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.data.model.ThanksMessage
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.util.StipsTime
import io.github.eyalshimshi.betterstips.util.TextUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Turns Stips' JSON into models. Pure functions: a bad item is skipped, never fatal. */
object Parsers {

    /** Reads the `{status, error_code, error_msg, data}` envelope and returns `data`. */
    fun envelope(text: String): JsonElement? {
        val parsed = try {
            Json.parseToJsonElement(text)
        } catch (e: Exception) {
            throw ApiException(ApiException.BAD_RESPONSE)
        }
        val root = parsed.asObject() ?: throw ApiException(ApiException.BAD_RESPONSE)
        val status = root.str("status")
        if (status != "ok") {
            val code = root.str("error_code")?.takeIf { it.isNotBlank() } ?: ApiException.GENERAL
            throw ApiException(code, root.str("error_msg")?.takeIf { it.isNotBlank() })
        }
        return root["data"]
    }

    fun questions(data: JsonElement?): List<Question> = data.asArray()?.mapNotNull { question(it) } ?: emptyList()

    fun pinnedQuestions(data: JsonElement?): List<Question> = questions(data.asObject()?.get("pinnedItems"))

    /** `omniobj` GET returns the item under `omniOmniObj`. */
    fun questionItem(data: JsonElement?): Question? = question(data.asObject()?.get("omniOmniObj"))

    fun question(element: JsonElement?): Question? {
        val item = element.asObject() ?: return null
        val d = item.obj("data") ?: return null
        val id = d.long("id") ?: return null
        val extra = item.obj("extra")
        val permissions = item.obj("meta")?.obj("permissions")
        return Question(
            id = id,
            title = d.str("q").orEmpty().trim(),
            body = d.str("text_content").orEmpty().trim(),
            topics = d.arr("tagslist")?.mapNotNull { it.primitiveText()?.trim()?.takeIf { tag -> tag.isNotEmpty() } }.orEmpty(),
            postedAt = StipsTime.parse(d.str("time")),
            answerCount = d.int("answer_count") ?: 0,
            pinCount = d.int("pin_count") ?: 0,
            channelId = d.int("channel_id") ?: 0,
            author = author(extra?.obj("item_profile"), d.str("name"), d.bool("anonflg")),
            cover = cover(d.str("photo"), extra?.obj("content_image")),
            link = d.str("q_link")?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
            userPinned = d.int("user_pinned") == 1,
            canEdit = permissions?.bool("edit") ?: false,
            canDelete = permissions?.bool("delete") == true || permissions?.bool("deleteWithoutMsg") == true,
            isOwner = permissions?.bool("itemOwner") ?: false,
            canReport = permissions?.bool("report") ?: false,
            raw = item.toString(),
        )
    }

    private fun cover(photo: String?, contentImage: JsonObject?): Cover? {
        val small = contentImage?.str("url_small")?.takeIf { it.startsWith("https://") }
        if (small != null) {
            val medium = contentImage.str("url_medium")?.takeIf { it.startsWith("https://") } ?: small
            return Cover.Unsplash(small, medium, contentImage.str("creator_name")?.takeIf { it.isNotBlank() })
        }
        val id = photo?.trim()
        if (id != null && id.isNotEmpty() && id != "0" && id.all { it.isDigit() }) return Cover.Stips(id)
        return null
    }

    fun author(profile: JsonObject?, fallbackName: String?, anonymousFlag: Boolean?): Author {
        val anonymous = profile?.bool("anonflg") ?: anonymousFlag ?: false
        // Present on special accounts only (measured: Net Knights); Stips may send it as an empty array otherwise.
        val special = profile?.obj("specialConfig")
        return Author(
            nickname = profile?.str("nickname")?.takeIf { it.isNotBlank() } ?: fallbackName?.takeIf { it.isNotBlank() } ?: "",
            userId = if (anonymous) null else profile?.long("userid"),
            photoStamp = if (anonymous) null else profile?.str("photostamp")?.takeIf { it.isNotBlank() },
            anonymous = anonymous,
            online = profile?.bool("online") ?: false,
            branding = if (anonymous) null else special?.str("itemSpecialRankBranding")?.trim()?.takeIf { it.isNotEmpty() },
            highlightName = !anonymous && special?.bool("itemNicknameSpecialRankColor") == true,
            platform = profile?.bool("isPlatfromAdmin") == true || profile?.bool("isPlatformAdmin") == true,
        )
    }

    fun answers(data: JsonElement?): List<Answer> = data.asArray()?.mapNotNull { answer(it) } ?: emptyList()

    fun answer(element: JsonElement?): Answer? {
        val item = element.asObject() ?: return null
        val d = item.obj("data") ?: return null
        val id = d.long("id") ?: return null
        val extra = item.obj("extra")
        val links = (1..3).mapNotNull { n ->
            val url = d.str("link$n")?.trim()
            if (url.isNullOrEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) null
            else AnswerLink(url, d.str("link${n}name")?.trim()?.takeIf { it.isNotEmpty() })
        }
        val permissions = item.obj("meta")?.obj("permissions")
        return Answer(
            id = id,
            questionId = d.long("askid") ?: 0,
            text = d.str("a").orEmpty().trim(),
            postedAt = StipsTime.parse(d.str("time")),
            score = d.int("votes_score") ?: 0,
            userVote = extra?.int("userVote") ?: 0,
            flowered = extra?.bool("flowered") ?: false,
            author = author(extra?.obj("item_profile"), d.str("name"), d.bool("anonflg")),
            links = links,
            canEdit = permissions?.bool("edit") ?: false,
            isOwner = permissions?.bool("itemOwner") ?: false,
            parentTitle = extra?.str("parent_item_title")?.trim()?.takeIf { it.isNotEmpty() },
            canDelete = permissions?.bool("delete") == true || permissions?.bool("deleteWithoutMsg") == true,
            canReport = permissions?.bool("report") ?: false,
            byAsker = extra?.bool("parentItemCreatorResponse") ?: false,
            raw = item.toString(),
        )
    }

    private val hexColor = Regex("#([0-9a-fA-F]{6})\\b")

    fun parseColors(text: String?): List<Int> = hexColor.findAll(text.orEmpty())
        .map { (0xFF000000L or it.groupValues[1].toLong(16)).toInt() }
        .toList()

    /** The live list from `smartdata.get` (RemoteConfig). */
    fun channelsFromConfig(data: JsonElement?): List<Channel> {
        val list = data.asObject()?.obj("results")?.obj("itemChannels")?.arr("channels") ?: return emptyList()
        return list.mapNotNull { element ->
            val c = element.asObject() ?: return@mapNotNull null
            val id = c.int("id") ?: return@mapNotNull null
            val name = c.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Channel(id, name, nameEn = null, icon = c.str("icon").orEmpty(), colors = parseColors(c.str("backgroundColorCss")))
        }
    }

    /** The bundled snapshot in `assets/channels.json`. */
    fun channelsFromAsset(text: String): List<Channel> {
        val root = Json.parseToJsonElement(text).asObject() ?: return emptyList()
        return root.arr("channels")?.mapNotNull { element ->
            val c = element.asObject() ?: return@mapNotNull null
            val id = c.int("id") ?: return@mapNotNull null
            val name = c.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val colors = c.arr("colors")?.mapNotNull { it.primitiveText() }?.flatMap { parseColors(it) }.orEmpty()
            Channel(id, name, c.str("nameEn"), c.str("icon").orEmpty(), colors)
        }.orEmpty()
    }

    /** `user.login` returns `{appUser: {...}, a_token: "..."}` when it works. */
    fun login(data: JsonElement?): LoginResult? {
        val d = data.asObject() ?: return null
        val user = d.obj("appUser") ?: return null
        return LoginResult(
            token = d.str("a_token")?.takeIf { it.isNotBlank() },
            userId = user.long("id") ?: user.long("userid"),
            safeFilter = user.bool("safeFilter") ?: user.bool("safe_filter"),
        )
    }

    /** `user.login` with a token Stips no longer accepts answers `{"logged": false}` (measured). */
    fun loginRejected(data: JsonElement?): Boolean {
        val d = data.asObject() ?: return false
        return d.bool("logged") == false && d.obj("appUser") == null
    }

    /** `omniobj` GET for `objType: "user"`. Only the fields the site shows its owner are kept. */
    fun profile(data: JsonElement?): UserProfile? {
        val item = data.asObject()?.obj("omniOmniObj") ?: data.asObject() ?: return null
        val d = item.obj("data") ?: return null
        val id = d.long("id") ?: return null
        val extra = item.obj("extra")
        val itemProfile = extra?.obj("item_profile")
        val special = itemProfile?.obj("specialConfig")
        return UserProfile(
            userId = id,
            nickname = d.str("nickname").orEmpty(),
            points = d.int("points") ?: 0,
            female = d.int("gender") == 2,
            photoStamp = (extra?.str("photostamp") ?: itemProfile?.str("photostamp"))?.takeIf { it.isNotBlank() },
            online = itemProfile?.bool("online") ?: false,
            branding = special?.str("itemSpecialRankBranding")?.trim()?.takeIf { it.isNotEmpty() },
            highlightName = special?.bool("itemNicknameSpecialRankColor") == true,
            platform = itemProfile?.bool("isPlatfromAdmin") == true || itemProfile?.bool("isPlatformAdmin") == true,
            // A CSS gradient such as "linear-gradient(135deg, #f64f59, #b45ae0, #109cbb)" (measured).
            background = parseColors(special?.str("profileBackground")),
            raw = item.toString(),
        )
    }

    /** `profile.page_data {userid}`: counts, age, account age, badges and the bio (`text_status`). */
    fun profilePage(data: JsonElement?): ProfilePage? {
        val d = data.asObject() ?: return null
        val page = d.obj("user_profile_page")
        val bio = page?.obj("data")?.str("text_status")?.trim()?.takeIf { it.isNotEmpty() }
            ?: d.str("profile_quote")?.trim()?.takeIf { it.isNotEmpty() }
        val badges = d.arr("badges")?.mapNotNull { it.asObject() }.orEmpty()
        return ProfilePage(
            questions = d.int("questions") ?: 0,
            answers = d.int("answers") ?: 0,
            flowers = d.int("flowers") ?: 0,
            activeSince = d.str("hebrew_active_since")?.trim()?.takeIf { it.isNotEmpty() },
            age = d.int("age")?.takeIf { it in 1..120 },
            badges = badges.mapNotNull { it.str("name")?.takeIf { name -> name.isNotBlank() } },
            bio = bio,
            bioEditable = page?.obj("meta")?.obj("permissions")?.bool("edit") ?: false,
            customBadges = badges.filter { it.str("name") == "custom_rank" }
                .mapNotNull { it.str("label")?.trim()?.takeIf { label -> label.isNotEmpty() } },
            hideAge = d.bool("hideAge") == true,
            hideStats = d.bool("hideStats") == true,
            raw = d.toString(),
        )
    }

    /**
     * `feed.get`: the website's home feed, sections of today's questions by topic (#8). Like the site, a section that
     * repeats an earlier topic adds its new questions to it.
     */
    fun homeSections(data: JsonElement?): List<HomeSection> {
        val results = data.asObject()?.arr("results") ?: return emptyList()
        val sections = LinkedHashMap<String, MutableList<Question>>()
        val topics = HashMap<String, String>()
        results.forEach { element ->
            val e = element.asObject() ?: return@forEach
            // As on the site: a topic section shows its name and links to it; an item list shows its title.
            val topic = if (e.str("elementType") == "topic") e.str("name")?.trim()?.takeIf { it.isNotEmpty() } else null
            val title = (topic ?: e.str("title") ?: e.str("name"))?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            val questions = e.arr("items")?.mapNotNull { question(it) }.orEmpty()
            if (questions.isEmpty()) return@forEach
            val list = sections.getOrPut(title) { mutableListOf() }
            if (topic != null) topics.putIfAbsent(title, topic)
            questions.forEach { q -> if (list.none { it.id == q.id }) list.add(q) }
        }
        return sections.map { (title, questions) -> HomeSection(title, questions, topics[title]) }
    }

    /** `profile.actions_chart`: weekly counts, returned newest first; sorted oldest first here. */
    fun chart(data: JsonElement?): List<ChartPoint> =
        data.asObject()?.arr("chartData")?.mapNotNull { element ->
            val p = element.asObject() ?: return@mapNotNull null
            val time = StipsTime.parse(p.str("time")) ?: return@mapNotNull null
            ChartPoint(time, (p.int("actionsCount") ?: 0).coerceAtLeast(0))
        }?.sortedBy { it.time }.orEmpty()

    /** A thanks wall: `objectlist {objType:"thanksmsg", userid}`. The wall's owner may delete messages. */
    fun thanksMessages(data: JsonElement?): List<ThanksMessage> = data.asArray()?.mapNotNull { element ->
        val item = element.asObject() ?: return@mapNotNull null
        val d = item.obj("data") ?: return@mapNotNull null
        val id = d.long("id") ?: return@mapNotNull null
        val permissions = item.obj("meta")?.obj("permissions")
        ThanksMessage(
            id = id,
            text = d.str("msg").orEmpty().trim(),
            time = StipsTime.parse(d.str("time")),
            author = author(item.obj("extra")?.obj("item_profile"), null, false),
            canDelete = permissions?.bool("deleteWithoutMsg") == true || permissions?.bool("delete") == true,
        )
    }.orEmpty()

    /** `notifications.list`: HTML text and a link target ("ask" or "profile"). */
    fun notifications(data: JsonElement?): List<StipsNotification> = data.asArray()?.mapNotNull { element ->
        val n = element.asObject() ?: return@mapNotNull null
        val id = n.long("id") ?: return@mapNotNull null
        val targetId = n.long("linkItemId")
        val target = when {
            targetId == null -> null
            n.str("linkPageName") == "ask" -> NotificationTarget.OpenQuestion(targetId)
            n.str("linkPageName") == "profile" -> NotificationTarget.OpenProfile(targetId)
            else -> null
        }
        val html = TextUtils.htmlToPlain(n.str("contentHtml"))
        val msg = n.str("msgText")?.trim()?.takeIf { it.isNotEmpty() }
        StipsNotification(
            id = id,
            text = html.ifBlank { msg.orEmpty() },
            type = n.str("type").orEmpty(),
            viewed = n.bool("viewed") ?: true,
            time = StipsTime.parse(n.str("time")),
            target = target,
            note = if (html.isNotBlank()) msg else null,
            pointsRemoved = (n.int("pointsRemoved") ?: 0).coerceAtLeast(0),
            raw = n.toString(),
        )
    }.orEmpty()

    /** `messages.count`: unread messages and notifications. */
    fun unreadCounts(data: JsonElement?): UnreadCounts {
        val d = data.asObject() ?: return UnreadCounts()
        return UnreadCounts(
            messages = (d.int("messagesCount") ?: 0).coerceAtLeast(0),
            notifications = (d.int("notificationsCount") ?: 0).coerceAtLeast(0),
        )
    }

    /** `user.get_app_user`. Here gender is a word ("male"/"female"), unlike the user object's 1/2. */
    fun appUser(data: JsonElement?): AppUser? {
        val u = data.asObject()?.obj("appUser") ?: return null
        val id = u.long("id") ?: return null
        val gender = u.str("gender").orEmpty()
        return AppUser(
            id = id,
            points = u.int("points") ?: 0,
            flowers = u.int("flowersCount") ?: 0,
            safeFilter = u.bool("safeFilter") ?: true,
            female = gender == "female" || gender == "2",
        )
    }

    /** `smartdata.get` AskAdditionalData: whether the user gets updates on a question. */
    fun followState(data: JsonElement?): Boolean? =
        data.asObject()?.obj("results")?.bool("notificationsRegistered")

    /** `smartdata.get` utils/GetUrlMeta: a page's title, used to name attached links. */
    fun urlTitle(data: JsonElement?): String? =
        data.asObject()?.obj("results")?.str("title")?.trim()?.takeIf { it.isNotEmpty() }

    /** The photo upload script answers `{success: true}` or `{success: false, error: "…"}`. */
    fun uploadResult(text: String): Pair<Boolean, String?> {
        val root: JsonObject? = try {
            Json.parseToJsonElement(text).asObject()
        } catch (e: Exception) {
            null
        }
        if (root == null) return false to null
        return (root.bool("success") == true) to root.str("error")?.takeIf { it.isNotBlank() }
    }

    /** `messages.list`: one row per conversation; `itemProfile` is the other person. */
    fun conversations(data: JsonElement?, myUserId: Long?): List<Conversation> {
        val list = data.asObject()?.arr("messages") ?: return emptyList()
        return list.mapNotNull { element ->
            val m = element.asObject() ?: return@mapNotNull null
            val profile = m.obj("itemProfile") ?: m.obj("item_profile")
            val other = author(profile, null, false)
            if (other.userId == null) return@mapNotNull null
            Conversation(
                other = other,
                lastMessage = m.str("msg").orEmpty().trim(),
                time = StipsTime.parse(m.str("time")),
                timeText = m.str("hebrew_time"),
                unread = m.int("newCount") ?: m.int("new_count") ?: 0,
                lastFromMe = myUserId != null && m.long("fromuserid") == myUserId,
            )
        }
    }

    /** `messages.from_user`: the thread with one person, in the order the server sends. */
    fun messages(data: JsonElement?): List<ChatMessage> {
        val list = data.asObject()?.arr("messages") ?: return emptyList()
        return list.mapNotNull { element ->
            val m = element.asObject() ?: return@mapNotNull null
            val id = m.long("id") ?: return@mapNotNull null
            ChatMessage(
                id = id,
                text = m.str("msg").orEmpty().trim(),
                time = StipsTime.parse(m.str("time")),
                fromUserId = m.long("fromuserid"),
                toUserId = m.long("touserid"),
            )
        }
    }

    /** `messages.from_user` with `userBlocked`, which the site reads on the first page: you blocked this person. */
    fun chatThread(data: JsonElement?): ChatThread =
        ChatThread(messages(data), data.asObject()?.bool("userBlocked") ?: false)

    /** Creating an object returns `{newid: …}`. */
    fun newId(data: JsonElement?): Long? = data.asObject()?.long("newid")

    /** `item.autotags {title}` and `item.tag_suggest {q}`: `{tags: [...]}`. */
    fun tags(data: JsonElement?): List<String> =
        data.asObject()?.arr("tags")?.mapNotNull { it.primitiveText()?.trim()?.takeIf { tag -> tag.isNotEmpty() } }?.distinct().orEmpty()

    /** `photos.tag_gallery {tags}`: Stips' own photos for those tags. */
    fun galleryPhotos(data: JsonElement?): List<GalleryPhoto> =
        data.asObject()?.arr("photos")?.mapNotNull { element ->
            val id = element.asObject()?.str("photo_id")?.trim()
            if (id.isNullOrEmpty() || !id.all { it.isDigit() }) null else GalleryPhoto(id)
        }?.distinctBy { it.photoId }.orEmpty()

    /** `ask/DailyAskCount`: `{allowed, maxPerDay}`. Unknown answers allow asking; the server has the last word. */
    fun askAllowance(data: JsonElement?): AskAllowance {
        val r = data.asObject()?.obj("results")
        return AskAllowance(allowed = r?.bool("allowed") ?: true, maxPerDay = r?.int("maxPerDay"))
    }

    /** `objectlist {method: "penfriendsitem.new"}`. */
    fun penPals(data: JsonElement?): List<PenPalPost> = data.asArray()?.mapNotNull { element ->
        val item = element.asObject() ?: return@mapNotNull null
        val d = item.obj("data") ?: return@mapNotNull null
        val id = d.long("id") ?: return@mapNotNull null
        if (d.bool("active") == false) return@mapNotNull null
        val permissions = item.obj("meta")?.obj("permissions")
        PenPalPost(
            id = id,
            text = d.str("msg").orEmpty().trim(),
            time = StipsTime.parse(d.str("time")),
            author = author(item.obj("extra")?.obj("item_profile"), null, false),
            canDelete = permissions?.bool("delete") == true || permissions?.bool("deleteWithoutMsg") == true,
        )
    }.orEmpty()

    /** `penfriends/PostAllowed`: `{allowed, postInterval, nextTimeInMinutes}`. */
    fun penPalAllowance(data: JsonElement?): PenPalAllowance {
        val r = data.asObject()?.obj("results")
        return PenPalAllowance(allowed = r?.bool("allowed") ?: true, intervalMinutes = r?.int("postInterval"), nextInMinutes = r?.int("nextTimeInMinutes"))
    }

    /** `penfriends/LastMessageForUser`: `{messageText}`. */
    fun lastPenPalMessage(data: JsonElement?): String? =
        data.asObject()?.obj("results")?.str("messageText")?.trim()?.takeIf { it.isNotEmpty() }

    /** `item.send_flower {id}` answers `{flowerSent: true}`. */
    fun flowerSent(data: JsonElement?): Boolean = data.asObject()?.bool("flowerSent") ?: false

    /** `users/Search {q}`: `{profiles: [...]}` in the shape of `item_profile`. */
    fun userSearch(data: JsonElement?): List<Author> =
        data.asObject()?.obj("results")?.arr("profiles")?.mapNotNull { element ->
            val profile = element.asObject() ?: return@mapNotNull null
            val user = author(profile, null, false)
            if (user.userId == null || user.nickname.isBlank()) null else user
        }?.distinctBy { it.userId }.orEmpty()
}
