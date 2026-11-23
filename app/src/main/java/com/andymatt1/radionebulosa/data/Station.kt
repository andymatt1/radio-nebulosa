package com.andymatt1.radionebulosa.data

import android.os.Bundle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.json.Json

/**
 * Modelo de una emisora. Los nombres de los campos se mapean directamente
 * desde la respuesta de Radio Browser API (`/json/stations/search`).
 */
@Serializable
data class Station(
    @SerialName("stationuuid") val id: String = "",
    val name: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val url: String = "",
    val favicon: String = "",
    val country: String = "",
    val state: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val votes: Int = 0,
    @SerialName("lastcheckok") val lastCheckOk: Int = 1
) {
    /** URL definitiva del stream (Radio Browser la resuelve en `url_resolved`). */
    val streamUrl: String
        get() = urlResolved.ifBlank { url }

    val displayName: String
        get() = name.replace(Regex("\\s+"), " ").trim().ifBlank { "Emisora sin nombre" }

    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }

    /** Ciudad legible para la UI (Radio Browser no siempre trae el campo ciudad). */
    val cityName: String
        get() = CityResolver.resolve(displayName, state, tagList)

    /** Iniciales usadas como respaldo visual cuando no hay logo. */
    val initials: String
        get() = displayName.split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")

    /** Todo el texto por el que se puede buscar una emisora. */
    val searchableText: String
        get() = listOf(displayName, cityName, state, codec, tags, streamUrl.substringBefore("?"))
            .joinToString(" ")
}

/** Búsqueda tolerante a acentos: "bogota" encuentra "Bogotá". */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase(Locale.ROOT)

/** Serialización de la emisora dentro de los metadatos de MediaSession. */
object StationCodec {
    const val EXTRA_KEY = "radio_nebulosa_station"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun encode(station: Station): String = json.encodeToString(station)

    fun decode(value: String?): Station? =
        value?.let { runCatching { json.decodeFromString<Station>(it) }.getOrNull() }

    fun bundleFor(station: Station): Bundle = Bundle().apply {
        putString(EXTRA_KEY, encode(station))
    }
}
