package com.andymatt1.radionebulosa.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Favoritos persistidos con la emisora completa, no solo su ID.
 * Así una emisora sigue en "Mis favoritos" aunque desaparezca del catálogo
 * temporal de Radio Browser. También se conserva la última emisora escuchada.
 */
class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nebulosa_prefs", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(loadIds())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _favoriteStations = MutableStateFlow(loadStations())
    val favoriteStations: StateFlow<List<Station>> = _favoriteStations.asStateFlow()

    /** Agrega o quita una emisora de favoritos desde cualquier pantalla. */
    fun toggle(station: Station) {
        val ids = _favorites.value.toMutableSet()
        val wasFavorite = !ids.add(station.id)
        if (wasFavorite) ids.remove(station.id)

        val stations = _favoriteStations.value.filter { it.id != station.id }.toMutableList()
        if (!wasFavorite) stations.add(station)

        persist(ids, stations)
    }

    /** Quita un favorito incluso si solo se conoce su ID (datos de versiones antiguas). */
    fun remove(stationId: String) {
        persist(
            _favorites.value - stationId,
            _favoriteStations.value.filter { it.id != stationId }
        )
    }

    fun isFavorite(stationId: String): Boolean = _favorites.value.contains(stationId)

    fun saveLastStation(station: Station) {
        prefs.edit().putString(KEY_LAST_STATION, StationCodec.encode(station)).apply()
    }

    fun loadLastStation(): Station? = StationCodec.decode(prefs.getString(KEY_LAST_STATION, null))

    private fun persist(ids: Set<String>, stations: List<Station>) {
        _favorites.value = ids
        _favoriteStations.value = stations
        prefs.edit()
            .putStringSet(KEY_FAVORITES, ids)
            .putStringSet(KEY_FAVORITE_STATIONS, stations.map(StationCodec::encode).toSet())
            .apply()
    }

    private fun loadIds(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    private fun loadStations(): List<Station> =
        (prefs.getStringSet(KEY_FAVORITE_STATIONS, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { StationCodec.decode(it) }.getOrNull() }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_FAVORITE_STATIONS = "favorite_stations"
        const val KEY_LAST_STATION = "last_station"
    }
}
