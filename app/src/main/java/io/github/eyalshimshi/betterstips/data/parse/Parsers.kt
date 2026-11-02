package io.github.eyalshimshi.betterstips.data.parse

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.model.LoginResult
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.data.model.UserProfile
import io.github.eyalshimshi.betterstips.util.StipsTime
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
        return Author(
            nickname = profile?.str("nickname")?.takeIf { it.isNotBlank() } ?: fallbackName?.takeIf { it.isNotBlank() } ?: "",
            userId = if (anonymous) null else profile?.long("userid"),
            photoStamp = if (anonymous) null else profile?.str("photostamp")?.takeIf { it.isNotBlank() },
            anonymous = anonymous,
            online = profile?.bool("online") ?: false,
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

    /** `omniobj` GET for `objType: "user"`. Only the fields the site shows its owner are kept. */
    fun profile(data: JsonElement?): UserProfile? {
        val item = data.asObject()?.obj("omniOmniObj") ?: data.asObject() ?: return null
        val d = item.obj("data") ?: return null
        val id = d.long("id") ?: return null
        val extra = item.obj("extra")
        return UserProfile(
            userId = id,
            nickname = d.str("nickname").orEmpty(),
            points = d.int("points") ?: 0,
            female = d.int("gender") == 2,
            photoStamp = (extra?.str("photostamp") ?: extra?.obj("item_profile")?.str("photostamp"))?.takeIf { it.isNotBlank() },
        )
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

    /** Creating an object returns `{newid: …}`. */
    fun newId(data: JsonElement?): Long? = data.asObject()?.long("newid")
}
