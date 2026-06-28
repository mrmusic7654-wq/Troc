// app/src/main/java/com/example/ui/theme/Theme.kt
package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MilkyWhite,
    onPrimary = ShadowBlack,
    primaryContainer = ShadowBlackCard,
    onPrimaryContainer = MilkyWhiteText,
    secondary = BalanceGold,
    onSecondary = ShadowBlack,
    secondaryContainer = BalanceGoldSubtle,
    onSecondaryContainer = BalanceGold,
    tertiary = BalanceGoldLight,
    onTertiary = ShadowBlack,
    tertiaryContainer = BalanceGoldSubtle,
    onTertiaryContainer = BalanceGoldLight,
    error = ErrorRed,
    onError = MilkyWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
    background = ShadowBlack,
    onBackground = MilkyWhiteText,
    surface = ShadowBlackCard,
    onSurface = MilkyWhiteText,
    surfaceVariant = ShadowBlackSurface,
    onSurfaceVariant = MutedGrayDark,
    outline = BorderGrayDark,
    outlineVariant = BorderGraySubtle,
    inverseSurface = MilkyWhite,
    inverseOnSurface = ShadowBlackText,
    inversePrimary = ShadowBlack,
    scrim = ShadowBlack.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = ShadowBlack,
    onPrimary = MilkyWhite,
    primaryContainer = MilkyWhiteCard,
    onPrimaryContainer = ShadowBlackText,
    secondary = BalanceGold,
    onSecondary = MilkyWhite,
    secondaryContainer = BalanceGoldSubtle,
    onSecondaryContainer = BalanceGoldDark,
    tertiary = BalanceGoldDark,
    onTertiary = MilkyWhite,
    tertiaryContainer = BalanceGoldSubtle,
    onTertiaryContainer = BalanceGoldDark,
    error = ErrorRed,
    onError = MilkyWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
    background = MilkyWhite,
    onBackground = ShadowBlackText,
    surface = MilkyWhiteCard,
    onSurface = ShadowBlackText,
    surfaceVariant = MilkyWhiteSurface,
    onSurfaceVariant = MutedGrayLight,
    outline = BorderGrayLight,
    outlineVariant = BorderGrayLight.copy(alpha = 0.5f),
    inverseSurface = ShadowBlack,
    inverseOnSurface = MilkyWhiteText,
    inversePrimary = MilkyWhite,
    scrim = ShadowBlack.copy(alpha = 0.4f)
)

private val LocalCustomColors = staticCompositionLocalOf { DarkColorScheme }

object TrocTheme {
    val colorScheme: ColorScheme
        @Composable get() = LocalCustomColors.current
}

@Composable
fun MyApplicationTheme(
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

    CompositionLocalProvider(LocalCustomColors provides colorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
