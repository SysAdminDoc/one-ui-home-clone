package com.oneuihomeclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.oneuihomeclone.ui.OneUiHomeCloneApp
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OneUiHomeCloneTheme {
                OneUiHomeCloneApp()
            }
        }
    }
}
