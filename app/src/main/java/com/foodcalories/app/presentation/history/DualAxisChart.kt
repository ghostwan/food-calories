package com.foodcalories.app.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodcalories.app.R

data class ChartDataPoint(
    val label: String,
    val calories: Int?,
    val weight: Float?
)

private val CaloriesColor = Color(0xFF2196F3)
private val WeightColor = Color(0xFFF44336)
private val GoalColor = Color(0xFF4CAF50)

@Composable
fun DualAxisChart(
    data: List<ChartDataPoint>,
    caloriesGoal: Int?,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_chart_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val leftPadding = 40.dp.toPx()
                val rightPadding = 40.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 20.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                val caloriesValues = data.mapNotNull { it.calories }
                val weightValues = data.mapNotNull { it.weight }

                if (caloriesValues.isEmpty() && weightValues.isEmpty()) return@Canvas

                // Calculate ranges
                val calMin = (caloriesValues.minOrNull() ?: 0) * 0.8f
                val calMax = (caloriesValues.maxOrNull() ?: 2500) * 1.1f
                val calRange = (calMax - calMin).coerceAtLeast(100f)

                val wMin = ((weightValues.minOrNull() ?: 60f) - 2f)
                val wMax = ((weightValues.maxOrNull() ?: 80f) + 2f)
                val wRange = (wMax - wMin).coerceAtLeast(5f)

                // Draw goal line
                if (caloriesGoal != null && caloriesGoal > 0) {
                    val goalY = topPadding + chartHeight * (1f - (caloriesGoal - calMin) / calRange)
                    if (goalY in topPadding..topPadding + chartHeight) {
                        drawLine(
                            color = GoalColor,
                            start = Offset(leftPadding, goalY),
                            end = Offset(leftPadding + chartWidth, goalY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                }

                // Draw axis labels
                val calMaxLabel = "${calMax.toInt()}"
                val calMinLabel = "${calMin.toInt()}"
                drawText(
                    textMeasurer = textMeasurer,
                    text = calMaxLabel,
                    topLeft = Offset(0f, topPadding - 6.dp.toPx()),
                    style = TextStyle(color = CaloriesColor, fontSize = 9.sp)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = calMinLabel,
                    topLeft = Offset(0f, topPadding + chartHeight - 6.dp.toPx()),
                    style = TextStyle(color = CaloriesColor, fontSize = 9.sp)
                )

                val wMaxLabel = String.format("%.0f", wMax)
                val wMinLabel = String.format("%.0f", wMin)
                val wMaxMeasured = textMeasurer.measure(wMaxLabel, TextStyle(fontSize = 9.sp))
                val wMinMeasured = textMeasurer.measure(wMinLabel, TextStyle(fontSize = 9.sp))
                drawText(
                    textMeasurer = textMeasurer,
                    text = wMaxLabel,
                    topLeft = Offset(size.width - wMaxMeasured.size.width, topPadding - 6.dp.toPx()),
                    style = TextStyle(color = WeightColor, fontSize = 9.sp)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = wMinLabel,
                    topLeft = Offset(size.width - wMinMeasured.size.width, topPadding + chartHeight - 6.dp.toPx()),
                    style = TextStyle(color = WeightColor, fontSize = 9.sp)
                )

                // Draw calories curve
                drawCurve(
                    data = data,
                    getValue = { it.calories?.toFloat() },
                    color = CaloriesColor,
                    minVal = calMin,
                    range = calRange,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight
                )

                // Draw weight curve
                drawCurve(
                    data = data,
                    getValue = { it.weight },
                    color = WeightColor,
                    minVal = wMin,
                    range = wRange,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight
                )

                // Draw date labels
                if (data.size >= 2) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = data.first().label,
                        topLeft = Offset(leftPadding, topPadding + chartHeight + 4.dp.toPx()),
                        style = TextStyle(color = labelColor, fontSize = 9.sp)
                    )
                    val lastLabel = data.last().label
                    val lastMeasured = textMeasurer.measure(lastLabel, TextStyle(fontSize = 9.sp))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = lastLabel,
                        topLeft = Offset(
                            leftPadding + chartWidth - lastMeasured.size.width,
                            topPadding + chartHeight + 4.dp.toPx()
                        ),
                        style = TextStyle(color = labelColor, fontSize = 9.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = CaloriesColor, label = stringResource(R.string.history_legend_calories))
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = WeightColor, label = stringResource(R.string.history_legend_weight))
                if (caloriesGoal != null && caloriesGoal > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = GoalColor, label = stringResource(R.string.history_legend_goal), dashed = true)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(16.dp, 3.dp)) {
            if (dashed) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun DrawScope.drawCurve(
    data: List<ChartDataPoint>,
    getValue: (ChartDataPoint) -> Float?,
    color: Color,
    minVal: Float,
    range: Float,
    leftPadding: Float,
    topPadding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val points = mutableListOf<Offset>()
    val step = if (data.size > 1) chartWidth / (data.size - 1) else 0f

    for (i in data.indices) {
        val value = getValue(data[i]) ?: continue
        val x = leftPadding + i * step
        val y = topPadding + chartHeight * (1f - (value - minVal) / range)
        points.add(Offset(x, y))
    }

    if (points.size < 2) {
        // Draw single point as dot
        points.firstOrNull()?.let {
            drawCircle(color = color, radius = 4.dp.toPx(), center = it)
        }
        return
    }

    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val cpx = (prev.x + curr.x) / 2
        path.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw dots at data points
    for (point in points) {
        drawCircle(color = color, radius = 3.dp.toPx(), center = point)
    }
}
