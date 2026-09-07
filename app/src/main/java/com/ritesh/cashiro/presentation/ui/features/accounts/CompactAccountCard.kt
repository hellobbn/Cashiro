package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.presentation.ui.theme.AccountSurfaceElevation
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.utils.CurrencyFormatter

/** Compact account list treatment; detailed actions and supplementary content stay available. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CompactAccountCard(
    account: AccountBalanceEntity,
    isHidden: Boolean,
    isMain: Boolean,
    onClick: () -> Unit,
    onUpdateBalance: () -> Unit,
    onEditAccount: () -> Unit,
    onViewHistory: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSetAsMain: () -> Unit,
    onMergeAccount: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = AccountSurfaceElevation)
    ) {
        WalletStyleRow(
            title = account.bankName,
            amount = CurrencyFormatter.formatCurrency(account.balance, account.currency),
            subtitle = account.currency + (if (!account.isWallet && account.accountLast4.isNotBlank()) " · •• " + account.accountLast4 else "") + (if (isMain) " · " + stringResource(R.string.main) else ""),
            icon = { BrandIcon(merchantName = account.bankName, size = 24.dp, showBackground = false,
                accountIconResId = account.iconResId, accountIconName = account.iconName, accountColorHex = account.color) },
            trailing = {
            Box {
                IconButton(onClick = { menuExpanded = true }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.more_options)) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    val actions = listOf(
                        R.string.update_balance to onUpdateBalance,
                        R.string.edit_details to onEditAccount,
                        R.string.history to onViewHistory,
                        R.string.merge_account to onMergeAccount,
                        (if (isHidden) R.string.show else R.string.hide) to onToggleVisibility
                    )
                    actions.forEach { (label, action) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { menuExpanded = false; action() })
                    }
                    if (!isMain) DropdownMenuItem(text = { Text(stringResource(R.string.set_as_main)) }, onClick = { menuExpanded = false; onSetAsMain() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }, onClick = { menuExpanded = false; onDeleteAccount() })
                }
            }
            }
        )
        content()
    }
}
