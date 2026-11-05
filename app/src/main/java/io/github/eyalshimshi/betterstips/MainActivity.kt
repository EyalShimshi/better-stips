package io.github.eyalshimshi.betterstips

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.eyalshimshi.betterstips.ui.AppRoot
import io.github.eyalshimshi.betterstips.ui.components.LocalAppContainer
import io.github.eyalshimshi.betterstips.ui.theme.StipsTheme
import io.github.eyalshimshi.betterstips.ui.theme.isDarkTheme
import io.github.eyalshimshi.betterstips.util.DeepLink
import io.github.eyalshimshi.betterstips.util.Links
import io.github.eyalshimshi.betterstips.util.Shortcuts
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** A link to open (from another app or a phone notification), until AppRoot has navigated to it. */
    private val pendingLink = MutableStateFlow<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as StipsApp).container
        container.warmUp()
        // After a rotation or a restore, the back stack already holds the linked screen.
        if (savedInstanceState == null) pendingLink.value = linkFrom(intent)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val link by pendingLink.collectAsStateWithLifecycle()
            val dark = isDarkTheme(settings)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                StipsTheme(settings) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppRoot(pendingLink = link, onLinkHandled = { pendingLink.value = null })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val link = linkFrom(intent)
        if (link != null) pendingLink.value = link
    }

    /** A Stips link (from another app or a phone notification), a launcher shortcut, or a chats widget row. */
    private fun linkFrom(intent: Intent?): DeepLink? =
        Links.deepLink(intent?.dataString) ?: Shortcuts.link(intent?.action, intent?.dataString)
}
