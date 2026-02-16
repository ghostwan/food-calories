package com.ghostwan.snapcal.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
    caloriesOrigin: Int = 0,
    weightOrigin: Int = 60,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var chartWidthPx by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f && chartWidthPx > 0f && data.size > 1) {
            val baseStep = chartWidthPx / (data.size - 1)
            val totalScaledWidth = (data.size - 1) * baseStep * scale
            val minOffset = -(totalScaledWidth - chartWidthPx).coerceAtLeast(0f)
            offsetX = (offsetX + panChange.x).coerceIn(minOffset, 0f)
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
                    .pointerInput(data.size, scale, offsetX) {
                        detectTapGestures { tapOffset ->
                            val lp = 40.dp.toPx()
                            val rp = 40.dp.toPx()
                            val cw = size.width - lp - rp
                            if (data.size < 2) return@detectTapGestures
                            val baseStep = cw / (data.size - 1)
                            val sStep = baseStep * scale
                            val tapDataX = tapOffset.x - lp - offsetX
                            val idx = (tapDataX / sStep + 0.5f).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (idx == selectedIndex) -1 else idx
                        }
                    }
                    .transformable(state = transformableState)
            ) {
                val leftPadding = 40.dp.toPx()
                val rightPadding = 40.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                chartWidthPx = chartWidth

                val step = if (data.size > 1) chartWidth / (data.size - 1) else 0f
                val scaledStep = step * scale

                val caloriesValues = if (showCalories) data.mapNotNull { it.calories } else emptyList()
                val weightValues = if (showWeight) data.mapNotNull { it.weight } else emptyList()
                val burnedValues = if (showBurned) data.mapNotNull { it.burnedCalories } else emptyList()

                if (caloriesValues.isEmpty() && weightValues.isEmpty() && burnedValues.isEmpty()) return@Canvas

                // Calculate ranges — include goals and burned
                val allCalValues = caloriesValues + burnedValues
                var calMaxRaw = (allCalValues.maxOrNull() ?: 2500).toFloat()
                if (caloriesGoal != null && caloriesGoal > 0) {
                    calMaxRaw = maxOf(calMaxRaw, caloriesGoal.toFloat())
                }
                val calMin = caloriesOrigin.toFloat()
                val calMax = maxOf(calMaxRaw * 1.1f, calMin + 100f)
                val calRange = (calMax - calMin).coerceAtLeast(100f)

                var wMaxRaw = weightValues.maxOrNull() ?: 80f
                if (targetWeight != null && targetWeight > 0f) {
                    wMaxRaw = maxOf(wMaxRaw, targetWeight)
                }
                val wMin = weightOrigin.toFloat()
                val wMax = maxOf(wMaxRaw * 1.1f, wMin + 5f)
                val wRange = (wMax - wMin).coerceAtLeast(5f)

                // Draw goal lines (fixed, not affected by zoom)
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

                // Draw curves clipped to chart area
                clipRect(leftPadding, 0f, leftPadding + chartWidth, size.height) {
                    if (showCalories) {
                        drawCurve(
                            data = data,
                            getValue = { it.calories?.toFloat() },
                            color = CaloriesColor,
                            minVal = calMin,
                            range = calRange,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            scaledStep = scaledStep,
                            panOffsetX = offsetX,
                            chartHeight = chartHeight
                        )
                    }
                    if (showBurned) {
                        drawCurve(
                            data = data,
                            getValue = { it.burnedCalories?.toFloat() },
                            color = BurnedColor,
                            minVal = calMin,
                            range = calRange,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            scaledStep = scaledStep,
                            panOffsetX = offsetX,
                            chartHeight = chartHeight
                        )
                    }
                    if (showWeight) {
                        drawCurve(
                            data = data,
                            getValue = { it.weight },
                            color = WeightColor,
                            minVal = wMin,
                            range = wRange,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            scaledStep = scaledStep,
                            panOffsetX = offsetX,
                            chartHeight = chartHeight
                        )
                    }
                }

                // Draw axis labels (fixed position, outside chart area)
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

                // Date labels (move with zoom/pan)
                if (data.size >= 2) {
                    val dateLabelY = topPadding + chartHeight + 4.dp.toPx()
                    val dateLabelStyle = TextStyle(color = labelColor, fontSize = 9.sp)

                    val approxLabelWidth = 35.dp.toPx()
                    val maxLabels = (chartWidth / approxLabelWidth).toInt().coerceAtLeast(2)
                    val labelInterval = maxOf(1, data.size / maxLabels)

                    for (i in data.indices step labelInterval) {
                        val x = leftPadding + i * scaledStep + offsetX
                        if (x < leftPadding - 20.dp.toPx() || x > leftPadding + chartWidth + 20.dp.toPx()) continue
                        val label = data[i].label
                        val measured = textMeasurer.measure(label, dateLabelStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(x - measured.size.width / 2, dateLabelY),
                            style = dateLabelStyle
                        )
                    }
                    // Always show last label if visible and not already drawn
                    if ((data.size - 1) % labelInterval != 0) {
                        val lastX = leftPadding + (data.size - 1) * scaledStep + offsetX
                        if (lastX >= leftPadding && lastX <= leftPadding + chartWidth + 20.dp.toPx()) {
                            val lastLabel = data.last().label
                            val lastMeasured = textMeasurer.measure(lastLabel, dateLabelStyle)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = lastLabel,
                                topLeft = Offset(lastX - lastMeasured.size.width / 2, dateLabelY),
                                style = dateLabelStyle
                            )
                        }
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

                // Selected point tooltip
                if (selectedIndex in data.indices) {
                    val sel = data[selectedIndex]
                    val screenX = leftPadding + selectedIndex * scaledStep + offsetX

                    // Vertical line
                    drawLine(
                        color = labelColor.copy(alpha = 0.3f),
                        start = Offset(screenX, topPadding),
                        end = Offset(screenX, topPadding + chartHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    // Highlight circles
                    if (showCalories && sel.calories != null) {
                        val y = topPadding + chartHeight * (1f - (sel.calories.toFloat() - calMin) / calRange)
                        drawCircle(color = CaloriesColor, radius = 6.dp.toPx(), center = Offset(screenX, y))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(screenX, y))
                    }
                    if (showBurned && sel.burnedCalories != null) {
                        val y = topPadding + chartHeight * (1f - (sel.burnedCalories.toFloat() - calMin) / calRange)
                        drawCircle(color = BurnedColor, radius = 6.dp.toPx(), center = Offset(screenX, y))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(screenX, y))
                    }
                    if (showWeight && sel.weight != null) {
                        val y = topPadding + chartHeight * (1f - (sel.weight - wMin) / wRange)
                        drawCircle(color = WeightColor, radius = 6.dp.toPx(), center = Offset(screenX, y))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(screenX, y))
                    }

                    // Build tooltip text
                    val tooltipParts = mutableListOf<String>()
                    tooltipParts.add(sel.label)
                    if (showCalories && sel.calories != null) tooltipParts.add("${sel.calories} kcal")
                    if (showBurned && sel.burnedCalories != null) tooltipParts.add("\uD83D\uDD25 ${sel.burnedCalories}")
                    if (showWeight && sel.weight != null) tooltipParts.add("\u2696 ${String.format("%.1f", sel.weight)} kg")
                    val tooltipText = tooltipParts.joinToString("  \u00B7  ")
                    val tooltipStyle = TextStyle(fontSize = 10.sp, color = Color.White)
                    val tooltipMeasured = textMeasurer.measure(tooltipText, tooltipStyle)
                    val tooltipW = tooltipMeasured.size.width + 16.dp.toPx()
                    val tooltipH = tooltipMeasured.size.height + 8.dp.toPx()
                    val tooltipX = (screenX - tooltipW / 2).coerceIn(0f, size.width - tooltipW)
                    val tooltipY = (topPadding - tooltipH - 4.dp.toPx()).coerceAtLeast(0f)

                    drawRoundRect(
                        color = Color(0xCC333333.toInt()),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipW, tooltipH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = tooltipText,
                        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx()),
                        style = tooltipStyle
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
    scaledStep: Float,
    panOffsetX: Float,
    chartHeight: Float
) {
    val points = mutableListOf<Offset>()

    for (i in data.indices) {
        val value = getValue(data[i]) ?: continue
        val x = leftPadding + i * scaledStep + panOffsetX
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
