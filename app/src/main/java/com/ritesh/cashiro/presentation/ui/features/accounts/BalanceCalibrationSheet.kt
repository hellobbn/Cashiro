@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.repository.LocalAccountHoldings
import com.ritesh.cashiro.presentation.ui.components.AccountCurrencyChoice
import com.ritesh.cashiro.presentation.ui.components.CashiroModalBottomSheet
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Sets an account's balance (a card's amount owed) to what the bank shows. An account holding
 * several currencies is set one currency at a time, starting with its main one.
 */
@Composable
fun BalanceCalibrationSheet(
    account: AccountBalanceEntity,
    onDismiss: () -> Unit,
    onSave: (balance: BigDecimal, currency: String) -> Unit
) {
    CashiroModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val pockets = account.accountId?.let { LocalAccountHoldings.current[it] }?.pockets.orEmpty()
        var currency by remember(account.id) { mutableStateOf(account.currency) }
        val current = pockets.firstOrNull { it.currency == currency }?.balance ?: account.balance
        val title = stringResource(if (account.isCreditCard) R.string.update_outstanding_title else R.string.balance_calibration)
        Column {
            if (pockets.size > 1) {
                AccountCurrencyChoice(
                    currencies = listOf(account.currency) + pockets.map { it.currency }.filter { it != account.currency },
                    selected = currency,
                    onSelect = { currency = it },
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }
            key(currency) {
                NumberPad(
                    initialValue = current.toPlainString(),
                    title = title,
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    doneButtonLabel = title,
                    onDone = { value ->
                        value.toBigDecimalOrNull()?.let { onSave(it, currency) }
                        onDismiss()
                    }
                )
            }
        }
    }
}
