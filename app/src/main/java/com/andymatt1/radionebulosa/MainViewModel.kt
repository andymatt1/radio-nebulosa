package com.andymatt1.radionebulosa

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andymatt1.radionebulosa.data.FavoritesStore
import com.andymatt1.radionebulosa.data.RadioRepository
import com.andymatt1.radionebulosa.data.Station
import com.andymatt1.radionebulosa.data.StationCodec
import com.andymatt1.radionebulosa.player.RadioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado compartido entre el catálogo, el mini-reproductor y la vista completa. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext
    val favorites = FavoritesStore(context)
    private val repository = RadioRepository

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _currentStation = MutableStateFlow<Station?>(favorites.loadLastStation())
    val currentStation: StateFlow<Station?> = _currentStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var pendingStation: Station? = null
    private var pendingResume = false

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { syncFromMediaItem(it) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _buffering.value = playbackState == Player.STATE_BUFFERING
        }
    }

    init {
        connectToPlayer()
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { repository.fetchColombianStations() }
                .onSuccess { _stations.value = it }
                .onFailure { e ->
                    _error.value = when (e) {
                        is java.io.IOException -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
                        else -> "No se pudo cargar el catálogo: ${e.message ?: "error desconocido"}"
                    }
                }
            _loading.value = false
        }
    }

    fun play(station: Station) {
        _currentStation.value = station
        favorites.saveLastStation(station)
        pendingResume = false
        val controller = _controller.value
        if (controller == null) {
            pendingStation = station
            connectToPlayer()
            return
        }
        // Si la emisora ya está cargada, solo reanudar: no reiniciar el stream.
        if (controller.currentMediaItem?.mediaId == station.id &&
            controller.playbackState != Player.STATE_IDLE
        ) {
            controller.play()
            syncPlayerState()
            return
        }
        controller.setMediaItem(mediaItemFor(station))
        controller.prepare()
        controller.play()
        syncPlayerState()
    }

    fun togglePlayPause() {
        val controller = _controller.value
        if (controller == null) {
            // El controlador se está reconectando: recordamos la intención.
            pendingResume = true
            connectToPlayer()
            return
        }
        if (controller.isPlaying) controller.pause() else startPlayback(controller)
        syncPlayerState()
    }

    /** Reanuda el stream actual sin volver a buscar la emisora. */
    private fun startPlayback(controller: MediaController) {
        val item = controller.currentMediaItem
        if (item != null) {
            if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
            controller.play()
            return
        }
        val station = _currentStation.value ?: favorites.loadLastStation()
        if (station != null) {
            play(station)
        } else {
            connectToPlayer()
        }
    }

    /** Fuerza una recarga del stream (por si la emisora se quedó colgada). */
    fun reconnect() {
        pendingResume = false
        val controller = _controller.value
        if (controller == null) {
            connectToPlayer()
            return
        }
        val station = _currentStation.value ?: favorites.loadLastStation()
        val item = controller.currentMediaItem ?: station?.let { mediaItemFor(it) }
        if (item != null) {
            controller.setMediaItem(item)
            controller.prepare()
            controller.play()
            syncPlayerState()
        }
    }

    /** Refresca la UI con el estado real del reproductor al volver a la app. */
    fun syncPlayerState() {
        _controller.value?.let(::updateStateFrom)
    }

    private fun updateStateFrom(controller: MediaController) {
        controller.currentMediaItem?.let(::syncFromMediaItem)
        _isPlaying.value = controller.isPlaying
        _buffering.value = controller.playbackState == Player.STATE_BUFFERING
        _volume.value = controller.volume.coerceIn(0f, 1f)
    }

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        _volume.value = volume
        _controller.value?.setVolume(volume)
    }

    private fun connectToPlayer() {
        val token = SessionToken(context, ComponentName(context, RadioPlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (controller == null || _controller.value != null) return@addListener

                controller.addListener(playerListener)
                _controller.value = controller
                updateStateFrom(controller)

                val pending = pendingStation
                val shouldResume = pendingResume
                pendingStation = null
                pendingResume = false
                when {
                    pending != null -> play(pending)
                    shouldResume -> startPlayback(controller)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun mediaItemFor(station: Station): MediaItem {
        val artwork = station.logoUrls.firstOrNull()?.let(Uri::parse)
        val metadata = MediaMetadata.Builder()
            .setTitle(station.displayName)
            .setArtist(station.cityName)
            .setArtworkUri(artwork)
            .setExtras(StationCodec.bundleFor(station))
            .build()

        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun syncFromMediaItem(item: MediaItem) {
        StationCodec.decode(item.mediaMetadata.extras?.getString(StationCodec.EXTRA_KEY))
            ?.let { _currentStation.value = it }
    }

    override fun onCleared() {
        _controller.value?.removeListener(playerListener)
        _controller.value?.release()
        pendingStation = null
        pendingResume = false
        super.onCleared()
    }
}
