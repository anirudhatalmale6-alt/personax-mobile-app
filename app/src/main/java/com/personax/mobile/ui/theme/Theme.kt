package com.personax.mobile.ui.theme

import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0A0B10)
val BgCard = Color(0xFF111218)
val BgSurface = Color(0xFF161B22)
val Border = Color(0xFF1F2033)
val BorderHover = Color(0xFF2A2B3D)
val TextPrimary = Color(0xFFE1E4E8)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)
val TextDim = Color(0xFF52556A)
val Accent = Color(0xFF10B981)
val AccentDark = Color(0xFF059669)
val AccentLight = Color(0xFF34D399)
val Danger = Color(0xFFEF4444)
val Warning = Color(0xFFEAB308)
val Info = Color(0xFF3B82F6)

val Samsung = Color(0xFF3B82F6)
val Google = Color(0xFFEA4335)
val Xiaomi = Color(0xFFF97316)
val Oppo = Color(0xFF22C55E)
val OnePlus = Color(0xFFEF4444)
val Vivo = Color(0xFFA78BFA)
val Motorola = Color(0xFF22D3EE)
val Realme = Color(0xFFFACC15)

fun brandColor(manufacturer: String): Color = when {
    manufacturer.contains("Samsung", true) -> Samsung
    manufacturer.contains("Google", true) -> Google
    manufacturer.contains("Xiaomi", true) -> Xiaomi
    manufacturer.contains("OPPO", true) -> Oppo
    manufacturer.contains("OnePlus", true) -> OnePlus
    manufacturer.contains("vivo", true) -> Vivo
    manufacturer.contains("Motorola", true) -> Motorola
    manufacturer.contains("Realme", true) -> Realme
    else -> TextMuted
}
