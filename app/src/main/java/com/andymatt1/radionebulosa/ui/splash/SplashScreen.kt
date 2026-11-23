package com.andymatt1.radionebulosa.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andymatt1.radionebulosa.ui.theme.NeonCyan
import com.andymatt1.radionebulosa.ui.theme.NeonMagenta
import com.andymatt1.radionebulosa.ui.theme.NeonViolet
import com.andymatt1.radionebulosa.ui.theme.SpaceBlack
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Splash animado: un robot cósmico "despierta" y enciende la interfaz mientras
 * aparece el mensaje de bienvenida. Botón "Saltar" disponible en todo momento.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(120)
        progress.animateTo(1f, animationSpec = tween(2_600, easing = FastOutSlowInEasing))
        delay(220)
        onFinish()
    }

    val p = progress.value
    val textAlpha = ((p - 0.45f) / 0.35f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF1A0B2E), SpaceBlack))),
        contentAlignment = Alignment.Center
    ) {
        StarField()

        Text(
            text = "Saltar",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .clickable { onFinish() }
                .padding(8.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RobotCanvas(progress = p)

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Bienvenido a Radio Nebulosa",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = (1f - textAlpha) * 24f }
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Las emisoras de Colombia, en un solo lugar",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB9B9C9),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha).padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun StarField() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(36) { index ->
            val x = ((index * 47) % 100) / 100f * size.width
            val y = ((index * 83) % 100) / 100f * size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.06f + (index % 3) * 0.04f),
                radius = 1.2f + (index % 2),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun RobotCanvas(progress: Float) {
    Canvas(modifier = Modifier.size(220.dp)) {
        val awake = ((progress - 0.35f) / 0.45f).coerceIn(0f, 1f)
        val pulse = 0.94f + 0.06f * sin(progress * 22f).toFloat()
        val cx = size.width / 2f

        // Halo neón que se enciende al despertar.
        drawCircle(
            color = NeonViolet.copy(alpha = 0.08f + 0.22f * awake),
            radius = size.minDimension * 0.46f * pulse,
            center = Offset(cx, size.height * 0.52f)
        )

        // Antena y su luz magenta.
        drawLine(
            color = NeonCyan.copy(alpha = 0.35f + 0.65f * awake),
            start = Offset(cx, size.height * 0.20f),
            end = Offset(cx, size.height * 0.09f),
            strokeWidth = 4f
        )
        drawCircle(
            color = NeonMagenta.copy(alpha = 0.3f + 0.7f * awake),
            radius = 7f,
            center = Offset(cx, size.height * 0.07f)
        )

        // Cabeza: baja al inicio y se levanta con la animación.
        val headTop = size.height * (0.40f - 0.08f * progress)
        drawRoundRect(
            color = Color(0xFF242442),
            topLeft = Offset(size.width * 0.26f, headTop),
            size = Size(size.width * 0.48f, size.height * 0.27f),
            cornerRadius = CornerRadius(26f)
        )

        val eyeColor = if (awake > 0.25f) NeonCyan else Color(0xFF4A4A6A)
        val eyeY = headTop + size.height * 0.10f
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(size.width * 0.36f, eyeY),
            size = Size(size.width * 0.10f, size.height * 0.055f),
            cornerRadius = CornerRadius(10f)
        )
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(size.width * 0.54f, eyeY),
            size = Size(size.width * 0.10f, size.height * 0.055f),
            cornerRadius = CornerRadius(10f)
        )

        // Boca / panel de estado.
        drawRoundRect(
            color = NeonViolet.copy(alpha = 0.35f + 0.5f * awake),
            topLeft = Offset(size.width * 0.42f, headTop + size.height * 0.19f),
            size = Size(size.width * 0.16f, size.height * 0.025f),
            cornerRadius = CornerRadius(6f)
        )

        // Cuerpo.
        val bodyTop = size.height * 0.70f
        drawRoundRect(
            color = Color(0xFF17172B),
            topLeft = Offset(size.width * 0.31f, bodyTop),
            size = Size(size.width * 0.38f, size.height * 0.19f),
            cornerRadius = CornerRadius(18f)
        )
        drawCircle(
            color = NeonMagenta.copy(alpha = 0.4f + 0.6f * awake),
            radius = 8f,
            center = Offset(cx, bodyTop + size.height * 0.09f)
        )

        // Ondas de señal al despertar.
        if (awake > 0.15f) {
            repeat(3) { index ->
                drawArc(
                    color = NeonCyan.copy(alpha = 0.35f * awake / (index + 1)),
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(
                        cx - (34f + index * 18f),
                        bodyTop - 6f - index * 12f
                    ),
                    size = Size(68f + index * 36f, 68f + index * 36f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
