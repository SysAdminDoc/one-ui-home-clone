package com.oneuihomeclone.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Samsung-style haptic tick helpers. Named after the interaction (not the constant)
 * so call sites document intent. All functions are no-ops when the platform doesn't
 * support the requested constant — never crash, never throw.
 *
 * Drop-to-edge page creation uses [edgePageCreation] which maps to the strongest
 * "clock" style tick available on the device. Long-press entry to edit mode uses
 * [longPressEntry] which is shorter and softer.
 */
object Haptics {

    fun edgePageCreation(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }

    fun longPressEntry(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }

    fun dragPickup(view: View) {
        // DRAG_START (API 30+) is the correct semantic grab feel. The pre-30 fallback
        // uses LONG_PRESS (not CONTEXT_CLICK) because CONTEXT_CLICK on older OEM skins
        // maps to a right-click-like "tick" that feels wrong under a grab gesture.
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.DRAG_START
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }
}
