package com.zaneschepke.wireguardautotunnel.ui.screens.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.panel.MonoText
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelDivider
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelSection
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * Subnets granted to the account, switched per configuration. Saving rewrites the tunnel's
 * `.conf`, because the enabled subnets change `AllowedIPs` and DNS.
 */
@Composable
fun PanelSubnetsScreen(
    configId: Int,
    viewModel: SharedAppViewModel = koinActivityViewModel(),
) {
    val navController = LocalNavController.current
    val subnets by viewModel.panelSubnets.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { viewModel.loadPanelSubnets(configId) }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        PanelSection(title = stringResource(R.string.panel_subnets)) {
            val current = subnets
            when {
                current == null ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                current.isEmpty() ->
                    Text(
                        stringResource(R.string.panel_subnets_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                else ->
                    current.forEachIndexed { index, subnet ->
                        if (index > 0) PanelDivider()
                        SurfaceRow(
                            leading = { Icon(Icons.Outlined.Lan, contentDescription = null) },
                            title = subnet.comment.ifBlank { subnet.name },
                            description = { MonoText(subnet.routes) },
                            onClick = { viewModel.togglePanelSubnet(subnet.id) },
                            trailing = { _ ->
                                ThemedSwitch(
                                    checked = subnet.enabled,
                                    onClick = { viewModel.togglePanelSubnet(subnet.id) },
                                )
                            },
                        )
                    }
            }
        }
        PanelSection {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Save, contentDescription = null) },
                title = stringResource(R.string.save),
                onClick = {
                    viewModel.savePanelSubnets(configId)
                    navController.pop()
                },
            )
        }
    }
}
