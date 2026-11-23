package com.andymatt1.radionebulosa.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andymatt1.radionebulosa.ui.theme.NeonCyan
import com.andymatt1.radionebulosa.ui.theme.NeonViolet

/**
 * Logo protagonista con respaldo cósmico: si la emisora no trae favicon o la
 * imagen falla, se ve una tarjeta degradada con las iniciales.
 */
@Composable
fun StationLogo(
    station: Station,
    size: Dp,
    cornerPercent: Int = 18,
    modifier: Modifier = Modifier,
    customImage: String? = null
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerPercent))
            .background(
                Brush.linearGradient(
                    listOf(
                        NeonViolet.copy(alpha = 0.85f),
                        NeonCyan.copy(alpha = 0.55f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = station.initials,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        // La foto elegida por el usuario tiene prioridad sobre el favicon.
        val image = customImage?.takeIf { it.isNotBlank() } ?: station.favicon
        if (image.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(image)
                    .crossfade(true)
                    .build(),
                contentDescription = station.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
