package com.andymatt1.radionebulosa.data

import android.os.Bundle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.net.URL
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
    val homepage: String = "",
    val country: String = "",
    @SerialName("countrycode") val countryCode: String = "",
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

    /**
     * Candidatos de logo en orden de preferencia. Radio Browser deja el
     * favicon vacío (o con el texto "null") en muchas emisoras colombianas;
     * en ese caso se deriva un icono del dominio de su página oficial usando
     * el servicio de DuckDuckGo, que no rastrea al usuario. Como último
     * recurso se prueba el host del stream.
     */
    val logoUrls: List<String>
        get() = buildList {
            cleanUrl(favicon)?.let(::add)
            homepageHost()?.let { add(iconServiceUrl(it)) }
            streamHost()?.let { add(iconServiceUrl(it)) }
        }.distinct()

    private fun homepageHost(): String? = cleanUrl(homepage)?.let(::hostOf)

    private fun streamHost(): String? = cleanUrl(streamUrl)?.let(::hostOf)

    private fun iconServiceUrl(host: String): String =
        "https://icons.duckduckgo.com/ip3/$host.ico"

    private fun cleanUrl(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank() || value.equals("null", true) || value.equals("none", true)) {
            return null
        }
        val candidate = when {
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            value.startsWith("//") -> "https:$value"
            else -> "https://$value"
        }
        return runCatching { URL(candidate).toString() }.getOrNull()
    }

    private fun hostOf(url: String): String? =
        runCatching { URL(url).host }
            .getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() && !it.contains(' ') }

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
