package com.andymatt1.radionebulosa.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.andymatt1.radionebulosa.MainActivity
import com.andymatt1.radionebulosa.R
import com.andymatt1.radionebulosa.RadioNebulosaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servicio foreground que hospeda ExoPlayer y MediaSession.
 * Gracias a MediaSessionService la notificación, la pantalla de bloqueo y
 * los controles externos funcionan sin código adicional.
 */
@UnstableApi
class RadioPlayerService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var retryJob: Job? = null
    private var retryAttempt = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        // El proveedor debe configurarse antes de super.onCreate() para que
        // MediaSessionService lo use al crear la notificación.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(RadioNebulosaApp.CHANNEL_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_stat_radio)
        setMediaNotificationProvider(notificationProvider)
        super.onCreate()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("RadioNebulosa/1.0 (Android)")

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) retryAttempt = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                // Reintento automático: los streams de radio suelen fallar al conectar.
                player?.currentMediaItem?.let { scheduleRetry(it) }
            }
        })

        player = exoPlayer

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(openApp)
            .build()
    }

    private fun scheduleRetry(item: MediaItem) {
        if (retryAttempt >= MAX_RETRIES) return
        retryJob?.cancel()
        retryAttempt++
        retryJob = scope.launch {
            delay(1_000L * retryAttempt)
            player?.let { p ->
                p.setMediaItem(item)
                p.prepare()
                p.play()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        retryJob?.cancel()
        scope.cancel()
        player?.release()
        session?.release()
        session = null
        player = null
        super.onDestroy()
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
