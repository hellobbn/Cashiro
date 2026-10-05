package com.ritesh.cashiro.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ritesh.cashiro.data.database.entity.TransactionType

/**
 * Whether the app is drawing its dark scheme. The app's own theme setting can differ from the
 * system's, so read it from the color scheme rather than isSystemInDarkTheme().
 */
val isAppInDarkTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** The color of an amount of this type, in the light or dark variant the app is showing. */
@Composable
@ReadOnlyComposable
fun transactionTypeColor(type: TransactionType): Color {
    val dark = isAppInDarkTheme
    return when (type) {
        TransactionType.INCOME, TransactionType.BORROWED -> if (dark) income_dark else income_light
        TransactionType.EXPENSE, TransactionType.LENT -> if (dark) expense_dark else expense_light
        TransactionType.CREDIT -> if (dark) credit_dark else credit_light
        TransactionType.TRANSFER, TransactionType.BALANCE_UPDATE -> if (dark) transfer_dark else transfer_light
        TransactionType.INVESTMENT -> if (dark) investment_dark else investment_light
    }
}

/** Positive / caution colors for meters and statuses (utilization, overdue), theme-aware. */
val successColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme) success_dark else success_light

val warningColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme) warning_dark else warning_light
