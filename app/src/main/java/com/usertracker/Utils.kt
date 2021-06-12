package com.usertracker

import android.content.Context
import android.preference.PreferenceManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object Utils {
    fun writeStringToPreferences(key: String, value: String, activity: Context?) {
        if (activity == null) {
            return
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val sharedPrefEditor = sharedPreferences.edit()
        sharedPrefEditor.putString(key, value)
        sharedPrefEditor.apply()
    }

    fun getStringFromPreferences(key: String, defaultValue: String, activity: Context?): String? {
        if (activity == null) {
            return defaultValue
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return sharedPreferences.getString(key, defaultValue)
    }

}