package io.github.eyalshimshi.betterstips.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QuestionAnswer
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.MyList
import io.github.eyalshimshi.betterstips.ui.components.Avatar
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import kotlinx.coroutines.launch

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
) {
    val container = LocalAppContainer.current
    val session by container.session.session.collectAsStateWithLifecycle()
    val saved by container.saved.items.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
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
            val profile = session.profile
            if (session.signedIn) {
                item(key = "header") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (profile != null) {
                            Avatar(profile.avatar, 96.dp, large = true)
                            Text(profile.nickname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.points, profile.points),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item(key = "lists") {
                    Column {
                        GroupRow(stringResource(R.string.my_questions), 0, 3, { onOpenMyList(MyList.Asked) }, icon = Icons.Outlined.Edit)
                        GroupRow(stringResource(R.string.my_answers), 1, 3, { onOpenMyList(MyList.Answered) }, icon = Icons.Outlined.QuestionAnswer)
                        GroupRow(
                            stringResource(R.string.saved_title),
                            2,
                            3,
                            onOpenSaved,
                            summary = if (saved.isEmpty()) null else saved.size.toString(),
                            icon = Icons.Outlined.Bookmarks,
                        )
                    }
                }
            } else {
                item(key = "invite") {
                    MessageCard(
                        stringResource(R.string.profile_signed_out_title),
                        stringResource(R.string.profile_signed_out_body),
                        stringResource(R.string.action_sign_in),
                        onSignIn,
                    )
                }
                item(key = "saved") {
                    GroupRow(
                        stringResource(R.string.saved_title),
                        0,
                        1,
                        onOpenSaved,
                        summary = if (saved.isEmpty()) null else saved.size.toString(),
                        icon = Icons.Outlined.Bookmarks,
                    )
                }
            }
            item(key = "settings") {
                Column {
                    Spacer(Modifier.height(16.dp))
                    val rows = if (session.signedIn) 2 else 1
                    GroupRow(stringResource(R.string.settings_title), 0, rows, onOpenSettings, icon = Icons.Outlined.Settings)
                    if (session.signedIn) {
                        GroupRow(
                            stringResource(R.string.sign_out),
                            1,
                            rows,
                            { scope.launch { container.session.signOut() } },
                            icon = Icons.AutoMirrored.Outlined.Logout,
                        )
                    }
                }
            }
        }
    }
}
