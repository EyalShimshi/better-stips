package io.github.eyalshimshi.betterstips.ui.ask

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.MessageCard
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.channelLabel
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.rememberAnonymousModeOn
import io.github.eyalshimshi.betterstips.util.AskRules

/** Asking a question (#4): the question, then its tags, then a photo from Stips' gallery. */
@Composable
fun AskScreen(channelId: Int?, onClose: () -> Unit, onPosted: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel(key = "ask-$channelId") { AskViewModel(it.repository, it.session, it.settings, channelId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val anonymousMode = rememberAnonymousModeOn(settings.anonymousUntil)
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.postedId) {
        val id = state.postedId ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onPosted(id)
    }
    LaunchedEffect(anonymousMode) {
        if (anonymousMode && !state.anonymous) vm.setAnonymous(true)
    }
    BackHandler(enabled = state.step != AskStep.Question) { vm.back() }

    val steps = if (vm.photoNeeded) 3 else 2
    val stepIndex = state.step.ordinal
    val last = stepIndex == steps - 1
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ask_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close)) }
                },
            )
        },
        bottomBar = {
            if (state.allowance?.allowed != false) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding(),
                    ) {
                        LinearProgressIndicator(
                            progress = { (stepIndex + 1f) / steps },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.ask_step, stepIndex + 1, steps),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            if (stepIndex > 0) {
                                OutlinedButton(onClick = { vm.back() }, enabled = !state.posting) { Text(stringResource(R.string.ask_back)) }
                                Spacer(Modifier.width(8.dp))
                            }
                            Button(onClick = { vm.next() }, enabled = !state.posting) {
                                if (state.posting) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text(stringResource(if (last) R.string.ask_post else R.string.ask_next))
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val allowance = state.allowance
            when {
                allowance != null && !allowance.allowed -> MessageCard(
                    stringResource(R.string.ask_limit_title),
                    stringResource(R.string.ask_limit_body, allowance.maxPerDay ?: 0),
                    stringResource(R.string.action_close),
                    onClose,
                )
                state.step == AskStep.Question -> QuestionStep(
                    state = state,
                    anonymousMode = anonymousMode,
                    channelName = channels.firstOrNull { it.id == state.channelId }?.let { channelLabel(it) },
                    vm = vm,
                )
                state.step == AskStep.Tags -> TagsStep(state, vm)
                else -> PhotoStep(state, vm)
            }
        }
    }
}

@Composable
private fun QuestionStep(state: AskUiState, anonymousMode: Boolean, channelName: String?, vm: AskViewModel) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (channelName != null) {
            InputChip(
                selected = true,
                onClick = vm::leaveChannel,
                label = { Text(stringResource(R.string.ask_in_channel, channelName)) },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ask_leave_channel), modifier = Modifier.size(18.dp)) },
            )
        }
        val titleProblem = AskRules.titleProblem(state.title)
        OutlinedTextField(
            value = state.title,
            onValueChange = vm::setTitle,
            label = { Text(stringResource(R.string.ask_title_label)) },
            placeholder = { Text(stringResource(R.string.ask_title_hint)) },
            supportingText = {
                val problem = if (state.showErrors) titleProblem else null
                Text(if (problem != null) problemText(problem, AskRules.TITLE_MIN, AskRules.TITLE_MAX) else "${state.title.trim().length} / ${AskRules.TITLE_MAX}")
            },
            isError = state.showErrors && titleProblem != null,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        val bodyProblem = AskRules.bodyProblem(state.body)
        OutlinedTextField(
            value = state.body,
            onValueChange = vm::setBody,
            label = { Text(stringResource(R.string.ask_body_label)) },
            supportingText = {
                val problem = if (state.showErrors) bodyProblem else null
                Text(if (problem != null) problemText(problem, 0, AskRules.BODY_MAX) else "${state.body.length} / ${AskRules.BODY_MAX}")
            },
            isError = state.showErrors && bodyProblem != null,
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.emojisRemoved) {
            // Stips cuts a question at its first emoji, so they're left out (#12).
            Text(
                stringResource(R.string.no_emoji_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.anonymous,
                enabled = !anonymousMode,
                onClick = {
                    haptics.performHapticFeedback(if (state.anonymous) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    vm.setAnonymous(!state.anonymous)
                },
                label = { Text(stringResource(if (anonymousMode) R.string.chip_anonymous_mode else R.string.ask_anonymous)) },
                leadingIcon = {
                    Icon(if (state.anonymous) Icons.Outlined.PersonOff else Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            if (!state.showLink && state.link.isEmpty()) {
                AssistChip(
                    onClick = vm::showLink,
                    label = { Text(stringResource(R.string.add_link)) },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        if (state.anonymous) {
            val nicknameOk = AskRules.nicknameOk(state.nickname)
            OutlinedTextField(
                value = state.nickname,
                onValueChange = vm::setNickname,
                label = { Text(stringResource(R.string.ask_nickname_label)) },
                supportingText = { Text(stringResource(R.string.ask_nickname_help, AskRules.NICKNAME_MIN, AskRules.NICKNAME_MAX)) },
                isError = !nicknameOk,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.showLink || state.link.isNotEmpty()) {
            val linkBad = state.link.isNotBlank() && AskRules.normalizeLink(state.link) == null
            OutlinedTextField(
                value = state.link,
                onValueChange = vm::setLink,
                label = { Text(stringResource(R.string.ask_link_label)) },
                placeholder = { Text(stringResource(R.string.paste_link_here)) },
                singleLine = true,
                isError = linkBad,
                supportingText = if (AskRules.isYouTube(AskRules.normalizeLink(state.link))) {
                    { Text(stringResource(R.string.ask_youtube_note)) }
                } else {
                    null
                },
                trailingIcon = {
                    IconButton(onClick = vm::removeLink) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_link)) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TagsStep(state: AskUiState, vm: AskViewModel) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.ask_tags_chosen, state.tags.size, AskRules.TAGS_MAX),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (state.showErrors && state.tags.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (state.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { vm.toggleTag(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ask_remove_tag), modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.tagInput,
            onValueChange = vm::setTagInput,
            label = { Text(stringResource(R.string.ask_add_tag)) },
            singleLine = true,
            enabled = state.tags.size < AskRules.TAGS_MAX,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.addTypedTag() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.completions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.completions.forEach { tag ->
                    SuggestionChip(onClick = { vm.addTypedTag(tag) }, label = { Text(tag) })
                }
            }
        }
        Text(stringResource(R.string.ask_tags_suggested), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (state.suggested.isEmpty()) {
            Text(stringResource(R.string.ask_tags_none), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.suggested.forEach { tag ->
                    val selected = tag in state.tags
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptics.performHapticFeedback(if (selected) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                            vm.toggleTag(tag)
                        },
                        label = { Text(tag) },
                        enabled = selected || state.tags.size < AskRules.TAGS_MAX,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoStep(state: AskUiState, vm: AskViewModel) {
    val loader = LocalAppContainer.current.imageLoader
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.photoQuery,
            onValueChange = vm::setPhotoQuery,
            label = { Text(stringResource(R.string.ask_photo_search)) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { vm.searchPhotos() }) { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.ask_photo_search)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.searchPhotos() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        val error = state.error
        if (error != null) {
            Text(
                errorMessage(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.showErrors && state.photoId == null) {
            Text(
                stringResource(R.string.ask_photo_needed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        when {
            state.photosLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.photos.isEmpty() -> Text(
                stringResource(R.string.ask_photo_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.photos, key = { it.photoId }) { photo ->
                    val selected = photo.photoId == state.photoId
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (selected) {
                                    Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                vm.selectPhoto(photo.photoId)
                            },
                    ) {
                        AsyncImage(
                            model = photo.thumbnailUrl,
                            contentDescription = null,
                            imageLoader = loader,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.ask_photo_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun problemText(problem: AskRules.Problem, min: Int, max: Int): String = when (problem) {
    AskRules.Problem.Empty -> stringResource(R.string.ask_required)
    AskRules.Problem.TooShort -> stringResource(R.string.ask_too_short, min)
    AskRules.Problem.TooLong -> stringResource(R.string.ask_too_long, max)
    AskRules.Problem.RepeatedLetters -> stringResource(R.string.answer_problem_repeated)
}
