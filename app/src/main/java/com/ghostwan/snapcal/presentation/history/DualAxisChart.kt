package com.ghostwan.snapcal.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostwan.snapcal.R

data class ChartDataPoint(
    val label: String,
    val calories: Int?,
    val weight: Float?,
    val burnedCalories: Int? = null
)

private val CaloriesColor = Color(0xFF4CAF50)
private val WeightColor = Color(0xFFF44336)
private val BurnedColor = Color(0xFFFF9800)
private val CaloriesGoalColor = Color(0xFF388E3C)
private val WeightGoalColor = Color(0xFFF44336)

@Composable
fun DualAxisChart(
    data: List<ChartDataPoint>,
    caloriesGoal: Int?,
    targetWeight: Float?,
    nextDateLabel: String? = null,
    showCalories: Boolean = true,
    showWeight: Boolean = true,
    showBurned: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offsetX = (offsetX + panChange.x).coerceIn(
                -(scale - 1f) * 500f,
                0f
            )
        } else {
            offsetX = 0f
        }
    }

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
                    .height(220.dp)
                    .clipToBounds()
                    .transformable(state = transformableState)
            ) {
                val leftPadding = 40.dp.toPx()
                val rightPadding = 40.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                val caloriesValues = if (showCalories) data.mapNotNull { it.calories } else emptyList()
                val weightValues = if (showWeight) data.mapNotNull { it.weight } else emptyList()
                val burnedValues = if (showBurned) data.mapNotNull { it.burnedCalories } else emptyList()

                if (caloriesValues.isEmpty() && weightValues.isEmpty() && burnedValues.isEmpty()) return@Canvas

                // Calculate ranges — start at zero, include goals and burned
                val allCalValues = caloriesValues + burnedValues
                var calMaxRaw = (allCalValues.maxOrNull() ?: 2500).toFloat()
                if (caloriesGoal != null && caloriesGoal > 0) {
                    calMaxRaw = maxOf(calMaxRaw, caloriesGoal.toFloat())
                }
                val calMin = 0f
                val calMax = calMaxRaw * 1.1f
                val calRange = calMax.coerceAtLeast(100f)

                var wMaxRaw = weightValues.maxOrNull() ?: 80f
                if (targetWeight != null && targetWeight > 0f) {
                    wMaxRaw = maxOf(wMaxRaw, targetWeight)
                }
                val wMin = 0f
                val wMax = wMaxRaw * 1.1f
                val wRange = wMax.coerceAtLeast(5f)

                // Apply zoom + pan transform for the chart content
                withTransform({
                    clipRect(leftPadding, 0f, leftPadding + chartWidth, size.height)
                    translate(left = offsetX)
                    scale(scaleX = scale, scaleY = 1f, pivot = Offset(leftPadding, 0f))
                }) {
                    // Draw calories goal line
                    if (showCalories && caloriesGoal != null && caloriesGoal > 0) {
                        val goalY = topPadding + chartHeight * (1f - (caloriesGoal - calMin) / calRange)
                        drawLine(
                            color = CaloriesGoalColor,
                            start = Offset(leftPadding, goalY),
                            end = Offset(leftPadding + chartWidth, goalY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    // Draw target weight line
                    if (showWeight && targetWeight != null && targetWeight > 0f) {
                        val targetY = topPadding + chartHeight * (1f - (targetWeight - wMin) / wRange)
                        drawLine(
                            color = WeightGoalColor,
                            start = Offset(leftPadding, targetY),
                            end = Offset(leftPadding + chartWidth, targetY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }

                    // Draw calories curve
                    if (showCalories) {
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
                    }

                    // Draw burned calories curve
                    if (showBurned) {
                        drawCurve(
                            data = data,
                            getValue = { it.burnedCalories?.toFloat() },
                            color = BurnedColor,
                            minVal = calMin,
                            range = calRange,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            chartWidth = chartWidth,
                            chartHeight = chartHeight
                        )
                    }

                    // Draw weight curve
                    if (showWeight) {
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
                    }
                }

                // Draw axis labels (outside transform so they stay fixed)
                val axisLabelStyle = TextStyle(fontSize = 9.sp)
                val calMaxLabel = "${calMax.toInt()}"
                val calMidLabel = "${((calMax + calMin) / 2).toInt()}"
                val calMinLabel = "${calMin.toInt()}"
                val midY = topPadding + chartHeight / 2 - 6.dp.toPx()

                drawText(
                    textMeasurer = textMeasurer,
                    text = calMaxLabel,
                    topLeft = Offset(0f, topPadding - 6.dp.toPx()),
                    style = axisLabelStyle.copy(color = CaloriesColor)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = calMidLabel,
                    topLeft = Offset(0f, midY),
                    style = axisLabelStyle.copy(color = CaloriesColor.copy(alpha = 0.6f))
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = calMinLabel,
                    topLeft = Offset(0f, topPadding + chartHeight - 6.dp.toPx()),
                    style = axisLabelStyle.copy(color = CaloriesColor)
                )

                val wMaxLabel = String.format("%.0f", wMax)
                val wMidLabel = String.format("%.0f", (wMax + wMin) / 2)
                val wMinLabel = String.format("%.0f", wMin)
                val wMaxMeasured = textMeasurer.measure(wMaxLabel, axisLabelStyle)
                val wMidMeasured = textMeasurer.measure(wMidLabel, axisLabelStyle)
                val wMinMeasured = textMeasurer.measure(wMinLabel, axisLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = wMaxLabel,
                    topLeft = Offset(size.width - wMaxMeasured.size.width, topPadding - 6.dp.toPx()),
                    style = axisLabelStyle.copy(color = WeightColor)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = wMidLabel,
                    topLeft = Offset(size.width - wMidMeasured.size.width, midY),
                    style = axisLabelStyle.copy(color = WeightColor.copy(alpha = 0.6f))
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = wMinLabel,
                    topLeft = Offset(size.width - wMinMeasured.size.width, topPadding + chartHeight - 6.dp.toPx()),
                    style = axisLabelStyle.copy(color = WeightColor)
                )

                // Draw date labels (first, middle, last + next day)
                if (data.size >= 2) {
                    val dateLabelY = topPadding + chartHeight + 4.dp.toPx()
                    val dateLabelStyle = TextStyle(color = labelColor, fontSize = 9.sp)

                    // First date
                    drawText(
                        textMeasurer = textMeasurer,
                        text = data.first().label,
                        topLeft = Offset(leftPadding, dateLabelY),
                        style = dateLabelStyle
                    )

                    // Middle date
                    if (data.size >= 5) {
                        val midIndex = data.size / 2
                        val midLabel = data[midIndex].label
                        val midMeasured = textMeasurer.measure(midLabel, dateLabelStyle)
                        val midX = leftPadding + midIndex * (chartWidth / (data.size - 1))
                        drawText(
                            textMeasurer = textMeasurer,
                            text = midLabel,
                            topLeft = Offset(midX - midMeasured.size.width / 2, dateLabelY),
                            style = dateLabelStyle
                        )
                    }

                    // Last date
                    val lastLabel = data.last().label
                    val lastMeasured = textMeasurer.measure(lastLabel, dateLabelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = lastLabel,
                        topLeft = Offset(
                            leftPadding + chartWidth - lastMeasured.size.width,
                            dateLabelY
                        ),
                        style = dateLabelStyle
                    )

                    // Next day label (right edge, faded)
                    if (nextDateLabel != null) {
                        val nextMeasured = textMeasurer.measure(nextDateLabel, dateLabelStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = nextDateLabel,
                            topLeft = Offset(
                                size.width - nextMeasured.size.width,
                                dateLabelY
                            ),
                            style = TextStyle(color = labelColor.copy(alpha = 0.5f), fontSize = 9.sp)
                        )
                    }
                }

                // Zoom indicator
                if (scale > 1.01f) {
                    val zoomLabel = String.format("×%.1f", scale)
                    val zoomMeasured = textMeasurer.measure(zoomLabel, TextStyle(fontSize = 9.sp))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = zoomLabel,
                        topLeft = Offset(
                            leftPadding + chartWidth / 2 - zoomMeasured.size.width / 2,
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
                if (showCalories) {
                    LegendItem(color = CaloriesColor, label = stringResource(R.string.history_legend_calories))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (showBurned) {
                    LegendItem(color = BurnedColor, label = stringResource(R.string.history_legend_burned))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (showWeight) {
                    LegendItem(color = WeightColor, label = stringResource(R.string.history_legend_weight))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (caloriesGoal != null && caloriesGoal > 0) {
                    LegendItem(color = CaloriesGoalColor, label = stringResource(R.string.history_legend_cal_goal), dashed = true)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (targetWeight != null && targetWeight > 0f) {
                    LegendItem(color = WeightGoalColor, label = stringResource(R.string.history_legend_weight_goal), dashed = true)
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

    for (point in points) {
        drawCircle(color = color, radius = 3.dp.toPx(), center = point)
    }
}
