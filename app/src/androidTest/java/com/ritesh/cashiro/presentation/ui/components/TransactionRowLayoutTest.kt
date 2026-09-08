package com.ritesh.cashiro.presentation.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import com.ritesh.cashiro.utils.formatAmount
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransactionRowLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val transaction = TransactionEntity(
        id = 1, amount = BigDecimal("42.50"), merchantName = "Layout test merchant",
        category = "Shopping", transactionType = TransactionType.EXPENSE,
        dateTime = LocalDateTime.of(2026, 9, 7, 12, 0), transactionHash = "row-layout-fixture", currency = "CNY"
    )

    @Test fun groupedDateVisibilityAndLocaleUpdatesAreRespected() {
        val showDate = mutableStateOf(false)
        val locale = mutableStateOf(Locale.US)
        val override = mutableStateOf<String?>(null)
        val cardStyle = mutableStateOf(false)
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale.value) }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                CashiroTheme(dynamicColor = false) {
                    TransactionItem(transaction = transaction, showDate = showDate.value, subtitleOverride = override.value, useCardStyle = cardStyle.value)
                }
            }
        }
        rule.onNodeWithText("Sep 7", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { showDate.value = true }
        rule.onNodeWithText("Sep 7", useUnmergedTree = true).assertIsDisplayed()
        rule.runOnIdle { locale.value = Locale.SIMPLIFIED_CHINESE }
        rule.onNodeWithText("9月7日", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Sep 7", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { override.value = "Statement note" }
        rule.onNodeWithText("Statement note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("9月7日", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { cardStyle.value = true }
        rule.onNodeWithText("Statement note", useUnmergedTree = true).assertIsDisplayed()
        rule.runOnIdle { override.value = null }
        rule.onNodeWithText("9月7日", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun iconlessTagPreservesText() {
        rule.setContent {
            CashiroTheme(dynamicColor = false) {
                SubtitleTag("Shopping", androidx.compose.ui.graphics.Color.Blue)
            }
        }
        rule.onNodeWithText("Shopping", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun longMerchantUsesBoundedTwoLinesAndKeepsAmountAndClick() {
        val title = "A very long merchant name that should wrap within the row instead of measuring and scrolling an unbounded line"
        val tx = transaction.copy(merchantName = title)
        var clicks = 0
        rule.setContent {
            CashiroTheme(dynamicColor = false) {
                Box(Modifier.width(340.dp)) {
                    TransactionItem(transaction = tx, showDate = false, onClick = { clicks++ })
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(title, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(2, layouts.single().lineCount)
        rule.onNodeWithText(tx.formatAmount(), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(title).performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }
}
