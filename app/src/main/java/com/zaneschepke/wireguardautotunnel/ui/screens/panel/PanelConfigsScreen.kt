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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.network.panel.PanelConfig
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * Account configurations of the panel. One configuration is one device: the list is the place to
 * create, enable, delete and import them, and the row that belongs to the running tunnel is
 * highlighted.
 */
@Composable
fun PanelConfigsScreen(viewModel: SharedAppViewModel = koinActivityViewModel()) {
    val navController = LocalNavController.current
    val configs by viewModel.panelConfigs.collectAsStateWithLifecycle()
    val links by viewModel.panelLinks.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedPanelConfigId.collectAsStateWithLifecycle()

    var createDialog by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PanelConfig?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshPanelConfigs() }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column {
            GroupLabel(
                stringResource(R.string.panel_configs),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            DescriptionText(
                stringResource(R.string.panel_configs_description),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val current = configs
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
                        stringResource(R.string.panel_config_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                else ->
                    current.forEach { config ->
                        val link = links.firstOrNull { it.configId == config.id }
                        SurfaceRow(
                            leading = { Icon(Icons.Outlined.Router, contentDescription = null) },
                            title = config.name,
                            description = {
                                DescriptionText(
                                    listOfNotNull(
                                            config.ip.ifBlank { null },
                                            stringResource(
                                                if (config.enabled)
                                                    R.string.panel_config_state_enabled
                                                else R.string.panel_config_state_disabled,
                                            ),
                                            handshakeLabel(config.handshake),
                                            link?.let {
                                                stringResource(
                                                    R.string.panel_config_linked,
                                                    it.tunnelName,
                                                )
                                            },
                                        )
                                        .joinToString(" · "),
                                )
                            },
                            selected = config.id == selectedId,
                            onClick = { viewModel.switchToPanelConfig(config.id) },
                            trailing = { modifier ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ThemedSwitch(
                                        checked = config.enabled,
                                        onClick = { viewModel.togglePanelConfig(config.id) },
                                    )
                                    IconButton(
                                        onClick = { deleteTarget = config },
                                        modifier = modifier,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            stringResource(R.string.panel_config_delete),
                                        )
                                    }
                                }
                            },
                        )
                    }
            }
        }
        Column {
            SurfaceRow(
                leading = { Icon(Icons.Filled.Add, contentDescription = null) },
                title = stringResource(R.string.panel_config_create),
                onClick = { createDialog = true },
            )
        }
        Column {
            SurfaceRow(
                leading = { Icon(Icons.Outlined.Public, contentDescription = null) },
                title = stringResource(R.string.panel_exits),
                onClick = { navController.push(Route.PanelExits) },
            )
            SurfaceRow(
                leading = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                title = stringResource(R.string.panel_sign_out),
                onClick = {
                    viewModel.panelSignOut()
                    navController.pop()
                },
            )
        }
    }

    if (createDialog) {
        CreateConfigDialog(
            onDismiss = { createDialog = false },
            onConfirm = { name ->
                viewModel.createPanelConfig(name)
                createDialog = false
            },
        )
    }

    deleteTarget?.let { config ->
        InfoDialog(
            onDismiss = { deleteTarget = null },
            onAttest = {
                viewModel.deletePanelConfig(config.id)
                deleteTarget = null
            },
            title = stringResource(R.string.panel_config_delete),
            body = {
                Text(
                    text =
                        stringResource(R.string.panel_config_delete_message) + "\n\n" + config.name,
                )
            },
            confirmText = stringResource(R.string.delete),
        )
    }
}

@Composable
private fun CreateConfigDialog(onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    InfoDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.panel_config_create),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigurationTextBox(
                    value = name,
                    label = stringResource(R.string.panel_config_create_hint),
                    hint = "",
                    onValueChange = { name = it },
                )
            }
        },
        confirmText = stringResource(R.string.okay),
        onAttest = { onConfirm(name.trim().ifBlank { null }) },
    )
}
