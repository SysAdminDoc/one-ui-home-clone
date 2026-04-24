package com.oneuihomeclone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.oneuihomeclone.ui.OneUiHomeCloneApp
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme

class MainActivity : ComponentActivity() {

    /**
     * Bumped every time the system delivers a HOME intent (user pressed HOME or selected
     * launcher from the home-app picker). Compose observes this to reset overlay state
     * and scroll back to the default home page.
     */
    private var homeIntentTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OneUiHomeCloneTheme {
                OneUiHomeCloneApp(
                    homeIntentTick = homeIntentTick,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // We're registered as HOME + DEFAULT — any re-entry (HOME key, launcher picker)
        // arrives here because launchMode=singleTask. Increment tick so Compose can
        // observe via LaunchedEffect(homeIntentTick) and collapse overlays.
        if (Intent.ACTION_MAIN == intent.action &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            homeIntentTick += 1
        }
    }
}
