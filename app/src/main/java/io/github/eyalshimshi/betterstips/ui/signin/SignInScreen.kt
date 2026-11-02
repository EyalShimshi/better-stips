package io.github.eyalshimshi.betterstips.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.eyalshimshi.betterstips.R
import io.github.eyalshimshi.betterstips.data.SessionStore
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.SITE_URL
import io.github.eyalshimshi.betterstips.ui.components.BackButton
import io.github.eyalshimshi.betterstips.ui.components.appViewModel
import io.github.eyalshimshi.betterstips.ui.components.asApiException
import io.github.eyalshimshi.betterstips.ui.components.errorMessage
import io.github.eyalshimshi.betterstips.ui.components.openInBrowser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val busy: Boolean = false,
    val error: ApiException? = null,
    val done: Boolean = false,
)

class SignInViewModel(private val session: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }

    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun togglePassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun signIn() {
        val current = _state.value
        if (current.busy || current.email.isBlank() || current.password.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                session.signIn(current.email, current.password)
                // The password leaves memory with this screen; only the token is kept.
                _state.update { it.copy(busy = false, password = "", done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.asApiException()) }
            }
        }
    }
}

@Composable
fun SignInScreen(onDone: () -> Unit) {
    val vm = appViewModel { SignInViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.done) { if (state.done) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_in_title)) },
                navigationIcon = { BackButton(onDone) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = vm::setEmail,
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::setPassword,
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.signIn() }),
                trailingIcon = {
                    IconButton(onClick = vm::togglePassword) {
                        Icon(
                            if (state.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(if (state.showPassword) R.string.hide_password else R.string.show_password),
                        )
                    }
                },
                isError = state.error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            val error = state.error
            if (error != null) {
                Text(
                    if (error.code == ApiException.GENERAL && error.serverMessage.isNullOrBlank()) {
                        stringResource(R.string.sign_in_failed)
                    } else {
                        errorMessage(error)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = vm::signIn,
                enabled = !state.busy && state.email.isNotBlank() && state.password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_sign_in))
                }
            }
            Text(
                stringResource(R.string.sign_in_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { openInBrowser(context, "$SITE_URL/signup") }) {
                Text(stringResource(R.string.sign_in_create_account))
            }
            TextButton(onClick = { openInBrowser(context, "$SITE_URL/reset-password") }) {
                Text(stringResource(R.string.sign_in_forgot))
            }
            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
