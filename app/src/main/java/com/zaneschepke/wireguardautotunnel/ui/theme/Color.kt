package com.zaneschepke.wireguardautotunnel.ui.theme

import androidx.compose.ui.graphics.Color

val OffWhite = Color(0xFFF2F2F4)
val CoolGray = Color(0xFF8D9D9F)
val LightGrey = Color(0xFFECEDEF)
val Aqua = Color(0xFF76BEBD)
val Plantation = Color(0xFF2E3538)
val Shark = Color(0xFF21272A)
val BalticSea = Color(0xFF1C1B1F)

// amoled
val ElectricTeal = Color(0xFF4DD0E1)

// Status colors
val SilverTree = Color(0xFF6DB58B)
val AlertRed = Color(0xFFCF6679)
val Straw = Color(0xFFD4C483)

val Disabled = CoolGray.copy(alpha = 0.4f)

// Other colors
val ConfigHeaderColor = Color(0xFFBB86FC)
val ConfigKeyColor = Color(0xFF03DAC5)
val Heart = Color(0xFFDB61A2)

// deeps kaskad site palette (panel/static/theme.css)
val KaskadBg = Color(0xFF161616)
val KaskadDeep = Color(0xFF080808)
val KaskadLayer = Color(0xFF242424)
val KaskadPeach = Color(0xFFFAB283)
val KaskadOrange = Color(0xFFD16427)
val KaskadOrangeSoft = Color(0xFFFFD8C6)
val KaskadText = Color(0xFFF2F2F2)
val KaskadTextDark = Color(0xFF161616)

sealed class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val onSurface: Color,
    val onBackground: Color,
    val outline: Color,
) {

    data object Light :
        ThemeColors(
            background = LightGrey.copy(alpha = 0.95f),
            surface = OffWhite,
            primary = Aqua,
            secondary = LightGrey,
            onSurface = BalticSea,
            outline = Plantation.copy(alpha = .75f),
            onBackground = BalticSea,
        )

    data object Dark :
        ThemeColors(
            background = BalticSea,
            surface = Shark,
            primary = Aqua,
            secondary = Plantation,
            onSurface = OffWhite,
            outline = CoolGray,
            onBackground = OffWhite,
        )

    data object Kaskad :
        ThemeColors(
            background = KaskadBg,
            surface = KaskadLayer,
            primary = KaskadPeach,
            secondary = KaskadDeep,
            onSurface = KaskadText,
            outline = CoolGray.copy(alpha = 0.7f),
            onBackground = KaskadText,
        )

    data object KaskadLight :
        ThemeColors(
            background = Color(0xFFFAFAFA),
            surface = LightGrey,
            primary = KaskadOrange,
            secondary = KaskadOrangeSoft,
            onSurface = KaskadTextDark,
            outline = Plantation.copy(alpha = 0.6f),
            onBackground = KaskadTextDark,
        )
}
