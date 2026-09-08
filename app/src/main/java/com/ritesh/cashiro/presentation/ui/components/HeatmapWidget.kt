package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalHazeApi::class)
@Composable
fun HeatmapWidget(
    modifier: Modifier = Modifier,
    data: Map<LocalDate, Int>,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val weeksToShow = 26 // Show last 6 months
    val today = LocalDate.now()
    val endDate = today
    // Start from the Monday of N weeks ago
    val startDate = endDate.minusWeeks((weeksToShow - 1).toLong()).with(DayOfWeek.MONDAY)
    
    val monthLabels = remember(startDate, endDate) {
        val allMonthStarts = mutableListOf<Pair<Int, String>>()
        var current = startDate
        var lastMonth = -1
        var weekIndex = 0
        
        while (current <= endDate) {
            if (current.monthValue != lastMonth) {
                val formatter = DateTimeFormatter.ofPattern("MMM")
                allMonthStarts.add(weekIndex to current.format(formatter))
                lastMonth = current.monthValue
            }
            current = current.plusWeeks(1)
            weekIndex++
        }

        val filteredLabels = mutableListOf<Pair<Int, String>>()
        for (i in allMonthStarts.indices) {
            val (week, label) = allMonthStarts[i]
            
            // case for the first label: skip it if it's too close to the second one
            if (i == 0 && allMonthStarts.size > 1) {
                val nextWeek = allMonthStarts[1].first
                if (nextWeek - week < 4) continue
            }
            
            //ensure at least 4 weeks between labels
            if (filteredLabels.isEmpty()) {
                filteredLabels.add(week to label)
            } else {
                val lastAddedWeek = filteredLabels.last().first
                if (week - lastAddedWeek >= 4) {
                    filteredLabels.add(week to label)
                }
            }
        }
        filteredLabels
    }

    val scrollState = rememberScrollState()
    
    // Scroll to end (latest data) on initial load
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val density = LocalDensity.current
    // Match the old per-cell layout rounding exactly, including at fractional densities.
    val cellPx = with(density) { 14.dp.roundToPx() }
    val gapPx = with(density) { 4.dp.roundToPx() }
    val gridWidth = with(density) { (weeksToShow * cellPx + (weeksToShow - 1) * gapPx).toDp() }
    val gridHeight = with(density) { (7 * cellPx + 6 * gapPx).toDp() }
    val primary = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cellColors = remember(data, startDate, today, primary, emptyColor) {
        List(weeksToShow * 7) { index ->
            val date = startDate.plusDays(index.toLong())
            val count = data[date] ?: 0
            when {
                date > today || count <= 0 -> emptyColor
                count == 1 -> primary.copy(alpha = 0.25f)
                count < 3 -> primary.copy(alpha = 0.5f)
                count < 5 -> primary.copy(alpha = 0.75f)
                else -> primary
            }
        }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(Dimensions.Radius.lg))
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        inputScale = HazeInputScale.Auto
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            ),
        shape = RoundedCornerShape(Dimensions.Radius.lg),
        color = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .padding(Dimensions.Padding.content)
                .horizontalScroll(scrollState)
        ) {
            // One draw node instead of 182 clipped Boxes and 26 Column layouts. Dates/colors
            // are cached above, so scrolling does not allocate a new date model for each cell.
            Canvas(Modifier.padding(bottom = 8.dp).size(gridWidth, gridHeight)) {
                val step = (cellPx + gapPx).toFloat()
                val corner = CornerRadius(4.dp.toPx())
                cellColors.forEachIndexed { index, color ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(index / 7 * step, index % 7 * step),
                        size = Size(cellPx.toFloat(), cellPx.toFloat()),
                        cornerRadius = corner
                    )
                }
            }

            // Labels (Months)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                monthLabels.forEach { (weekIndex, label) ->
                    // horizontal offset based on weekIndex
                    val xOffset = with(density) { (weekIndex * (cellPx + gapPx)).toDp() }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.offset(x = xOffset)
                    )
                }
            }
        }
    }
}
