package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

enum class PillButtonVariant { PRIMARY, ACCENT, GHOST }

@Composable
fun PillButton(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillButtonVariant = PillButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 44.dp,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val background = when (variant) {
        PillButtonVariant.PRIMARY -> if (enabled) palette.ink else palette.chalk
        PillButtonVariant.ACCENT -> if (enabled) palette.crimson else palette.chalk
        PillButtonVariant.GHOST -> palette.chalk
    }
    val content = when (variant) {
        PillButtonVariant.GHOST -> palette.ink
        PillButtonVariant.ACCENT -> Color.White
        else -> if (isSystemInDarkTheme()) SakuTheme.palette.crimson else Color.White
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(background, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (text != null) {
                Text(text, style = type.labelMd, color = content)
            }
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.padding(start = if (text != null) 8.dp else 0.dp)
                )
            }
        }
    }
}
