package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.entity.Asset

internal data class ApkChoice(val asset: Asset, val version: String)

internal object ApkAssetSelector {
    private val abiSuffixes =
        mapOf(
            "arm64-v8a" to "-arm64",
            "armeabi-v7a" to "-armv7",
            "x86_64" to "-x64",
            "x86" to "-x86",
        )

    fun select(assets: List<Asset>, flavor: String, supportedAbis: List<String>): ApkChoice? {
        // Must match the asset name built in app/build.gradle.kts:
        // "<APP_NAME>-<flavor>-v<version>[-<abi>].apk". The stable asset
        // "deepskaskad.apk" has no version prefix and is ignored on purpose,
        // so the version can be parsed from the file name.
        val prefix = "deepskaskad-$flavor-v"
        val apks = assets.filter { it.name.startsWith(prefix) && it.name.endsWith(".apk") }

        fun suffixOf(asset: Asset): String? =
            abiSuffixes.values.firstOrNull { asset.name.endsWith("$it.apk") }

        val chosen =
            supportedAbis.firstNotNullOfOrNull { abi ->
                val suffix = abiSuffixes[abi] ?: return@firstNotNullOfOrNull null
                apks.firstOrNull { it.name.endsWith("$suffix.apk") }
            } ?: apks.firstOrNull { suffixOf(it) == null } ?: return null

        val version =
            chosen.name
                .removePrefix(prefix)
                .removeSuffix(".apk")
                .removeSuffix(suffixOf(chosen) ?: "")
        return ApkChoice(chosen, version)
    }
}
