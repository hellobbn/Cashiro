package com.ritesh.cashiro.presentation.ui.features.transactions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransactionListInteractionTest {
    @get:Rule val rule = createComposeRule()
    private fun fixture(id: Long) = TransactionEntity(
        id = id, amount = BigDecimal("42.50"), merchantName = "Shop $id",
        category = "Shopping", transactionType = TransactionType.EXPENSE,
        dateTime = LocalDateTime.of(2026, 9, 7, 12, 0),
        transactionHash = "list-test-$id", currency = "CNY"
    )

    @Test fun plainRowsScrollAndLocalSheetDismissKeepsPosition() {
        val transactions = (1L..1000L).map(::fixture)
        var listState: androidx.compose.foundation.lazy.LazyListState? = null
        rule.setContent { CashiroTheme(dynamicColor = false) {
            val state = rememberLazyListState()
            listState = state
            var selected by remember { mutableStateOf<TransactionEntity?>(null) }
            LazyColumn(Modifier.fillMaxSize().testTag("list"), state = state) {
                items(transactions, key = { it.id }, contentType = { "transaction" }) { tx ->
                    TransactionItem(transaction = tx, onClick = { selected = tx })
                }
            }
            selected?.let { tx -> TransactionSummarySheet(tx, { selected = null }, { selected = null }) }
        } }
        rule.onNodeWithTag("list").performScrollToIndex(150)
        var index = 0
        rule.runOnIdle { index = listState!!.firstVisibleItemIndex; assertTrue(index > 0) }
        rule.onNodeWithText("Shop ${index + 1}").performClick()
        rule.onNodeWithText("CNY", useUnmergedTree = true).assertIsDisplayed()
        val close = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.close)
        rule.onNodeWithText(close).performClick()
        rule.runOnIdle { assertEquals(index, listState!!.firstVisibleItemIndex) }
        rule.onNodeWithTag("list").performTouchInput { swipeUp() }
        rule.runOnIdle { assertTrue(listState!!.firstVisibleItemIndex > index) }
    }

    @Test fun sheetDetailsButtonUsesSelectedTransaction() {
        var opened: Long? = null
        rule.setContent { CashiroTheme(dynamicColor = false) {
            val tx = fixture(27)
            TransactionSummarySheet(tx, {}, { opened = tx.id })
        } }
        val details = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.transaction_details)
        rule.onNodeWithText(details).performClick()
        rule.runOnIdle { assertEquals(27L, opened) }
    }
}
