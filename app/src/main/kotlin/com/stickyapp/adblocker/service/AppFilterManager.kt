package com.stickyapp.adblocker.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class AppFilterManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_filter",
        Context.MODE_PRIVATE
    )

    fun getSelectedApps(): Set<String> {
        return prefs.getStringSet("selected_apps", setOf()) ?: setOf()
    }

    fun addSelectedApp(packageName: String) {
        val apps = getSelectedApps().toMutableSet()
        apps.add(packageName)
        saveSelectedApps(apps)
    }

    fun removeSelectedApp(packageName: String) {
        val apps = getSelectedApps().toMutableSet()
        apps.remove(packageName)
        saveSelectedApps(apps)
    }

    fun isAppSelected(packageName: String): Boolean {
        return getSelectedApps().contains(packageName)
    }

    fun toggleApp(packageName: String) {
        if (isAppSelected(packageName)) {
            removeSelectedApp(packageName)
        } else {
            addSelectedApp(packageName)
        }
    }

    fun clearAllApps() {
        prefs.edit().remove("selected_apps").apply()
    }

    private fun saveSelectedApps(apps: Set<String>) {
        prefs.edit().putStringSet("selected_apps", apps).apply()
    }
}
