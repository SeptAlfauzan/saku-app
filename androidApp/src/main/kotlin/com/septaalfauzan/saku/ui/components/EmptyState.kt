package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun EmptyState(
    title: String,
    body: String,
    ctaLabel: String? = null,
    onCta: () -> Unit = {},
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = type.headlineSm, color = palette.ink)
        Spacer(Modifier.height(4.dp))
        Text(body, style = type.bodyMd, color = palette.slate)
        if (ctaLabel != null) {
            Spacer(Modifier.height(12.dp))
            PillButton(text = ctaLabel, onClick = onCta, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}
