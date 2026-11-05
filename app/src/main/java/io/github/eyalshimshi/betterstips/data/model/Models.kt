package io.github.eyalshimshi.betterstips.data.model

import java.time.Instant

const val SITE_URL = "https://stips.co.il"
const val CDN_URL = "https://cdn.stips.co.il"

data class Author(
    val nickname: String,
    val userId: Long?,
    val photoStamp: String?,
    val anonymous: Boolean,
    val online: Boolean,
    /** Stips' branding for a special account (`specialConfig.itemSpecialRankBranding`, e.g. `RANK_NET_KNIGHT`). */
    val branding: String? = null,
    /** Stips colors this account's name (`specialConfig.itemNicknameSpecialRankColor`). */
    val highlightName: Boolean = false,
    /** Stips' own system account, "מערכת סטיפס" (`isPlatfromAdmin`, spelled so by Stips). */
    val platform: Boolean = false,
) {
    /** An organization working inside Stips, by Stips' branding or by [Organizations] (#8). */
    val organization: Boolean get() = !anonymous && (branding != null || Organizations.isKnown(userId))

    /** Gets the verified mark: an organization, or Stips itself. */
    val verified: Boolean get() = platform || organization

    /** The cropped square photo: `t` is 50 px, `a` 200 px (measured). */
    fun avatarUrl(large: Boolean = false): String? {
        if (anonymous || userId == null || photoStamp.isNullOrBlank()) return null
        val size = if (large) "a" else "t"
        return "$CDN_URL/photos/user_profile/$size/$userId/$photoStamp.jpg"
    }

    /** The whole uploaded photo (`l`, the largest Stips keeps), for the full-screen viewer. */
    fun fullPhotoUrl(): String? {
        if (anonymous || userId == null || photoStamp.isNullOrBlank()) return null
        return "$CDN_URL/photos/user_profile/l/$userId/$photoStamp.jpg"
    }
}

/**
 * Organizations that answer on Stips (#8). Stips brands some of them itself (`specialConfig`, for example Net Knights);
 * others, such as Sahar's volunteers, get no flag from Stips, so they're listed here by account.
 */
object Organizations {
    /** Sahar's volunteers (emotional support online) and the Net Knights (online safety), as the owner named them. */
    val KNOWN_USER_IDS: Set<Long> = setOf(10486L, 253000L)

    fun isKnown(userId: Long?): Boolean = userId != null && userId in KNOWN_USER_IDS
}

sealed interface Cover {
    val thumbnailUrl: String
    val largeUrl: String

    /** The biggest version, for the full-screen viewer. */
    val fullUrl: String

    /** A photo from Stips' own gallery. */
    data class Stips(val photoId: String) : Cover {
        override val thumbnailUrl get() = "$CDN_URL/photos/s/$photoId.jpg"
        override val largeUrl get() = "$CDN_URL/photos/w400/$photoId.jpg"

        /** `z` is the largest size Stips serves (about 500–640 px wide). */
        override val fullUrl get() = "$CDN_URL/photos/z/$photoId.jpg"
    }

    /** A stock photo the asker picked; Unsplash asks for the photographer's credit. */
    data class Unsplash(val small: String, val medium: String, val creatorName: String?) : Cover {
        override val thumbnailUrl get() = small
        override val largeUrl get() = medium

        /** Unsplash resizes on request: asking for 1080 px wide works (measured). */
        override val fullUrl get() = Cover.widen(medium, 1080)
    }

    companion object {
        private val widthParam = Regex("([?&])w=\\d+")

        /** The same Unsplash image at [width] pixels. Pure. */
        fun widen(url: String, width: Int): String = when {
            widthParam.containsMatchIn(url) -> widthParam.replace(url) { it.groupValues[1] + "w=" + width }
            url.contains('?') -> "$url&w=$width"
            else -> "$url?w=$width"
        }
    }
}

data class Question(
    val id: Long,
    val title: String,
    val body: String,
    val topics: List<String>,
    val postedAt: Instant?,
    val answerCount: Int,
    val pinCount: Int,
    val channelId: Int,
    val author: Author,
    val cover: Cover?,
    /** The link the asker attached (`q_link`), if any. */
    val link: String? = null,
    /** What Stips lets the signed-in user do with this question (`meta.permissions`). */
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isOwner: Boolean = false,
    val canReport: Boolean = false,
    /** Whether the signed-in user pinned it (`user_pinned == 1`). */
    val userPinned: Boolean = false,
    /** The item as Stips sent it (compact JSON), for expert mode (#8). */
    val raw: String? = null,
) {
    val shareUrl: String get() = "$SITE_URL/ask/$id"
}

data class AnswerLink(val url: String, val title: String?)

data class Answer(
    val id: Long,
    val questionId: Long,
    val text: String,
    val postedAt: Instant?,
    val score: Int,
    val userVote: Int,
    val flowered: Boolean,
    val author: Author,
    val links: List<AnswerLink>,
    /** Stips allows editing (it does for a while after posting). */
    val canEdit: Boolean = false,
    /** The signed-in user wrote it (also when it's anonymous). */
    val isOwner: Boolean = false,
    /** The question's title, sent with lists such as a user's flowered answers. */
    val parentTitle: String? = null,
    val canDelete: Boolean = false,
    val canReport: Boolean = false,
    /** The asker's own reply on their question (it can't get a flower). */
    val byAsker: Boolean = false,
    /** The item as Stips sent it (compact JSON), for expert mode (#8). */
    val raw: String? = null,
)

data class Channel(
    val id: Int,
    /** The Hebrew name; Stips' API filters channels by this name. */
    val name: String,
    val nameEn: String?,
    val icon: String,
    /** ARGB colors of the channel's gradient, strongest first. */
    val colors: List<Int>,
) {
    val seedColor: Int? get() = colors.firstOrNull()
}

data class UserProfile(
    val userId: Long,
    val nickname: String,
    /** Stips' credibility score ("אמינות"), 0–100. */
    val points: Int,
    val female: Boolean,
    val photoStamp: String?,
    val online: Boolean = false,
    /** As on [Author]: Stips' branding and colored name for special accounts, and its own system account. */
    val branding: String? = null,
    val highlightName: Boolean = false,
    val platform: Boolean = false,
    /** The colors of the header background Stips gives some special accounts (`specialConfig.profileBackground`). */
    val background: List<Int> = emptyList(),
    /** The user object as Stips sent it (compact JSON), for expert mode (#8). */
    val raw: String? = null,
) {
    val avatar: Author
        get() = Author(
            nickname,
            userId,
            photoStamp,
            anonymous = false,
            online = online,
            branding = branding,
            highlightName = highlightName,
            platform = platform,
        )
}

/** `profile.page_data`: what a profile page shows besides the user object. */
data class ProfilePage(
    val questions: Int,
    val answers: Int,
    val flowers: Int,
    /** Stips' Hebrew phrase, e.g. "6 שנים ו-9 חודשים". */
    val activeSince: String?,
    val age: Int?,
    val badges: List<String>,
    val bio: String?,
    val bioEditable: Boolean,
    /** Labels of `custom_rank` badges, which Stips names per account (e.g. "מגן מקוון"). */
    val customBadges: List<String> = emptyList(),
    /** The account asks Stips to hide its age, or its counts (`hideAge`, `hideStats`); the site hides them. */
    val hideAge: Boolean = false,
    val hideStats: Boolean = false,
    /** The page data as Stips sent it (compact JSON), for expert mode (#8). */
    val raw: String? = null,
)

/**
 * A section of the website's home feed (`feed.get`, #8): a heading and some of today's questions. A `topic` section's
 * heading is the topic's name and opens it ([topic]); an `itemList` section has a plain title.
 */
data class HomeSection(val title: String, val questions: List<Question>, val topic: String? = null)

enum class ChartAction(val apiName: String) { Answers("ans"), Questions("ask"), Flowers("flowers") }

data class ChartPoint(val time: Instant, val count: Int)

data class ThanksMessage(
    val id: Long,
    val text: String,
    val time: Instant?,
    val author: Author,
    val canDelete: Boolean,
)

sealed interface NotificationTarget {
    data class OpenQuestion(val id: Long) : NotificationTarget
    data class OpenProfile(val id: Long) : NotificationTarget
}

/** Where a question opens when a notification leads to it (v0.10). */
sealed interface QuestionFocus {
    /** New answers: [count] of them when the notification says how many, as of [at] (the notification's time). */
    data class NewAnswers(val count: Int?, val at: Instant?) : QuestionFocus

    /** A flower on one of the user's own answers. */
    data object MyFlower : QuestionFocus
}

/** Stips' moderators, "נאמני סטיפס", known from their profile badges (v0.10). */
enum class ModeratorRank {
    Moderator,
    Senior,
    ;

    companion object {
        fun of(badges: Collection<String>): ModeratorRank? = when {
            "senior_moderator" in badges -> Senior
            "moderator" in badges -> Moderator
            else -> null
        }
    }
}

data class StipsNotification(
    val id: Long,
    val text: String,
    val type: String,
    val viewed: Boolean,
    val time: Instant?,
    val target: NotificationTarget?,
    /** A note from Stips' team (`msgText`), shown in a box under the text. */
    val note: String? = null,
    /** Credibility points a moderator removed (`pointsRemoved`). */
    val pointsRemoved: Int = 0,
    /** What Stips sent, compact, for expert mode's details. */
    val raw: String? = null,
)

data class UnreadCounts(val messages: Int = 0, val notifications: Int = 0)

/** `user.get_app_user`: the signed-in account. */
data class AppUser(val id: Long, val points: Int, val flowers: Int, val safeFilter: Boolean, val female: Boolean)

data class LoginResult(val token: String?, val userId: Long?, val safeFilter: Boolean?)

data class Conversation(
    val other: Author,
    val lastMessage: String,
    val time: Instant?,
    val timeText: String?,
    val unread: Int,
    val lastFromMe: Boolean,
)

data class ChatMessage(
    val id: Long,
    val text: String,
    val time: Instant?,
    val fromUserId: Long?,
    val toUserId: Long?,
)

/** A page of `messages.from_user`. [blocked]: you blocked messages from this person (`userBlocked`, first page). */
data class ChatThread(val messages: List<ChatMessage>, val blocked: Boolean)

data class SavedQuestion(val id: Long, val title: String, val coverUrl: String?, val savedAt: Long)

enum class FeedSort { New, Hot }

enum class HotWindow(val apiName: String) { Now("now"), Today("today"), Week("week"), Month("month") }

/** The signed-in user's lists, as Stips names them. */
enum class MyList(val apiName: String) { Asked("asked"), Answered("ansed"), Pinned("pinned") }

data class FeedQuery(
    val sort: FeedSort = FeedSort.New,
    val window: HotWindow = HotWindow.Now,
    val channelName: String? = null,
    val topic: String? = null,
    val myList: MyList? = null,
) {
    /** The site offers now/today/week on the main list, and week/month inside a channel or topic. */
    val hotWindows: List<HotWindow>
        get() = if (channelName == null && topic == null) {
            listOf(HotWindow.Now, HotWindow.Today, HotWindow.Week)
        } else {
            listOf(HotWindow.Week, HotWindow.Month)
        }

    fun normalized(): FeedQuery = if (window in hotWindows) this else copy(window = hotWindows.first())

    /** A short stable key for caching this list's first page. */
    val cacheKey: String
        get() = listOf(
            sort.name, if (sort == FeedSort.Hot) window.name else "", channelName.orEmpty(), topic.orEmpty(), myList?.name.orEmpty(),
        ).joinToString("|")
}

/** A photo from Stips' own gallery (`photos.tag_gallery`), offered as a question's cover. */
data class GalleryPhoto(val photoId: String) {
    val thumbnailUrl: String get() = "$CDN_URL/photos/w400/$photoId.jpg"
}

/** A post on the pen-pals board (חברים לעט): a short message; replies go to private messages. */
data class PenPalPost(val id: Long, val text: String, val time: Instant?, val author: Author, val canDelete: Boolean)

/** Whether the user may post to the pen-pals board now (`penfriends/PostAllowed`). */
data class PenPalAllowance(val allowed: Boolean, val intervalMinutes: Int?, val nextInMinutes: Int?)

/** `ask/DailyAskCount`: Stips limits how many questions one may ask a day. */
data class AskAllowance(val allowed: Boolean, val maxPerDay: Int?)

/** A question about to be asked, in the fields the site sends (`omniobj` PUT, `objType: "ask"`). */
data class AskDraft(
    val title: String,
    val body: String,
    val tags: List<String>,
    val photoId: String?,
    val link: String?,
    val anonymous: Boolean,
    val nickname: String?,
    val channelId: Int?,
)
