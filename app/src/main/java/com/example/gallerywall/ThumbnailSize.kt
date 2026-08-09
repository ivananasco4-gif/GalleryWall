package com.example.gallerywall

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tamaños de miniatura disponibles para la malla.
 * "micro" reproduce la densidad del video de referencia; "mediano" es el
 * tamaño usado en la demo web de muestra.
 */
enum class ThumbnailSize(val label: String, val shortLabel: String, val cellDp: Dp) {
    MICRO("Micro", "MC", 10.dp),
    NANO("Nano", "N", 16.dp),
    PEQUENO("Pequeño", "P", 24.dp),
    MEDIANO("Mediano", "M", 34.dp),
    GRANDE("Grande", "G", 50.dp);

    companion object {
        val DEFAULT = MEDIANO

        fun fromName(name: String?): ThumbnailSize =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

private const val PREFS_NAME = "gallery_wall_prefs"
private const val KEY_THUMBNAIL_SIZE = "thumbnail_size"
private const val KEY_AUTO_MOVE = "auto_move_enabled"

object ThumbnailSizePrefs {
    fun load(context: Context): ThumbnailSize {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ThumbnailSize.fromName(prefs.getString(KEY_THUMBNAIL_SIZE, null))
    }

    fun save(context: Context, size: ThumbnailSize) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THUMBNAIL_SIZE, size.name)
            .apply()
    }
}

/** Recuerda si el usuario quiere que el lente derive solo cuando no lo toca. */
object AutoMovePrefs {
    fun load(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_MOVE, false) // apagado por defecto
    }

    fun save(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_MOVE, enabled)
            .apply()
    }
}
