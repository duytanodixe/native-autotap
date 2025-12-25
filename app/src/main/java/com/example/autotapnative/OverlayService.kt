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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray

class OverlayService : Service() {

    companion object {
        private const val ACTION_REFRESH_ALL = "com.example.autotapnative.REFRESH_ALL"

        fun newRefreshIntent(context: Context): Intent =
            Intent(ACTION_REFRESH_ALL).setPackage(context.packageName)

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

    private var allProfiles = mutableListOf<Profile>()
    private var activeProfile: Profile? = null

    private val dotViews = mutableMapOf<String, View>()

    private var isRunning = false

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_REFRESH_ALL) {
                loadAndSetupProfiles()
                (toolbarView?.findViewById<Spinner>(R.id.spinnerProfiles))?.let {
                    updateProfileSpinner(it)
                }
                reloadDotsUI()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        loadAndSetupProfiles()
        createToolbar()
        reloadDotsUI()

        val filter = IntentFilter(ACTION_REFRESH_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(refreshReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {
        }

        toolbarView?.let { windowManager.removeView(it) }
        toolbarView = null

        removeAllDotViews()
    }

    private fun loadAndSetupProfiles() {
        allProfiles = ProfileManager.loadProfiles(this)
        if (allProfiles.isEmpty()) {
            allProfiles.add(Profile("Default", mutableListOf()))
        }

        val activeProfileName = ProfileManager.getActiveProfileName(this)
        activeProfile = allProfiles.find { it.name == activeProfileName } ?: allProfiles.first()
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
        val btnRemoveDot = view.findViewById<ImageButton>(R.id.btnRemoveDot)
        val btnSaveProfile = view.findViewById<ImageButton>(R.id.btnSaveProfile)
        val spinnerProfiles = view.findViewById<Spinner>(R.id.spinnerProfiles)

        updateProfileSpinner(spinnerProfiles)

        spinnerProfiles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedProfileName = allProfiles[position].name
                if (activeProfile?.name != selectedProfileName) {
                    activeProfile = allProfiles[position]
                    ProfileManager.setActiveProfileName(this@OverlayService, selectedProfileName)
                    reloadDotsUI()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlayPause.setOnClickListener {
            toggleAutoTap(it as ImageButton)
        }

        btnAddDot.setOnClickListener {
            addDot()
        }

        btnRemoveDot.setOnClickListener {
            removeDot()
        }

        btnSaveProfile.setOnClickListener {
            ProfileManager.saveProfiles(this, allProfiles)
            Toast.makeText(this, "Đã lưu tất cả profile!", Toast.LENGTH_SHORT).show()
        }

        windowManager.addView(view, params)
    }

    private fun updateProfileSpinner(spinner: Spinner) {
        val profileNames = allProfiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profileNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val activeProfileIndex = allProfiles.indexOfFirst { it.name == activeProfile?.name }
        if (activeProfileIndex != -1) {
            spinner.setSelection(activeProfileIndex)
        }
    }

    private fun reloadDotsUI() {
        removeAllDotViews()
        activeProfile?.dots?.forEachIndexed { index, dot -> createDotView(dot, index + 1) }
        sendDotsToAccessibilityService()
    }

    private fun removeAllDotViews() {
        dotViews.values.forEach { windowManager.removeView(it) }
        dotViews.clear()
    }

    private fun createDotView(dot: Dot, displayId: Int) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.view_dot, null)
        dotViews[dot.id] = view

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
        tvId.text = displayId.toString()

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
                        dot.x = params.x.toFloat()
                        dot.y = params.y.toFloat()
                        return true
                    }
                    MotionEvent.ACTION_UP -> { return true }
                }
                return false
            }
        })

        view.findViewById<ImageButton>(R.id.btnDotSettings).setOnClickListener {
            val intent = Intent(this, DotSettingsActivity::class.java).apply {
                putExtra(DotSettingsActivity.EXTRA_DOT_ID, dot.id)
                putExtra("extra_profile_name", activeProfile?.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        windowManager.addView(view, params)
    }

    private fun toggleAutoTap(button: ImageButton) {
        isRunning = !isRunning
        sendDotsToAccessibilityService()

        if (isRunning) {
            button.setImageResource(android.R.drawable.ic_media_pause)
            sendBroadcast(Intent(AutoTapAccessibilityService.ACTION_START).setPackage(packageName))
            Toast.makeText(this, "Bắt đầu profile: ${activeProfile?.name}", Toast.LENGTH_SHORT).show()
        } else {
            button.setImageResource(android.R.drawable.ic_media_play)
            sendBroadcast(Intent(AutoTapAccessibilityService.ACTION_STOP).setPackage(packageName))
            Toast.makeText(this, "Dừng auto-tap", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDotsToAccessibilityService() {
        val dotsToSend = activeProfile?.dots ?: return
        val arr = JSONArray()
        for (d in dotsToSend) {
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
        val currentProfile = activeProfile ?: return
        val id = (System.currentTimeMillis() % 100000).toString() // More unique ID
        val newDot = Dot(
            id = id,
            actionIntervalTime = 1000,
            holdTime = 200,
            antiDetection = 0f,
            startDelay = 0,
            x = 500f,
            y = 500f
        )
        currentProfile.dots.add(newDot)
        createDotView(newDot, currentProfile.dots.size)
    }

    private fun removeDot() {
        val currentProfile = activeProfile ?: return
        if (currentProfile.dots.isNotEmpty()) {
            val dotToRemove = currentProfile.dots.removeLast()
            dotViews.remove(dotToRemove.id)?.let { windowManager.removeView(it) }
        }
    }
}
