package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel

@Composable
fun PanelExitsDialog(viewModel: SharedAppViewModel, onDismiss: () -> Unit) {
    val exits by viewModel.panelExits.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadPanelExits() }

    InfoDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.panel_exits),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.panel_exits_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val current = exits
                when {
                    current == null ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    current.isEmpty() ->
                        Text(
                            stringResource(R.string.panel_exits_auto),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    else ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            current.forEachIndexed { index, exit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.Circle,
                                        contentDescription = null,
                                        tint = if (exit.alive) SilverTree else AlertRed,
                                        modifier = Modifier.size(10.dp),
                                    )
                                    Column(
                                        modifier = Modifier.padding(start = 12.dp).weight(1f)
                                    ) {
                                        Text(
                                            exit.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        exit.rttMs?.let {
                                            Text(
                                                "${it.toInt()} ms",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePanelExit(exit.id, up = true) },
                                        enabled = index > 0,
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
                            }
                        }
                }
                TextButton(onClick = {
                    viewModel.panelSignOut()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.panel_sign_out))
                }
            }
        },
        confirmText = stringResource(R.string.save),
        confirmEnabled = !exits.isNullOrEmpty(),
        onAttest = {
            viewModel.savePanelExitOrder()
            onDismiss()
        },
    )
}
