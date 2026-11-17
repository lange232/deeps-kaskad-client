package com.zaneschepke.wireguardautotunnel.ui.common.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.ui.theme.KaskadShapes
import com.zaneschepke.wireguardautotunnel.ui.theme.KaskadSpacing

/**
 * A block of rows drawn like the panel: one flat card with a hairline border and a small caption
 * above it. Screens built from these read the same as the site instead of Material's floating
 * cards.
 */
/**
 * True inside a panel card, so rows stop painting their own background and the card shows one
 * flat surface with hairlines instead of stacked Material blocks.
 */
val LocalInPanel = compositionLocalOf { false }

/** The card itself: flat surface, 1 dp border, small radius. */
@Composable
fun PanelContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInPanel provides true) {
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(KaskadShapes.lg))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(KaskadShapes.lg),
                    )
        ) {
            content()
        }
    }
}

@Composable
fun PanelSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = KaskadSpacing.lg)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.outline,
                    ),
                modifier = Modifier.padding(start = 2.dp, bottom = KaskadSpacing.sm),
            )
        }
        PanelContainer(modifier = Modifier.fillMaxWidth()) { Column(content = content) }
    }
}

/** Hairline between rows inside a [PanelSection]. */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
    )
}

/** Technical values (addresses, keys, counters) in the panel's monospace style. */
@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = color,
        modifier = modifier,
    )
}
