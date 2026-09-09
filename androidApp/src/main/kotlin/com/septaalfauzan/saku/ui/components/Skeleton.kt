package com.septaalfauzan.saku.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, width: Dp, height: Dp, radius: Dp = 16.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800)),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .alpha(alpha)
            .background(SakuTheme.palette.chalk, RoundedCornerShape(radius)),
    )
}
