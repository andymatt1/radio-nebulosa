package com.andymatt1.radionebulosa.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andymatt1.radionebulosa.ui.theme.NeonCyan

/** Ondas de audio animadas que indican "EN VIVO". */
@Composable
fun AudioWave(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    val transition = rememberInfiniteTransition(label = "audio_wave")
    val bars = List(4) { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 380 + index * 110,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bars.forEach { bar ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + 10 * bar.value).dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
