package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.PanelConfigLinkDao
import com.zaneschepke.wireguardautotunnel.data.entity.PanelConfigLink
import com.zaneschepke.wireguardautotunnel.domain.repository.PanelConfigLinkRepository
import kotlinx.coroutines.flow.Flow

class RoomPanelConfigLinkRepository(private val dao: PanelConfigLinkDao) :
    PanelConfigLinkRepository {

    override val flow: Flow<List<PanelConfigLink>> = dao.getAllFlow()

    override suspend fun getAll(): List<PanelConfigLink> = dao.getAll()

    override suspend fun getByConfigId(configId: Int): PanelConfigLink? =
        dao.getByConfigId(configId)

    override suspend fun getByTunnelName(tunnelName: String): PanelConfigLink? =
        dao.getByTunnelName(tunnelName)

    override suspend fun save(link: PanelConfigLink) = dao.upsert(link)

    override suspend fun delete(configId: Int) = dao.deleteByConfigId(configId)

    override suspend fun clear() = dao.clear()
}
