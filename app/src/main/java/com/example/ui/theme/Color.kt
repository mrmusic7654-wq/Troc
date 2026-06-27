// app/src/main/java/com/example/ui/theme/Color.kt
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════
// Troc Agent • Yin-Yang Color System
// ═══════════════════════════════════════

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Milky White • Yang (Light/Active)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val MilkyWhite          = Color(0xFFF5F5F0)
val MilkyWhiteCard      = Color(0xFFFFFFFF)
val MilkyWhiteText      = Color(0xFFF5F5F0)
val MilkyWhiteSurface   = Color(0xFFFAFAF7)
val MilkyWhiteHover     = Color(0xFFEEEEE8)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Shadow Black • Yin (Dark/Receptive)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val ShadowBlack         = Color(0xFF0A0A0A)
val ShadowBlackCard     = Color(0xFF161616)
val ShadowBlackText     = Color(0xFF0A0A0A)
val ShadowBlackSurface  = Color(0xFF111111)
val ShadowBlackHover    = Color(0xFF1C1C1C)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Balance Gold • Harmony Accent
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val BalanceGold          = Color(0xFFCBB28D)
val BalanceGoldLight     = Color(0xFFD4C4A8)
val BalanceGoldDark      = Color(0xFFB89B6E)
val BalanceGoldSubtle    = Color(0xFFCBB28D).copy(alpha = 0.12f)
val BalanceGoldGlow      = Color(0xFFCBB28D).copy(alpha = 0.25f)
val BalanceGoldMuted     = Color(0xFFCBB28D).copy(alpha = 0.50f)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Borders • Threshold Between Worlds
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val BorderGrayLight     = Color(0xFFE5E5DE)
val BorderGrayDark      = Color(0xFF2C2C2B)
val BorderGraySubtle    = Color(0xFF2C2C2B).copy(alpha = 0.5f)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Muted Grays • The Space Between
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val MutedGrayLight      = Color(0xFF7C7C75)
val MutedGrayDark       = Color(0xFF9E9E96)
val MutedGraySubtle     = Color(0xFF9E9E96).copy(alpha = 0.5f)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Semantic Colors • Emotional States
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val SuccessGreen        = Color(0xFF4CAF50)
val SuccessGreenBg      = Color(0xFF4CAF50).copy(alpha = 0.10f)
val WarningAmber        = Color(0xFFFFB74D)
val WarningAmberBg      = Color(0xFFFFB74D).copy(alpha = 0.10f)
val ErrorRed            = Color(0xFFEF5350)
val ErrorRedBg          = Color(0xFFEF5350).copy(alpha = 0.10f)
val InfoBlue            = Color(0xFF64B5F6)
val InfoBlueBg          = Color(0xFF64B5F6).copy(alpha = 0.10f)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Premium Gradient • Elevated Experience
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
val PremiumGradientStart    = Color(0xFFCBB28D)
val PremiumGradientEnd      = Color(0xFFE8D5B7)
val PremiumDarkStart        = Color(0xFFB89B6E)
val PremiumDarkEnd          = Color(0xFF8B734A)
