package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.LongArrow
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.success_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.math.BigDecimal

@OptIn(ExperimentalHazeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    totalBalance: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int = 0,
    currency: String,
    abbreviatedName: String,
    userName: String,
    balanceHistory: List<BalancePoint> = emptyList(),
    thisMonthValue: String = "",
    thisYearValue: String = "",
    dateRangeLabel: String = "",
    availableCurrenciesCount: Int = 0,
    onCurrencyClick: () -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() },
    embedded: Boolean = false
) {
    val summaryContentColor = if (embedded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val summaryContentColorVariant = if (embedded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    var isExpanded by remember { mutableStateOf(false) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        CashiroCard(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(
                    MaterialTheme.motionScheme.fastSpatialSpec()
                )
                .clip(RoundedCornerShape(Dimensions.Radius.lg))
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            inputScale = HazeInputScale.Auto
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded

                        }
                    ) else Modifier
                ),
            shape = RoundedCornerShape(Spacing.lg),
            colors = CardDefaults.cardColors(
                containerColor = if (embedded) Color.Transparent else if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            onClick = { isExpanded = !isExpanded }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = fadeIn() + expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
                    exit = fadeOut() + shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
                ) {
                    // Collapsed View
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.net_worth_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = summaryContentColorVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(totalBalance, currency),
                                style = if (embedded) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = summaryContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (monthlyChange >= BigDecimal.ZERO) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (monthlyChange >= BigDecimal.ZERO) success_dark else expense_dark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.percent_this_month_format, if (monthlyChangePercent >= 0) "+" else "", monthlyChangePercent.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = summaryContentColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Sparkline
                        if (balanceHistory.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(40.dp)
                            ) {
                                BalanceSparkline(
                                    data = balanceHistory.map { it.balance },
                                    lineColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
                    exit = fadeOut() + shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
                ) {
                    // Expanded View
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(stringResource(R.string.net_worth_label), style = MaterialTheme.typography.labelLarge, color = summaryContentColor)
                            Column(
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatCurrency(totalBalance, currency),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = summaryContentColor,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                // Currency Selector
                                BlurredAnimatedVisibility(availableCurrenciesCount > 1) {
                                    BalanceCurrencySelector(
                                        currency = currency,
                                        onClick = onCurrencyClick
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Large Graph
                        if (balanceHistory.isNotEmpty()) {
                            BalanceChart(
                                primaryCurrency = currency,
                                balanceHistory = balanceHistory,
                                backgroundColor = if (blurEffects) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                height = 180
                            )
                            Text(
                                text = dateRangeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = summaryContentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        HorizontalDivider(color = summaryContentColor.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(Spacing.md))
                        // horizontal summary items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryItem(label = stringResource(R.string.this_month_lbl), value = thisMonthValue)
                            VerticalDivider(
                                modifier = Modifier.height(30.dp),
                                color = summaryContentColor.copy(alpha = 0.1f)
                            )
                            SummaryItem(label = stringResource(R.string.this_year_lbl), value = thisYearValue)
                            VerticalDivider(
                                modifier = Modifier.height(30.dp),
                                color = summaryContentColor.copy(alpha = 0.1f)
                            )
                            SummaryItem(
                                label = stringResource(R.string.balance_label),
                                value = CurrencyFormatter.formatCurrency(totalBalance, currency)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
        // Collapse/Expand Icon
        Icon(
            imageVector = Iconax.LongArrow,
            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
            tint = summaryContentColorVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .height(28.dp)
                .width(40.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BalanceSparkline(
    data: List<BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    
    val max = data.maxOf { it }.toFloat()
    val min = data.minOf { it }.toFloat()
    val range = (max - min).takeIf { it > 0 } ?: 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index.toFloat() / (data.size - 1) * width
            val y = height - ((value.toFloat() - min) / range * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Gradient fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

/** Native tonal action: a compact visual capsule with Material's minimum touch target. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BalanceCurrencySelector(currency: String, onClick: () -> Unit) {
    val description = stringResource(R.string.select_currency)
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "$description: $currency" },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = currency, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}
