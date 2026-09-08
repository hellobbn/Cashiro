package com.ritesh.cashiro.presentation.ui.features.home

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withTimeoutOrNull

internal data class HomeRecentTransactions(
    val transactions: List<TransactionEntity>,
    val currency: String,
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val hasError: Boolean = false
)

/** Local rows are ready before optional exchange-rate enrichment, including when offline. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeHomeRecentTransactions(
    transactions: Flow<List<TransactionEntity>>,
    currency: StateFlow<String>,
    rateChanges: StateFlow<Long>,
    rate: suspend (String, String) -> BigDecimal?,
    conversionTimeoutMillis: Long = 2_000
): Flow<HomeRecentTransactions> = combine(transactions, currency, rateChanges) { rows, target, _ ->
    HomeRecentTransactions(rows, target)
}.transformLatest { local ->
    emit(local)
    if (local.transactions.none { it.currency != local.currency }) return@transformLatest
    val converted = try {
        withTimeoutOrNull(conversionTimeoutMillis) {
            val rates = local.transactions.map { it.currency }.distinct()
                .filter { it != local.currency }.associateWith { rate(it, local.currency) }
            buildMap {
                local.transactions.forEach { tx ->
                    rates[tx.currency]?.let { exchangeRate ->
                        put(tx.id, tx.amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP))
                    }
                }
            }
        }.orEmpty()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyMap()
    }
    // Missing/failed rates leave the original amount/currency visible, never a mislabeled fallback.
    emit(local.copy(convertedAmounts = converted))
}.catch { emit(HomeRecentTransactions(emptyList(), currency.value, hasError = true)) }
