package com.zaneschepke.wireguardautotunnel.ui.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.panel.PanelButton

@Composable
fun InfoDialog(
    onAttest: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    body: @Composable (() -> Unit),
    confirmText: String,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissText: String = stringResource(R.string.cancel),
    onDismissButton: () -> Unit = onDismiss,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = { onDismiss() },
                confirmButton = {
                    PanelButton(
                        text = confirmText,
                        onClick = onAttest,
                        enabled = confirmEnabled,
                        filled = true,
                    )
                },
                dismissButton = {
                    PanelButton(
                        text = dismissText,
                        onClick = onDismissButton,
                        filled = false,
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(text = title) },
                text = { body() },
                properties = DialogProperties(usePlatformDefaultWidth = true),
            )
        }
    }
}
