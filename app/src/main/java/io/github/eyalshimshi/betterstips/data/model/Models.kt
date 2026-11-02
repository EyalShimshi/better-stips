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
) {
    fun avatarUrl(large: Boolean = false): String? {
        if (anonymous || userId == null || photoStamp.isNullOrBlank()) return null
        val size = if (large) "l" else "t"
        return "$CDN_URL/photos/user_profile/$size/$userId/$photoStamp.jpg"
    }
}

sealed interface Cover {
    val thumbnailUrl: String
    val largeUrl: String

    /** A photo the asker uploaded to Stips. */
    data class Stips(val photoId: String) : Cover {
        override val thumbnailUrl get() = "$CDN_URL/photos/s/$photoId.jpg"
        override val largeUrl get() = "$CDN_URL/photos/w400/$photoId.jpg"
    }

    /** A stock photo the asker picked; Unsplash asks for the photographer's credit. */
    data class Unsplash(val small: String, val medium: String, val creatorName: String?) : Cover {
        override val thumbnailUrl get() = small
        override val largeUrl get() = medium
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
    val points: Int,
    val female: Boolean,
    val photoStamp: String?,
) {
    val avatar: Author get() = Author(nickname, userId, photoStamp, anonymous = false, online = false)
}

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
