package com.andymatt1.radionebulosa.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andymatt1.radionebulosa.R
import com.andymatt1.radionebulosa.ui.theme.Carbon
import com.andymatt1.radionebulosa.ui.theme.CarbonLight
import com.andymatt1.radionebulosa.ui.theme.MutedText
import com.andymatt1.radionebulosa.ui.theme.NeonCyan
import com.andymatt1.radionebulosa.ui.theme.NeonViolet
import com.andymatt1.radionebulosa.ui.theme.SpaceBlack
import com.andymatt1.radionebulosa.updater.UpdateManager

/** Ajustes con versión, verificación y auto-instalación del APK. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val updateState by UpdateManager.state.collectAsState()
    val currentVersion = rememberVersionName()

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Carbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = NeonCyan)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Versión instalada",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedText
                        )
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Actualizaciones de Radio Nebulosa",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Carbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Descarga las mejoras más recientes directamente desde la app. " +
                            "Al terminar se instalará el APK y la app se reabrirá sola.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC6C6D6)
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { UpdateManager.checkForUpdates(context) },
                        enabled = !updateState.isChecking && !updateState.isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (updateState.isChecking) "Buscando…" else "Buscar actualizaciones"
                        )
                    }

                    val info = updateState.info
                    if (info != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Última versión publicada: ${info.latestVersion}",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan
                        )
                        if (info.changelog.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = info.changelog,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFC6C6D6)
                            )
                        }
                    }

                    if (updateState.updateAvailable) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { UpdateManager.downloadAndInstall(context) },
                            enabled = !updateState.isDownloading,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (updateState.isDownloading) {
                                    "Descargando ${(updateState.progress * 100).toInt()}%"
                                } else {
                                    "Descargar e instalar ${info?.latestVersion ?: ""}"
                                }
                            )
                        }
                    }

                    if (updateState.isDownloading) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { updateState.progress },
                            color = NeonViolet,
                            trackColor = CarbonLight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    updateState.message?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Acerca del catálogo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Emisoras obtenidas en tiempo real desde la API abierta de " +
                            "Radio Browser, filtradas estrictamente por Colombia (CO).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC6C6D6)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return androidx.compose.runtime.remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: context.getString(R.string.app_version_placeholder)
    }
}
