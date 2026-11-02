package io.github.eyalshimshi.betterstips.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.model.FeedQuery
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.feed.FeedFilters
import io.github.eyalshimshi.betterstips.ui.feed.FeedViewModel
import io.github.eyalshimshi.betterstips.ui.feed.QuestionList
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenQuestion: (Long) -> Unit, onOpenTopic: (String) -> Unit, onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { FeedViewModel(it.repository, FeedQuery(), useCache = true) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val channels by container.channels.channels.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { SearchBarRow(onSearch = onOpenTopic, onOpenSettings = onOpenSettings) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        QuestionList(
            state = state,
            channels = channels,
            emptyTitle = stringResource(R.string.empty_feed),
            onOpen = { onOpenQuestion(it.id) },
            onRefresh = { vm.refresh() },
            onLoadMore = { vm.loadMore() },
            onRetry = { vm.retry() },
            modifier = Modifier.padding(padding),
            listState = listState,
            header = {
                item(key = "filters") {
                    FeedFilters(
                        query,
                        channels,
                        showChannels = true,
                        onChange = {
                            vm.setQuery(it)
                            scope.launch { listState.scrollToItem(0) }
                        },
                    )
                }
            },
        )
    }
}

/** The pill at the top of Home. Stips has no full-text search, so it opens a topic (D-15). */
@Composable
private fun SearchBarRow(onSearch: (String) -> Unit, onOpenSettings: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            placeholder = { Text(stringResource(R.string.search_topic_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val topic = text.trim().removePrefix("#").trim()
                    if (topic.isNotEmpty()) {
                        keyboard?.hide()
                        onSearch(topic)
                    }
                },
            ),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.action_settings))
        }
    }
}
