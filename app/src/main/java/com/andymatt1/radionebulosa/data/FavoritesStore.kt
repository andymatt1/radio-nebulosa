package com.andymatt1.radionebulosa.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencias ligeras: favoritos y última emisora escuchada. */
class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nebulosa_prefs", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggle(stationId: String) {
        val current = _favorites.value.toMutableSet()
        if (!current.add(stationId)) current.remove(stationId)
        _favorites.value = current
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun isFavorite(stationId: String): Boolean = _favorites.value.contains(stationId)

    fun saveLastStation(station: Station) {
        prefs.edit().putString(KEY_LAST_STATION, StationCodec.encode(station)).apply()
    }

    fun loadLastStation(): Station? = StationCodec.decode(prefs.getString(KEY_LAST_STATION, null))

    private fun loadFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_LAST_STATION = "last_station"
    }
}
