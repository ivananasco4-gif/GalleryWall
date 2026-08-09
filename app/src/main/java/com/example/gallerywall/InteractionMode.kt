package com.example.gallerywall

import android.content.Context

enum class InteractionMode(val label: String) {
    BURBUJA("Burbuja"),
    ELEVACION("Elevación");

    companion object {
        val DEFAULT = BURBUJA

        fun fromName(name: String?): InteractionMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

private const val PREFS_NAME = "gallery_wall_prefs"
private const val KEY_MODE = "interaction_mode"

object InteractionModePrefs {
    fun load(context: Context): InteractionMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return InteractionMode.fromName(prefs.getString(KEY_MODE, null))
    }

    fun save(context: Context, mode: InteractionMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
