package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.data.entity.PanelConfigLink
import kotlinx.coroutines.flow.Flow

/** Binds panel configurations to the local tunnels imported from them. */
interface PanelConfigLinkRepository {
    val flow: Flow<List<PanelConfigLink>>

    suspend fun getAll(): List<PanelConfigLink>

    suspend fun getByConfigId(configId: Int): PanelConfigLink?

    suspend fun getByTunnelName(tunnelName: String): PanelConfigLink?

    suspend fun save(link: PanelConfigLink)

    suspend fun delete(configId: Int)

    suspend fun clear()
}
