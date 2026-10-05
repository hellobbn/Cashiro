@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.ritesh.cashiro.presentation.ui.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.common.TransactionDecoration
import com.ritesh.cashiro.presentation.ui.components.BalanceChart
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.presentation.ui.theme.isAppInDarkTheme
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.displayTitle
import com.ritesh.cashiro.utils.formatAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The top of Home: net worth with what makes it up (assets less what the cards owe), the
 * trend on request, and this month's spending and income.
 */
@Composable
internal fun HomeSummaryCard(
    netWorth: BigDecimal,
    currency: String,
    // What the credit cards owe, in [currency]; null while unknown
    liabilities: BigDecimal?,
    monthExpenses: BigDecimal,
    monthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    balanceHistory: List<BalancePoint>,
    trendLabel: String,
    canChangeCurrency: Boolean,
    onCurrencyClick: () -> Unit,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTrend by rememberSaveable { mutableStateOf(false) }
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    val muted = content.copy(alpha = 0.72f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = content
    ) {
        Column(Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())) {
            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.net_worth_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = muted,
                        modifier = Modifier.weight(1f)
                    )
                    if (canChangeCurrency) {
                        TextButton(onClick = onCurrencyClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(currency, style = MaterialTheme.typography.labelLarge, color = content)
                            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp), tint = content)
                        }
                    }
                    if (balanceHistory.size >= 2) {
                        IconToggleButton(checked = showTrend, onCheckedChange = { showTrend = it }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ShowChart,
                                contentDescription = stringResource(R.string.home_trend),
                                tint = if (showTrend) MaterialTheme.colorScheme.primary else muted
                            )
                        }
                    }
                }
                Text(
                    text = CurrencyFormatter.formatCurrency(netWorth, currency),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 12.dp)
                )
                if (liabilities != null && liabilities.signum() > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                    ) {
                        LabeledFigure(stringResource(R.string.home_assets), CurrencyFormatter.formatCurrency(netWorth + liabilities, currency), muted)
                        LabeledFigure(stringResource(R.string.home_liabilities), CurrencyFormatter.formatCurrency(liabilities, currency), muted)
                    }
                }
            }

            AnimatedVisibility(
                visible = showTrend,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
                    BalanceChart(
                        primaryCurrency = currency,
                        balanceHistory = balanceHistory,
                        backgroundColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        height = 160
                    )
                    Text(
                        trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            HorizontalDivider(Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp), color = content.copy(alpha = 0.12f))
            // This month, opening the comparison with last month
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMonthClick)
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_month_expenses), style = MaterialTheme.typography.labelMedium, color = muted)
                    Text(
                        CurrencyFormatter.formatCurrency(monthExpenses.abs(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    expenseComparison(monthExpenses.abs(), lastMonthExpenses.abs())?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_month_income), style = MaterialTheme.typography.labelMedium, color = muted)
                    Text(
                        CurrencyFormatter.formatCurrency(monthIncome.abs(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = muted, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
private fun LabeledFigure(label: String, value: String, labelColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** "12% more than last month", once there is a last month to compare with. */
@Composable
private fun expenseComparison(current: BigDecimal, last: BigDecimal): String? {
    if (last.signum() == 0) return null
    val percent = (current - last).multiply(BigDecimal(100)).divide(last, 0, RoundingMode.HALF_UP).toInt()
    return when {
        percent > 0 -> stringResource(R.string.home_vs_last_month_more, "$percent%")
        percent < 0 -> stringResource(R.string.home_vs_last_month_less, "${-percent}%")
        else -> stringResource(R.string.home_vs_last_month_same)
    }
}

/**
 * The latest transactions as plain rows in one card: icon, merchant, category and day, and
 * the amount signed by direction (spending in the text color, income in green).
 */
@Composable
internal fun RecentTransactionsCard(
    transactions: List<TransactionEntity>,
    decorate: (TransactionEntity) -> TransactionDecoration,
    mainCurrency: String,
    isLoading: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.home_recent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.view_all))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(18.dp))
            }
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    com.ritesh.cashiro.presentation.ui.components.LoadingCircle(Modifier.size(48.dp))
                }
                transactions.isEmpty() -> Text(
                    stringResource(R.string.no_transactions_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
                else -> Column {
                    transactions.forEachIndexed { index, transaction ->
                        if (index > 0) {
                            HorizontalDivider(
                                Modifier.padding(start = 68.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                        val decoration = remember(transaction, decorate) { decorate(transaction) }
                        RecentTransactionRow(transaction, decoration, mainCurrency) { onTransactionClick(transaction) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(
    transaction: TransactionEntity,
    decoration: TransactionDecoration,
    mainCurrency: String,
    onClick: () -> Unit
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val day = remember(transaction.dateTime, locale) { dayLabel(transaction.dateTime.toLocalDate(), locale) }
    val today = stringResource(R.string.today_lbl)
    val yesterday = stringResource(R.string.yesterday_lbl)
    val dayText = when (day) {
        DAY_TODAY -> today
        DAY_YESTERDAY -> yesterday
        else -> day
    }
    val category = decoration.subcategory?.name ?: decoration.category?.name ?: transaction.category
    val subtitle = listOfNotNull(category.takeIf { it.isNotBlank() }, dayText).joinToString(" · ")
    val incoming = transaction.transactionType == TransactionType.INCOME || transaction.transactionType == TransactionType.BORROWED
    val outgoing = transaction.transactionType in setOf(
        TransactionType.EXPENSE, TransactionType.CREDIT, TransactionType.LENT
    )
    val sign = when {
        incoming -> "+"
        outgoing -> "−"
        else -> ""
    }
    val amountColor = when {
        incoming -> if (isAppInDarkTheme) income_dark else income_light
        outgoing -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val foreign = !transaction.currency.equals(mainCurrency, ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandIcon(
            merchantName = transaction.displayTitle(),
            size = 40.dp,
            categoryEntity = decoration.category,
            subcategoryEntity = decoration.subcategory,
            category = transaction.category,
            subcategory = transaction.subcategory,
            accountIconResId = decoration.account?.iconResId ?: 0,
            accountIconName = decoration.account?.iconName,
            accountColorHex = decoration.account?.color
        )
        Column(Modifier.weight(1f)) {
            Text(
                transaction.displayTitle(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                sign + transaction.formatAmount(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                maxLines = 1
            )
            if (foreign && decoration.convertedAmount != null) {
                Text(
                    "≈ " + CurrencyFormatter.formatCurrency(decoration.convertedAmount, mainCurrency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private const val DAY_TODAY = "today"
private const val DAY_YESTERDAY = "yesterday"

/** Today, yesterday, else 9月1日 / Sep 1 (with the year once it is not this one). */
private fun dayLabel(date: LocalDate, locale: java.util.Locale): String {
    val now = LocalDate.now()
    return when (date) {
        now -> DAY_TODAY
        now.minusDays(1) -> DAY_YESTERDAY
        else -> {
            val zh = locale.language == "zh"
            val pattern = when {
                date.year == now.year -> if (zh) "M月d日" else "MMM d"
                else -> if (zh) "yyyy年M月d日" else "MMM d, yyyy"
            }
            date.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
}
