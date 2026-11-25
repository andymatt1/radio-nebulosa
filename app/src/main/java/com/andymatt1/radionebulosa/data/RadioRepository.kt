package com.andymatt1.radionebulosa.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente de Radio Browser API. Sólo pide emisoras de Colombia
 * (`countrycode=CO`) y prueba varios espejos si alguno falla.
 */
object RadioRepository {
    private val mirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    private const val USER_AGENT = "RadioNebulosa/1.4.1 (Android)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun fetchColombianStations(): List<Station> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        for (mirror in mirrors) {
            // Equivalente a URLSearchParams: construye la query sin espacios
            // ni saltos de línea y codifica cualquier valor automáticamente.
            val url = mirror.toHttpUrlOrNull()?.newBuilder()
                ?.addPathSegments("json/stations/search")
                ?.addQueryParameter("countrycode", "CO")
                ?.addQueryParameter("hidebroken", "true")
                ?.addQueryParameter("order", "votes")
                ?.addQueryParameter("reverse", "true")
                ?.addQueryParameter("limit", "2000")
                ?.build()
                ?: continue

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} en $mirror")
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) throw IOException("Respuesta vacía en $mirror")

                    val stations = json.decodeFromString<List<Station>>(body)
                        .filter { it.streamUrl.isNotBlank() }
                        .filter { it.countryCode.equals("CO", ignoreCase = true) }
                        .distinctBy { it.displayName.lowercase() + it.streamUrl }
                        .sortedByDescending { it.votes }

                    return@withContext stations
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: IOException("No se pudo conectar con Radio Browser")
    }
}
