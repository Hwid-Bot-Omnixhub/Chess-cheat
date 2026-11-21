package com.fracture.chessassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var iconView: View
    private lateinit var panelView: View
    private lateinit var iconParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private var isPanelVisible = false
    private var assistantEnabled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        buildIcon()
        buildPanel()
    }

    private fun startForegroundNotification() {
        val channelId = "chess_assistant"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Chess Assistant", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Chess Assistant")
            .setContentText("Floating assistant running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, notification)
    }

    private fun buildIcon() {
        iconView = TextView(this).apply {
            text = "♞"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0xDD1A1A1A.toInt())
            width = dp(52)
            height = dp(52)
        }

        iconParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        iconView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = iconParams.x
                    initialY = iconParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) isDragging = true
                    if (isDragging) {
                        iconParams.x = initialX + dx.toInt()
                        iconParams.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(iconView, iconParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) togglePanel()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(iconView, iconParams)
    }

    private fun buildPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE1A1A1A.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val title = TextView(this).apply {
            text = "Chess Assistant"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(0, 0, 0, dp(8))
        }
        panel.addView(title)

        val toggle = Switch(this).apply {
            text = "Enable analysis"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = assistantEnabled
            setOnCheckedChangeListener { _, checked ->
                assistantEnabled = checked
                // TODO: connect this boolean to your Stockfish / vision pipeline
            }
        }
        panel.addView(toggle)

        val minimizeBtn = Button(this).apply {
            text = "Minimize"
            setOnClickListener { togglePanel() }
        }
        panel.addView(minimizeBtn)

        panelView = panel

        panelParams = WindowManager.LayoutParams(
            dp(220),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 380
        }
    }

    private fun togglePanel() {
        if (isPanelVisible) {
            windowManager.removeView(panelView)
            isPanelVisible = false
            iconParams.y = panelParams.y
            windowManager.updateViewLayout(iconView, iconParams)
        } else {
            panelParams.x = iconParams.x
            panelParams.y = iconParams.y + dp(60)
            windowManager.addView(panelView, panelParams)
            isPanelVisible = true
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        if (isPanelVisible) windowManager.removeView(panelView)
        windowManager.removeView(iconView)
    }
}
