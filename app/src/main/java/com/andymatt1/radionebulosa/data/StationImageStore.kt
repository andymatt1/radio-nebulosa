package com.andymatt1.radionebulosa.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fotos elegidas por el usuario desde su dispositivo, una por emisora.
 * Se guarda la URI con permiso persistente para que sobreviva a reinicios.
 */
class StationImageStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("station_images", Context.MODE_PRIVATE)

    private val _images = MutableStateFlow(loadImages())
    val images: StateFlow<Map<String, String>> = _images.asStateFlow()

    fun uriFor(stationId: String): String? = _images.value[stationId]

    fun save(stationId: String, uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Algunos proveedores no conceden permiso persistente; la URI
            // sigue siendo válida mientras dure la sesión.
        }

        val updated = _images.value + (stationId to uri.toString())
        _images.value = updated
        prefs.edit().putStringSet(KEY, updated.encode()).apply()
    }

    fun remove(stationId: String) {
        val updated = _images.value - stationId
        _images.value = updated
        prefs.edit().putStringSet(KEY, updated.encode()).apply()
    }

    private fun loadImages(): Map<String, String> =
        (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).decode()

    private fun Map<String, String>.encode(): Set<String> =
        map { "${it.key}|${it.value}" }.toSet()

    private fun Set<String>.decode(): Map<String, String> =
        mapNotNull { entry ->
            val separator = entry.indexOf('|')
            if (separator > 0) {
                entry.substring(0, separator) to entry.substring(separator + 1)
            } else {
                null
            }
        }.toMap()

    private companion object {
        const val KEY = "images"
    }
}
