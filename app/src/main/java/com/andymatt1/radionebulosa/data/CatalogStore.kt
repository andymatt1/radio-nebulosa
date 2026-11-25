package com.andymatt1.radionebulosa.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Catálogo persistido en el dispositivo. La app abre mostrando este caché y
 * solo consulta Radio Browser cuando ya expiró; así el arranque deja de
 * depender de la red.
 */
@Serializable
data class CachedCatalog(
    val fetchedAt: Long,
    val stations: List<Station>
)

class CatalogStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("nebulosa_catalog", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun load(): CachedCatalog? {
        val raw = prefs.getString(KEY_CATALOG, null) ?: return null
        return runCatching { json.decodeFromString<CachedCatalog>(raw) }
            .getOrNull()
            ?.takeIf { it.stations.isNotEmpty() }
    }

    fun isFresh(cached: CachedCatalog): Boolean =
        System.currentTimeMillis() - cached.fetchedAt < FRESH_FOR_MS

    fun save(stations: List<Station>) {
        if (stations.isEmpty()) return
        val payload = json.encodeToString(
            CachedCatalog(fetchedAt = System.currentTimeMillis(), stations = stations)
        )
        prefs.edit().putString(KEY_CATALOG, payload).apply()
    }

    private companion object {
        const val KEY_CATALOG = "catalog"
        const val FRESH_FOR_MS = 10 * 60 * 1000L
    }
}
