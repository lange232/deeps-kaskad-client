package com.zaneschepke.wireguardautotunnel.ui.theme

import androidx.compose.ui.unit.dp

val LockedDownBannerHeight = 20.dp

/**
 * Metrics taken from the panel's theme.css (panel/static/theme.css): radii 2..10px, control heights
 * 24/28/32px, compact rows. App screens use these instead of the Material defaults so the client
 * feels like the site.
 */
object KaskadShapes {
    val xs = 2.dp
    val sm = 4.dp
    val md = 6.dp
    val lg = 8.dp
    val xl = 10.dp
    val full = 9999.dp
}

object KaskadSizes {
    val controlSmall = 24.dp
    val controlMedium = 28.dp
    val controlLarge = 32.dp
}

object KaskadSpacing {
    val xs = 2.dp
    val sm = 4.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
}
