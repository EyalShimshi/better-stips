package io.github.eyalshimshi.betterstips.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.AppContainer
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.ThemeMode
import io.github.eyalshimshi.betterstips.data.Timings
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import io.github.eyalshimshi.betterstips.ui.profile.GroupLabel
import io.github.eyalshimshi.betterstips.ui.profile.GroupRow
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.notify.Notifier
import io.github.eyalshimshi.betterstips.ui.notifications.rememberPhoneNotificationsToggle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenMyProfile: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val session by container.session.session.collectAsStateWithLifecycle()
    val timings by container.timings.recent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val store = container.settings
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.action_failed)
    val toggleNotifications = rememberPhoneNotificationsToggle(snackbar)
    val notificationsOn = settings.backgroundNotifications && Notifier.canPost(context)
    var safeBusy by remember { mutableStateOf(false) }
    fun toggleSafeMode() {
        safeBusy = true
        scope.launch {
            try {
                container.repository.setSafeMode(!session.safeFilter)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(failedText)
            } finally {
                safeBusy = false
            }
        }
    }
    val external: @Composable () -> Unit = {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.opens_in_browser))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (session.signedIn) {
                item(key = "account") {
                    Column {
                        GroupLabel(stringResource(R.string.settings_account))
                        GroupRow(
                            stringResource(R.string.settings_edit_profile),
                            0,
                            5,
                            onOpenMyProfile,
                            summary = stringResource(R.string.settings_edit_profile_summary),
                        )
                        GroupRow(
                            title = stringResource(R.string.settings_safe_mode),
                            summary = stringResource(R.string.settings_safe_mode_summary),
                            index = 1,
                            count = 5,
                            onClick = if (safeBusy) null else ({ toggleSafeMode() }),
                            trailing = { Switch(checked = session.safeFilter, onCheckedChange = null, enabled = !safeBusy) },
                        )
                        GroupRow(
                            stringResource(R.string.settings_account_details),
                            2,
                            5,
                            { openInBrowser(context, "$SITE_URL/settings/account-details") },
                            summary = stringResource(R.string.settings_on_site),
                            trailing = external,
                        )
                        GroupRow(
                            stringResource(R.string.settings_change_password),
                            3,
                            5,
                            { openInBrowser(context, "$SITE_URL/settings/change-password") },
                            summary = stringResource(R.string.settings_on_site),
                            trailing = external,
                        )
                        GroupRow(stringResource(R.string.sign_out), 4, 5, { scope.launch { container.session.signOut() } })
                    }
                }
            }
            item(key = "updates") {
                Column {
                    GroupLabel(stringResource(R.string.settings_updates))
                    GroupRow(
                        title = stringResource(R.string.settings_phone_notifications),
                        summary = stringResource(
                            if (session.signedIn) R.string.settings_phone_notifications_summary else R.string.settings_phone_notifications_signed_out,
                        ),
                        index = 0,
                        count = 2,
                        onClick = { toggleNotifications(!notificationsOn) },
                        trailing = { Switch(checked = notificationsOn, onCheckedChange = null) },
                    )
                    GroupRow(
                        title = stringResource(R.string.settings_live_new),
                        summary = stringResource(R.string.settings_live_new_summary),
                        index = 1,
                        count = 2,
                        onClick = { store.update { it.copy(autoRefreshNew = !it.autoRefreshNew) } },
                        trailing = { Switch(checked = settings.autoRefreshNew, onCheckedChange = null) },
                    )
                }
            }
            item(key = "colors") {
                Column {
                    GroupLabel(stringResource(R.string.settings_colors))
                    GroupRow(
                        title = stringResource(R.string.settings_dynamic),
                        summary = stringResource(if (dynamicAvailable) R.string.settings_dynamic_summary else R.string.settings_dynamic_unavailable),
                        index = 0,
                        count = 4,
                        onClick = if (dynamicAvailable) ({ store.update { it.copy(dynamicColor = !it.dynamicColor) } }) else null,
                        trailing = {
                            Switch(
                                checked = settings.dynamicColor && dynamicAvailable,
                                onCheckedChange = null,
                                enabled = dynamicAvailable,
                            )
                        },
                    )
                    GroupRow(
                        title = stringResource(R.string.settings_accent),
                        summary = stringResource(R.string.settings_accent_summary),
                        index = 1,
                        count = 4,
                        onClick = null,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .fillMaxWidth(),
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(AppSettingsAccents) { accent ->
                                val selected = !settings.dynamicColor && settings.accent == accent
                                Swatch(accent, selected) { store.update { it.copy(accent = accent, dynamicColor = false) } }
                            }
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                            SingleChoiceSegmentedButtonRow(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                val modes = ThemeMode.entries
                                modes.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = settings.themeMode == mode,
                                        onClick = { store.update { it.copy(themeMode = mode) } },
                                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                    ) {
                                        Text(
                                            stringResource(
                                                when (mode) {
                                                    ThemeMode.System -> R.string.theme_system
                                                    ThemeMode.Light -> R.string.theme_light
                                                    ThemeMode.Dark -> R.string.theme_dark
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    GroupRow(
                        title = stringResource(R.string.settings_pure_black),
                        summary = stringResource(R.string.settings_pure_black_summary),
                        index = 3,
                        count = 4,
                        onClick = { store.update { it.copy(pureBlack = !it.pureBlack) } },
                        trailing = { Switch(checked = settings.pureBlack, onCheckedChange = null) },
                    )
                }
            }
            item(key = "interface") {
                Column {
                    GroupLabel(stringResource(R.string.settings_interface))
                    GroupRow(
                        title = stringResource(R.string.settings_images),
                        summary = stringResource(R.string.settings_images_summary),
                        index = 0,
                        count = 2,
                        onClick = { store.update { it.copy(showImages = !it.showImages) } },
                        trailing = { Switch(checked = settings.showImages, onCheckedChange = null) },
                    )
                    GroupRow(
                        title = stringResource(R.string.settings_animations),
                        summary = stringResource(R.string.settings_animations_summary),
                        index = 1,
                        count = 2,
                        onClick = { store.update { it.copy(animations = !it.animations) } },
                        trailing = { Switch(checked = settings.animations, onCheckedChange = null) },
                    )
                }
            }
            item(key = "performance") {
                Column {
                    GroupLabel(stringResource(R.string.settings_performance))
                    if (timings.isEmpty()) {
                        GroupRow(stringResource(R.string.settings_performance_empty), 0, 1, null)
                    } else {
                        val shown = timings.take(8)
                        shown.forEachIndexed { index, timing ->
                            GroupRow(
                                title = stringResource(if (timing.label == Timings.QUESTION) R.string.timing_question else R.string.timing_feed),
                                index = index,
                                count = shown.size,
                                onClick = null,
                                trailing = {
                                    Text(stringResource(R.string.timing_value, String.format(Locale.getDefault(), "%.2f", timing.millis / 1000.0)))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "stips") {
                Column {
                    GroupLabel(stringResource(R.string.settings_stips))
                    GroupRow(stringResource(R.string.settings_contact), 0, 4, { openInBrowser(context, "$SITE_URL/contact-us") }, trailing = external)
                    GroupRow(stringResource(R.string.settings_rules), 1, 4, { openInBrowser(context, "$SITE_URL/rules") }, trailing = external)
                    GroupRow(stringResource(R.string.settings_about_stips), 2, 4, { openInBrowser(context, "$SITE_URL/about") }, trailing = external)
                    GroupRow(
                        stringResource(R.string.settings_delete_account),
                        3,
                        4,
                        { openInBrowser(context, "$SITE_URL/delete-account") },
                        summary = stringResource(R.string.settings_on_site),
                        trailing = external,
                    )
                }
            }
            item(key = "about") {
                Column {
                    GroupLabel(stringResource(R.string.settings_about))
                    GroupRow(stringResource(R.string.about_version), 0, 5, null, summary = container.versionName)
                    GroupRow(
                        stringResource(R.string.about_source),
                        1,
                        5,
                        { openInBrowser(context, AppContainer.REPO_URL) },
                        summary = stringResource(R.string.about_source_summary),
                    )
                    GroupRow(
                        stringResource(R.string.about_license),
                        2,
                        5,
                        { openInBrowser(context, "https://www.gnu.org/licenses/gpl-3.0.html") },
                        summary = stringResource(R.string.about_license_summary),
                    )
                    GroupRow(
                        stringResource(R.string.about_report),
                        3,
                        5,
                        { openInBrowser(context, AppContainer.REPO_URL + "/issues/new") },
                        summary = stringResource(R.string.about_report_summary),
                    )
                    GroupRow(stringResource(R.string.disclaimer), 4, 5, null)
                }
            }
        }
    }
}

private val AppSettingsAccents: List<Int> get() = io.github.eyalshimshi.betterstips.data.AppSettings.ACCENTS

@Composable
private fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    val label = "#" + String.format(Locale.ROOT, "%06X", color and 0xFFFFFF)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
    }
}
