package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.presentation.ui.theme.AccountCardElevation
import com.ritesh.cashiro.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.common.icons.IconProvider
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Balance
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Eye
import com.ritesh.cashiro.presentation.ui.icons.EyeSlash
import com.ritesh.cashiro.presentation.ui.icons.HierarchySquare3
import com.ritesh.cashiro.presentation.ui.icons.History
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountCard(
    account: AccountBalanceEntity,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
    showMoreOptions: Boolean = true,
    onClick: (() -> Unit)? = null,
    isMain: Boolean = false,
    onUpdateBalance: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onSetAsMain: () -> Unit = {},
    onMergeAccount: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AccountCardElevation)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current
            val iconResource = remember(account.bankName, account.iconResId, account.iconName) {
                IconProvider.getIconForTransaction(
                    context = context,
                    merchantName = account.bankName,
                    accountIconResId = account.iconResId,
                    accountIconName = account.iconName
                )
            }


            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(if (account.isCreditCard) R.string.outstanding_label else R.string.balance_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showMoreOptions) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = stringResource(R.string.more_options),
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = MaterialTheme.shapes.large,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.update_balance)) },
                                    leadingIcon = {
                                        Icon(
                                            Iconax.Balance,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onUpdateBalance()
                                    },
                                )

                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_details)) },
                                    leadingIcon = {
                                        Icon(
                                            Iconax.Edit2,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditAccount()
                                    },
                                )

                                if (onMergeAccount != null) {
                                    HorizontalDivider(
                                        thickness = 1.5.dp,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.merge_account)) },
                                        leadingIcon = {
                                            Icon(
                                                Iconax.HierarchySquare3,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onMergeAccount()
                                        },
                                    )
                                }

                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.history)) },
                                    leadingIcon = {
                                        Icon(
                                            Iconax.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onViewHistory()
                                    },
                                )

                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                DropdownMenuItem(
text = {
                                            Text(
                                                if (isHidden) stringResource(R.string.show) else stringResource(R.string.hide)
                                            )
                                        },
                                    leadingIcon = {
                                        Icon(
                                            if (isHidden)
                                                Iconax.Eye
                                            else
                                                Iconax.EyeSlash,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onToggleVisibility()
                                    },
                                )
                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                                if (!isMain) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.set_as_main)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700) // Gold
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onSetAsMain()
                                        },
                                    )
                                    HorizontalDivider(
                                        thickness = 1.5.dp,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Iconax.Bag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteAccount()
                                    },
                                )
                            }
                        }
                    }
                }

                // Balance
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        account.balance,
                        account.currency
                    ),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Bottom Section (Bank Info)
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = account.bankName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (account.isWallet) stringResource(R.string.type_wallet)
                                    else "**** **** **** ${account.accountLast4}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                if (isMain) {
                                    Surface(
                                        shape = RoundedCornerShape(Spacing.xxl),
                                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                        border = BorderStroke(
                                            1.dp,
                                            Color(0xFFFFD700).copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(4.dp,),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                BrandIcon(
                                    merchantName = account.bankName,
                                    size = 48.dp,
                                    showBackground = true,
                                    accountIconResId = account.iconResId,
                                    accountIconName = account.iconName,
                                    accountColorHex = account.color
                                )
                            }
                        }

                    }
                    // Extra content (e.g., Credit Card stats, Linked Cards)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}