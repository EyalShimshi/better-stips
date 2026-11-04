package io.github.eyalshimshi.betterstips.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.MainActivity
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.StipsApp
import io.github.eyalshimshi.betterstips.data.api.RateGuard
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import io.github.eyalshimshi.betterstips.diag.NetLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Phone notifications for Stips updates. Stips has no push service for other apps, so when the user turns this on,
 * a job asks for the unread counts every 15 minutes (a tiny request) and fetches the list only when something is new.
 */
object Notifier {
    private const val CHANNEL_ID = "stips_updates"
    private const val JOB_ID = 4201
    private const val PREFS = "notify"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = context.getString(R.string.notification_channel_description)
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Turns the 15-minute check on or off. Safe to call often. Returns false when Android refused the job
     * (it must never crash the app: issues #1 and #2 were a refused job thrown from here).
     */
    fun schedule(context: Context, enabled: Boolean): Boolean {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return !enabled
        return try {
            if (!enabled) {
                scheduler.cancel(JOB_ID)
                // Next time it's turned on, start fresh rather than replaying what arrived meanwhile.
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
                return true
            }
            if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return true
            val info = JobInfo.Builder(JOB_ID, ComponentName(context, NotificationCheckJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build()
            scheduler.schedule(info) == JobScheduler.RESULT_SUCCESS
        } catch (e: RuntimeException) {
            // SecurityException (a missing permission), IllegalArgumentException or IllegalStateException (job limits).
            NetLog.event("notifications", "job not scheduled: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    suspend fun check(context: Context, container: AppContainer) {
        if (!container.settings.settings.value.backgroundNotifications) return
        if (!container.session.session.value.signedIn || !canPost(context)) return
        if (!RateGuard.allowBackground()) {
            NetLog.event("notifications", "check skipped: staying under Stips' request limit")
            return
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val counts = container.repository.unreadCounts()
        NetLog.event("notifications", "check: ${counts.notifications} notifications, ${counts.messages} messages unread")
        if (!prefs.contains("lastNotificationId")) {
            // First check after turning this on: remember where things stand instead of replaying old items.
            val newest = if (counts.notifications > 0) container.repository.notifications(page = 1).maxOfOrNull { it.id } else null
            prefs.edit().putLong("lastNotificationId", newest ?: 0L).putInt("lastMessagesCount", counts.messages).apply()
            return
        }
        if (counts.notifications > 0) {
            val lastSeen = prefs.getLong("lastNotificationId", 0L)
            val list = container.repository.notifications(page = 1)
            list.filter { !it.viewed && it.id > lastSeen }.take(5).forEach { post(context, it) }
            val newest = list.maxOfOrNull { it.id } ?: lastSeen
            prefs.edit().putLong("lastNotificationId", maxOf(newest, lastSeen)).apply()
        }
        val lastMessages = prefs.getInt("lastMessagesCount", 0)
        if (counts.messages > lastMessages) postMessages(context, counts.messages)
        prefs.edit().putInt("lastMessagesCount", counts.messages).apply()
    }

    private fun post(context: Context, notification: StipsNotification) {
        if (!canPost(context)) return
        ensureChannel(context)
        val link = when (val target = notification.target) {
            is NotificationTarget.OpenQuestion -> "$SITE_URL/ask/${target.id}"
            is NotificationTarget.OpenProfile -> "$SITE_URL/profile/${target.id}"
            null -> null
        }
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(notification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setContentIntent(openApp(context, link, notification.id.toInt()))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notification.id.toInt(), built)
        } catch (e: SecurityException) {
            // The permission was revoked between the check and now.
        }
    }

    private fun postMessages(context: Context, count: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val text = context.resources.getQuantityString(R.plurals.notification_messages, count, count)
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(openApp(context, "$SITE_URL/messages", 1))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, built)
        } catch (e: SecurityException) {
            // As above.
        }
    }

    private fun openApp(context: Context, link: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (link != null) {
                action = Intent.ACTION_VIEW
                data = Uri.parse(link)
            }
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

/** Runs [Notifier.check] in the background; Android decides the exact time within each 15-minute window. */
class NotificationCheckJob : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val container = (applicationContext as StipsApp).container
        work = container.appScope.launch {
            try {
                Notifier.check(applicationContext, container)
            } catch (e: Exception) {
                // Offline or signed out: try again next time.
                NetLog.event("notifications", "check failed: ${e.javaClass.simpleName}: ${e.message.orEmpty().take(80)}")
            } finally {
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
