package com.ritesh.cashiro.presentation.ui.features.analytics

import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Internal state for combining all filter parameters.
 * Used in reactive Flow to trigger data reload when any filter changes.
 */
data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: Set<TransactionTypeFilter>,
    val currency: String?
)

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val currency: String = "CNY",
    val baseCurrency: String = "CNY",
    val isLoading: Boolean = true,
    val spendingTrend: List<BalancePoint> = emptyList(),
    val convertedMerchantAmounts: Map<String, BigDecimal> = emptyMap()
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val accountIconName: String? = null
)
