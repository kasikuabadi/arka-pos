package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Vibrant Palette Custom Theme Colors
val VibrantPrimary = Color(0xFF6750A4)
val VibrantOnPrimary = Color(0xFFFFFFFF)
val VibrantPrimaryContainer = Color(0xFFEADDFF)
val VibrantOnPrimaryContainer = Color(0xFF21005D)

val VibrantSecondary = Color(0xFF10B981) // Emerald green metrics / complete
val VibrantTertiary = Color(0xFFF59E0B) // Amber yellow alerts
val VibrantDanger = Color(0xFFEF4444) // Coral red warning / cancellation

val VibrantBg = Color(0xFFF8F9FF) // Soft vibrant light background
val VibrantSidebarBg = Color(0xFFF3F3F9) // Slightly darker sidebar gray-blue
val VibrantSurface = Color(0xFFFFFFFF) // White containers
val VibrantBorder = Color(0xFFE2E8F0) // slate-200 border
val VibrantLightBorder = Color(0xFFF1F5F9) // slate-100 border

val VibrantTextPrimary = Color(0xFF0F172A) // slate-900 (main dark text)
val VibrantTextSecondary = Color(0xFF64748B) // slate-500 (secondary text)
val VibrantTextMuted = Color(0xFF94A3B8) // slate-400

// Map old Arka theme colors to Vibrant Palette variables to prevent breaking references
val ArkaDarkBg = VibrantBg
val ArkaCardBg = VibrantSurface
val ArkaPrimary = VibrantPrimary
val ArkaSuccess = VibrantSecondary
val ArkaAccent = VibrantTertiary
val ArkaDanger = VibrantDanger
val ArkaOnSurface = VibrantTextPrimary
val ArkaTextMuted = VibrantTextSecondary

