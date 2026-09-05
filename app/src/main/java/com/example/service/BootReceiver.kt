package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.example.overlay.LogType
import com.example.overlay.OverlayEventBus
import com.example.util.PreferenceHelper

/**
 * BroadcastReceiver triggered upon device boot to optionally auto-start
 * the Termux Overlay IPC daemon service if enabled by user in settings.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferenceHelper(context)
            if (prefs.isAutoStartEnabled) {
                if (Settings.canDrawOverlays(context)) {
                    Log.i(TAG, "Auto-starting OverlayBinderService on boot...")
                    OverlayEventBus.addLog(LogType.SERVICE_EVENT, "Auto-starting Overlay service after system boot")
                    OverlayBinderService.start(context)
                } else {
                    Log.w(TAG, "Auto-start skipped: SYSTEM_ALERT_WINDOW permission not granted")
                }
            } else {
                Log.d(TAG, "Auto-start on boot is disabled in user preferences")
            }
        }
    }
}
