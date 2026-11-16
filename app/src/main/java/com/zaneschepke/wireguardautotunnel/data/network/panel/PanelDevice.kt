package com.zaneschepke.wireguardautotunnel.data.network.panel

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Identifies this installation on the panel by the name of its configuration.
 *
 * The panel has no device registry: a configuration is identified only by its name, and the same
 * configuration must not be used by two devices at once (they would take the session from each
 * other). `ANDROID_ID` is stable for the app across reinstalls and differs per device and per
 * signing key, so a configuration named after it can be recognised on the next sign-in — including
 * after the app was reinstalled and the local links are gone.
 */
class PanelDevice(context: Context) {

    val name: String = buildName(context)

    private fun buildName(context: Context): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val id = androidId.lowercase().filter(Char::isLetterOrDigit).take(8).ifBlank { "unknown" }
        val model =
            (Build.MODEL ?: "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(12)
                .ifBlank { "android" }
        // The panel accepts ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,31}$ and names are unique per account.
        val candidate = "$model-$id"
        return if (candidate.length <= 32 && NAME_PATTERN.matches(candidate)) candidate else id
    }

    private companion object {
        val NAME_PATTERN = Regex("^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,31}$")
    }
}
