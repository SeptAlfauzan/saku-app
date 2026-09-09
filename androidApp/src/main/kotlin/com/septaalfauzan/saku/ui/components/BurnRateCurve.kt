package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun BurnRateCurve(values: List<Float>, modifier: Modifier = Modifier) {
    val palette = SakuTheme.palette
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).coerceAtLeast(1e-4f)
        val points = values.mapIndexed { i, v ->
            Offset(i * step, size.height - ((v - minV) / range) * (size.height - 8f) - 4f)
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        clipPath(area) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.crimson.copy(alpha = 0.18f), palette.crimson.copy(alpha = 0f)),
                ),
            )
        }
        drawLine(
            color = palette.hairline,
            start = Offset(0f, size.height - 2f),
            end = Offset(size.width, size.height - 2f),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
        drawPath(line, color = palette.crimson, style = Stroke(width = 2f))
        drawCircle(
            color = palette.crimson,
            radius = 4f,
            center = points.last(),
        )
        points.dropLast(1).forEach {
            drawCircle(color = palette.crimson, radius = 2.5f, center = it)
        }
    }
}
