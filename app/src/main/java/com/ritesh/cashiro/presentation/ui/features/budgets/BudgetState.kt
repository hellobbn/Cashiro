package com.ritesh.cashiro.presentation.ui.features.budgets

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.data.repository.CategoryLimitWithSpending
import com.ritesh.cashiro.data.database.entity.BudgetPeriod
import com.ritesh.cashiro.data.database.entity.BudgetTrackType
import com.ritesh.cashiro.data.database.entity.BudgetType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.PersonInfo
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.time.format.DateTimeFormatter

data class BudgetUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetWithSpending> = emptyList(),
    val selectedBudget: BudgetWithSpending? = null,
    val categoryLimitsWithSpending: List<CategoryLimitWithSpending> = emptyList(),
    val selectedBudgetTransactions: List<TransactionEntity> = emptyList(),
    val allAccounts: List<AccountBalanceEntity> = emptyList(),
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val baseCurrency: String = "CNY",
    val transactionPersonMapping: Map<Long, PersonInfo> = emptyMap(),
    val error: String? = null
)

data class EditBudgetState(
    val budgetId: Long? = null,
    val name: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val year: Int = LocalDateTime.now().year,
    val month: Int = LocalDateTime.now().monthValue,
    val startDate: LocalDateTime = LocalDateTime.now(),
    val endDate: LocalDateTime = LocalDateTime.now().plusMonths(1).minusDays(1),
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val trackType: BudgetTrackType = BudgetTrackType.ALL_TRANSACTIONS,
    val budgetType: BudgetType = BudgetType.EXPENSE,
    val accountIds: List<String> = emptyList(),
    val color: String = "#4CAF50",
    val currency: String = "CNY",
    val categoryLimits: List<EditCategoryLimit> = emptyList()
) {
    val isNewBudget: Boolean get() = budgetId == null
    
    fun getDefaultName(): String {
        return when (periodType) {
            BudgetPeriod.MONTHLY -> {
                val monthName = startDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                "$monthName ${startDate.year}"
            }
            BudgetPeriod.YEARLY -> "${startDate.year} Budget"
            BudgetPeriod.DAILY -> {
                val dateStr = startDate.format(DateTimeFormatter.ofPattern("MMM d"))
                "$dateStr Budget"
            }
            BudgetPeriod.WEEKLY -> {
                val startStr = startDate.format(DateTimeFormatter.ofPattern("MMM d"))
                "Weekly ($startStr)"
            }
            BudgetPeriod.CUSTOM -> "Custom Budget"
        }
    }
}

data class EditCategoryLimit(
    val id: Long? = null,
    val categoryName: String,
    val limitAmount: BigDecimal
)
