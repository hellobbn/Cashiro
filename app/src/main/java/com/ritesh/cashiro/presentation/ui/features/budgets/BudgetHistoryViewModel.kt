package com.ritesh.cashiro.presentation.ui.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.BudgetPeriod
import com.ritesh.cashiro.data.database.entity.BudgetType
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BudgetPeriodHistory(
    val name: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val amount: BigDecimal,
    val spent: BigDecimal,
    val currency: String
) {
    val percentUsed: Float get() = if (amount > BigDecimal.ZERO) {
        (spent.toFloat() / amount.toFloat()).coerceIn(0f, 1.1f)
    } else 0f
    
    val isOverBudget: Boolean get() = spent > amount
}

data class BudgetHistoryUiState(
    val isLoading: Boolean = false,
    val budget: BudgetEntity? = null,
    val periods: List<BudgetPeriodHistory> = emptyList(),
    val chartPoints: List<BalancePoint> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BudgetHistoryViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetHistoryUiState())
    val uiState: StateFlow<BudgetHistoryUiState> = _uiState.asStateFlow()

    fun loadBudgetHistory(budgetId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val budget = budgetRepository.getBudgetById(budgetId)
                if (budget != null) {
                    val periods = calculateHistoricalPeriods(budget)
                    val historyWithSpending = periods.map { period ->
                        val spending = calculateSpendingForPeriod(budget, period.first, period.second)
                        BudgetPeriodHistory(
                            name = formatPeriodName(period.first, period.second, budget.periodType),
                            startDate = period.first,
                            endDate = period.second,
                            amount = budget.amount,
                            spent = spending,
                            currency = budget.currency
                        )
                    }

                    val chartPoints = historyWithSpending.reversed().map {
                        BalancePoint(
                            timestamp = it.startDate,
                            balance = it.spent,
                            currency = it.currency
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budget = budget,
                            periods = historyWithSpending,
                            chartPoints = chartPoints
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Budget not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load history") }
            }
        }
    }

    /** The budget's periods, newest first, back to its first (see [BudgetPeriods]). */
    private fun calculateHistoricalPeriods(budget: BudgetEntity): List<Pair<LocalDateTime, LocalDateTime>> =
        com.ritesh.cashiro.domain.model.BudgetPeriods.history(budget).map { it.from to it.to }

    private suspend fun calculateSpendingForPeriod(
        budget: BudgetEntity,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BigDecimal {
        // Create a temporary budget entity for calculation
        // That period alone, as a one-off budget
        val tempBudget = budget.copy(
            startDate = startDate,
            endDate = endDate,
            periodType = BudgetPeriod.CUSTOM
        )
        val spending = budgetRepository.getBudgetWithSpending(tempBudget)
        return spending.currentSpending
    }

    private fun formatPeriodName(start: LocalDateTime, end: LocalDateTime, periodType: BudgetPeriod): String {
        val now = LocalDateTime.now()
        if (!now.isBefore(start) && !now.isAfter(end)) return context.getString(com.ritesh.cashiro.R.string.budget_current_period)
        val formats = com.ritesh.cashiro.utils.DateFormats
        return when (periodType) {
            BudgetPeriod.DAILY -> formats.shortDate(start.toLocalDate())
            BudgetPeriod.YEARLY -> start.year.toString()
            BudgetPeriod.MONTHLY -> if (start.dayOfMonth == 1) formats.yearMonth(start)
                else "${formats.shortDate(start.toLocalDate())} – ${formats.shortDate(end.toLocalDate())}"
            else -> "${formats.shortDate(start.toLocalDate())} – ${formats.shortDate(end.toLocalDate())}"
        }
    }
}
