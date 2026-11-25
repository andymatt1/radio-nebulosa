package com.andymatt1.radionebulosa.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andymatt1.radionebulosa.data.Station
import kotlin.math.abs

/**
 * Logo protagonista con respaldo cósmico. Prueba el favicon de la API y, si
 * no existe o falla, busca el icono del dominio de la emisora. Si ninguno
 * funciona, muestra una tarjeta degradada con las iniciales.
 */
@Composable
fun StationLogo(
    station: Station,
    size: Dp,
    cornerPercent: Int = 18,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val candidates = remember(station) { station.logoUrls }
    var attempt by remember(station) { mutableIntStateOf(0) }
    val currentUrl = candidates.getOrNull(attempt)

    // Color determinista por emisora: el respaldo deja de ser siempre igual.
    val seed = abs(station.displayName.hashCode())
    val startColor = Color.hsv((seed % 360) / 360f, 0.62f, 0.92f, 0.90f)
    val endColor = Color.hsv(((seed / 7L) % 360L).toInt() / 360f, 0.70f, 0.72f, 0.85f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerPercent))
            .background(Brush.linearGradient(listOf(startColor, endColor))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = station.initials,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        if (currentUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(true)
                    .listener(onError = { _, _ ->
                        if (attempt < candidates.lastIndex) attempt += 1
                    })
                    .build(),
                contentDescription = "Logo de ${station.displayName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
