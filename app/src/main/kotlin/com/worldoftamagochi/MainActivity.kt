package com.worldoftamagochi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldoftamagochi.data.Settings
import com.worldoftamagochi.designsystem.WotTheme
import com.worldoftamagochi.feature.home.HomeRoute
import com.worldoftamagochi.ui.sound.GameSounds
import com.worldoftamagochi.ui.sound.LocalGameSounds
import com.worldoftamagochi.ui.sound.SpriteGameSounds

class MainActivity : ComponentActivity() {
    private var sounds: SpriteGameSounds? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = (application as WotApplication).repository
        val sounds = SpriteGameSounds(this).also { this.sounds = it }
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = Settings())
            // Sound and vibration toggles apply everywhere through the composition.
            CompositionLocalProvider(
                LocalGameSounds provides if (settings.sound) sounds else GameSounds.None,
                LocalHapticFeedback provides if (settings.haptics) LocalHapticFeedback.current else NoHaptics,
            ) {
                WotTheme {
                    HomeRoute(repository)
                }
            }
        }
    }

    override fun onDestroy() {
        sounds?.release()
        sounds = null
        super.onDestroy()
    }
}

private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
