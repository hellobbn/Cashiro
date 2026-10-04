@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.ritesh.cashiro.presentation.ui.features.accounts

import com.ritesh.cashiro.presentation.ui.components.CashiroDialogDefaults
import com.ritesh.cashiro.presentation.ui.components.DialogConfirmButton
import com.ritesh.cashiro.presentation.ui.components.DialogDismissButton
import com.ritesh.cashiro.presentation.ui.components.AccountRow
import com.ritesh.cashiro.presentation.ui.components.CashiroModalBottomSheet

import com.ritesh.cashiro.utils.sumOfBigDecimal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.icons.Danger
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeAccountSelectionDialog(
    currentAccount: AccountBalanceEntity,
    allAccounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onNext: (List<AccountBalanceEntity>) -> Unit
) {
    // Filter out the current account
    val availableAccounts =
        remember(allAccounts, currentAccount) {
            allAccounts.filter {
                it.accountLast4 != currentAccount.accountLast4 ||
                        it.bankName != currentAccount.bankName
            }
        }

    var selectedAccounts by remember { mutableStateOf(setOf<AccountBalanceEntity>()) }

    CashiroModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.merge_accounts_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.merge_accounts_desc,
                        currentAccount.bankName,
                        currentAccount.accountLast4
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false) .clip(RoundedCornerShape(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(availableAccounts) { account ->
                        val isSelected = selectedAccounts.contains(account)
                        AccountSelectionItem(
                            account = account,
                            isSelected = isSelected,
                            onClick = {
                                selectedAccounts = if (isSelected) {
                                    selectedAccounts - account
                                } else {
                                    selectedAccounts + account
                                }
                            }
                        )
                    }
                    item{ Spacer(modifier = Modifier.height(48.dp))}
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onNext(selectedAccounts.toList()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedAccounts.isNotEmpty()
                ) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

@Composable
fun AccountSelectionItem(account: AccountBalanceEntity, isSelected: Boolean, onClick: () -> Unit) {
    AccountRow(
        account = account,
        selected = isSelected,
        onClick = onClick,
        trailing = { RadioButton(selected = isSelected, onClick = null) }
    )
}

enum class BalanceMergeOption {
        SUM,
        MANUAL,
        NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeBalanceOptionDialog(
    selectedAccounts: List<AccountBalanceEntity>,
    currentAccount: AccountBalanceEntity,
    onDismiss: () -> Unit,
    onOptionSelected: (BalanceMergeOption) -> Unit
) {
    CashiroModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.update_balance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            val totalBalance =
                currentAccount.balance + selectedAccounts.sumOfBigDecimal { acc: AccountBalanceEntity -> acc.balance }
            MergeOptionItem(
                title = stringResource(R.string.merge_option_sum),
                description = stringResource(
                    R.string.merge_option_sum_desc,
                    CurrencyFormatter.formatCurrency(totalBalance, currentAccount.currency)
                ),
                icon = Icons.Filled.Calculate,
                onClick = { onOptionSelected(BalanceMergeOption.SUM) }
            )
            MergeOptionItem(
                title = stringResource(R.string.merge_option_manual),
                description = stringResource(R.string.merge_option_manual_desc),
                icon = Icons.Filled.Edit,
                onClick = { onOptionSelected(BalanceMergeOption.MANUAL) }
            )
            MergeOptionItem(
                title = stringResource(R.string.merge_option_none),
                description = stringResource(
                    R.string.merge_option_none_desc,
                    CurrencyFormatter.formatCurrency(currentAccount.balance, currentAccount.currency)
                ),
                icon = Icons.Filled.Close,
                onClick = { onOptionSelected(BalanceMergeOption.NONE) }
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
fun MergeOptionItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun MergeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    blurEffects: Boolean = false,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Iconax.Danger,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = stringResource(R.string.merge_confirm_title)) },
        text = {
            Text(
                text = stringResource(R.string.merge_confirm_message),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.merge),
                onClick = onConfirm
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss
            )
        },
        containerColor = CashiroDialogDefaults.containerColor
    )
}
