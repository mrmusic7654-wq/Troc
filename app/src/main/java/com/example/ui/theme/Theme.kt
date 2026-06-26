package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MilkyWhite,
    onPrimary = ShadowBlack,
    primaryContainer = ShadowBlackCard,
    onPrimaryContainer = MilkyWhiteText,
    secondary = BalanceGold,
    onSecondary = ShadowBlack,
    background = ShadowBlack,
    onBackground = MilkyWhiteText,
    surface = ShadowBlackCard,
    onSurface = MilkyWhiteText,
    outline = BorderGrayDark,
    surfaceVariant = ShadowBlackCard,
    onSurfaceVariant = MutedGrayDark
)

private val LightColorScheme = lightColorScheme(
    primary = ShadowBlack,
    onPrimary = MilkyWhite,
    primaryContainer = MilkyWhiteCard,
    onPrimaryContainer = ShadowBlackText,
    secondary = BalanceGold,
    onSecondary = MilkyWhite,
    background = MilkyWhite,
    onBackground = ShadowBlackText,
    surface = MilkyWhiteCard,
    onSurface = ShadowBlackText,
    outline = BorderGrayLight,
    surfaceVariant = MilkyWhiteCard,
    onSurfaceVariant = MutedGrayLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to force our artistic Yin-Yang branding!
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
