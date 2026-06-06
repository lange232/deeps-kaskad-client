package com.zaneschepke.wireguardautotunnel.ui.state

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.zaneschepke.tunnel.Tunnel
import com.zaneschepke.tunnel.model.BackendMode
import com.zaneschepke.tunnel.state.ActiveTunnel
import com.zaneschepke.tunnel.state.BootstrapState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.ui.theme.CoolGray
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw

sealed class DisplayTunnelState {
    data object Disconnected : DisplayTunnelState()

    data object Connecting : DisplayTunnelState()

    data object ResolvingDns : DisplayTunnelState()

    data object EstablishingConnection : DisplayTunnelState()

    data object Ready : DisplayTunnelState()

    data object Connected : DisplayTunnelState()

    data object Degraded : DisplayTunnelState()

    @StringRes
    fun labelRes(): Int {
        return when (this) {
            Disconnected -> R.string.tunnel_state_disconnected
            Connecting -> R.string.tunnel_state_starting
            ResolvingDns -> R.string.tunnel_state_resolving_dns
            EstablishingConnection -> R.string.tunnel_state_establishing_connection
            Ready -> R.string.ready
            Connected -> R.string.tunnel_state_connected
            Degraded -> R.string.tunnel_state_handshake_failure
        }
    }

    fun asLocalizedString(context: Context): String {
        return context.getString(labelRes())
    }

    fun asColor(): Color {
        return when (this) {
            Disconnected -> CoolGray

            Connecting,
            ResolvingDns,
            EstablishingConnection,
            Ready -> Straw

            Connected -> SilverTree

            Degraded -> AlertRed
        }
    }

    companion object {
        private const val HANDSHAKE_FAILURE_DEGRADED_THRESHOLD_MS = 6_000L

        // During this window we avoid showing Degraded even if we see HandshakeFailure
        private const val POST_RESOLUTION_GRACE_PERIOD_MS = 3_500L

        fun from(activeTunnel: ActiveTunnel, now: Long): DisplayTunnelState {
            val transport = activeTunnel.transportState
            val bootstrap = activeTunnel.bootstrapState
            val mode = activeTunnel.mode
            val isVpnStyle = mode is BackendMode.Vpn || mode is BackendMode.Proxy.KillSwitchPrimary

            val bootstrapPhaseDone =
                bootstrap is BootstrapState.Complete || bootstrap is BootstrapState.None

            // Check if we recently completed peer resolution
            val recentlyResolvedPeers =
                activeTunnel.lastPeerUpdateMs > 0 &&
                    (now - activeTunnel.lastPeerUpdateMs) < POST_RESOLUTION_GRACE_PERIOD_MS

            return when {
                transport is Tunnel.State.Down -> Disconnected

                bootstrap is BootstrapState.Failed -> Degraded

                bootstrap is BootstrapState.ResolvingDns ||
                    bootstrap is BootstrapState.UpdatingPeers -> ResolvingDns

                transport is Tunnel.State.Up.Healthy -> Connected

                transport is Tunnel.State.Up.HandshakeFailure -> {
                    val age = now - activeTunnel.lastStateChangeMs

                    if (recentlyResolvedPeers && bootstrapPhaseDone) {
                        if (isVpnStyle) EstablishingConnection else Ready
                    } else if (
                        age > HANDSHAKE_FAILURE_DEGRADED_THRESHOLD_MS && bootstrapPhaseDone
                    ) {
                        Degraded
                    } else if (isVpnStyle && bootstrapPhaseDone) {
                        EstablishingConnection
                    } else if (bootstrapPhaseDone) {
                        Ready
                    } else {
                        Connecting
                    }
                }

                transport is Tunnel.State.Starting -> {
                    when {
                        bootstrapPhaseDone -> if (isVpnStyle) EstablishingConnection else Ready
                        else -> Connecting
                    }
                }

                bootstrapPhaseDone -> if (isVpnStyle) EstablishingConnection else Ready
                else -> Connecting
            }
        }
    }
}
