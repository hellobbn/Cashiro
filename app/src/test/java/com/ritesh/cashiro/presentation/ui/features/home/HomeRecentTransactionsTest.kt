package com.ritesh.cashiro.presentation.ui.features.home

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRecentTransactionsTest {
    private fun tx(id: Long = 1, currency: String = "USD") = TransactionEntity(
        id = id, amount = BigDecimal("10.00"), merchantName = "Recent fixture", category = "Shopping",
        transactionType = TransactionType.EXPENSE, dateTime = LocalDateTime.of(2026, 9, 8, 12, 0),
        transactionHash = "recent-$id", currency = currency,
        createdAt = LocalDateTime.of(2026, 9, 8, 12, 0), updatedAt = LocalDateTime.of(2026, 9, 8, 12, 0)
    )

    @Test fun localRowsAppearBeforeHangingRateAndSurviveTimeout() = runTest {
        val values = mutableListOf<HomeRecentTransactions>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeHomeRecentTransactions(MutableStateFlow(listOf(tx())), MutableStateFlow("CNY"), MutableStateFlow(0L),
                rate = { _, _ -> awaitCancellation() }, conversionTimeoutMillis = 100).collect { values += it }
        }
        runCurrent()
        assertEquals(listOf(tx()), values.first().transactions)
        assertFalse(values.first().hasError)
        assertTrue(values.first().convertedAmounts.isEmpty())
        advanceTimeBy(101); runCurrent()
        assertEquals(listOf(tx()), values.last().transactions)
        assertTrue(values.last().convertedAmounts.isEmpty())
    }

    @Test fun currencyChangeCancelsOldEnrichmentWithoutDelayingRows() = runTest {
        val currency = MutableStateFlow("CNY")
        val rows = MutableStateFlow(listOf(tx()))
        val values = mutableListOf<HomeRecentTransactions>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeHomeRecentTransactions(rows, currency, MutableStateFlow(0L), rate = { _, to ->
                if (to == "CNY") awaitCancellation() else BigDecimal("2")
            }).collect { values += it }
        }
        runCurrent()
        currency.value = "EUR"; runCurrent()
        assertEquals("EUR", values.last().currency)
        assertEquals(BigDecimal("20.00"), values.last().convertedAmounts[1L])
        rows.value = emptyList(); runCurrent()
        assertTrue(values.last().transactions.isEmpty())
        assertTrue(values.last().convertedAmounts.isEmpty())
    }

    @Test fun failedOrMissingRateNeverRelabelsOriginalAmount() = runTest {
        for (throwError in listOf(false, true)) {
            val values = mutableListOf<HomeRecentTransactions>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeHomeRecentTransactions(MutableStateFlow(listOf(tx())), MutableStateFlow("CNY"), MutableStateFlow(0L),
                    rate = { _, _ -> if (throwError) error("offline") else null }).collect { values += it }
            }
            runCurrent()
            assertFalse(values.last().hasError)
            assertEquals("USD", values.last().transactions.single().currency)
            assertTrue(values.last().convertedAmounts.isEmpty())
            job.cancel()
        }
    }

    @Test fun databaseFailureIsAnErrorStateRatherThanUnendingLoading() = runTest {
        val state = observeHomeRecentTransactions(flow { throw IllegalStateException("query failed") },
            MutableStateFlow("CNY"), MutableStateFlow(0L), rate = { _, _ -> null }).first()
        assertTrue(state.hasError)
    }

    @Test fun emptyAndSameCurrencyRowsDoNotFetchRates() = runTest {
        var calls = 0
        val rows = MutableStateFlow(listOf(tx(currency = "CNY")))
        val values = mutableListOf<HomeRecentTransactions>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeHomeRecentTransactions(rows, MutableStateFlow("CNY"), MutableStateFlow(0L),
                rate = { _, _ -> calls++; null }).collect { values += it }
        }
        runCurrent(); rows.value = emptyList(); runCurrent()
        assertEquals(0, calls)
        assertFalse(values.last().hasError)
        assertTrue(values.last().transactions.isEmpty())
    }

    @Test fun customRateRefreshRecalculatesWithoutHidingLocalRows() = runTest {
        val trigger = MutableStateFlow(0L)
        var rate = BigDecimal("2")
        val values = mutableListOf<HomeRecentTransactions>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeHomeRecentTransactions(MutableStateFlow(listOf(tx())), MutableStateFlow("CNY"), trigger,
                rate = { _, _ -> rate }).collect { values += it }
        }
        runCurrent(); rate = BigDecimal("3"); trigger.value++; runCurrent()
        assertEquals(BigDecimal("30.00"), values.last().convertedAmounts[1L])
        assertTrue(values.all { it.transactions.size == 1 && !it.hasError })
    }
}
