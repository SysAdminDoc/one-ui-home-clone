package com.oneuihomeclone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLauncherStatusTest {

    @Test
    fun shouldShowDefaultLauncherPrompt_requiresCheckedNonDefaultUndismissedState() {
        val nonDefault = DefaultLauncherState(
            checked = true,
            isDefaultLauncher = false,
            canOpenSettings = true,
        )

        assertTrue(shouldShowDefaultLauncherPrompt(nonDefault, dismissedForSession = false))
        assertFalse(shouldShowDefaultLauncherPrompt(nonDefault, dismissedForSession = true))
        assertFalse(
            shouldShowDefaultLauncherPrompt(
                nonDefault.copy(isDefaultLauncher = true),
                dismissedForSession = false,
            ),
        )
        assertFalse(
            shouldShowDefaultLauncherPrompt(
                DefaultLauncherState.Unknown,
                dismissedForSession = false,
            ),
        )
    }
}
