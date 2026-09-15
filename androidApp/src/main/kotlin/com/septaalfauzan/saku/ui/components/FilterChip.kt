package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val bg = if (selected) palette.crimson else palette.chalk
    val fg = if (selected) Color.White else palette.slate
    Text(
        text = label,
        style = type.labelMd,
        color = fg,
        modifier = modifier
            .background(bg, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
