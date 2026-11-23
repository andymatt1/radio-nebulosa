package com.andymatt1.radionebulosa.data

/** Filtros rápidos estilo "Sección Radio" de Claro Música. */
enum class Category(val label: String, private val keywords: List<String>) {
    ALL("Todas", emptyList()),
    VALLENATO("Vallenato", listOf("vallenato", "cumbia", "merengue")),
    SALSA("Salsa", listOf("salsa", "son cubano", "mambo")),
    URBANA("Urbana / Reggaetón", listOf("reggaeton", "reggaetón", "urbana", "trap", "hip hop", "hiphop", "dembow")),
    POPULAR("Popular / Despecho", listOf("popular", "despecho", "ranchera", "balada", "música popular", "musica popular")),
    TROPICAL("Tropical", listOf("tropical", "bachata", "son", "merengue")),
    POP_ROCK("Pop & Rock", listOf("pop", "rock", "alternativa", "indie", "metal")),
    NOTICIAS("Noticias & Deportes", listOf("news", "noticias", "deportes", "sports", "talk", "información")),
    CRISTIANA("Cristiana", listOf("cristiana", "religiosa", "gospel", "espiritual"));

    fun matches(station: Station): Boolean {
        if (this == ALL) return true
        val text = (station.tagList + station.displayName).joinToString(" ").normalizedForSearch()
        return keywords.any { text.contains(it) }
    }
}
