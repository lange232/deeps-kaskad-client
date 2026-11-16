package com.zaneschepke.wireguardautotunnel.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Binds a panel configuration to the local tunnel imported from it.
 *
 * An account owns several configurations (one per device), and each one has its own `.conf`.
 * Without this link the app could not tell which tunnel belongs to which configuration after a
 * restart, or after a tunnel is renamed.
 */
@Entity(
    tableName = "panel_config_link",
    indices = [Index(value = ["tunnel_name"], unique = true)],
)
data class PanelConfigLink(
    @PrimaryKey @ColumnInfo(name = "config_id") val configId: Int,
    @ColumnInfo(name = "tunnel_name") val tunnelName: String,
    @ColumnInfo(name = "server") val server: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)
