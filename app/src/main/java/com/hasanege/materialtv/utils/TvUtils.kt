package com.hasanege.materialtv.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch

object TvUtils {

    /**
     * Returns true if the physical hardware is an Android TV / Leanback device.
     */
    fun isHardwareTv(context: Context): Boolean {
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return true
        }
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Determines whether TV mode should be active based on user setting ("AUTO", "FORCE_TV", "FORCE_MOBILE")
     * and hardware capabilities.
     */
    fun isTvMode(context: Context, userPref: String): Boolean {
        return when (userPref.uppercase()) {
            "FORCE_TV" -> true
            "FORCE_MOBILE" -> {
                // If it's real Leanback hardware, safety guard prevents trapping the user without touch controls
                if (isHardwareTv(context)) true else false
            }
            else -> isHardwareTv(context) // AUTO mode
        }
    }
}

/**
 * TV helper for scrollable settings-style pages: D-pad up/down moves focus, and scrolls the page
 * when there's nothing focusable in that direction, so the remote can never get stuck.
 */
fun androidx.compose.ui.Modifier.tvDpadScroll(
    enabled: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
): androidx.compose.ui.Modifier = if (!enabled) this else composed {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    this.onPreviewKeyEvent { e ->
        if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val (dir, delta) = when (e.key) {
            androidx.compose.ui.input.key.Key.DirectionDown -> androidx.compose.ui.focus.FocusDirection.Down to 400f
            androidx.compose.ui.input.key.Key.DirectionUp -> androidx.compose.ui.focus.FocusDirection.Up to -400f
            else -> return@onPreviewKeyEvent false
        }
        if (!focus.moveFocus(dir)) {
            scope.launch { scrollState.animateScrollBy(delta) }
        }
        true
    }
}
