package com.zaneschepke.wireguardautotunnel.di

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.StableNetworkEngine
import com.zaneschepke.tunnel.ApplicationProvider
import com.zaneschepke.tunnel.backend.RootShell
import com.zaneschepke.tunnel.util.RootShellException
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.event.TunnelEventDispatcher
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelBackendProvider
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider
import com.zaneschepke.wireguardautotunnel.domain.repository.AutoTunnelSettingsRepository
import com.zaneschepke.wireguardautotunnel.lifecyle.AppVisibilityObserver
import com.zaneschepke.wireguardautotunnel.notification.AndroidNotificationService.NotificationChannels
import com.zaneschepke.wireguardautotunnel.notification.AndroidTunnelNotificationService
import com.zaneschepke.wireguardautotunnel.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.notification.NotificationService.Companion.PROXY_GROUP_KEY
import com.zaneschepke.wireguardautotunnel.notification.NotificationService.Companion.VPN_GROUP_KEY
import com.zaneschepke.wireguardautotunnel.notification.TunnelNotificationService
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelTileRefresher
import com.zaneschepke.wireguardautotunnel.util.extensions.to
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

val tunnelBackendProviderModule = module {
    single<TunnelNotificationService> { AndroidTunnelNotificationService(get()) }
    single { AppVisibilityObserver() }
    singleOf(::TunnelEventDispatcher)

    single<ApplicationProvider> {
        val notificationService = get<NotificationService>()
        val context = androidContext()
        object : ApplicationProvider {
            override val vpnInitNotification: Notification
                get() =
                    notificationService.createNotification(
                        channel = NotificationChannels.Tunnel.VPN,
                        title = context.getString(R.string.initializing),
                        onGoing = true,
                        groupKey = VPN_GROUP_KEY,
                    )

            override val proxyInitNotification: Notification
                get() =
                    notificationService.createNotification(
                        channel = NotificationChannels.Tunnel.Proxy,
                        title = context.getString(R.string.initializing),
                        onGoing = true,
                        groupKey = PROXY_GROUP_KEY,
                    )

            override val vpnNotificationId: Int
                get() = NotificationService.VPN_NOTIFICATION_ID

            override val proxyNotificationId: Int
                get() = NotificationService.PROXY_NOTIFICATION_ID

            override fun refreshTile(context: Context) {
                TunnelTileRefresher.refresh(context)
            }

            override fun createVpnConfigurePendingIntent(context: Context): PendingIntent {
                return PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        }
    }

    single {
        StableNetworkEngine(
            get<CoroutineScope>(named(Scope.APPLICATION)),
            get<NetworkMonitor>().connectivityStateFlow,
        )
    }

    single<NetworkMonitor> {
        AndroidNetworkMonitor(
            androidContext(),
            object : AndroidNetworkMonitor.ConfigurationListener {
                override suspend fun runRootShellCommand(cmd: String): String? {
                    return try {
                        withTimeout(3_000.milliseconds) {
                            withContext(Dispatchers.IO) {
                                val result = RootShell.run(cmd)
                                result.output
                            }
                        }
                    } catch (e: RootShellException) {
                        Timber.e(e)
                        null
                    }
                }

                override val detectionMethod =
                    get<AutoTunnelSettingsRepository>()
                        .flow
                        .distinctUntilChangedBy { it.wifiDetectionMethod }
                        .map { it.wifiDetectionMethod.to() }
            },
            get<CoroutineScope>(named(Scope.APPLICATION)),
        )
    }

    single<TunnelProvider> {
        TunnelBackendProvider(get(), get(named(Scope.APPLICATION)), get(named(Dispatcher.IO)))
    }
}
