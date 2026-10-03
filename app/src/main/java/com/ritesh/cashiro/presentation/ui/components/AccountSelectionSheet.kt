package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter

/**
 * Picker for an account: one compact row per account (icon, name, last four digits and
 * balance), so a typical list fits on screen without scrolling. The full [AccountCard] is
 * for the Accounts screen.
 */
@Composable
fun AccountSelectionSheet(
    accounts: List<AccountBalanceEntity>,
    selectedAccount: AccountBalanceEntity?,
    title: String = stringResource(R.string.select_account),
    onAccountSelected: (AccountBalanceEntity?) -> Unit,
    isTransitioning: Boolean = false,
    showNoneOption: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 12.dp).fillMaxWidth()
        )

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_accounts_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                userScrollEnabled = !isTransitioning
            ) {
                if (showNoneOption) {
                    item {
                        AccountOptionRow(
                            selected = selectedAccount == null,
                            onClick = { onAccountSelected(null) }
                        ) {
                            Text(
                                text = stringResource(R.string.none_manual_entry),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                items(accounts, key = { it.id }) { account ->
                    AccountOptionRow(
                        selected = selectedAccount?.id == account.id,
                        onClick = { onAccountSelected(account) }
                    ) {
                        BrandIcon(
                            merchantName = account.bankName,
                            size = 40.dp,
                            showBackground = true,
                            accountIconResId = account.iconResId,
                            accountIconName = account.iconName,
                            accountColorHex = account.color
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.bankName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (account.isWallet) stringResource(R.string.type_wallet)
                                else "•••• ${account.accountLast4}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(Spacing.sm))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (account.isCreditCard) {
                                Text(
                                    text = stringResource(R.string.outstanding_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = Spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            if (selected) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
