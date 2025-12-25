package com.example.autotapnative

data class Dot(
    val id: String,
    var actionIntervalTime: Long,
    var holdTime: Long,
    var antiDetection: Float,
    var startDelay: Long,
    var x: Float,
    var y: Float
)
