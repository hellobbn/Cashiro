package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BalanceCurrencySelectorTest {
    @get:Rule val rule = createComposeRule()

    @Test fun nativeCurrencyActionIsVisibleAndClickable() {
        var clicks = 0
        rule.setContent { MaterialTheme { BalanceCurrencySelector("CNY") { clicks++ } } }
        rule.onNodeWithText("CNY").assertIsDisplayed().assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun largeTextFitsNarrowLayoutAndPreservesAction() {
        var clicks = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                MaterialTheme {
                    Column(Modifier.width(280.dp)) { BalanceCurrencySelector("HKD") { clicks++ } }
                }
            }
        }
        rule.onNodeWithText("HKD").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }
}
