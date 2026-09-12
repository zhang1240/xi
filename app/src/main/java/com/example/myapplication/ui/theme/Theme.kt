package com.example.localledger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = LedgerBlueLight,
    secondary = Color(0xFF9CC9F0),
    tertiary = Color(0xFFFFC46B),
    background = Color(0xFF101820),
    surface = Color(0xFF182532),
    surfaceVariant = Color(0xFF263746),
    onBackground = Color(0xFFEAF3FA),
    onSurface = Color(0xFFEAF3FA)
)

private val LightColorScheme = lightColorScheme(
    primary = LedgerBlue,
    secondary = Color(0xFF4E7190),
    tertiary = LedgerOrange,
    background = LedgerWarm,
    surface = Color.White,
    surfaceVariant = Color(0xFFE4EEF7),
    onBackground = LedgerNavy,
    onSurface = LedgerNavy
)

private val LocalLedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun LocalLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        value = LocalLedgerColors provides if (darkTheme) DarkLedgerColors else LightLedgerColors,
        content = {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = LocalLedgerShapes,
                content = content
            )
        }
    )
}
