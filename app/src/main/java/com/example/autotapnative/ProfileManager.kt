package com.example.autotapnative

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object ProfileManager {

    private const val PREFS_NAME = "profile_prefs"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE_PROFILE = "active_profile_name"

    fun saveProfiles(context: Context, profiles: List<Profile>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            val profilesArray = JSONArray()
            for (profile in profiles) {
                val profileObject = JSONObject()
                profileObject.put("name", profile.name)
                val dotsArray = JSONArray()
                for (dot in profile.dots) {
                    val dotObject = JSONObject()
                    dotObject.put("id", dot.id)
                    dotObject.put("actionIntervalTime", dot.actionIntervalTime)
                    dotObject.put("holdTime", dot.holdTime)
                    dotObject.put("antiDetection", dot.antiDetection)
                    dotObject.put("startDelay", dot.startDelay)
                    dotObject.put("x", dot.x)
                    dotObject.put("y", dot.y)
                    dotsArray.put(dotObject)
                }
                profileObject.put("dots", dotsArray)
                profilesArray.put(profileObject)
            }
            prefs.putString(KEY_PROFILES, profilesArray.toString())
            prefs.commit() // Use commit for synchronous save
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving profiles", e)
        }
    }

    fun loadProfiles(context: Context): MutableList<Profile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILES, null)
        val profiles = mutableListOf<Profile>()

        if (json != null) {
            try {
                val profilesArray = JSONArray(json)
                for (i in 0 until profilesArray.length()) {
                    val profileObject = profilesArray.getJSONObject(i)
                    val name = profileObject.getString("name")
                    val dotsArray = profileObject.getJSONArray("dots")
                    val dots = mutableListOf<Dot>()
                    for (j in 0 until dotsArray.length()) {
                        val dotObject = dotsArray.getJSONObject(j)
                        dots.add(
                            Dot(
                                id = dotObject.getString("id"),
                                actionIntervalTime = dotObject.getLong("actionIntervalTime"),
                                holdTime = dotObject.getLong("holdTime"),
                                antiDetection = dotObject.getDouble("antiDetection").toFloat(),
                                startDelay = dotObject.getLong("startDelay"),
                                x = dotObject.getDouble("x").toFloat(),
                                y = dotObject.getDouble("y").toFloat()
                            )
                        )
                    }
                    profiles.add(Profile(name, dots))
                }
            } catch (e: Exception) {
                Log.e("ProfileManager", "Error loading profiles, clearing old data", e)
                prefs.edit().clear().commit() // Use commit for synchronous save
            }
        }
        return profiles
    }

    fun setActiveProfileName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putString(KEY_ACTIVE_PROFILE, name)
        prefs.commit() // Use commit for synchronous save
    }

    fun getActiveProfileName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_PROFILE, null)
    }
}
