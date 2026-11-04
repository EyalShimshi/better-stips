package io.github.eyalshimshi.betterstips.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.diag.NetLog
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.components.shareText
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The network log (#3): what the app sent to Stips recently and what came back, newest first. Copy or share it to a
 * bug report. It holds no passwords, cookies, tokens or message text.
 */
@Composable
fun NetLogScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val version by NetLog.version.collectAsStateWithLifecycle()
    val entries = remember(version) { NetLog.snapshot().asReversed() }
    val copiedText = stringResource(R.string.netlog_copied)
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm:ss.SSS") }
    val zone = remember { ZoneId.systemDefault() }

    fun fullText(): String = NetLog.format(NetLog.snapshot(), container.diagnosticsHeader())

    fun copy() {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Stips+ network log", fullText()))
        // Android 13+ shows its own confirmation.
        if (Build.VERSION.SDK_INT < 33) scope.launch { snackbar.showSnackbar(copiedText) }
    }

    val reportTitle = stringResource(R.string.netlog_report_title)
    val reportBody = stringResource(R.string.netlog_report_body)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.netlog_title)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { copy() }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.netlog_copy))
                    }
                    IconButton(onClick = { shareText(context, fullText()) }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                    }
                    IconButton(onClick = { NetLog.clear() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.netlog_clear))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "about") {
                MessageCard(
                    title = stringResource(R.string.netlog_about_title),
                    body = stringResource(R.string.netlog_about_body),
                    actionLabel = stringResource(R.string.netlog_report),
                    onAction = {
                        copy()
                        val url = AppContainer.REPO_URL + "/issues/new?title=" + URLEncoder.encode(reportTitle, "UTF-8") +
                            "&body=" + URLEncoder.encode(reportBody, "UTF-8")
                        openInBrowser(context, url)
                    },
                )
            }
            if (entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.netlog_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            items(entries) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(Modifier.width(92.dp)) {
                        Text(
                            Instant.ofEpochMilli(entry.timeMs).atZone(zone).format(clock),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            entry.kind,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.text.contains("FAILED") || entry.kind == "backoff") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        entry.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
