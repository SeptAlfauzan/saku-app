package com.septaalfauzan.saku.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun LabelCaps(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color? = null,
    style: TextStyle = SakuTheme.type.labelCaps,
) {
    Text(
        text = text.uppercase(),
        style = style,
        color = color ?: SakuTheme.palette.slate,
        modifier = modifier,
    )
}
