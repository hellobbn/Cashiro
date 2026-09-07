package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WalletStyleRowTest {
    @get:Rule val rule = createComposeRule()
    @Test fun nameAndAmountShareTheFirstBaseline() {
        rule.setContent { CashiroTheme(dynamicColor=false) {
            WalletStyleRow("Wallet", "¥420", "CNY", {}, Modifier.width(400.dp))
        } }
        fun baseline(text: String): Float {
            val node = rule.onNodeWithText(text)
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            return node.fetchSemanticsNode().boundsInRoot.top + layouts.single().firstBaseline
        }
        assertEquals(baseline("Wallet"), baseline("¥420"), 1f)
        rule.onNodeWithText("CNY").assertIsDisplayed()
    }
}
