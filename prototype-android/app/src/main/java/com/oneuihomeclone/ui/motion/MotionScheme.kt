package com.oneuihomeclone.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.oneuihomeclone.data.LauncherPreferences
import com.oneuihomeclone.data.MotionPresetKey

/**
 * Centralized motion presets so flipping Standard -> Reduced changes every transition in
 * lockstep. Consumed through [LocalMotionScheme] — call sites ask for a named spring:
 * ```
 * val spec = LocalMotionScheme.current.drawerOpen.spec<Dp>()
 * animateDpAsState(target, animationSpec = spec)
 * ```
 * rather than hard-coding `spring(dampingRatio, stiffness)` across the prototype.
 *
 * Params are exposed as raw `(dampingRatio, stiffness)` pairs rather than a concrete
 * `SpringSpec<Float>` because call sites animate typed values (`Dp`, `Offset`, `Color`).
 * Compose's generic-typed `spring<T>()` call must be built at the site where the type is
 * known, so [SpringParams.spec] supplies it.
 *
 * Reduced preset targets the "Reduce animations" accessibility setting: higher damping,
 * lower stiffness — perceivable motion without overshoot. It is *not* silent: haptics
 * and colour transitions remain independent by design (accessibility guidance: keep the
 * navigation cue, soften the parallax). [forPreset] accepts a `reduceMotionHint` so the
 * top-level [ProvideMotionScheme] can OR in the platform's animator-scale-disabled
 * signal (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`) without callers repeating the
 * logic.
 */
@Immutable
data class SpringParams(val dampingRatio: Float, val stiffness: Float)

@Immutable
data class MotionScheme(
    val pageTransition: SpringParams,
    val drawerOpen: SpringParams,
    val drawerClose: SpringParams,
    val editTrayEnter: SpringParams,
    val overlayCollapse: SpringParams,
    val widgetResize: SpringParams,
) {
    companion object {
        val Standard: MotionScheme = MotionScheme(
            pageTransition = SpringParams(0.82f, Spring.StiffnessMediumLow),
            drawerOpen = SpringParams(0.78f, Spring.StiffnessMedium),
            drawerClose = SpringParams(0.85f, Spring.StiffnessMedium),
            editTrayEnter = SpringParams(0.9f, Spring.StiffnessMedium),
            overlayCollapse = SpringParams(1.0f, Spring.StiffnessMedium),
            widgetResize = SpringParams(0.8f, Spring.StiffnessMediumLow),
        )

        val Reduced: MotionScheme = MotionScheme(
            pageTransition = SpringParams(1.0f, Spring.StiffnessLow),
            drawerOpen = SpringParams(1.0f, Spring.StiffnessLow),
            drawerClose = SpringParams(1.0f, Spring.StiffnessLow),
            editTrayEnter = SpringParams(1.0f, Spring.StiffnessLow),
            overlayCollapse = SpringParams(1.0f, Spring.StiffnessMedium),
            widgetResize = SpringParams(1.0f, Spring.StiffnessLow),
        )

        /**
         * @param preset user-selected motion preference.
         * @param reduceMotionHint platform-level override. [ProvideMotionScheme] reads
         *   `Settings.Global.ANIMATOR_DURATION_SCALE` and passes true when it is 0.0f —
         *   the user has disabled animations at the OS level via Developer Options or
         *   Accessibility → "Remove animations". If true, force REDUCED regardless of
         *   [preset].
         */
        fun forPreset(preset: MotionPresetKey, reduceMotionHint: Boolean = false): MotionScheme =
            if (reduceMotionHint || preset == MotionPresetKey.REDUCED) Reduced else Standard
    }
}

/** Build a typed `FiniteAnimationSpec` from a stored [SpringParams]. */
inline fun <reified T> SpringParams.spec(): FiniteAnimationSpec<T> =
    spring(dampingRatio = dampingRatio, stiffness = stiffness)

/**
 * Composition-locally scoped motion scheme. Top-level launcher shell sets this from the
 * user preference; transitions anywhere in the tree resolve via `LocalMotionScheme.current`.
 */
val LocalMotionScheme = staticCompositionLocalOf { MotionScheme.Standard }

/**
 * Top-level provider wired from the Activity. Seeds [LocalMotionScheme] from the user's
 * persisted [MotionPresetKey] (via [LauncherPreferences]) OR'd with the system-level
 * animator-scale=0 signal (users globally disable animations via Developer Options /
 * Accessibility → "Remove animations").
 *
 * Seed-from-snapshot is acceptable for v0.2.0 — live-switch without restart will land
 * once the motion preset toggle ships in the settings UI (follow-up iter). The snapshot
 * approach means changing the preset today requires an Activity recreate to take effect.
 */
@Composable
fun ProvideMotionScheme(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val preset = remember(context) { LauncherPreferences(context).snapshot().motionPreset }
    val systemReduce = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            ) == 0.0f
        }.getOrDefault(false)
    }
    val scheme = remember(preset, systemReduce) {
        MotionScheme.forPreset(preset, reduceMotionHint = systemReduce)
    }
    CompositionLocalProvider(LocalMotionScheme provides scheme, content = content)
}
