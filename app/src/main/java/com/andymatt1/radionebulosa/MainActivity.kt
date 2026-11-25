package com.andymatt1.radionebulosa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.andymatt1.radionebulosa.ui.home.HomeScreen
import com.andymatt1.radionebulosa.ui.player.PlayerScreen
import com.andymatt1.radionebulosa.ui.settings.SettingsScreen
import com.andymatt1.radionebulosa.ui.theme.RadioNebulosaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* El usuario decide */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            RadioNebulosaTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

                BackHandler(enabled = screen == Screen.PLAYER || screen == Screen.SETTINGS) {
                    screen = Screen.HOME
                }

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onOpenPlayer = { screen = Screen.PLAYER },
                        onSettings = { screen = Screen.SETTINGS }
                    )
                    Screen.PLAYER -> PlayerScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Media3 puede reconectar al servicio mientras la app estaba en segundo
        // plano; sincronizamos el estado real para que el botón de play no quede
        // desactualizado tras pausar desde la notificación.
        viewModel.syncPlayerState()
    }

    private enum class Screen { HOME, PLAYER, SETTINGS }
}
