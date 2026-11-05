package io.github.eyalshimshi.betterstips.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.MainActivity
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.StipsApp
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.Conversation
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.diag.NetLog
import io.github.eyalshimshi.betterstips.util.JumpRules
import io.github.eyalshimshi.betterstips.util.NotifyRules
import io.github.eyalshimshi.betterstips.util.Shortcuts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/** A check from an alarm has a broadcast's time: about 10 seconds. */
private const val ALARM_BUDGET_MS = 9_000L

/**
 * Phone notifications for Stips updates. Stips has no push service for other apps, so when the user turns this on,
 * Stips+ asks for the unread counts (a tiny request) and fetches lists only when something may be new:
 * - every [AppSettings.checkMinutes] while the phone is awake, from an alarm that doesn't wake it: nothing while the
 *   phone sleeps, and a check as soon as it wakes up (v0.11);
 * - every 15 minutes from a job that Android runs while the phone sleeps too, but holds back (for hours) while it saves
 *   battery, unless the user lets Stips+ run in the background.
 * [NotifyRules] decides what's new.
 */
object Notifier {
    private const val CHANNEL_ACTIVITY = "stips_activity"
    private const val CHANNEL_MESSAGES = "stips_messages"

    /** v0.2–v0.10's one channel, at default importance (nothing popped up); replaced by the two above. */
    private const val LEGACY_CHANNEL = "stips_updates"

    /**
     * Far above the ids WorkManager gives its own jobs (0, 1, 2…, for the widgets): scheduling a job with a taken id
     * replaces the other job, and WorkManager would have reached v0.2–v0.10's 4201 one day.
     */
    private const val JOB_ID = 2_104_201_000
    private const val LEGACY_JOB_ID = 4201
    private const val JOB_PERIOD_MS = 15 * 60_000L

    const val ACTION_CHECK = "io.github.eyalshimshi.betterstips.action.CHECK_NOTIFICATIONS"
    private const val ALARM_REQUEST = 4202
    private const val TEST_REQUEST = 4203
    private const val MESSAGES_ID = 1

    private const val TAG_UPDATE = "update"
    private const val TAG_CHAT = "chat"
    private const val TAG_CHATS = "chats"
    private const val TAG_TEST = "test"

    private const val PREFS = "notify"
    private const val KEY_KNOWN = "known"
    private const val KEY_LEGACY = "lastNotificationId"
    private const val KEY_CHATS = "chats"
    private const val KEY_MESSAGES = "lastMessagesCount"
    private const val KEY_CHATS_AT = "chatsListedAt"
    private const val KEY_STATUS = "status"
    private const val KEY_RAN_AT = "ranAt"

    /** Where a notification's question opens (v0.10): a kind, a count and a time, as [JumpRules] writes them. */
    private const val EXTRA_FOCUS = "io.github.eyalshimshi.betterstips.extra.FOCUS"
    private const val EXTRA_FOCUS_COUNT = "io.github.eyalshimshi.betterstips.extra.FOCUS_COUNT"
    private const val EXTRA_FOCUS_AT = "io.github.eyalshimshi.betterstips.extra.FOCUS_AT"

    enum class Trigger { Job, Alarm, User }

    /** The app is on screen (MainActivity says): its own counts are fresh, so automatic checks wait. */
    @Volatile
    var appVisible = false

    private val running = Mutex()
    private val _status = MutableStateFlow<NotifyRules.Status?>(null)

    @Volatile
    private var statusLoaded = false

    /** Where a phone notification asked its question to open, or null. */
    fun focusFrom(intent: Intent): QuestionFocus? =
        JumpRules.decode(intent.getStringExtra(EXTRA_FOCUS), intent.getIntExtra(EXTRA_FOCUS_COUNT, -1), intent.getLongExtra(EXTRA_FOCUS_AT, -1L))

    /** Answers and activity, and messages: both pop up (v0.11). The old channel goes. */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(LEGACY_CHANNEL) != null) manager.deleteNotificationChannel(LEGACY_CHANNEL)
        if (manager.getNotificationChannel(CHANNEL_ACTIVITY) == null) {
            val channel = NotificationChannel(
                CHANNEL_ACTIVITY,
                context.getString(R.string.notification_channel_activity),
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.description = context.getString(R.string.notification_channel_activity_description)
            manager.createNotificationChannel(channel)
        }
        if (manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
            val channel = NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.description = context.getString(R.string.notification_channel_messages_description)
            manager.createNotificationChannel(channel)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Notifications from Stips+ are off in Android's settings: all of them, or one of its channels. */
    fun blockedInAndroid(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return listOf(CHANNEL_ACTIVITY, CHANNEL_MESSAGES)
            .mapNotNull { manager.getNotificationChannel(it) }
            .any { it.importance == NotificationManager.IMPORTANCE_NONE }
    }

    /** Android lets Stips+ run in the background without saving battery on it, so the checks come on time. */
    fun runsInBackground(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    /** The user restricted Stips+'s background use in Android's settings: no checks run at all. */
    fun backgroundRestricted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 28 && context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true

    /** Android's standby group for Stips+ (expert mode): how freely it lets the app work in the background. */
    fun standbyBucket(context: Context): String? {
        if (Build.VERSION.SDK_INT < 28) return null
        val bucket = context.getSystemService(UsageStatsManager::class.java)?.appStandbyBucket ?: return null
        return when (bucket) {
            5 -> "exempted"
            10 -> "active"
            20 -> "working set"
            30 -> "frequent"
            40 -> "rare"
            45 -> "restricted"
            else -> bucket.toString()
        }
    }

    /** Android's own question, "let Stips+ always run in the background?", or the list where that's set. */
    @SuppressLint("BatteryLife")
    fun askToRunInBackground(context: Context) {
        val ask = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName))
        if (start(context, ask)) return
        if (start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return
        openAppSettings(context)
    }

    fun openAppSettings(context: Context) {
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (!start(context, intent)) openAppSettings(context)
    }

    /** Android's own settings for message notifications: sound, pop-up, lock screen (v0.12, the DM settings). */
    fun openMessageSettings(context: Context) {
        ensureChannels(context)
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_MESSAGES)
        if (!start(context, intent)) openNotificationSettings(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /**
     * Turns the checks on or off: the 15-minute job, and the alarm while the phone is awake. Safe to call often.
     * Returns false when Android refused the job (it must never crash the app: #1 and #2 were a refused job).
     */
    fun schedule(context: Context, enabled: Boolean, minutes: Int): Boolean {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return !enabled
        return try {
            cancelLegacyJob(context, scheduler)
            if (!enabled) {
                scheduler.cancel(JOB_ID)
                disarm(context)
                // Next time it's turned on, start fresh rather than replaying what arrived meanwhile.
                prefs(context).edit().clear().apply()
                _status.value = null
                return true
            }
            ensureChannels(context)
            arm(context, minutes)
            val component = ComponentName(context, NotificationCheckJob::class.java)
            if (scheduler.getPendingJob(JOB_ID)?.service == component) return true
            val info = JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(JOB_PERIOD_MS)
                .setPersisted(true)
                .build()
            scheduler.schedule(info) == JobScheduler.RESULT_SUCCESS
        } catch (e: RuntimeException) {
            // SecurityException (a missing permission), IllegalArgumentException or IllegalStateException (job limits).
            NetLog.event("notifications", "job not scheduled: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    /** The checks as [settings] say: on or off, and how often. */
    fun sync(context: Context, settings: AppSettings): Boolean =
        schedule(context, settings.backgroundNotifications, settings.checkMinutes)

    /** After a check: the next alarm, while the checks are on and someone is signed in (the app restarts it later). */
    fun rearm(context: Context, container: AppContainer) {
        val settings = container.settings.settings.value
        if (settings.backgroundNotifications && container.session.session.value.signedIn) {
            arm(context, settings.checkMinutes)
        } else {
            disarm(context)
        }
    }

    /** v0.2–v0.10's job had id 4201. It goes, but only if it's ours: WorkManager may hold that id by now. */
    private fun cancelLegacyJob(context: Context, scheduler: JobScheduler) {
        val legacy = scheduler.getPendingJob(LEGACY_JOB_ID) ?: return
        if (legacy.service == ComponentName(context, NotificationCheckJob::class.java)) scheduler.cancel(LEGACY_JOB_ID)
    }

    /** The next check while the phone is awake. The alarm doesn't wake the phone: asleep, it waits until it wakes. */
    private fun arm(context: Context, minutes: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val delay = NotifyRules.alarmMs(minutes)
        try {
            if (delay == null) {
                alarms.cancel(alarmIntent(context))
            } else {
                alarms.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delay, alarmIntent(context))
            }
        } catch (e: RuntimeException) {
            NetLog.event("notifications", "alarm not set: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun disarm(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST,
        Intent(context, NotificationAlarmReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The last check's result, for Settings. */
    fun status(context: Context): StateFlow<NotifyRules.Status?> {
        if (!statusLoaded) {
            statusLoaded = true
            if (_status.value == null) _status.value = NotifyRules.decodeStatus(prefs(context).getString(KEY_STATUS, null))
        }
        return _status.asStateFlow()
    }

    /** When Android last ran a check (the job or the alarm), even one with nothing to do; 0 if never. */
    fun lastRunAt(context: Context): Long = prefs(context).getLong(KEY_RAN_AT, 0L)

    /**
     * One check: from the job, the alarm, or "Check now" ([Trigger.User]). Automatic ones wait while the app is on
     * screen (its own counts are fresh there) and while the request budget is low. Returns what it found, or null when
     * it didn't check.
     */
    suspend fun run(context: Context, container: AppContainer, trigger: Trigger): NotifyRules.Status? {
        val byUser = trigger == Trigger.User
        val name = trigger.name.lowercase()
        if (!byUser) prefs(context).edit().putLong(KEY_RAN_AT, System.currentTimeMillis()).apply()
        val skip = skipReason(context, container, byUser)
        if (skip != null) {
            // Skipping while the app is open is the normal case; logging it would bury the network log.
            if (skip != SKIP_VISIBLE) NetLog.event("notifications", "$name check skipped: $skip")
            return null
        }
        if (!running.tryLock()) return null
        try {
            val status = try {
                val posted = check(context, container)
                val outcome = if (posted > 0) NotifyRules.Outcome.Posted else NotifyRules.Outcome.Nothing
                NotifyRules.Status(System.currentTimeMillis(), outcome, posted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val reason = (e as? ApiException)?.code ?: e.javaClass.simpleName
                NetLog.event("notifications", "$name check failed: $reason ${e.message.orEmpty().take(80)}")
                NotifyRules.Status(System.currentTimeMillis(), NotifyRules.Outcome.Failed, error = reason)
            }
            prefs(context).edit().putString(KEY_STATUS, NotifyRules.encodeStatus(status)).apply()
            _status.value = status
            return status
        } finally {
            running.unlock()
        }
    }

    private fun skipReason(context: Context, container: AppContainer, byUser: Boolean): String? = when {
        !byUser && !container.settings.settings.value.backgroundNotifications -> "turned off"
        !container.session.session.value.signedIn -> "signed out"
        !canPost(context) -> "Android doesn't allow notifications"
        !byUser && appVisible -> SKIP_VISIBLE
        !byUser && !RateGuard.allowBackground() -> "staying under Stips' request limit"
        else -> null
    }

    private const val SKIP_VISIBLE = "the app is on screen"

    /** Asks Stips what's unread, and posts what's new. Returns how many phone notifications it posted. */
    private suspend fun check(context: Context, container: AppContainer): Int {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val counts = container.repository.unreadCounts()
        var posted = 0

        // Answers, flowers, thanks: the first page, when something is unread (and once at the start, to know what's old).
        val legacy = if (prefs.contains(KEY_LEGACY)) prefs.getLong(KEY_LEGACY, 0L) else null
        val first = !prefs.contains(KEY_KNOWN) && legacy == null
        if (counts.notifications > 0 || first || legacy != null) {
            val list = container.repository.notifications(page = 1)
            val pick = NotifyRules.pick(list, NotifyRules.decode(prefs.getString(KEY_KNOWN, null)), first, legacy)
            pick.read.forEach { cancel(context, TAG_UPDATE, it.toInt()) }
            pick.post.forEach { post(context, it) }
            posted += pick.post.size
            prefs.edit().putString(KEY_KNOWN, NotifyRules.encode(pick.known)).remove(KEY_LEGACY).apply()
        }

        // Messages: the chats list when the unread count says something changed; one notification per chat. With
        // message notifications off (v0.12) nothing is kept, so turning them on again doesn't replay old ones.
        val lastUnread = if (prefs.contains(KEY_MESSAGES)) prefs.getInt(KEY_MESSAGES, 0) else null
        val chatsKnown = prefs.getString(KEY_CHATS, null)
        if (!container.settings.settings.value.chatNotifications) {
            if (chatsKnown != null) cancelAll(context, TAG_CHAT, TAG_CHATS)
            prefs.edit().remove(KEY_CHATS).apply()
        } else if (counts.messages <= 0) {
            if ((lastUnread ?: 0) > 0) cancelAll(context, TAG_CHAT, TAG_CHATS)
            prefs.edit().putString(KEY_CHATS, "").apply()
        } else if (NotifyRules.listChats(counts.messages, lastUnread, prefs.getLong(KEY_CHATS_AT, 0L), now)) {
            val chats = try {
                container.repository.conversations(1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetLog.event("notifications", "chats not listed: ${e.javaClass.simpleName}")
                null
            }
            if (chats != null) {
                val pick = NotifyRules.pickChats(chats, NotifyRules.decode(chatsKnown), first = chatsKnown == null)
                pick.read.forEach { cancel(context, TAG_CHAT, it.toInt()) }
                pick.post.forEach { postChat(context, it) }
                posted += pick.post.size
                prefs.edit().putString(KEY_CHATS, NotifyRules.encode(pick.known)).putLong(KEY_CHATS_AT, now).apply()
            } else if (lastUnread != null && counts.messages > lastUnread) {
                postMessages(context, counts.messages)
                posted++
            }
        }
        prefs.edit().putInt(KEY_MESSAGES, counts.messages).apply()
        NetLog.event("notifications", "check: ${counts.notifications} notifications, ${counts.messages} messages unread; $posted posted")
        return posted
    }

    private fun post(context: Context, notification: StipsNotification) {
        val target = notification.target
        val link = when (target) {
            is NotificationTarget.OpenQuestion -> "$SITE_URL/ask/${target.id}"
            is NotificationTarget.OpenProfile -> "$SITE_URL/profile/${target.id}"
            null -> null
        }
        val focus = if (target is NotificationTarget.OpenQuestion) JumpRules.focusFor(notification.type, notification.text, notification.time) else null
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(notification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context, link, notification.id.toInt(), focus))
            .setAutoCancel(true)
        notification.time?.let { builder.setWhen(it.toEpochMilli()).setShowWhen(true) }
        show(context, TAG_UPDATE, notification.id.toInt(), builder)
    }

    /** One chat: who wrote and their last message; a tap opens the chat. */
    private fun postChat(context: Context, chat: Conversation) {
        val userId = chat.other.userId ?: return
        val name = chat.other.nickname
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Shortcuts.ACTION_CHAT
            data = Uri.parse(Shortcuts.chatUri(userId, name))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val open = PendingIntent.getActivity(context, userId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText(chat.lastMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(chat.lastMessage))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(chat.unread)
            .setContentIntent(open)
            .setAutoCancel(true)
        chat.time?.let { builder.setWhen(it.toEpochMilli()).setShowWhen(true) }
        show(context, TAG_CHAT, userId.toInt(), builder)
        // The chats speak for themselves now.
        cancel(context, TAG_CHATS, MESSAGES_ID)
    }

    /** When the chats list couldn't be read: how many messages are unread. */
    private fun postMessages(context: Context, count: Int) {
        val text = context.resources.getQuantityString(R.plurals.notification_messages, count, count)
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context, "$SITE_URL/messages", MESSAGES_ID))
            .setAutoCancel(true)
        show(context, TAG_CHATS, MESSAGES_ID, builder)
    }

    /** A sample, so the user sees that (and how) notifications arrive. False without Android's permission. */
    fun postTest(context: Context): Boolean {
        if (!canPost(context)) return false
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context, null, TEST_REQUEST))
            .setAutoCancel(true)
        show(context, TAG_TEST, TEST_REQUEST, builder)
        return true
    }

    /** The bell was opened: the notifications about answers, flowers and thanks have been seen. */
    fun clearUpdates(context: Context) {
        cancelAll(context, TAG_UPDATE)
    }

    /** A chat was opened: its notification goes, with the general one about messages. */
    fun clearChat(context: Context, userId: Long) {
        cancel(context, TAG_CHAT, userId.toInt())
        cancel(context, TAG_CHATS, MESSAGES_ID)
    }

    private fun show(context: Context, tag: String, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        ensureChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(tag, id, builder.build())
        } catch (e: SecurityException) {
            // The permission was revoked between the check and now.
        }
    }

    private fun cancel(context: Context, tag: String, id: Int) {
        NotificationManagerCompat.from(context).cancel(tag, id)
    }

    private fun cancelAll(context: Context, vararg tags: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val active = try {
            manager.activeNotifications
        } catch (e: RuntimeException) {
            return
        }
        active.filter { it.tag in tags }.forEach { manager.cancel(it.tag, it.id) }
    }

    private fun openApp(context: Context, link: String?, requestCode: Int, focus: QuestionFocus? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (link != null) {
                action = Intent.ACTION_VIEW
                data = Uri.parse(link)
            }
            if (focus != null) {
                putExtra(EXTRA_FOCUS, JumpRules.kind(focus))
                putExtra(EXTRA_FOCUS_COUNT, JumpRules.count(focus))
                putExtra(EXTRA_FOCUS_AT, JumpRules.at(focus))
            }
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Checks every 15 minutes; Android picks the moment in each window, and holds it back while it saves battery. */
class NotificationCheckJob : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val container = (applicationContext as StipsApp).container
        work = container.appScope.launch {
            try {
                Notifier.run(applicationContext, container, Notifier.Trigger.Job)
            } finally {
                // Android clears alarms on a restart; the job comes back by itself and brings the alarm back.
                Notifier.rearm(applicationContext, container)
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return true
    }
}

/** The check while the phone is awake (v0.11): each alarm checks once and sets the next. */
class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Notifier.ACTION_CHECK) return
        val app = context.applicationContext as? StipsApp ?: return
        val container = app.container
        val pending = goAsync()
        container.appScope.launch {
            try {
                withTimeoutOrNull(ALARM_BUDGET_MS) { Notifier.run(app, container, Notifier.Trigger.Alarm) }
            } finally {
                Notifier.rearm(app, container)
                pending.finish()
            }
        }
    }
}
