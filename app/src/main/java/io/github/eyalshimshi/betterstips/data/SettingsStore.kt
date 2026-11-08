package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.model.HotWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { System, Light, Dark }

/** What Home shows below the channel chips (#8). */
enum class HomeLayout {
    /** The hottest questions as big cards, then the New feed (the default since v0.2). */
    HotAndNew,

    /** Like the website: the hottest questions, then sections of questions by topic, and no New feed. */
    LikeWebsite,

    /** Only the New feed. */
    NewOnly,
}

/** The tab the app opens on (#8). */
enum class StartTab { Home, PenPals, Chats, Profile }

/** How a question's answers are ordered (#8). */
enum class AnswersOrder { AsOnStips, Newest, Oldest, TopVoted }

/** How much Stips+ animates (#18): three presets, or each kind of animation picked by hand ([Custom]). */
enum class MotionPreset { Full, Less, Off, Custom }

/**
 * Which animations play (#18). [screens]: moving between screens, with the bottom bar, the intro and full-screen
 * covers; [jumps]: scrolling to an answer or a message that a reply or a notification points at; [scrolls]: back to
 * the top (tapping the open tab, refreshing) and the thread's ↓/↑ button; [glow]: the light on what was jumped to;
 * [elements]: buttons appearing, graphs and gauges filling, new questions sliding in, the Live dot. [quick] makes
 * jumps, scrolls and the glow faster. Pure.
 */
data class Motion(
    val screens: Boolean = true,
    val jumps: Boolean = true,
    val scrolls: Boolean = true,
    val glow: Boolean = true,
    val elements: Boolean = true,
    val quick: Boolean = false,
) {
    /** How long timed animations take, as a share of their usual time. */
    val pace: Float get() = if (quick) QUICK_PACE else 1f

    fun toBits(): Int = listOf(screens, jumps, scrolls, glow, elements, quick)
        .foldIndexed(0) { i, bits, on -> if (on) bits or (1 shl i) else bits }

    companion object {
        const val QUICK_PACE = 0.55f

        val FULL = Motion()

        /** The owner's "less" (#18): no screen transitions; jumps, scrolls, the glow and moving elements stay, faster. */
        val LESS = Motion(screens = false, quick = true)
        val OFF = Motion(screens = false, jumps = false, scrolls = false, glow = false, elements = false)

        fun of(preset: MotionPreset, custom: Motion): Motion = when (preset) {
            MotionPreset.Full -> FULL
            MotionPreset.Less -> LESS
            MotionPreset.Off -> OFF
            MotionPreset.Custom -> custom
        }

        fun fromBits(bits: Int): Motion {
            fun on(i: Int) = bits and (1 shl i) != 0
            return Motion(on(0), on(1), on(2), on(3), on(4), on(5))
        }

        /** The saved preset; v0.14 and earlier had one switch, so on is [MotionPreset.Full] and off [MotionPreset.Off]. */
        fun presetFrom(saved: String?, legacyAnimations: Boolean): MotionPreset =
            MotionPreset.entries.firstOrNull { it.name == saved } ?: if (legacyAnimations) MotionPreset.Full else MotionPreset.Off
    }
}

/** What swiping a question in a list does (#18). */
enum class QuestionSwipe { None, Pin, Follow, Share, CopyLink, OpenOnSite, Export, Report, Hide, HideUser }

/** What swiping an answer to the left does (#18); swiping it to the right replies (easy reply). */
enum class AnswerSwipe { None, Edit, Detach, Delete, History, Report, Hide, HideUser }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = true,
    val accent: Int = DEFAULT_ACCENT,
    val showImages: Boolean = true,
    /** How much the app animates (#18); [customMotion] is what "Customized" plays. See [motion]. */
    val motionPreset: MotionPreset = MotionPreset.Full,
    val customMotion: Motion = Motion.FULL,
    /** New questions appear on Home by themselves (polls every [liveIntervalSeconds] while Home is open). */
    val autoRefreshNew: Boolean = false,
    /** Phone notifications: Stips+ checks every [checkMinutes] while the phone is awake, and every 15 minutes anyway. */
    val backgroundNotifications: Boolean = false,
    /** Minutes between checks while the phone is awake: one of [CHECK_MINUTES]; 15 leaves only the job (v0.11). */
    val checkMinutes: Int = DEFAULT_CHECK_MINUTES,
    /** The one-time offer to turn phone notifications on (or let them run in the background) was answered (v0.11). */
    val notificationsPrompted: Boolean = false,
    /** Live: seconds between checks, [LIVE_MIN_SECONDS] to [LIVE_MAX_SECONDS] (#4). */
    val liveIntervalSeconds: Int = LIVE_MAX_SECONDS,
    /** Anonymous mode: answers and questions go out anonymously until this time (epoch ms); 0 is off (#4). */
    val anonymousUntil: Long = 0L,
    /** Expert mode (#8): diagnostics, load times, and ids and server data on questions, answers and profiles. */
    val expertMode: Boolean = false,
    /** Times under an hour count up every second on screen, from the phone's clock (#8). */
    val liveTimes: Boolean = false,
    val homeLayout: HomeLayout = HomeLayout.HotAndNew,
    /** How many hot questions Home shows: one of [HOT_COUNTS]. */
    val hotCount: Int = 5,
    /** The time window of Home's hot questions: one of [HOT_WINDOWS] (in a channel, Stips' own week/month apply). */
    val hotWindow: HotWindow = HotWindow.Now,
    val showChannelChips: Boolean = true,
    val startTab: StartTab = StartTab.Home,
    val answersOrder: AnswersOrder = AnswersOrder.AsOnStips,
    /** Swipe an answer to the right to reply to it with Stips' carets (v0.10). */
    val easyReply: Boolean = true,
    /** Answers that point up with carets show the answer they reply to (v0.10). */
    val replyQuotes: Boolean = true,
    /** The opening carets of an answer shown with quotes are left out; off, so answers show as written (v0.11). */
    val hideReplyCarets: Boolean = false,
    /** Easy reply in private chats: swipe a message to reply to it with carets (v0.12). Apart from answers' switch. */
    val chatEasyReply: Boolean = true,
    /** Chat messages that point up with carets show the message they reply to (v0.12). */
    val chatReplyQuotes: Boolean = true,
    /** The opening carets of a chat message shown with quotes are left out; off, so messages show as written (v0.12). */
    val chatHideReplyCarets: Boolean = false,
    /** Phone notifications include new chat messages (v0.12); off, they're only about answers, flowers and thanks. */
    val chatNotifications: Boolean = true,
    /** "נאמן סטיפס" by moderators' names on answers; each answerer is looked up once every two weeks (v0.10). */
    val moderatorBadges: Boolean = true,
    /** The first-run welcome (sign in, or go on as a guest) was answered (#8). */
    val onboarded: Boolean = false,
    /** Swiping a question in a list to the right (left to right, in Hebrew too) and to the left (#18). */
    val swipeRight: QuestionSwipe = QuestionSwipe.Pin,
    val swipeLeft: QuestionSwipe = QuestionSwipe.Report,
    /** Swiping an answer to the left (#18): someone else's, and your own. Swiping one to the right replies. */
    val answerSwipe: AnswerSwipe = AnswerSwipe.None,
    val myAnswerSwipe: AnswerSwipe = AnswerSwipe.Edit,
    /** Notifications about questions show the question's title and photo, read from Stips (#13); off by default. */
    val notificationDetails: Boolean = false,
) {
    fun anonymousModeOn(now: Long = System.currentTimeMillis()): Boolean = anonymousUntil > now

    /** The animations that play now (#18). */
    val motion: Motion get() = Motion.of(motionPreset, customMotion)

    companion object {
        /**
         * The owner's pick (#5 follow-up): Live at 2 s alone sent 30 calls a minute in #5. At 5 s it takes 12 of the
         * request budget's 30 a minute, leaving the rest for taps; the official app never checks by itself.
         */
        const val LIVE_MIN_SECONDS = 5
        const val LIVE_MAX_SECONDS = 30

        /** The choices for [hotCount]. */
        val HOT_COUNTS = listOf(3, 5, 10)

        /** The choices for [checkMinutes]. */
        val CHECK_MINUTES = listOf(1, 2, 5, 15)
        const val DEFAULT_CHECK_MINUTES = 2

        /** The choices for [hotWindow]: the windows Stips offers on its main hot list. */
        val HOT_WINDOWS = listOf(HotWindow.Now, HotWindow.Today, HotWindow.Week)

        /** Anonymous mode with no end time. */
        const val ANONYMOUS_FOREVER = Long.MAX_VALUE

        /** One Live check is the first New page: about 9.5 KB compressed (SPEC-04 §2.8). */
        private const val LIVE_CHECK_KB = 9.5

        /** Data Live uses in an hour of Home on screen at [seconds] between checks. Pure. */
        fun liveMegabytesPerHour(seconds: Int): Double =
            3_600.0 / seconds.coerceIn(LIVE_MIN_SECONDS, LIVE_MAX_SECONDS) * LIVE_CHECK_KB / 1_024.0

        /** The wait before the next Live check: the chosen rate, doubled after each failed check, at most a minute. Pure. */
        fun liveDelayMs(seconds: Int, misses: Int): Long {
            val base = seconds.coerceIn(LIVE_MIN_SECONDS, LIVE_MAX_SECONDS) * 1_000L
            return (base shl misses.coerceIn(0, 5)).coerceAtMost(60_000L)
        }

        /** Stips green: the fallback seed when dynamic color is off (D-06). */
        val DEFAULT_ACCENT: Int = 0xFF09C286.toInt()

        /** Stips green, then DESIGN-PHILOSOPHY's 8 presets. */
        val ACCENTS: List<Int> = listOf(
            0xFF09C286, 0xFF6750A4, 0xFF0061A4, 0xFF006A6A, 0xFF386A20, 0xFF8B5000, 0xFF9C4146, 0xFFB4236C, 0xFF0038B8,
        ).map { it.toInt() }
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putInt("themeMode", next.themeMode.ordinal)
            .putBoolean("pureBlack", next.pureBlack)
            .putBoolean("dynamicColor", next.dynamicColor)
            .putInt("accent", next.accent)
            .putBoolean("showImages", next.showImages)
            .putString("motionPreset", next.motionPreset.name)
            .putInt("motionCustom", next.customMotion.toBits())
            .putBoolean("autoRefreshNew", next.autoRefreshNew)
            .putBoolean("backgroundNotifications", next.backgroundNotifications)
            .putInt("checkMinutes", next.checkMinutes)
            .putBoolean("notificationsPrompted", next.notificationsPrompted)
            .putInt("liveIntervalSeconds", next.liveIntervalSeconds)
            .putLong("anonymousUntil", next.anonymousUntil)
            .putBoolean("expertMode", next.expertMode)
            .putBoolean("liveTimes", next.liveTimes)
            .putString("homeLayout", next.homeLayout.name)
            .putInt("hotCount", next.hotCount)
            .putString("hotWindow", next.hotWindow.name)
            .putBoolean("showChannelChips", next.showChannelChips)
            .putString("startTab", next.startTab.name)
            .putString("answersOrder", next.answersOrder.name)
            .putBoolean("easyReply", next.easyReply)
            .putBoolean("replyQuotes", next.replyQuotes)
            .putBoolean("hideReplyCarets", next.hideReplyCarets)
            .putBoolean("chatEasyReply", next.chatEasyReply)
            .putBoolean("chatReplyQuotes", next.chatReplyQuotes)
            .putBoolean("chatHideReplyCarets", next.chatHideReplyCarets)
            .putBoolean("chatNotifications", next.chatNotifications)
            .putBoolean("moderatorBadges", next.moderatorBadges)
            .putBoolean("onboarded", next.onboarded)
            .putString("swipeRight", next.swipeRight.name)
            .putString("swipeLeft", next.swipeLeft.name)
            .putString("answerSwipe", next.answerSwipe.name)
            .putString("myAnswerSwipe", next.myAnswerSwipe.name)
            .putBoolean("notificationDetails", next.notificationDetails)
            .apply()
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = ThemeMode.entries.getOrNull(prefs.getInt("themeMode", 0)) ?: ThemeMode.System,
            pureBlack = prefs.getBoolean("pureBlack", defaults.pureBlack),
            dynamicColor = prefs.getBoolean("dynamicColor", defaults.dynamicColor),
            accent = prefs.getInt("accent", defaults.accent),
            showImages = prefs.getBoolean("showImages", defaults.showImages),
            motionPreset = Motion.presetFrom(prefs.getString("motionPreset", null), prefs.getBoolean("animations", true)),
            customMotion = prefs.getInt("motionCustom", -1).let { if (it < 0) defaults.customMotion else Motion.fromBits(it) },
            autoRefreshNew = prefs.getBoolean("autoRefreshNew", defaults.autoRefreshNew),
            backgroundNotifications = prefs.getBoolean("backgroundNotifications", defaults.backgroundNotifications),
            checkMinutes = prefs.getInt("checkMinutes", defaults.checkMinutes).takeIf { it in AppSettings.CHECK_MINUTES }
                ?: defaults.checkMinutes,
            notificationsPrompted = prefs.getBoolean("notificationsPrompted", defaults.notificationsPrompted),
            liveIntervalSeconds = prefs.getInt("liveIntervalSeconds", defaults.liveIntervalSeconds)
                .coerceIn(AppSettings.LIVE_MIN_SECONDS, AppSettings.LIVE_MAX_SECONDS),
            anonymousUntil = prefs.getLong("anonymousUntil", defaults.anonymousUntil),
            expertMode = prefs.getBoolean("expertMode", defaults.expertMode),
            liveTimes = prefs.getBoolean("liveTimes", defaults.liveTimes),
            homeLayout = enumOr(prefs.getString("homeLayout", null), defaults.homeLayout),
            hotCount = prefs.getInt("hotCount", defaults.hotCount).takeIf { it in AppSettings.HOT_COUNTS } ?: defaults.hotCount,
            hotWindow = enumOr(prefs.getString("hotWindow", null), defaults.hotWindow).takeIf { it in AppSettings.HOT_WINDOWS }
                ?: defaults.hotWindow,
            showChannelChips = prefs.getBoolean("showChannelChips", defaults.showChannelChips),
            startTab = enumOr(prefs.getString("startTab", null), defaults.startTab),
            answersOrder = enumOr(prefs.getString("answersOrder", null), defaults.answersOrder),
            easyReply = prefs.getBoolean("easyReply", defaults.easyReply),
            replyQuotes = prefs.getBoolean("replyQuotes", defaults.replyQuotes),
            hideReplyCarets = prefs.getBoolean("hideReplyCarets", defaults.hideReplyCarets),
            chatEasyReply = prefs.getBoolean("chatEasyReply", defaults.chatEasyReply),
            chatReplyQuotes = prefs.getBoolean("chatReplyQuotes", defaults.chatReplyQuotes),
            chatHideReplyCarets = prefs.getBoolean("chatHideReplyCarets", defaults.chatHideReplyCarets),
            chatNotifications = prefs.getBoolean("chatNotifications", defaults.chatNotifications),
            moderatorBadges = prefs.getBoolean("moderatorBadges", defaults.moderatorBadges),
            onboarded = prefs.getBoolean("onboarded", defaults.onboarded),
            swipeRight = enumOr(prefs.getString("swipeRight", null), defaults.swipeRight),
            swipeLeft = enumOr(prefs.getString("swipeLeft", null), defaults.swipeLeft),
            answerSwipe = enumOr(prefs.getString("answerSwipe", null), defaults.answerSwipe),
            myAnswerSwipe = enumOr(prefs.getString("myAnswerSwipe", null), defaults.myAnswerSwipe),
            notificationDetails = prefs.getBoolean("notificationDetails", defaults.notificationDetails),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
