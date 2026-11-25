package com.andymatt1.radionebulosa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta mística: violeta profundo + neón cósmico.
val SpaceBlack = Color(0xFF05030E)
val Carbon = Color(0xFF100A1C)
val CarbonLight = Color(0xFF1A1230)
val NeonViolet = Color(0xFFA56BFF)
val NeonCyan = Color(0xFF5BE7E6)
val NeonMagenta = Color(0xFFFF5CC8)
val MutedText = Color(0xFFA79ABF)

private val NebulosaColors = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = SpaceBlack,
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = SpaceBlack,
    onBackground = Color.White,
    surface = Carbon,
    onSurface = Color.White,
    surfaceVariant = CarbonLight,
    onSurfaceVariant = Color(0xFFB9B9C9),
    error = Color(0xFFFF6B6B)
)

private val NebulosaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun RadioNebulosaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NebulosaColors,
        typography = NebulosaTypography,
        content = content
    )
}
