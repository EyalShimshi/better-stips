package io.github.eyalshimshi.betterstips.ui.question

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Question
import io.github.eyalshimshi.betterstips.util.ThreadExport

/** How much of the export the dialog shows; copying and sharing take all of it. */
private const val PREVIEW_CHARS = 4_000

/**
 * "Export thread" (#16): the question and its answers as Markdown or JSON ([ThreadExport]), with a preview, to copy
 * or share. [answers] in Stips' order, [hidden] the answers the person hid.
 */
@Composable
internal fun ExportThreadDialog(
    question: Question,
    answers: List<Answer>,
    hidden: Set<Long>,
    channel: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var format by rememberSaveable { mutableStateOf(ThreadExport.Format.Markdown) }
    val text = remember(format, question, answers, hidden, channel) { ThreadExport.export(format, question, answers, hidden, channel) }
    var copied by remember(format) { mutableStateOf(false) }
    var failed by remember(format) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        title = { Text(stringResource(R.string.export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.export_body), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val formats = ThreadExport.Format.entries
                    formats.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = format == option,
                            onClick = { format = option },
                            shape = SegmentedButtonDefaults.itemShape(index, formats.size),
                        ) {
                            Text(stringResource(if (option == ThreadExport.Format.Markdown) R.string.export_markdown else R.string.export_json))
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    // The export reads left to right (its labels are English), whatever the app's language.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            if (text.length > PREVIEW_CHARS) text.take(PREVIEW_CHARS).trimEnd() + "\n\u2026" else text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        )
                    }
                }
                if (failed) {
                    Text(
                        stringResource(R.string.export_too_long),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { if (!share(context, text)) failed = true }) { Text(stringResource(R.string.action_share)) }
                TextButton(onClick = { if (copy(context, text)) copied = true else failed = true }) {
                    Text(stringResource(if (copied) R.string.export_copied else R.string.action_copy))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** Copies the export; false when Android refuses it (a very long thread can be over its limit). */
private fun copy(context: Context, text: String): Boolean = try {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("Stips thread", text))
    clipboard != null
} catch (e: RuntimeException) {
    false
}

private fun share(context: Context, text: String): Boolean = try {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
    true
} catch (e: RuntimeException) {
    false
}
