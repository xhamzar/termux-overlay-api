package com.example.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("termux_overlay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_DEFAULT_ALPHA = "key_default_alpha"
        private const val KEY_DEFAULT_POS_X = "key_default_pos_x"
        private const val KEY_DEFAULT_POS_Y = "key_default_pos_y"
    }

    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var defaultAlpha: Float
        get() = prefs.getFloat(KEY_DEFAULT_ALPHA, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_ALPHA, value).apply()

    var defaultPosX: Int
        get() = prefs.getInt(KEY_DEFAULT_POS_X, 50)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_POS_X, value).apply()

    var defaultPosY: Int
        get() = prefs.getInt(KEY_DEFAULT_POS_Y, 150)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_POS_Y, value).apply()
}
