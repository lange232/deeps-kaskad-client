package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.textbox.ConfigurationTextBox

private const val DEFAULT_PANEL_URL = "https://omega.deffhost.ru:8080"

@Composable
fun PanelLoginDialog(onDismiss: () -> Unit, onConfirm: (server: String, email: String, password: String) -> Unit) {
    var server by rememberSaveable { mutableStateOf(DEFAULT_PANEL_URL) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(server, email, password) { isError = false }

    InfoDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.add_from_panel),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    stringResource(R.string.add_from_panel_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ConfigurationTextBox(
                    value = server,
                    label = stringResource(R.string.panel_server),
                    hint = DEFAULT_PANEL_URL,
                    onValueChange = { server = it },
                    isError = isError,
                )
                ConfigurationTextBox(
                    value = email,
                    label = stringResource(R.string.panel_email),
                    hint = "user@example.com",
                    onValueChange = { email = it },
                    isError = isError,
                )
                ConfigurationTextBox(
                    value = password,
                    label = stringResource(R.string.panel_password),
                    hint = "••••••••",
                    onValueChange = { password = it },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmText = stringResource(R.string.okay),
        onAttest = {
            if (server.startsWith("https://") && email.isNotBlank() && password.isNotBlank()) {
                onConfirm(server.trim(), email.trim(), password)
            } else isError = true
        },
    )
}
