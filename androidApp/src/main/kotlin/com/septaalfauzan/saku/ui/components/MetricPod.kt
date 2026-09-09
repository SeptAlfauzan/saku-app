package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun MetricPod(
    label: String,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val iconColor = if (accent) palette.crimson else palette.slate
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.chalk, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(palette.canvas, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LabelCaps(label, color = palette.slate)
            Text(text, style = type.headlineSm, color = if (accent) palette.crimson else palette.ink)
        }
    }
}
