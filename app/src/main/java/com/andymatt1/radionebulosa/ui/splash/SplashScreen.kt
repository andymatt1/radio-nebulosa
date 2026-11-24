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
    // Robot tipo chatbot (inspirado en el enlace) redibujado en Compose y
    // adaptado a la identidad cósmica de Radio Nebulosa: violeta, cian y
    // magenta, con burbuja de chat y ecualizador de radio en el pecho.
    Canvas(modifier = Modifier.size(220.dp)) {
        val awake = ((progress - 0.30f) / 0.45f).coerceIn(0f, 1f)
        val pulse = 0.95f + 0.05f * sin(progress * 20f).toFloat()
        val cx = size.width / 2f
        val cy = size.height * 0.50f

        // Halo cósmico que se enciende al despertar.
        drawCircle(
            color = NeonViolet.copy(alpha = 0.06f + 0.26f * awake),
            radius = size.minDimension * 0.50f * pulse,
            center = Offset(cx, cy)
        )

        // Partículas orbitando alrededor del robot.
        repeat(5) { index ->
            val angle = (progress * 5f + index * 1.26f).toFloat()
            val orbit = size.minDimension * 0.43f
            drawCircle(
                color = if (index % 2 == 0) NeonCyan else NeonMagenta,
                alpha = 0.25f + 0.45f * awake,
                radius = 2.6f,
                center = Offset(
                    cx + kotlin.math.cos(angle) * orbit,
                    cy + kotlin.math.sin(angle) * orbit * 0.82f
                )
            )
        }

        // Antena con luz parpadeante.
        val antennaY = size.height * 0.09f
        drawLine(
            color = NeonCyan.copy(alpha = 0.4f + 0.6f * awake),
            start = Offset(cx, size.height * 0.24f),
            end = Offset(cx, antennaY + 8f),
            strokeWidth = 4f
        )
        drawCircle(
            color = NeonMagenta.copy(alpha = 0.35f + 0.65f * awake),
            radius = 6f + 2f * awake * pulse,
            center = Offset(cx, antennaY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.55f * awake),
            radius = 2f,
            center = Offset(cx, antennaY)
        )

        // Cabeza: se eleva durante la animación.
        val headTop = size.height * (0.38f - 0.07f * progress)
        val headLeft = size.width * 0.25f
        val headWidth = size.width * 0.50f
        val headHeight = size.height * 0.29f
        val headCorner = CornerRadius(34f)

        // Orejas laterales.
        drawRoundRect(
            color = Color(0xFF2B2450),
            topLeft = Offset(headLeft - 11f, headTop + headHeight * 0.30f),
            size = Size(13f, headHeight * 0.38f),
            cornerRadius = CornerRadius(6f)
        )
        drawRoundRect(
            color = Color(0xFF2B2450),
            topLeft = Offset(headLeft + headWidth - 2f, headTop + headHeight * 0.30f),
            size = Size(13f, headHeight * 0.38f),
            cornerRadius = CornerRadius(6f)
        )

        // Carcasa y borde neón.
        drawRoundRect(
            color = Color(0xFF312757),
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = headCorner
        )
        drawRoundRect(
            color = NeonCyan.copy(alpha = 0.18f + 0.42f * awake),
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = headCorner,
            style = Stroke(width = 2.5f)
        )

        // Pantalla facial.
        val faceTop = headTop + headHeight * 0.12f
        val faceLeft = headLeft + headWidth * 0.10f
        val faceWidth = headWidth * 0.80f
        val faceHeight = headHeight * 0.70f
        drawRoundRect(
            color = Color(0xFF090916),
            topLeft = Offset(faceLeft, faceTop),
            size = Size(faceWidth, faceHeight),
            cornerRadius = CornerRadius(24f)
        )

        // Ojos con resplandor.
        val eyeColor = if (awake > 0.20f) NeonCyan else Color(0xFF454560)
        val eyeY = faceTop + faceHeight * 0.25f
        val eyeWidth = faceWidth * 0.17f
        val eyeHeight = faceHeight * 0.13f
        val leftEyeX = faceLeft + faceWidth * 0.20f
        val rightEyeX = faceLeft + faceWidth * 0.63f
        drawCircle(
            color = eyeColor.copy(alpha = 0.16f * awake),
            radius = 18f * pulse,
            center = Offset(leftEyeX + eyeWidth / 2f, eyeY + eyeHeight / 2f)
        )
        drawCircle(
            color = eyeColor.copy(alpha = 0.16f * awake),
            radius = 18f * pulse,
            center = Offset(rightEyeX + eyeWidth / 2f, eyeY + eyeHeight / 2f)
        )
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(leftEyeX, eyeY),
            size = Size(eyeWidth, eyeHeight),
            cornerRadius = CornerRadius(10f)
        )
        drawRoundRect(
            color = eyeColor,
            topLeft = Offset(rightEyeX, eyeY),
            size = Size(eyeWidth, eyeHeight),
            cornerRadius = CornerRadius(10f)
        )

        // Sonrisa: arco magenta que se enciende al despertar.
        drawArc(
            color = NeonMagenta.copy(alpha = 0.25f + 0.65f * awake),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - faceWidth * 0.17f, faceTop + faceHeight * 0.46f),
            size = Size(faceWidth * 0.34f, faceHeight * 0.28f),
            style = Stroke(width = 4f)
        )

        // Burbuja de chat: detalle del robot IA del enlace.
        val bubbleX = size.width * 0.70f
        val bubbleY = headTop - 14f
        drawRoundRect(
            color = NeonViolet.copy(alpha = 0.30f + 0.55f * awake),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(44f, 28f),
            cornerRadius = CornerRadius(12f)
        )
        repeat(3) { index ->
            drawCircle(
                color = Color.White.copy(alpha = 0.45f + 0.45f * awake),
                radius = 2.6f,
                center = Offset(bubbleX + 12f + index * 10f, bubbleY + 14f)
            )
        }
        drawCircle(
            color = NeonViolet.copy(alpha = 0.30f + 0.55f * awake),
            radius = 4f,
            center = Offset(bubbleX + 9f, bubbleY + 31f)
        )

        // Cuello y cuerpo.
        drawRoundRect(
            color = Color(0xFF242442),
            topLeft = Offset(cx - 12f, headTop + headHeight - 2f),
            size = Size(24f, 16f),
            cornerRadius = CornerRadius(6f)
        )
        val bodyTop = size.height * 0.74f
        val bodyLeft = size.width * 0.30f
        val bodyWidth = size.width * 0.40f
        val bodyHeight = size.height * 0.18f
        drawRoundRect(
            color = Color(0xFF17172B),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(22f)
        )
        drawRoundRect(
            color = NeonViolet.copy(alpha = 0.20f + 0.45f * awake),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(22f),
            style = Stroke(width = 2f)
        )

        // Ecualizador de radio en el pecho.
        repeat(5) { index ->
            val wave = (sin(progress * 9f + index.toFloat()) * 0.5f + 0.5f).toFloat()
            val barHeight = (6f + 14f * wave) * awake
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.40f + 0.55f * awake),
                topLeft = Offset(
                    cx - 30f + index * 14f,
                    bodyTop + bodyHeight * 0.55f - barHeight / 2f
                ),
                size = Size(6f, barHeight.coerceAtLeast(3f)),
                cornerRadius = CornerRadius(3f)
            )
        }

        // Ondas de señal radiofónica al despertar.
        if (awake > 0.12f) {
            repeat(3) { index ->
                drawArc(
                    color = NeonCyan.copy(alpha = 0.32f * awake / (index + 1)),
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(
                        cx - (36f + index * 18f),
                        bodyTop - 8f - index * 12f
                    ),
                    size = Size(72f + index * 36f, 72f + index * 36f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
