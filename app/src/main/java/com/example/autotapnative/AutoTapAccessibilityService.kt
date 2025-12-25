package com.example.autotapnative

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.Timer
import java.util.TimerTask
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AutoTapAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoTapAccessibilityService? = null
    }

    private val timers = mutableMapOf<String, Timer>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("AutoTapService", "Service connected and instance set.")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoTap()
        instance = null
        Log.d("AutoTapService", "Service destroyed and instance cleared.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopAutoTap()
    }

    fun startAutoTap(dots: List<Dot>) {
        stopAutoTap() 
        Log.d("AutoTapService", "Starting auto-tap for ${dots.size} dots.")

        if (dots.isEmpty()) {
            Log.w("AutoTapService", "Dot list is empty. Nothing to tap.")
            return
        }

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

    fun stopAutoTap() {
        if (timers.isEmpty()) return
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
