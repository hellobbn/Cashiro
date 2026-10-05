package com.ritesh.cashiro.presentation.ui.features.ai

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.ai.AiStep
import com.ritesh.cashiro.data.ai.LedgerChange
import com.ritesh.cashiro.data.ai.LedgerTools
import com.ritesh.cashiro.presentation.ui.features.transactions.localizedDateFormatter
import com.ritesh.cashiro.presentation.ui.features.transactions.withLast4
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.transactionTypeColor
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.time.LocalDate
import kotlinx.coroutines.delay

/** Transactions listed under a step before the rest are only counted. */
private const val SHOWN_PER_STEP = 4

/**
 * The run so far: a status card (model, elapsed time, proposals) and then each step the model
 * took, newest last. The last step is the one in progress.
 */
fun LazyListScope.aiProgress(phase: AiPhase.Running, model: String) {
    item(key = "progress-status") { StatusCard(phase, model) }
    item(key = "progress-files") {
        val images = phase.images
        val done = images != null
        StepRow(
            icon = Icons.Rounded.Description,
            active = !done,
            title = when {
                phase.files == 0 -> stringResource(R.string.ai_step_no_files)
                images == null -> pluralStringResource(R.plurals.ai_step_reading_files, phase.files, phase.files)
                images == 0 -> pluralStringResource(R.plurals.ai_step_read_files, phase.files, phase.files)
                else -> pluralStringResource(R.plurals.ai_step_read_files, phase.files, phase.files) + " · " +
                    pluralStringResource(R.plurals.ai_step_images, images, images)
            }
        )
    }
    itemsIndexed(phase.steps, key = { index, _ -> "progress-$index" }) { index, step ->
        Step(step, active = index == phase.steps.lastIndex)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusCard(phase: AiPhase.Running, model: String) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = SystemClock.elapsedRealtime()
        }
    }
    val seconds = ((now - phase.startedAt) / 1000).coerceAtLeast(0)
    val turns = phase.steps.count { it is AiStep.Asking }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pluralStringResource(R.plurals.ai_progress_proposed, phase.proposed, phase.proposed),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        listOfNotNull(
                            model,
                            "%d:%02d".format(seconds / 60, seconds % 60),
                            turns.takeIf { it > 0 }?.let { stringResource(R.string.ai_progress_turn, it) }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.ai_progress_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Step(step: AiStep, active: Boolean) {
    val dates = localizedDateFormatter(withYear = false)
    when (step) {
        is AiStep.Asking -> StepRow(
            icon = Icons.Rounded.CheckCircle,
            active = active,
            title = stringResource(if (active) R.string.ai_step_asking else R.string.ai_step_answered, step.turn)
        )
        is AiStep.Note -> StepRow(icon = Icons.Rounded.FormatQuote, title = step.text, quote = true)
        is AiStep.Searched -> {
            fun day(text: String?) = text?.let { runCatching { LocalDate.parse(it.take(10)).format(dates) }.getOrDefault(it) }
            val range = listOfNotNull(day(step.from), day(step.to)).distinct().joinToString("–")
            StepRow(
                icon = Icons.Rounded.Search,
                title = listOfNotNull(
                    stringResource(R.string.ai_step_searched),
                    range.ifEmpty { null },
                    step.text?.let { "“$it”" }
                ).joinToString(" · "),
                detail = step.found?.let { pluralStringResource(R.plurals.ai_step_found, it, it) }
            )
        }
        is AiStep.Proposed -> ProposedStep(step)
    }
}

@Composable
private fun ProposedStep(step: AiStep.Proposed) {
    val changes = step.changes
    val rejected = if (step.rejected) stringResource(R.string.ai_step_rejected) else null
    if (changes.isEmpty()) {
        StepRow(icon = Icons.Rounded.WarningAmber, title = rejected ?: stringResource(R.string.ai_step_nothing, step.tool))
        return
    }
    val dates = localizedDateFormatter(withYear = false)
    val (icon, title) = when (step.tool) {
        LedgerTools.ADD -> Icons.Rounded.AddCircleOutline to
            pluralStringResource(R.plurals.ai_step_added, changes.size, changes.size)
        LedgerTools.UPDATE -> Icons.Rounded.Edit to
            pluralStringResource(R.plurals.ai_step_updated, changes.size, changes.size)
        LedgerTools.DELETE -> Icons.Rounded.DeleteOutline to
            pluralStringResource(R.plurals.ai_step_deleted, changes.size, changes.size)
        else -> Icons.Rounded.AccountBalance to stringResource(R.string.ai_step_accounts)
    }
    StepRow(icon = icon, title = title, detail = rejected) {
        changes.take(SHOWN_PER_STEP).forEach { change ->
            when (change) {
                is LedgerChange.Add -> TransactionLine(
                    change.draft.dateTime.toLocalDate().format(dates),
                    change.draft.merchant,
                    CurrencyFormatter.formatCurrency(change.draft.amount, change.draft.currency),
                    transactionTypeColor(change.draft.type)
                )
                is LedgerChange.Update -> TransactionLine(
                    change.after.dateTime.toLocalDate().format(dates),
                    if (change.before.merchantName != change.after.merchantName) {
                        "${change.before.merchantName} → ${change.after.merchantName}"
                    } else {
                        listOfNotNull(change.after.merchantName, change.after.category).joinToString(" · ")
                    },
                    CurrencyFormatter.formatCurrency(change.after.amount, change.after.currency),
                    transactionTypeColor(change.after.transactionType)
                )
                is LedgerChange.Delete -> TransactionLine(
                    change.transaction.dateTime.toLocalDate().format(dates),
                    change.transaction.merchantName,
                    CurrencyFormatter.formatCurrency(change.transaction.amount, change.transaction.currency),
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                is LedgerChange.CreateAccount -> DetailText(
                    stringResource(R.string.ai_step_new_account, accountName(change.account.bankName, change.account.accountLast4))
                )
                is LedgerChange.SetBalance -> DetailText(
                    stringResource(
                        R.string.ai_step_balance,
                        accountName(change.account.bankName, change.account.accountLast4),
                        CurrencyFormatter.formatCurrency(change.balance, change.account.currency)
                    )
                )
                is LedgerChange.UpdateAccount -> DetailText(
                    stringResource(
                        R.string.ai_step_account_edit,
                        accountName(change.before.bankName, change.before.accountLast4),
                        accountName(change.after.bankName, change.after.accountLast4)
                    )
                )
            }
        }
        if (changes.size > SHOWN_PER_STEP) {
            DetailText(stringResource(R.string.ai_step_more, changes.size - SHOWN_PER_STEP))
        }
    }
}

private fun accountName(bankName: String, last4: String) = withLast4(bankName, last4) ?: bankName

@Composable
private fun TransactionLine(date: String, merchant: String, amount: String, amountColor: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            merchant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(amount, style = MaterialTheme.typography.bodySmall, color = amountColor)
    }
}

@Composable
private fun DetailText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One line of the timeline: an icon (a spinner while [active]), a title and what it found. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepRow(
    icon: ImageVector,
    title: String,
    active: Boolean = false,
    detail: String? = null,
    quote: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (active) {
                LoadingIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (quote) FontStyle.Italic else null,
                color = if (quote) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = if (quote) 4 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
            detail?.let { DetailText(it) }
            content()
        }
    }
}
