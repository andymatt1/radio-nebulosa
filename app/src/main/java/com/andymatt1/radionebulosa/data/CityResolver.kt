package com.andymatt1.radionebulosa.data

import java.util.Locale

/**
 * Radio Browser expone el campo `state` (departamento) y a veces ciudad dentro
 * de `tags`. Este resolvedor muestra una ciudad reconocible para el usuario.
 */
object CityResolver {
    private val knownCities: List<Pair<String, List<String>>> = listOf(
        "Bogotá" to listOf("bogotá", "bogota"),
        "Medellín" to listOf("medellín", "medellin"),
        "Cali" to listOf("cali"),
        "Barranquilla" to listOf("barranquilla"),
        "Bucaramanga" to listOf("bucaramanga"),
        "Cartagena" to listOf("cartagena"),
        "Cúcuta" to listOf("cúcuta", "cucuta"),
        "Pereira" to listOf("pereira"),
        "Manizales" to listOf("manizales"),
        "Armenia" to listOf("armenia"),
        "Ibagué" to listOf("ibagué", "ibague"),
        "Pasto" to listOf("pasto"),
        "Neiva" to listOf("neiva"),
        "Villavicencio" to listOf("villavicencio"),
        "Santa Marta" to listOf("santa marta", "santamarta"),
        "Sincelejo" to listOf("sincelejo"),
        "Popayán" to listOf("popayán", "popayan"),
        "Montería" to listOf("montería", "monteria"),
        "Florencia" to listOf("florencia"),
        "Riohacha" to listOf("riohacha"),
        "Valledupar" to listOf("valledupar")
    )

    fun resolve(name: String, state: String, tags: List<String>): String {
        val text = (listOf(name, state) + tags).joinToString(" ").lowercase(Locale.ROOT)

        val city = knownCities.firstOrNull { (_, keys) ->
            keys.any { key -> text.contains(key) }
        }?.first

        if (city != null) return city

        val cleanState = state.trim()
        return if (cleanState.isNotEmpty()) {
            cleanState.replaceFirstChar { it.uppercaseChar() }
        } else {
            "Colombia"
        }
    }
}
