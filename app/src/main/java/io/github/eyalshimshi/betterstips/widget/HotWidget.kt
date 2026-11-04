package io.github.eyalshimshi.betterstips.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.eyalshimshi.betterstips.MainActivity
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.StipsApp
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import io.github.eyalshimshi.betterstips.ui.theme.toColorScheme

/** The receiver Android talks to; Glance does the rest. */
class HotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HotWidget()
}

/**
 * Asks the launcher to place the widget (Settings → Updates). Returns false when the launcher can't, so the caller
 * can explain how to add it by hand.
 */
fun requestPinHotWidget(context: Context): Boolean = try {
    val manager = context.getSystemService(AppWidgetManager::class.java)
    manager != null && manager.isRequestPinAppWidgetSupported &&
        manager.requestPinAppWidget(ComponentName(context, HotWidgetReceiver::class.java), null, null)
} catch (e: RuntimeException) {
    false
}

/**
 * "Hot right now" on the home screen (D-51): the hottest questions, as on Home. Tap a question to open it, the title
 * to open the app, ⟳ to refresh. Wallpaper colors on Android 12+ when the app uses them, otherwise the app's accent.
 */
class HotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as StipsApp).container
        val feed = container.hotWidget
        feed.ensureLoaded()
        feed.refresh(byUser = false)
        provideContent {
            val state by feed.state.collectAsState()
            val settings by container.settings.settings.collectAsState()
            val accent = remember(settings.accent) { accentColors(settings.accent) }
            val dynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            GlanceTheme(colors = if (dynamic) GlanceTheme.colors else accent) {
                HotWidgetContent(state)
            }
        }
    }
}

/** The ⟳ button, and "Tap to try again". */
class RefreshHotWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as StipsApp).container.hotWidget.refresh(byUser = true)
    }
}

private fun accentColors(accent: Int) = SchemeGenerator.palettes(accent).let {
    ColorProviders(light = it.toColorScheme(dark = false), dark = it.toColorScheme(dark = true))
}

@Composable
private fun HotWidgetContent(state: HotWidgetFeed.State) {
    val context = LocalContext.current
    val refresh = actionRunCallback<RefreshHotWidget>()
    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(R.drawable.ic_notification),
                title = context.getString(R.string.hot_now),
                iconColor = GlanceTheme.colors.primary,
                modifier = GlanceModifier.clickable(actionStartActivity(openApp(context, link = null))),
                actions = {
                    if (state.refreshing) {
                        Box(GlanceModifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(GlanceModifier.size(24.dp), color = GlanceTheme.colors.primary)
                        }
                    } else {
                        CircleIconButton(
                            imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
                            contentDescription = context.getString(R.string.widget_refresh),
                            onClick = refresh,
                            backgroundColor = null,
                        )
                    }
                },
            )
        },
    ) {
        when {
            state.items.isNotEmpty() -> QuestionList(state.items)
            !state.loaded || state.refreshing -> Message(context.getString(R.string.widget_loading), onClick = null)
            state.failed -> Message(context.getString(R.string.hot_failed), onClick = refresh)
            else -> Message(context.getString(R.string.widget_empty), onClick = refresh)
        }
    }
}

@Composable
private fun QuestionList(items: List<Question>) {
    LazyColumn(GlanceModifier.fillMaxSize()) {
        itemsIndexed(items) { index, question ->
            QuestionRow(rank = index + 1, question = question)
        }
    }
}

/** The rank (as on Home's cards), the title, then answers and pins. */
@Composable
private fun QuestionRow(rank: Int, question: Question) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(actionStartActivity(openApp(context, question.shareUrl))),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = rank.toString(),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(24.dp),
        )
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = question.title,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = metaText(context, question),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Message(text: String, onClick: Action?) {
    val base = GlanceModifier.fillMaxSize().padding(8.dp)
    Box(
        modifier = if (onClick != null) base.clickable(onClick) else base,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center),
        )
    }
}

/** "12 answers · 3 pins". No time: the widget may redraw only hourly, so "5 min" would soon be wrong. */
private fun metaText(context: Context, question: Question): String {
    val resources = context.resources
    val answers = if (question.answerCount == 0) {
        context.getString(R.string.no_answers)
    } else {
        resources.getQuantityString(R.plurals.answers_count, question.answerCount, question.answerCount)
    }
    if (question.pinCount <= 0) return answers
    return answers + " · " + resources.getQuantityString(R.plurals.pins_count, question.pinCount, question.pinCount)
}

/** Opens the app, or one question in it, bringing the running app forward like a phone notification does. */
private fun openApp(context: Context, link: String?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (link != null) {
            action = Intent.ACTION_VIEW
            data = Uri.parse(link)
        }
    }
