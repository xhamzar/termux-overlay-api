package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.IOverlayService
import com.example.MainActivity
import com.example.overlay.OverlayEventBus
import com.example.overlay.OverlayManager

/**
 * Android Bound & Foreground Service that exposes the AIDL IOverlayService interface
 * for Termux IPC and maintains stable execution in the Android background.
 */
class OverlayBinderService : Service() {

    companion object {
        private const val TAG = "OverlayBinderService"
        const val CHANNEL_ID = "channel_termux_overlay_daemon"
        const val NOTIFICATION_ID = 40401

        // Intent Action commands for CLI shell execution fallback
        const val ACTION_SHOW_TEXT = "com.example.termuxoverlay.SHOW_TEXT"
        const val ACTION_UPDATE_TEXT = "com.example.termuxoverlay.UPDATE_TEXT"
        const val ACTION_SHOW_BUTTON = "com.example.termuxoverlay.SHOW_BUTTON"
        const val ACTION_SHOW_IMAGE = "com.example.termuxoverlay.SHOW_IMAGE"
        const val ACTION_HIDE = "com.example.termuxoverlay.HIDE"
        const val ACTION_SET_ALPHA = "com.example.termuxoverlay.SET_ALPHA"
        const val ACTION_SET_POS = "com.example.termuxoverlay.SET_POS"
        const val ACTION_STOP_SERVICE = "com.example.termuxoverlay.STOP_SERVICE"

        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_ALPHA = "extra_alpha"
        const val EXTRA_POS_X = "extra_pos_x"
        const val EXTRA_POS_Y = "extra_pos_y"

        fun start(context: Context) {
            val intent = Intent(context, OverlayBinderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayBinderService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var overlayManager: OverlayManager

    /**
     * AIDL Binder Stub implementation of IOverlayService
     */
    private val binder = object : IOverlayService.Stub() {
        override fun showText(text: String?) {
            Log.d(TAG, "AIDL showText: $text")
            text?.let { overlayManager.showText(it) }
        }

        override fun hide() {
            Log.d(TAG, "AIDL hide()")
            overlayManager.hide()
        }

        override fun updateText(text: String?) {
            Log.d(TAG, "AIDL updateText: $text")
            text?.let { overlayManager.updateText(it) }
        }

        override fun showButton(label: String?) {
            Log.d(TAG, "AIDL showButton: $label")
            label?.let { overlayManager.showButton(it) }
        }

        override fun showImage(path: String?) {
            Log.d(TAG, "AIDL showImage: $path")
            path?.let { overlayManager.showImage(it) }
        }

        override fun isShowing(): Boolean {
            return overlayManager.isShowing()
        }

        override fun setPosition(x: Int, y: Int) {
            overlayManager.setPosition(x, y)
        }

        override fun setAlpha(alpha: Float) {
            overlayManager.setAlpha(alpha)
        }

        override fun setTextColor(hexColor: String?) {
            hexColor?.let { overlayManager.setTextColor(it) }
        }

        override fun setBackgroundColor(hexColor: String?) {
            hexColor?.let { overlayManager.setBackgroundColor(it) }
        }

        override fun getLastAction(): String {
            return overlayManager.getLastAction()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayBinderService onCreate")
        overlayManager = OverlayManager(applicationContext)
        createNotificationChannel()
        startAsForeground()
        OverlayEventBus.setServiceRunning(true)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Client bound to IOverlayService from: ${intent?.action}")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_SHOW_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Hello from Termux!"
                overlayManager.showText(text)
            }
            ACTION_UPDATE_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                overlayManager.updateText(text)
            }
            ACTION_SHOW_BUTTON -> {
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "ACTION"
                overlayManager.showButton(label)
            }
            ACTION_SHOW_IMAGE -> {
                val path = intent.getStringExtra(EXTRA_PATH) ?: ""
                overlayManager.showImage(path)
            }
            ACTION_HIDE -> {
                overlayManager.hide()
            }
            ACTION_SET_ALPHA -> {
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, 0.95f)
                overlayManager.setAlpha(alpha)
            }
            ACTION_SET_POS -> {
                val x = intent.getIntExtra(EXTRA_POS_X, 50)
                val y = intent.getIntExtra(EXTRA_POS_Y, 150)
                overlayManager.setPosition(x, y)
            }
            ACTION_STOP_SERVICE -> {
                overlayManager.hide()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "OverlayBinderService onDestroy")
        overlayManager.hide()
        OverlayEventBus.setServiceRunning(false)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Termux Overlay Daemon",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Termux Overlay IPC service active in background"
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.w(TAG, "startForeground fallback without type: ${e.message}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = Intent(this, OverlayBinderService::class.java).apply {
            action = ACTION_HIDE
        }
        val hidePendingIntent = PendingIntent.getService(
            this,
            1,
            hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayBinderService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Termux Overlay Service Active")
            .setContentText("Listening for Termux Binder IPC and CLI commands")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setColor(Color.parseColor("#1F6FEB"))
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hide Overlay", hidePendingIntent)
            .addAction(android.R.drawable.ic_lock_power_off, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
