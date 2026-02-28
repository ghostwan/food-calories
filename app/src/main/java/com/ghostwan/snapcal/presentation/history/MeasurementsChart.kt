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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.BodyMeasurement

data class MeasurementChartPoint(
    val label: String,
    val waist: Float?,
    val hips: Float?,
    val chest: Float?,
    val arms: Float?,
    val thighs: Float?
)

private val WaistColor = Color(0xFFE91E63)   // Rose
private val HipsColor = Color(0xFF9C27B0)    // Violet
private val ChestColor = Color(0xFF2196F3)   // Bleu
private val ArmsColor = Color(0xFF4CAF50)    // Vert
private val ThighsColor = Color(0xFFFF9800)  // Orange

@Composable
fun MeasurementsChart(
    data: List<MeasurementChartPoint>,
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
                text = stringResource(R.string.history_measurements),
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
                            val rp = 16.dp.toPx()
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
                val rightPadding = 16.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                chartWidthPx = chartWidth

                val step = if (data.size > 1) chartWidth / (data.size - 1) else 0f
                val scaledStep = step * scale

                // Gather all values to compute Y range
                val allValues = data.flatMap {
                    listOfNotNull(it.waist, it.hips, it.chest, it.arms, it.thighs)
                }
                if (allValues.isEmpty()) return@Canvas

                val rawMin = allValues.min()
                val rawMax = allValues.max()
                val padding = ((rawMax - rawMin) * 0.1f).coerceAtLeast(2f)
                val yMin = (rawMin - padding).coerceAtLeast(0f)
                val yMax = rawMax + padding
                val yRange = (yMax - yMin).coerceAtLeast(5f)

                // Draw curves
                clipRect(leftPadding, 0f, leftPadding + chartWidth, size.height) {
                    drawMeasurementCurve(data, { it.waist }, WaistColor, yMin, yRange, leftPadding, topPadding, scaledStep, offsetX, chartHeight)
                    drawMeasurementCurve(data, { it.hips }, HipsColor, yMin, yRange, leftPadding, topPadding, scaledStep, offsetX, chartHeight)
                    drawMeasurementCurve(data, { it.chest }, ChestColor, yMin, yRange, leftPadding, topPadding, scaledStep, offsetX, chartHeight)
                    drawMeasurementCurve(data, { it.arms }, ArmsColor, yMin, yRange, leftPadding, topPadding, scaledStep, offsetX, chartHeight)
                    drawMeasurementCurve(data, { it.thighs }, ThighsColor, yMin, yRange, leftPadding, topPadding, scaledStep, offsetX, chartHeight)
                }

                // Y-axis labels (cm)
                val axisLabelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
                val yMaxLabel = "${yMax.toInt()} cm"
                val yMidLabel = "${((yMax + yMin) / 2).toInt()} cm"
                val yMinLabel = "${yMin.toInt()} cm"
                val midY = topPadding + chartHeight / 2 - 6.dp.toPx()

                drawText(textMeasurer, yMaxLabel, Offset(0f, topPadding - 6.dp.toPx()), axisLabelStyle)
                drawText(textMeasurer, yMidLabel, Offset(0f, midY), axisLabelStyle.copy(color = labelColor.copy(alpha = 0.6f)))
                drawText(textMeasurer, yMinLabel, Offset(0f, topPadding + chartHeight - 6.dp.toPx()), axisLabelStyle)

                // Date labels
                if (data.size >= 2) {
                    val dateLabelY = topPadding + chartHeight + 4.dp.toPx()
                    val dateLabelStyle = TextStyle(color = labelColor, fontSize = 9.sp)
                    val approxLabelWidth = 35.dp.toPx()
                    val maxLabels = (chartWidth / approxLabelWidth).toInt().coerceAtLeast(2)
                    val labelInterval = maxOf(1, data.size / maxLabels)

                    for (i in data.indices step labelInterval) {
                        val x = leftPadding + i * scaledStep + offsetX
                        if (x < leftPadding - 20.dp.toPx() || x > leftPadding + chartWidth + 20.dp.toPx()) continue
                        val measured = textMeasurer.measure(data[i].label, dateLabelStyle)
                        drawText(textMeasurer, data[i].label, Offset(x - measured.size.width / 2, dateLabelY), dateLabelStyle)
                    }
                    if ((data.size - 1) % labelInterval != 0) {
                        val lastX = leftPadding + (data.size - 1) * scaledStep + offsetX
                        if (lastX >= leftPadding && lastX <= leftPadding + chartWidth + 20.dp.toPx()) {
                            val lastMeasured = textMeasurer.measure(data.last().label, dateLabelStyle)
                            drawText(textMeasurer, data.last().label, Offset(lastX - lastMeasured.size.width / 2, dateLabelY), dateLabelStyle)
                        }
                    }
                }

                // Zoom indicator
                if (scale > 1.01f) {
                    val zoomLabel = String.format("×%.1f", scale)
                    val zoomMeasured = textMeasurer.measure(zoomLabel, TextStyle(fontSize = 9.sp))
                    drawText(
                        textMeasurer, zoomLabel,
                        Offset(leftPadding + chartWidth / 2 - zoomMeasured.size.width / 2, topPadding + chartHeight + 4.dp.toPx()),
                        TextStyle(color = labelColor, fontSize = 9.sp)
                    )
                }

                // Selected point tooltip
                if (selectedIndex in data.indices) {
                    val sel = data[selectedIndex]
                    val screenX = leftPadding + selectedIndex * scaledStep + offsetX

                    drawLine(
                        color = labelColor.copy(alpha = 0.3f),
                        start = Offset(screenX, topPadding),
                        end = Offset(screenX, topPadding + chartHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    // Highlight circles
                    val seriesData = listOf(
                        sel.waist to WaistColor,
                        sel.hips to HipsColor,
                        sel.chest to ChestColor,
                        sel.arms to ArmsColor,
                        sel.thighs to ThighsColor
                    )
                    for ((value, color) in seriesData) {
                        if (value != null) {
                            val y = topPadding + chartHeight * (1f - (value - yMin) / yRange)
                            drawCircle(color = color, radius = 6.dp.toPx(), center = Offset(screenX, y))
                            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(screenX, y))
                        }
                    }

                    // Tooltip
                    val parts = mutableListOf(sel.label)
                    if (sel.waist != null) parts.add("${String.format("%.0f", sel.waist)}")
                    if (sel.hips != null) parts.add("${String.format("%.0f", sel.hips)}")
                    if (sel.chest != null) parts.add("${String.format("%.0f", sel.chest)}")
                    if (sel.arms != null) parts.add("${String.format("%.0f", sel.arms)}")
                    if (sel.thighs != null) parts.add("${String.format("%.0f", sel.thighs)}")
                    val tooltipText = parts.joinToString("  ·  ")
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
                    drawText(textMeasurer, tooltipText, Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx()), tooltipStyle)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MeasurementLegendItem(color = WaistColor, label = stringResource(R.string.history_waist))
                Spacer(modifier = Modifier.width(8.dp))
                MeasurementLegendItem(color = HipsColor, label = stringResource(R.string.history_hips))
                Spacer(modifier = Modifier.width(8.dp))
                MeasurementLegendItem(color = ChestColor, label = stringResource(R.string.history_chest))
                Spacer(modifier = Modifier.width(8.dp))
                MeasurementLegendItem(color = ArmsColor, label = stringResource(R.string.history_arms))
                Spacer(modifier = Modifier.width(8.dp))
                MeasurementLegendItem(color = ThighsColor, label = stringResource(R.string.history_thighs))
            }
        }
    }
}

@Composable
private fun MeasurementLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(16.dp, 3.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun DrawScope.drawMeasurementCurve(
    data: List<MeasurementChartPoint>,
    getValue: (MeasurementChartPoint) -> Float?,
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

    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

    for (point in points) {
        drawCircle(color = color, radius = 3.dp.toPx(), center = point)
    }
}
