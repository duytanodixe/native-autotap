package com.example.autotapnative

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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var toolbarView: View? = null

    private var allProfiles = mutableListOf<Profile>()
    private var activeProfile: Profile? = null

    private val dotViews = mutableMapOf<String, View>()

    private var isRunning = false
    companion object {
        const val ACTION_HIDE_OVERLAY = "com.example.autotapnative.HIDE_OVERLAY"
        const val ACTION_SHOW_OVERLAY = "com.example.autotapnative.SHOW_OVERLAY"
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        loadAndSetupProfiles()
        createToolbar()
        reloadDotsUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            AutoTapAccessibilityService.instance?.stopAutoTap()
        }
        toolbarView?.let { windowManager.removeView(it) }
        toolbarView = null
        removeAllDotViews()
    }

    private fun loadAndSetupProfiles() {
        allProfiles = ProfileManager.loadProfiles(this)
        if (allProfiles.isEmpty()) {
            allProfiles.add(Profile("Default", mutableListOf()))
            ProfileManager.saveProfiles(this, allProfiles)
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
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,     WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT
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
        val btnChooseProfile = view.findViewById<ImageButton>(R.id.btnChooseProfile)

        btnChooseProfile.setOnClickListener {
            showProfileListOverlay()
        }

        updateProfileSpinner(spinnerProfiles)

        spinnerProfiles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedProfileName = allProfiles.getOrNull(position)?.name ?: return
                if (activeProfile?.name != selectedProfileName) {
                    if (isRunning) { toggleAutoTap(btnPlayPause) }
                    activeProfile = allProfiles[position]
                    ProfileManager.setActiveProfileName(this@OverlayService, selectedProfileName)
                    reloadDotsUI()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlayPause.setOnClickListener { toggleAutoTap(it as ImageButton) }
        btnAddDot.setOnClickListener { addDot() }
        btnRemoveDot.setOnClickListener { removeDot() }
        btnSaveProfile.setOnClickListener { showSaveProfileOverlay() }

        windowManager.addView(view, params)
    }

    private fun updateProfileSpinner(spinner: Spinner) {
        val profileNames = allProfiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profileNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val activeProfileIndex = allProfiles.indexOfFirst { it.name == activeProfile?.name }
        if (activeProfileIndex != -1) {
            spinner.setSelection(activeProfileIndex, false)
        }
    }

    private fun reloadDotsUI() {
        removeAllDotViews()
        activeProfile?.dots?.forEachIndexed { index, dot -> createDotView(dot, index + 1) }
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

        // ⚠️ params.x/y là TOP-LEFT
        params.x = dot.x.toInt()
        params.y = dot.y.toInt()

        view.findViewById<TextView>(R.id.tvDotId).text = displayId.toString()

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

                        // ===== FIX CHÍNH Ở ĐÂY =====
                        // LẤY TÂM DOT (CHUẨN CHO Accessibility Tap)
                        val centerX = params.x + view.width / 2f
                        val centerY = params.y + view.height / 2f

                        dot.x = centerX
                        dot.y = centerY

                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        saveAllProfiles()
                        return true
                    }
                }
                return false
            }
        })

        view.findViewById<ImageButton>(R.id.btnDotSettings).setOnClickListener {

            // 1️⃣ TẠM TẮT OVERLAY
            val hideIntent = Intent(this, OverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            startService(hideIntent)

            // 2️⃣ MỞ DOT SETTINGS
            val intent = Intent(this, DotSettingsActivity::class.java).apply {
                putExtra(DotSettingsActivity.EXTRA_DOT_ID, dot.id)
                putExtra(DotSettingsActivity.EXTRA_PROFILE_NAME, activeProfile?.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }


        windowManager.addView(view, params)
    }


    private fun toggleAutoTap(button: ImageButton) {
        if (AutoTapAccessibilityService.instance == null) {
            Toast.makeText(this, "Dịch vụ trợ năng chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            return
        }

        isRunning = !isRunning

        if (isRunning) {
            button.setImageResource(android.R.drawable.ic_media_pause)
            val dotsToTap = activeProfile?.dots ?: emptyList()
            // START round-robin
            AutoTapAccessibilityService.instance?.startAutoTap(dotsToTap)
            Toast.makeText(this, "Bắt đầu profile: ${activeProfile?.name}", Toast.LENGTH_SHORT).show()
        } else {
            button.setImageResource(android.R.drawable.ic_media_play)
            AutoTapAccessibilityService.instance?.stopAutoTap()
            Toast.makeText(this, "Dừng auto-tap", Toast.LENGTH_SHORT).show()
        }
    }


    private fun addDot() {
        val currentProfile = activeProfile ?: return
        val id = System.currentTimeMillis().toString()
        val newDot = Dot(id = id, actionIntervalTime = 1000, holdTime = 100, antiDetection = 0f, startDelay = 0, x = 300f, y = 300f)
        currentProfile.dots.add(newDot)
        createDotView(newDot, currentProfile.dots.size)
        saveAllProfiles()
    }

    private fun removeDot() {
        val currentProfile = activeProfile ?: return
        if (currentProfile.dots.isNotEmpty()) {
            val dotToRemove = currentProfile.dots.removeLast()
            dotViews.remove(dotToRemove.id)?.let { windowManager.removeView(it) }
            saveAllProfiles()
        }
    }

    private fun saveAllProfiles() {
        ProfileManager.saveProfiles(this, allProfiles)
    }

    private fun showSaveProfileOverlay() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_save_profile, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val etName = view.findViewById<android.widget.EditText>(R.id.etProfileName)
        val btnSave = view.findViewById<android.widget.Button>(R.id.btnSave)
        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener

            // clone dot list
            val newDots = activeProfile?.dots?.map {
                it.copy(id = System.currentTimeMillis().toString() + kotlin.random.Random.nextInt())
            }?.toMutableList() ?: mutableListOf()

            val newProfile = Profile(name, newDots)
            allProfiles.add(newProfile)
            ProfileManager.saveProfiles(this, allProfiles)

            updateProfileSpinner(toolbarView!!.findViewById(R.id.spinnerProfiles))
            windowManager.removeView(view)

            Toast.makeText(this, "Đã lưu profile: $name", Toast.LENGTH_SHORT).show()
        }

        btnCancel.setOnClickListener {
            windowManager.removeView(view)
        }

        windowManager.addView(view, params)
    }

    private fun showProfileListOverlay() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_profile_list, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val listView = view.findViewById<android.widget.ListView>(R.id.listProfiles)
        val names = allProfiles.map { it.name }

        listView.adapter =
            ArrayAdapter(this, R.layout.item_profile, R.id.tvProfileName, names)


        listView.setOnItemClickListener { _, _, position, _ ->
            switchToProfile(allProfiles[position])
            windowManager.removeView(view)
        }

        windowManager.addView(view, params)
    }
    private fun switchToProfile(profile: Profile) {
        if (isRunning) {
            AutoTapAccessibilityService.instance?.stopAutoTap()
            isRunning = false
        }

        removeAllDotViews()

        activeProfile = profile
        ProfileManager.setActiveProfileName(this, profile.name)

        profile.dots.forEachIndexed { index, dot ->
            createDotView(dot, index + 1)
        }

        updateProfileSpinner(toolbarView!!.findViewById(R.id.spinnerProfiles))

        Toast.makeText(this, "Đã chọn profile: ${profile.name}", Toast.LENGTH_SHORT).show()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_SHOW_OVERLAY -> showOverlay()
        }
        return START_STICKY
    }

    private fun hideOverlay() {
        toolbarView?.let {
            windowManager.removeView(it)
            toolbarView = null
        }
        removeAllDotViews()
    }

    private fun showOverlay() {
        if (toolbarView == null) {
            createToolbar()
            reloadDotsUI()
        }
    }


}
