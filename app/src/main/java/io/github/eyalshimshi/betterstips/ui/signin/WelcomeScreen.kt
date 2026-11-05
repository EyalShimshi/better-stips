package io.github.eyalshimshi.betterstips.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.ui.components.appViewModel

/**
 * The first screen after installing (#8): sign in with a Stips account, or look around as a guest. It covers the app
 * until one of them happens; the choice is remembered, and links or shortcuts that open the app skip it.
 */
@Composable
fun WelcomeScreen(onGuest: () -> Unit, modifier: Modifier = Modifier) {
    val vm = appViewModel { SignInViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            SignInFields(vm, state)
            OutlinedButton(onClick = onGuest, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_guest))
            }
            Text(
                stringResource(R.string.welcome_guest_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SignInLinks()
        }
    }
}
