package io.github.aedev.flow.youtubesync.account

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * The kind of device the fork runs on. A TV (Fire TV, Android TV) never links an account, so it never
 * becomes the YouTube Sync Device: it gets subscriptions and playlists through the Device Sync (ADR 0002).
 */
data class ThisDevice(
    val isTelevision: Boolean,
) {
    companion object {
        fun from(context: Context): ThisDevice {
            val uiMode = context.getSystemService(UiModeManager::class.java)?.currentModeType
            val pm = context.packageManager
            return ThisDevice(
                isTelevision =
                    uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
                        pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                        pm.hasSystemFeature("amazon.hardware.fire_tv"),
            )
        }
    }
}
