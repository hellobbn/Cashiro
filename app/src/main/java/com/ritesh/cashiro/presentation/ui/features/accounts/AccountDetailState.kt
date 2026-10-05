package com.ritesh.cashiro.presentation.ui.features.accounts

import com.ritesh.cashiro.data.currency.Conversions
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.PersonInfo
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import java.math.BigDecimal

data class AccountDetailUiState(
    val bankName: String = "",
    val accountLast4: String = "",
    val currentBalance: AccountBalanceEntity? = null,
    val balanceHistory: List<AccountBalanceEntity> = emptyList(),
    val balanceChartData: List<BalancePoint> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val primaryCurrency: String = "CNY",
    val baseCurrency: String = "CNY",
    val hasMultipleCurrencies: Boolean = false,
    // Transactions in the main currency, where a rate is known
    val conversions: Conversions = Conversions(),
    // An account holding several currencies is shown one currency at a time
    val currencies: List<String> = emptyList(),
    val selectedCurrency: String? = null,
    val isLoading: Boolean = true
)

enum class DateRange(val label: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    ALL_TIME("All Time")
}
