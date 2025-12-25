package com.example.autotap

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DotStorage {

    private const val PREF_NAME = "dots_prefs"
    private const val KEY_DOTS = "dots"

    fun loadDots(context: Context): MutableList<Dot> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DOTS, null) ?: return mutableListOf()
        val list = mutableListOf<Dot>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
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
        } catch (_: Exception) {
        }
        return list
    }

    fun saveDots(context: Context, dots: List<Dot>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (d in dots) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("actionIntervalTime", d.actionIntervalTime)
            obj.put("holdTime", d.holdTime)
            obj.put("antiDetection", d.antiDetection.toDouble())
            obj.put("startDelay", d.startDelay)
            obj.put("x", d.x.toDouble())
            obj.put("y", d.y.toDouble())
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DOTS, arr.toString()).apply()
    }
}


