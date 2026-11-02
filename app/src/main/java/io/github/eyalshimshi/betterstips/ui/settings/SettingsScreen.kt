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
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val timings by container.timings.recent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val store = container.settings

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
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
