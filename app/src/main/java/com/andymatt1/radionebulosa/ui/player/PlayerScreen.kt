package com.andymatt1.radionebulosa.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andymatt1.radionebulosa.MainViewModel
import com.andymatt1.radionebulosa.ui.common.StationLogo
import com.andymatt1.radionebulosa.ui.theme.MutedText
import com.andymatt1.radionebulosa.ui.theme.NeonCyan
import com.andymatt1.radionebulosa.ui.theme.NeonMagenta
import com.andymatt1.radionebulosa.ui.theme.NeonViolet
import com.andymatt1.radionebulosa.ui.theme.SpaceBlack

/** Reproductor completo: logo protagonista, halo animado y controles. */
@Composable
fun PlayerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val station by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val buffering by viewModel.buffering.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val favorites by viewModel.favorites.favorites.collectAsState()
    val context = LocalContext.current

    val transition = rememberInfiniteTransition(label = "player_halo")
    val haloScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    if (station == null) {
        Box(Modifier.fillMaxSize().background(SpaceBlack)) {
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                text = "Selecciona una emisora para comenzar",
                color = MutedText,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    val current = station!!

    Box(Modifier.fillMaxSize().background(SpaceBlack)) {
        // Fondo dinámico y difuminado a partir del logo oficial.
        if (current.favicon.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(current.favicon)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(56.dp).alpha(0.38f)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xB3050508), SpaceBlack)
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "REPRODUCIENDO",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
            }

            Spacer(Modifier.weight(0.32f))

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((214 * haloScale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NeonViolet.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                StationLogo(station = current, size = 196.dp, cornerPercent = 50)
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = current.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${current.cityName}  •  En vivo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC6C6D6),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                current.tagList.take(3).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NeonViolet.copy(alpha = 0.18f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD8C8FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.26f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MutedText)
                Spacer(Modifier.width(10.dp))
                Slider(
                    value = volume,
                    onValueChange = viewModel::setVolume,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonViolet,
                        inactiveTrackColor = Color(0xFF343445)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.favorites.toggle(current) }) {
                        Icon(
                            imageVector = if (favorites.contains(current.id)) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = if (favorites.contains(current.id)) {
                                "Quitar de favoritos"
                            } else {
                                "Agregar a favoritos"
                            },
                            tint = if (favorites.contains(current.id)) NeonMagenta else MutedText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (favorites.contains(current.id)) "Guardada" else "Favorito",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (favorites.contains(current.id)) NeonMagenta else MutedText
                    )
                }

                FloatingActionButton(
                    onClick = viewModel::togglePlayPause,
                    containerColor = NeonViolet,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = viewModel::reconnect) {
                    Icon(
                        Icons.Filled.Replay,
                        contentDescription = "Reconectar",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (buffering) "Conectando con la emisora…" else "Transmisión en vivo",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
