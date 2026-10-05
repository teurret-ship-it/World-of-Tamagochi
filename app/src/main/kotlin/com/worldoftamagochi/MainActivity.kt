package com.worldoftamagochi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.worldoftamagochi.designsystem.WotTheme
import com.worldoftamagochi.feature.home.HomeRoute
import com.worldoftamagochi.ui.sound.LocalGameSounds
import com.worldoftamagochi.ui.sound.SpriteGameSounds

class MainActivity : ComponentActivity() {
    private var sounds: SpriteGameSounds? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val sounds = SpriteGameSounds(this).also { this.sounds = it }
        setContent {
            CompositionLocalProvider(LocalGameSounds provides sounds) {
                WotTheme {
                    HomeRoute()
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
