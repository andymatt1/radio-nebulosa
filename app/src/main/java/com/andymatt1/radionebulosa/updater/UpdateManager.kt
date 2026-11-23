package com.andymatt1.radionebulosa.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Consulta `version.json`, descarga el APK con progreso y lanza el instalador
 * del sistema. Tras la instalación, PackageReplacedReceiver reabre la app.
 */
object UpdateManager {

    data class UiState(
        val isChecking: Boolean = false,
        val info: UpdateInfo? = null,
        val updateAvailable: Boolean = false,
        val isDownloading: Boolean = false,
        val progress: Float = 0f,
        val message: String? = null
    )

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/andymatt1/radio-nebulosa/main/version.json"

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdates(context: Context) {
        if (_state.value.isChecking) return
        scope.launch {
            _state.value = UiState(isChecking = true, message = "Consultando actualizaciones…")
            val result = withContext(Dispatchers.IO) { runCatching { fetchInfo() } }

            result.onSuccess { info ->
                val available = isNewer(info.latestVersion, currentVersion(context))
                _state.value = UiState(
                    info = info,
                    updateAvailable = available,
                    message = if (available) {
                        "Nueva versión disponible: ${info.latestVersion}"
                    } else {
                        "Ya tienes la versión más reciente (${currentVersion(context)})."
                    }
                )
            }.onFailure { error ->
                _state.value = UiState(
                    message = "No se pudo consultar: ${error.message ?: "error de red"}"
                )
            }
        }
    }

    fun downloadAndInstall(context: Context) {
        val info = _state.value.info
        if (info == null || info.apkUrl.isBlank() || _state.value.isDownloading) return

        scope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                progress = 0f,
                message = "Descargando Radio Nebulosa ${info.latestVersion}…"
            )

            val result = withContext(Dispatchers.IO) {
                runCatching { downloadApk(context, info.apkUrl) }
            }

            result.onSuccess { file ->
                _state.value = _state.value.copy(
                    isDownloading = false,
                    progress = 1f,
                    message = "Descarga completada. Instalando…"
                )
                installApk(context, file)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isDownloading = false,
                    message = "No se pudo descargar: ${error.message ?: "error de red"}"
                )
            }
        }
    }

    private fun fetchInfo(): UpdateInfo {
        val request = Request.Builder()
            .url(VERSION_URL)
            .header("User-Agent", "RadioNebulosa/1.0 (Android)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("version.json vacío")
            return json.decodeFromString<UpdateInfo>(body)
        }
    }

    private fun downloadApk(context: Context, url: String): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val outputFile = File(directory, "radio-nebulosa-update.apk")
        if (outputFile.exists()) outputFile.delete()

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Respuesta vacía")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            _state.value = _state.value.copy(
                                progress = (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }
        }
        return outputFile
    }

    private fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(settingsIntent)
            _state.value = _state.value.copy(
                message = "Activa \"Instalar aplicaciones desconocidas\" y vuelve a descargar."
            )
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun currentVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"

    /** Comparador semántico simple: 1.10.0 > 1.9.0. */
    private fun isNewer(candidate: String, current: String): Boolean {
        fun parse(version: String): List<Int> = version.trim()
            .removePrefix("v")
            .split(".")
            .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }

        val a = parse(candidate)
        val b = parse(current)
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }
}
