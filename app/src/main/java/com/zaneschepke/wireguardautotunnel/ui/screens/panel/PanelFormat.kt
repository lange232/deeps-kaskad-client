package com.zaneschepke.wireguardautotunnel.ui.screens.panel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R

/** "12 min", "3 h", "2 d": the handshake age the panel reports as a unix timestamp. */
@Composable
fun handshakeLabel(handshake: Long): String {
    if (handshake <= 0L) return stringResource(R.string.panel_config_no_handshake)
    val seconds = (System.currentTimeMillis() / 1000 - handshake).coerceAtLeast(0)
    val age =
        when {
            seconds < 60 -> "1 min"
            seconds < 3600 -> "${seconds / 60} min"
            seconds < 86_400 -> "${seconds / 3600} h"
            else -> "${seconds / 86_400} d"
        }
    return stringResource(R.string.panel_config_handshake, age)
}

/** Compact byte counter for the traffic the panel reports for a configuration. */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value >= 10) "${value.toInt()} ${units[unit]}"
    else String.format("%.1f %s", value, units[unit])
}
