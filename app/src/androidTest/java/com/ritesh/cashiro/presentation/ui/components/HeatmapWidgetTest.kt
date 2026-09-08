package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HeatmapWidgetTest {
    @get:Rule val rule = createComposeRule()

    @Test fun gridPreservesCellGeometryBucketsAndDataUpdates() {
        val start = LocalDate.now().minusWeeks(25).with(DayOfWeek.MONDAY)
        val data = mutableStateOf(mapOf(start to 1, start.plusDays(1) to 2, start.plusDays(2) to 4, start.plusDays(3) to 5))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color.Red, surfaceContainerLow = Color.Black, surfaceContainerHigh = Color.White)) {
                    Box(Modifier.width(600.dp).padding(top = 160.dp)) {
                        HeatmapWidget(Modifier.testTag("heatmap"), data.value, blurEffects = false)
                    }
                }
            }
        }
        val before = rule.onNodeWithTag("heatmap").captureToImage().toPixelMap()
        // Outer padding 16 + inner padding 16 + 7px to the center of the 14px cell.
        for ((day, red) in listOf(0 to 0.25f, 1 to 0.5f, 2 to 0.75f, 3 to 1f)) {
            val color = before[39, 23 + day * 18]
            assertEquals(red, color.red, 0.02f)
            assertEquals(0f, color.green, 0.02f)
        }
        assertEquals(1f, before[39, 23 + 4 * 18].green, 0.02f) // Empty day uses the empty-cell color.
        rule.runOnIdle { data.value = data.value + (start to 5) }
        val after = rule.onNodeWithTag("heatmap").captureToImage().toPixelMap()
        assertEquals(1f, after[39, 23].red, 0.02f)
        assertEquals(0f, after[39, 23].green, 0.02f)
    }
}
