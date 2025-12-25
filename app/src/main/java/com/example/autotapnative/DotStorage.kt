package com.example.autotapnative

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object DotStorage {

    private const val PREFS_NAME = "dot_prefs"
    private const val KEY_DOTS = "dots"

    fun saveDots(context: Context, dots: List<Dot>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            val jsonArray = JSONArray()
            for (dot in dots) {
                val jsonObject = JSONObject()
                jsonObject.put("id", dot.id)
                jsonObject.put("actionIntervalTime", dot.actionIntervalTime)
                jsonObject.put("holdTime", dot.holdTime)
                jsonObject.put("antiDetection", dot.antiDetection)
                jsonObject.put("startDelay", dot.startDelay)
                jsonObject.put("x", dot.x)
                jsonObject.put("y", dot.y)
                jsonArray.put(jsonObject)
            }
            prefs.putString(KEY_DOTS, jsonArray.toString())
            prefs.apply()
        } catch (e: Exception) {
            Log.e("DotStorage", "Error saving dots", e)
        }
    }

    fun loadDots(context: Context): List<Dot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DOTS, null)
        val dots = mutableListOf<Dot>()

        if (json != null) {
            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    dots.add(
                        Dot(
                            id = jsonObject.getString("id"),
                            actionIntervalTime = jsonObject.getLong("actionIntervalTime"),
                            holdTime = jsonObject.getLong("holdTime"),
                            antiDetection = jsonObject.getDouble("antiDetection").toFloat(),
                            startDelay = jsonObject.getLong("startDelay"),
                            x = jsonObject.getDouble("x").toFloat(),
                            y = jsonObject.getDouble("y").toFloat()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DotStorage", "Error loading dots, clearing old data", e)
                // If parsing fails, clear the corrupted data
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            }
        }

        return dots
    }
}
