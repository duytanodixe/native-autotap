package com.example.autotapnative

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AutoTapAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_SET_DOTS = "com.example.autotapnative.SET_DOTS"
        const val ACTION_START = "com.example.autotapnative.START"
        const val ACTION_STOP = "com.example.autotapnative.STOP"
        const val EXTRA_DOTS = "dots_json"
    }

    private val timers = mutableMapOf<String, Timer>()
    private val dots = mutableListOf<Dot>()

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SET_DOTS -> {
                    val json = intent.getStringExtra(EXTRA_DOTS)
                    if (json != null) {
                        setDotsFromJson(json)
                    }
                }
                ACTION_START -> startAutoTap()
                ACTION_STOP -> stopAutoTap()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AutoTapService", "Service connected")
        val filter = IntentFilter().apply {
            addAction(ACTION_SET_DOTS)
            addAction(ACTION_START)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(controlReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(controlReceiver)
        } catch (_: Exception) {
        }
        stopAutoTap()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    private fun setDotsFromJson(json: String) {
        try {
            val arr = JSONArray(json)
            val newDots = mutableListOf<Dot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                newDots.add(
                    Dot(
                        id = obj.getString("id"),
                        actionIntervalTime = obj.getLong("actionIntervalTime"),
                        holdTime = obj.getLong("holdTime"),
                        antiDetection = obj.getDouble("antiDetection").toFloat(),
                        startDelay = obj.getLong("startDelay"),
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat()
                    )
                )
            }
            setDots(newDots)
        } catch (e: Exception) {
            Log.e("AutoTapService", "Failed to parse dots json", e)
        }
    }

    private fun setDots(newDots: List<Dot>) {
        stopAutoTap()
        dots.clear()
        dots.addAll(newDots)
        Log.d("AutoTapService", "Dots updated: ${dots.size}")
    }

    private fun startAutoTap() {
        stopAutoTap()
        Log.d("AutoTapService", "Starting auto-tap for ${dots.size} dots.")

        for (dot in dots) {
            if (dot.actionIntervalTime <= 0) {
                Log.w("AutoTapService", "Skipping dot ${dot.id} due to invalid interval: ${dot.actionIntervalTime}")
                continue
            }
            val timer = Timer()
            timers[dot.id] = timer
            timer.schedule(object : TimerTask() {
                override fun run() {
                    performAutoTap(dot)
                }
            }, dot.startDelay, dot.actionIntervalTime)
        }
    }

    private fun stopAutoTap() {
        timers.values.forEach { it.cancel() }
        timers.clear()
        Log.d("AutoTapService", "Stopped auto-tap.")
    }

    private fun performAutoTap(dot: Dot) {
        val radius = dot.antiDetection.toDouble()
        var jitterX = 0.0
        var jitterY = 0.0

        if (radius > 0) {
            val t = 2 * Math.PI * Random.nextDouble()
            val r = radius * sqrt(Random.nextDouble())
            jitterX = r * cos(t)
            jitterY = r * sin(t)
        }

        val x = dot.x + jitterX.toFloat()
        val y = dot.y + jitterY.toFloat()

        Log.d("AutoTapService", "Tapping at ($x, $y) for dot ${dot.id}")

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    dot.holdTime
                )
            )
            .build()

        dispatchGesture(gesture, null, null)
    }
}
