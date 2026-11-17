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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelChip
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelDivider
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelSection
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * Routing of one configuration: its internet exits in order and the subnets it is allowed to
 * reach. Both live in the configuration, not in the account, and are saved together.
 */
@Composable
fun PanelRoutingScreen(
    configId: Int,
    viewModel: SharedAppViewModel = koinActivityViewModel(),
) {
    val navController = LocalNavController.current
    val exits by viewModel.panelExits.collectAsStateWithLifecycle()
    val subnets by viewModel.panelSubnets.collectAsStateWithLifecycle()

    LaunchedEffect(configId) {
        viewModel.loadPanelExits(configId)
        viewModel.loadPanelSubnets(configId)
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        PanelSection(title = stringResource(R.string.panel_routing_exits)) {
            val current = exits
            when {
                current == null -> LoadingRow()
                current.isEmpty() ->
                    Text(
                        stringResource(R.string.panel_exits_auto),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                else ->
                    current.forEachIndexed { index, exit ->
                        if (index > 0) PanelDivider()
                        SurfaceRow(
                            leading = {
                                Icon(
                                    Icons.Rounded.Circle,
                                    contentDescription = null,
                                    tint = if (exit.alive) SilverTree else AlertRed,
                                    modifier = Modifier.size(12.dp),
                                )
                            },
                            title = exit.comment.ifBlank { exit.name },
                            description = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (!exit.comment.isBlank()) DescriptionText(exit.name)
                                    exit.rttMs?.let { PanelChip("${it.toInt()} ms") }
                                }
                            },
                            trailing = { modifier ->
                                Row {
                                    IconButton(
                                        onClick = { viewModel.movePanelExit(exit.id, up = true) },
                                        enabled = index > 0,
                                        modifier = modifier,
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            stringResource(R.string.move_up),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePanelExit(exit.id, up = false) },
                                        enabled = index < current.size - 1,
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            stringResource(R.string.move_down),
                                        )
                                    }
                                }
                            },
                        )
                    }
            }
        }

        PanelSection(title = stringResource(R.string.panel_routing_subnets)) {
            val current = subnets
            when {
                current == null -> LoadingRow()
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
                    viewModel.savePanelRouting(configId)
                    navController.pop()
                },
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}
