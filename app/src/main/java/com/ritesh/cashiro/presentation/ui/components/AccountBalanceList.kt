package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.common.icons.InstitutionCatalog
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.utils.formatBalance
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/** All visible account balances share the home screen's vertical scroll. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountBalanceList(
    bankAccounts: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    blurEffects: Boolean,
    modifier: Modifier = Modifier,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    animatedContentScope: AnimatedVisibilityScope? = null,
    hazeState: HazeState = remember { HazeState() }
) {
    val accounts = bankAccounts + creditCards
    if (accounts.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .testTag("account_balance_list"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.accounts),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        accounts.forEachIndexed { index, account ->
            key(account.bankName, account.accountLast4) {
                val isCreditCard = account.isCreditCard || account in creditCards
                val accountType = stringResource(
                    when {
                        account.isWallet -> R.string.type_wallet
                        isCreditCard -> R.string.type_credit_card
                        InstitutionCatalog.find(account.bankName)?.isBroker == true ->
                            R.string.type_investment_account
                        else -> R.string.type_savings_account
                    }
                )
                val subtitle = if (account.isWallet || account.accountLast4.isBlank()) {
                    accountType
                } else {
                    "$accountType · ••${account.accountLast4}"
                }
                val shape = ListItemPosition.from(index, accounts.size).toShape()
                val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Keep the full amount readable on narrow screens and with large text.
                    val stackBalance = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
                    val balanceWidth = maxWidth * 0.45f
                    val accountIcon: @Composable () -> Unit = {
                        BrandIcon(
                            merchantName = account.bankName,
                            size = 40.dp,
                            accountIconResId = account.iconResId,
                            accountIconName = account.iconName,
                            accountColorHex = account.color
                        )
                    }
                    val accountName: @Composable () -> Unit = {
                        Text(
                            text = account.bankName,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val balanceContent: @Composable () -> Unit = {
                        Column(
                            modifier = if (stackBalance) Modifier.fillMaxWidth() else Modifier.widthIn(max = balanceWidth),
                            horizontalAlignment = if (stackBalance) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = stringResource(
                                    if (isCreditCard) R.string.outstanding_label else R.string.balance_label
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BasicText(
                                text = account.formatBalance(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = if (stackBalance) TextAlign.Start else TextAlign.End
                                ),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 10.sp,
                                    maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    stepSize = 0.5.sp
                                )
                            )
                        }
                    }
                    ListItem(
                        headline = {
                            if (stackBalance) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    accountIcon()
                                    Column(modifier = Modifier.weight(1f)) {
                                        accountName()
                                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else accountName()
                        },
                        supporting = {
                            if (stackBalance) {
                                balanceContent()
                            } else {
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leading = if (stackBalance) null else accountIcon,
                        trailing = if (stackBalance) null else balanceContent,
                        onClick = { onAccountClick(account.bankName, account.accountLast4) },
                        padding = PaddingValues(0.dp),
                        shape = shape,
                        listColor = if (blurEffects) containerColor.copy(alpha = 0.5f) else containerColor,
                        modifier = Modifier
                            .testTag("account_balance_${account.bankName}_${account.accountLast4}")
                            .then(
                                if (animatedContentScope != null) {
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(
                                            key = "account_${account.bankName}_${account.accountLast4}"
                                        ),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                } else Modifier
                            )
                            .clip(shape)
                            .then(
                                if (blurEffects) Modifier.hazeEffect(hazeState) {
                                    style = HazeDefaults.style(
                                        backgroundColor = Color.Transparent,
                                        tint = HazeDefaults.tint(containerColor),
                                        blurRadius = 20.dp
                                    )
                                } else Modifier
                            )
                    )
                }
            }
        }
    }
}
