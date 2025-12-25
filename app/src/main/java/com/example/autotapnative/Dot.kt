package com.example.autotap

data class Dot(
    val id: String,
    var actionIntervalTime: Long, // ms
    var holdTime: Long,           // ms
    var antiDetection: Float,     // bán kính jitter (px)
    var startDelay: Long,         // ms
    var x: Float,
    var y: Float
)


