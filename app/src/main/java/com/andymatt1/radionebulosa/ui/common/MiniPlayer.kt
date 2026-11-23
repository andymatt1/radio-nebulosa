package com.andymatt1.radionebulosa.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andymatt1.radionebulosa.data.Station
import com.andymatt1.radionebulosa.ui.theme.CarbonLight
import com.andymatt1.radionebulosa.ui.theme.MutedText
import com.andymatt1.radionebulosa.ui.theme.NeonViolet

/** Mini-reproductor flotante que aparece al seleccionar una emisora. */
@Composable
fun MiniPlayer(
    station: Station,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable(onClick = onOpen),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = CarbonLight,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(station = station, size = 52.dp, cornerPercent = 14)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = station.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlaying) AudioWave()
                    Spacer(Modifier.width(if (isPlaying) 6.dp else 0.dp))
                    Text(
                        text = "EN VIVO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                }
            }

            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = NeonViolet,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
