package com.ritesh.cashiro.presentation.ui.features.home

import com.ritesh.cashiro.data.currency.Conversions
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.domain.model.PersonInfo
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import java.math.BigDecimal

data class HomeUiState(
    val currentMonthIncome: BigDecimal = BigDecimal.ZERO,
    val currentMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val lastMonthIncome: BigDecimal = BigDecimal.ZERO,
    val lastMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val upcomingSubscriptionsTotal: BigDecimal = BigDecimal.ZERO,
    val upcomingSubscriptionsCurrency: String = "CNY",
    val accountBalances: List<AccountBalanceEntity> = emptyList(),
    val creditCards: List<AccountBalanceEntity> = emptyList(),
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val totalAvailableCredit: BigDecimal = BigDecimal.ZERO,
    val selectedCurrency: String = "CNY",
    val availableCurrencies: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "User",
    val profileImageUri: Uri? = null,
    val profileBackgroundColor: Color = Color.Transparent,
    val bannerImageUri: Uri? = null,
    val showBannerImage: Boolean = false,
    val activeBudgets: List<BudgetWithSpending> = emptyList(),
    val balanceHistory: List<BalancePoint> = emptyList(),
    val transactionHeatmap: Map<java.time.LocalDate, Int> = emptyMap(),
    // Transactions in the main currency, where a rate is known
    val conversions: Conversions = Conversions(),
    val baseCurrency: String = "CNY",
    val lendBorrowSummary: com.ritesh.cashiro.domain.model.LendBorrowSummary = com.ritesh.cashiro.domain.model.LendBorrowSummary()
)
