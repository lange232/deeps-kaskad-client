package com.zaneschepke.tunnel.state

import com.zaneschepke.tunnel.model.DnsBoostrapConfig

data class RuntimeDnsConfig(
    val protocol: String = "plain",
    val upstream: String = DnsBoostrapConfig.DEFAULT_PLAIN_UPSTREAM,
) {
    companion object {
        fun from(dnsConfig: DnsBoostrapConfig): RuntimeDnsConfig {
            val upstream =
                when (dnsConfig) {
                    is DnsBoostrapConfig.DoH ->
                        dnsConfig.upstream ?: DnsBoostrapConfig.DEFAULT_DOH_UPSTREAM

                    is DnsBoostrapConfig.DoT ->
                        dnsConfig.upstream ?: DnsBoostrapConfig.DEFAULT_DOT_UPSTREAM

                    is DnsBoostrapConfig.Plain ->
                        dnsConfig.upstream ?: DnsBoostrapConfig.DEFAULT_PLAIN_UPSTREAM
                }
            return RuntimeDnsConfig(protocol = dnsConfig.protocol, upstream = upstream)
        }
    }
}
