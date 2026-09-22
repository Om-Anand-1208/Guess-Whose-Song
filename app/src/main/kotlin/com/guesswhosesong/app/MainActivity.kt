package com.guesswhosesong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guesswhosesong.app.ui.GWSNavHost
import com.guesswhosesong.app.ui.theme.GuessWhoseSongTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuessWhoseSongTheme {
                GWSNavHost()
            }
        }
    }
}
