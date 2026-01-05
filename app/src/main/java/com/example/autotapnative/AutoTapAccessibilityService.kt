package com.example.autotapnative

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AutoTapAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoTapAccessibilityService? = null
    }

    private val handlers = mutableMapOf<String, Handler>()
    private val runnables = mutableMapOf<String, Runnable>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var gestureBusy = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        stopAutoTap()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        stopAutoTap()
    }

    // ===== START =====
    fun startAutoTap(dots: List<Dot>) {
        stopAutoTap()

        dots.forEach { dot ->
            val handler = Handler(Looper.getMainLooper())

            val runnable = object : Runnable {
                override fun run() {
                    if (!gestureBusy) {
                        gestureBusy = true
                        performAutoTap(dot)
                    }
                    handler.postDelayed(
                        this,
                        dot.actionIntervalTime.coerceAtLeast(50)
                    )
                }
            }

            handlers[dot.id] = handler
            runnables[dot.id] = runnable

            // startDelay RIÊNG
            handler.postDelayed(runnable, dot.startDelay)
        }
    }

    // ===== STOP =====
    fun stopAutoTap() {
        handlers.values.forEach {
            it.removeCallbacksAndMessages(null)
        }
        handlers.clear()
        runnables.clear()
        gestureBusy = false
    }

    // ===== TAP =====
    private fun performAutoTap(dot: Dot) {
        val radius = dot.antiDetection.toDouble()
        var dx = 0.0
        var dy = 0.0

        if (radius > 0) {
            val t = Random.nextDouble() * 2 * Math.PI
            val r = radius * sqrt(Random.nextDouble())
            dx = r * cos(t)
            dy = r * sin(t)
        }

        val path = Path().apply {
            moveTo(dot.x + dx.toFloat(), dot.y + dy.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    dot.holdTime.coerceAtLeast(20)
                )
            )
            .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    gestureBusy = false
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureBusy = false
                }
            },
            null
        )
    }
}


