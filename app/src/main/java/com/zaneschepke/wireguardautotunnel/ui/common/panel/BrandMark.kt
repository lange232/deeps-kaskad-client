package com.zaneschepke.wireguardautotunnel.ui.common.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/** The panel header brand: muted "DEEPS" followed by the accented "KASKAD". */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWord("DEEPS", MaterialTheme.colorScheme.outline)
        BrandWord("KASKAD", MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BrandWord(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.02.em,
            ),
        color = color,
        maxLines = 1,
    )
}
