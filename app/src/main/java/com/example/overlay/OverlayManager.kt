package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * OverlayManager responsible for managing the floating window on top of Android system.
 * Uses WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY with smooth dragging physics,
 * dynamic sizing, transparency control, and support for text, button, and image layouts.
 */
class OverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "OverlayManager"
        const val ACTION_OVERLAY_BUTTON_CLICKED = "com.example.termuxoverlay.ACTION_BUTTON_CLICKED"
        const val EXTRA_BUTTON_LABEL = "extra_button_label"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? =
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var rootView: View? = null
    private var contentContainer: LinearLayout? = null
    private var headerTitle: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    // Window properties
    private var currentAlpha = 0.95f
    private var currentX = 50
    private var currentY = 150
    private var customTextColor: Int? = null
    private var customBgColor: Int? = null
    private var lastActionString: String = ""

    private var currentContentMode = ContentMode.NONE
    private var activeTextView: TextView? = null

    enum class ContentMode {
        NONE, TEXT, BUTTON, IMAGE
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW permission is granted.
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Display floating text overlay.
     */
    fun showText(text: String) {
        postOnMain {
            if (!ensureOverlayFrameCreated()) return@postOnMain
            currentContentMode = ContentMode.TEXT
            headerTitle?.text = "TERMINAL OUTPUT"

            contentContainer?.removeAllViews()

            val scrollView = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val maxHeight = dpToPx(350)
                    // limit scrollview height
                }
                isVerticalScrollBarEnabled = true
            }

            val textView = TextView(context).apply {
                this.text = text
                setTextColor(customTextColor ?: Color.parseColor("#E6EDF3"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.MONOSPACE
                setLineSpacing(dpToPx(4).toFloat(), 1.0f)
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(12))
                setTextIsSelectable(true)
            }
            activeTextView = textView
            scrollView.addView(textView)
            contentContainer?.addView(scrollView)

            OverlayEventBus.setCurrentText(text)
            OverlayEventBus.addLog(LogType.IPC_SHOW_TEXT, "Show text: \"${text.take(40)}\"")
        }
    }

    /**
     * Update text in the currently visible overlay.
     */
    fun updateText(text: String) {
        postOnMain {
            if (rootView == null || currentContentMode != ContentMode.TEXT || activeTextView == null) {
                // If not currently showing text, create it
                showText(text)
                return@postOnMain
            }
            activeTextView?.text = text
            OverlayEventBus.setCurrentText(text)
            OverlayEventBus.addLog(LogType.IPC_UPDATE_TEXT, "Update text: \"${text.take(40)}\"")
        }
    }

    /**
     * Display a floating actionable button.
     */
    fun showButton(label: String) {
        postOnMain {
            if (!ensureOverlayFrameCreated()) return@postOnMain
            currentContentMode = ContentMode.BUTTON
            headerTitle?.text = "ACTION REQUIRED"

            contentContainer?.removeAllViews()

            val buttonContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(16))
            }

            val actionButton = FrameLayout(context).apply {
                minimumHeight = dpToPx(48)
                minimumWidth = dpToPx(160)

                val btnBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(Color.parseColor("#1F6FEB"))
                }
                val rippleColor = ColorStateList.valueOf(Color.parseColor("#58A6FF"))
                background = RippleDrawable(rippleColor, btnBg, null)

                isClickable = true
                isFocusable = true

                val btnText = TextView(context).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12))
                }

                addView(btnText)

                setOnClickListener {
                    triggerButtonAction(label)
                }
            }

            buttonContainer.addView(actionButton)
            contentContainer?.addView(buttonContainer)

            OverlayEventBus.addLog(LogType.IPC_SHOW_BUTTON, "Show button: [$label]")
        }
    }

    /**
     * Display an image from local storage path.
     */
    fun showImage(path: String) {
        postOnMain {
            if (!ensureOverlayFrameCreated()) return@postOnMain
            currentContentMode = ContentMode.IMAGE
            headerTitle?.text = "IMAGE VIEWER"

            contentContainer?.removeAllViews()

            val file = File(path)
            if (!file.exists() || !file.canRead()) {
                val errorText = TextView(context).apply {
                    text = "File not found or unreadable:\n$path"
                    setTextColor(Color.parseColor("#F85149"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.MONOSPACE
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(16))
                }
                contentContainer?.addView(errorText)
                OverlayEventBus.addLog(LogType.ERROR, "Image not found at path: $path")
                return@postOnMain
            }

            val bitmap = decodeSampledBitmap(path, dpToPx(320), dpToPx(320))
            if (bitmap == null) {
                val errorText = TextView(context).apply {
                    text = "Failed to decode image file:\n$path"
                    setTextColor(Color.parseColor("#F85149"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(16))
                }
                contentContainer?.addView(errorText)
                OverlayEventBus.addLog(LogType.ERROR, "Bitmap decoding failed: $path")
                return@postOnMain
            }

            val imageView = ImageView(context).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val maxW = dpToPx(300)
                    val maxH = dpToPx(300)
                    setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                }
            }

            contentContainer?.addView(imageView)
            OverlayEventBus.addLog(LogType.IPC_SHOW_IMAGE, "Show image from: $path")
        }
    }

    /**
     * Hide and remove floating overlay.
     */
    fun hide() {
        postOnMain {
            if (rootView != null) {
                try {
                    windowManager.removeView(rootView)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing overlay view: ${e.message}")
                }
                rootView = null
                contentContainer = null
                headerTitle = null
                activeTextView = null
                currentContentMode = ContentMode.NONE
                OverlayEventBus.setOverlayVisible(false)
                OverlayEventBus.addLog(LogType.IPC_HIDE, "Overlay window hidden")
            }
        }
    }

    /**
     * Returns true if overlay is currently added to WindowManager.
     */
    fun isShowing(): Boolean {
        return rootView != null
    }

    /**
     * Set window position offsets.
     */
    fun setPosition(x: Int, y: Int) {
        postOnMain {
            currentX = x
            currentY = y
            params?.let { p ->
                p.x = x
                p.y = y
                if (rootView != null) {
                    try {
                        windowManager.updateViewLayout(rootView, p)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update position: ${e.message}")
                    }
                }
            }
            OverlayEventBus.addLog(LogType.IPC_CONFIG, "Set position: ($x, $y)")
        }
    }

    /**
     * Set alpha transparency (0.1 to 1.0).
     */
    fun setAlpha(alpha: Float) {
        postOnMain {
            val clamped = alpha.coerceIn(0.1f, 1.0f)
            currentAlpha = clamped
            params?.let { p ->
                p.alpha = clamped
                if (rootView != null) {
                    try {
                        windowManager.updateViewLayout(rootView, p)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update alpha: ${e.message}")
                    }
                }
            }
            OverlayEventBus.addLog(LogType.IPC_CONFIG, "Set alpha: $clamped")
        }
    }

    fun setTextColor(hexColor: String) {
        postOnMain {
            try {
                val parsed = Color.parseColor(hexColor)
                customTextColor = parsed
                activeTextView?.setTextColor(parsed)
                OverlayEventBus.addLog(LogType.IPC_CONFIG, "Set text color: $hexColor")
            } catch (e: Exception) {
                Log.e(TAG, "Invalid text color: $hexColor")
            }
        }
    }

    fun setBackgroundColor(hexColor: String) {
        postOnMain {
            try {
                val parsed = Color.parseColor(hexColor)
                customBgColor = parsed
                rootView?.let { view ->
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dpToPx(16).toFloat()
                        setColor(parsed)
                        setStroke(dpToPx(1), Color.parseColor("#30363D"))
                    }
                    view.background = bg
                }
                OverlayEventBus.addLog(LogType.IPC_CONFIG, "Set background color: $hexColor")
            } catch (e: Exception) {
                Log.e(TAG, "Invalid bg color: $hexColor")
            }
        }
    }

    fun getLastAction(): String {
        return lastActionString
    }

    // -------------------------------------------------------------
    // Internal Setup & Dragging Physics
    // -------------------------------------------------------------

    private fun ensureOverlayFrameCreated(): Boolean {
        if (!canDrawOverlays()) {
            val msg = "SYSTEM_ALERT_WINDOW permission missing. Please grant overlay permission in settings."
            Log.e(TAG, msg)
            OverlayEventBus.addLog(LogType.ERROR, msg)
            return false
        }

        if (rootView != null) {
            return true
        }

        try {
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = currentX
                y = currentY
                alpha = currentAlpha
            }
            this.params = layoutParams

            val root = createOverlayContainer()
            this.rootView = root

            windowManager.addView(root, layoutParams)
            OverlayEventBus.setOverlayVisible(true)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay to WindowManager: ${e.message}", e)
            OverlayEventBus.addLog(LogType.ERROR, "WindowManager addView error: ${e.message}")
            return false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayContainer(): View {
        // Main container card
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(customBgColor ?: Color.parseColor("#E60D1117"))
                setStroke(dpToPx(1), Color.parseColor("#30363D"))
            }
            background = bg
            elevation = dpToPx(10).toFloat()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minimumWidth = dpToPx(220)
        }

        // Header Title & Drag Bar
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val headerBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dpToPx(16).toFloat(), dpToPx(16).toFloat(),
                    dpToPx(16).toFloat(), dpToPx(16).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#161B22"))
            }
            background = headerBg
            setPadding(dpToPx(12), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        // Terminal Status Indicator Dot (Green)
        val statusDot = View(context).apply {
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3FB950"))
            }
            background = dotDrawable
            layoutParams = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                marginEnd = dpToPx(8)
            }
        }
        header.addView(statusDot)

        // Title text
        val title = TextView(context).apply {
            text = "TERMUX OVERLAY"
            setTextColor(Color.parseColor("#8B949E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerTitle = title
        header.addView(title)

        // Close Button (Small X button with 48dp minimum touch target area)
        val closeFrame = FrameLayout(context).apply {
            minimumWidth = dpToPx(36)
            minimumHeight = dpToPx(36)
            val closeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#21262D"))
            }
            background = closeBg
            isClickable = true
            isFocusable = true

            val closeText = TextView(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#8B949E"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(closeText)

            setOnClickListener {
                hide()
            }
        }
        header.addView(closeFrame)

        // Attach Dragging Physics to Header and Container
        setupDraggableTouchListener(header)

        container.addView(header)

        // Dynamic Content Body
        val contentBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        this.contentContainer = contentBody
        container.addView(contentBody)

        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableTouchListener(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var startX = 0
            private var startY = 0
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        startX = p.x
                        startY = p.y
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (!isDragging && (abs(dx) > dpToPx(4) || abs(dy) > dpToPx(4))) {
                            isDragging = true
                        }
                        if (isDragging) {
                            p.x = max(0, startX + dx.toInt())
                            p.y = max(0, startY + dy.toInt())
                            currentX = p.x
                            currentY = p.y
                            try {
                                if (rootView != null) {
                                    windowManager.updateViewLayout(rootView, p)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Touch drag update failed: ${e.message}")
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            return true
                        }
                        return false
                    }
                }
                return false
            }
        })
    }

    private fun triggerButtonAction(label: String) {
        lastActionString = label
        OverlayEventBus.setLastAction(label)
        OverlayEventBus.addLog(LogType.USER_CLICK, "Button clicked: \"$label\"")

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}

        // Send broadcast back to Termux / system listeners
        val intent = Intent(ACTION_OVERLAY_BUTTON_CLICKED).apply {
            putExtra(EXTRA_BUTTON_LABEL, label)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            // Set package to allow reception
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    private fun postOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding file at $path", e)
            null
        }
    }
}
