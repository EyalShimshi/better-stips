package io.github.eyalshimshi.betterstips.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.user.UserProfileScreen

/** One row of a grouped card: 24 dp outer corners, 4 dp inner corners, 2 dp gaps (DESIGN-PHILOSOPHY §3.4). */
@Composable
fun GroupRow(
    title: String,
    index: Int,
    count: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val outer: Dp = 24.dp
    val inner: Dp = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            { Text(summary) }
        } else {
            null
        },
        leadingContent = if (icon != null) {
            { Icon(icon, contentDescription = null) }
        } else {
            null
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 2.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun ProfileScreen(
    onSignIn: () -> Unit,
    onOpenMyList: (MyList) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    onOpenConversation: (Long, String) -> Unit,
) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val userId = session.userId
    if (session.signedIn && userId != null) {
        // Signed in, the tab is your own profile: the same page everyone else sees, plus your lists and tools.
        UserProfileScreen(
            userId = userId,
            onBack = null,
            onOpenQuestion = onOpenQuestion,
            onOpenConversation = onOpenConversation,
            onOpenMyList = onOpenMyList,
            onOpenSaved = onOpenSaved,
            onOpenSettings = onOpenSettings,
        )
    } else {
        SignedOutProfile(onSignIn, onOpenSaved, onOpenSettings)
    }
}

@Composable
private fun SignedOutProfile(onSignIn: () -> Unit, onOpenSaved: () -> Unit, onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val saved by container.saved.items.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "invite") {
                MessageCard(
                    stringResource(R.string.profile_signed_out_title),
                    stringResource(R.string.profile_signed_out_body),
                    stringResource(R.string.action_sign_in),
                    onSignIn,
                )
            }
            item(key = "lists") {
                Column {
                    GroupRow(
                        stringResource(R.string.saved_title),
                        0,
                        2,
                        onOpenSaved,
                        summary = if (saved.isEmpty()) null else saved.size.toString(),
                        icon = Icons.Outlined.Bookmarks,
                    )
                    GroupRow(stringResource(R.string.settings_title), 1, 2, onOpenSettings, icon = Icons.Outlined.Settings)
                }
            }
        }
    }
}