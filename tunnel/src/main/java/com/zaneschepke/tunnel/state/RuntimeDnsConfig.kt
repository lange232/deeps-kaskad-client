package com.zaneschepke.tunnel.state

import com.zaneschepke.tunnel.model.DnsBoostrapConfig

data class RuntimeDnsConfig(
    val protocol: String = "plain",
    val upstream: String = DnsBoostrapConfig.DEFAULT_UPSTREAM,
    val underlyingDnsServers: String = DnsBoostrapConfig.DEFAULT_UPSTREAM,
)
