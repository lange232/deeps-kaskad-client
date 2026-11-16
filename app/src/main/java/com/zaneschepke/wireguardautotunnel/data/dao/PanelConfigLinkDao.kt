package com.zaneschepke.wireguardautotunnel.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zaneschepke.wireguardautotunnel.data.entity.PanelConfigLink
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelConfigLinkDao {

    @Upsert suspend fun upsert(link: PanelConfigLink)

    @Query("SELECT * FROM panel_config_link") suspend fun getAll(): List<PanelConfigLink>

    @Query("SELECT * FROM panel_config_link") fun getAllFlow(): Flow<List<PanelConfigLink>>

    @Query("SELECT * FROM panel_config_link WHERE config_id = :configId")
    suspend fun getByConfigId(configId: Int): PanelConfigLink?

    @Query("SELECT * FROM panel_config_link WHERE tunnel_name = :tunnelName")
    suspend fun getByTunnelName(tunnelName: String): PanelConfigLink?

    @Query("DELETE FROM panel_config_link WHERE config_id = :configId")
    suspend fun deleteByConfigId(configId: Int)

    @Query("DELETE FROM panel_config_link") suspend fun clear()
}
