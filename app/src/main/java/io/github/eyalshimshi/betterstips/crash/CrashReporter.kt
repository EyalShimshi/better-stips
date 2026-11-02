package io.github.eyalshimshi.betterstips.crash

import android.app.Application
import android.content.Context
import android.os.Build
import io.github.eyalshimshi.betterstips.AppContainer
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.time.Instant

/** Saves a crash report on the phone; the next launch offers to copy or report it. Nothing is sent automatically. */
object CrashReporter {
    private const val FILE_NAME = "crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(app.filesDir, FILE_NAME).writeText(report(app, thread, error))
            } catch (ignored: Throwable) {
                // Never let the crash catcher crash.
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pending(context: Context): String? = File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    fun issueUrl(report: String): String {
        val firstLine = report.lineSequence().firstOrNull { it.startsWith("Exception:") }?.removePrefix("Exception:")?.trim()
        val title = "Crash: " + (firstLine ?: "unexpected error").take(80)
        val body = "**What I was doing:**\n\n\n**Details:**\n```\n" + report.take(5_000) + "\n```"
        return AppContainer.REPO_URL + "/issues/new?title=" + URLEncoder.encode(title, "UTF-8") + "&body=" + URLEncoder.encode(body, "UTF-8")
    }

    private fun report(context: Context, thread: Thread, error: Throwable): String {
        @Suppress("DEPRECATION")
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("App: Stips+ $version")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Time: ${Instant.now()}")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${error.javaClass.name}: ${error.message.orEmpty().take(200)}")
            appendLine()
            append(trace)
        }
    }
}
