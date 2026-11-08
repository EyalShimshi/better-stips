package io.github.eyalshimshi.betterstips.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.util.ImageLinks
import io.github.eyalshimshi.betterstips.util.Links

/**
 * A link attached to a question or an answer. A title written for it shows with the link's real site after it, so
 * it can't pass for another site (#14). A picture link also gets "Show image", which opens it in the app's viewer
 * (#15); the link itself still opens the browser.
 */
@Composable
fun AttachedLink(url: String, title: String?, style: TextStyle, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val image = remember(url) { ImageLinks.classify(url) }
    val host = remember(url) { Links.host(url) }
    var viewing by rememberSaveable(url) { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(8.dp))
                .clickable { if (image != null) openInBrowser(context, url) else uriHandler.openUri(url) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                title ?: host,
                style = style,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (title != null && !title.equals(host, ignoreCase = true)) {
                Spacer(Modifier.width(6.dp))
                Text(host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (image != null) {
            Spacer(Modifier.width(8.dp))
            ShowImageButton(onClick = { viewing = true })
        }
    }
    if (viewing && image != null) ImageLinkViewer(image, onDismiss = { viewing = false })
}

/** "Show image" (#15): a small tonal pill. */
@Composable
fun ShowImageButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.image_show), style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}
