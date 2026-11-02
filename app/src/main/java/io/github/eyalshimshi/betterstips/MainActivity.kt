package io.github.eyalshimshi.betterstips

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
import io.github.eyalshimshi.betterstips.util.Links

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as StipsApp).container
        val linkedQuestionId = intent?.dataString?.let { Links.questionId(it) }
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
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
                        AppRoot(initialQuestionId = linkedQuestionId)
                    }
                }
            }
        }
    }
}
