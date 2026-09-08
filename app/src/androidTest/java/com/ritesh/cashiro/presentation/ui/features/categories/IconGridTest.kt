package com.ritesh.cashiro.presentation.ui.features.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IconGridTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeCategoryIsLazyAndSelectionSurvivesFiltering() {
        val all = List(1000) { IconItem("Icon $it", "Test", "icon_$it", R.drawable.ic_launcher_foreground) }
        val icons = mutableStateOf(all)
        val selected = mutableStateOf<String?>(null)
        rule.setContent {
            CashiroTheme(dynamicColor = false) {
                Box(Modifier.size(320.dp, 400.dp)) {
                    IconGrid(icons.value, selected.value) { selected.value = it }
                }
            }
        }
        rule.onNodeWithContentDescription("Icon 999").assertDoesNotExist()
        rule.onNodeWithContentDescription("Icon 0").performClick()
        rule.runOnIdle { assertEquals("icon_0", selected.value); icons.value = listOf(all.first(), all.first(), all.first().copy(name = "Alias"), all.last()) }
        rule.onNodeWithContentDescription("Icon 999").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("icon_999", selected.value); icons.value = emptyList() }
        rule.onNodeWithContentDescription("Icon 999").assertDoesNotExist()
    }
}
