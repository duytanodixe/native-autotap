package com.example.autotapnative

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import org.json.JSONArray

class OverlayService : Service() {

    companion object {
        private const val ACTION_REFRESH_DOTS = "com.example.autotapnative.REFRESH_DOTS"

        fun newRefreshIntent(context: Context): Intent =
            Intent(ACTION_REFRESH_DOTS).setPackage(context.packageName)

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private var toolbarView: View? = null

    private val dots = mutableListOf<Dot>()
    private val dotViews = mutableMapOf<String, View>()
    private val dotParams = mutableMapOf<String, WindowManager.LayoutParams>()

    private var isRunning = false

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_REFRESH_DOTS) {
                reloadDots()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Load dots từ storage để giống workflow Flutter (loadDots)
        dots.clear()
        dots.addAll(DotStorage.loadDots(this))

        createToolbar()
        createAllDotViews()

        val filter = IntentFilter(ACTION_REFRESH_DOTS)
        registerReceiver(refreshReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {
        }

        toolbarView?.let { windowManager.removeView(it) }
        toolbarView = null

        dotViews.values.forEach { windowManager.removeView(it) }
        dotViews.clear()
        dotParams.clear()
    }

    private fun createToolbar() {
        if (toolbarView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.view_toolbar, null)
        toolbarView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        val btnPlayPause = view.findViewById<ImageButton>(R.id.btnPlayPause)
        val btnAddDot = view.findViewById<ImageButton>(R.id.btnAddDot)

        btnPlayPause.setOnClickListener {
            toggleAutoTap()
        }

        btnAddDot.setOnClickListener {
            addDot()
        }

        windowManager.addView(view, params)
    }

    private fun createAllDotViews() {
        for (d in dots) {
            createDotView(d)
        }
    }

    private fun removeAllDotViews() {
        dotViews.values.forEach { windowManager.removeView(it) }
        dotViews.clear()
        dotParams.clear()
    }

    private fun reloadDots() {
        dots.clear()
        dots.addAll(DotStorage.loadDots(this))
        removeAllDotViews()
        createAllDotViews()
        sendDots()
    }

    private fun createDotView(dot: Dot) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.view_dot, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = dot.x.toInt()
        params.y = dot.y.toInt()

        val tvId = view.findViewById<TextView>(R.id.tvDotId)
        val btnSettings = view.findViewById<ImageButton>(R.id.btnDotSettings)

        tvId.text = dot.id

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var moved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        dot.x = params.x.toFloat()
                        dot.y = params.y.toFloat()
                        moved = true
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            // Lưu lại vị trí mới
                            DotStorage.saveDots(this@OverlayService, dots)
                            sendDots()
                        }
                        return true
                    }
                }
                return false
            }
        })

        btnSettings.setOnClickListener {
            val intent = Intent(this, DotSettingsActivity::class.java).apply {
                putExtra(DotSettingsActivity.EXTRA_DOT_ID, dot.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        windowManager.addView(view, params)
        dotViews[dot.id] = view
        dotParams[dot.id] = params
    }

    private fun toggleAutoTap() {
        isRunning = !isRunning
        val ctx = this
        val startIntent = Intent(AutoTapAccessibilityService.ACTION_START).apply {
            setPackage(ctx.packageName)
        }
        val stopIntent = Intent(AutoTapAccessibilityService.ACTION_STOP).apply {
            setPackage(ctx.packageName)
        }

        // Luôn gửi dots trước
        sendDots()

        if (isRunning) {
            sendBroadcast(startIntent)
        } else {
            sendBroadcast(stopIntent)
        }
    }

    private fun sendDots() {
        val arr = JSONArray()
        for (d in dots) {
            val obj = org.json.JSONObject()
            obj.put("id", d.id)
            obj.put("actionIntervalTime", d.actionIntervalTime)
            obj.put("holdTime", d.holdTime)
            obj.put("antiDetection", d.antiDetection.toDouble())
            obj.put("startDelay", d.startDelay)
            obj.put("x", d.x.toDouble())
            obj.put("y", d.y.toDouble())
            arr.put(obj)
        }

        val intent = Intent(AutoTapAccessibilityService.ACTION_SET_DOTS).apply {
            setPackage(packageName)
            putExtra(AutoTapAccessibilityService.EXTRA_DOTS, arr.toString())
        }
        sendBroadcast(intent)
    }

    private fun addDot() {
        val id = (dots.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0 + 1).toString()
        val newDot = Dot(
            id = id,
            actionIntervalTime = 1000,
            holdTime = 200,
            antiDetection = 0f,
            startDelay = 0,
            x = 500f,
            y = 500f
        )
        dots.add(newDot)
        DotStorage.saveDots(this, dots)
        createDotView(newDot)
        sendDots()
    }
}
