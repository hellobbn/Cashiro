@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.domain.model.rule.TransactionRule
import com.ritesh.cashiro.domain.usecase.BatchApplyResult
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalHazeApi::class)
@Composable
fun RulesResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() },
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_rules_title)) },
        text = { Text(stringResource(R.string.reset_rules_confirm)) },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.reset),
                onClick = onConfirm,
                destructive = true
            )
        },
        dismissButton = {
            DialogDismissButton(stringResource(R.string.cancel), onDismiss)
        },
        containerColor = CashiroDialogDefaults.containerColor
    )
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun RulesDeleteDialog(
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    rule: TransactionRule,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() },
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_rule_title)) },
        text = { Text(stringResource(R.string.delete_rule_confirm, rule.name)) },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.delete),
                onClick = onDelete,
                destructive = true
            )
        },
        dismissButton = {
            DialogDismissButton(stringResource(R.string.cancel), onDismiss)
        },
        containerColor = CashiroDialogDefaults.containerColor
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
fun RulesBatchApplyDialog(
    rule: TransactionRule,
    progress: Pair<Int, Int>?,
    result: BatchApplyResult?,
    onDismiss: () -> Unit,
    onApplyToAll: () -> Unit,
    onApplyToUncategorized: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() },
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progress != null) {
                        stringResource(R.string.applying_rule)
                    } else {
                        stringResource(R.string.apply_rule_past_transactions_title)
                    }
                )
                if(progress == null && result == null) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (progress == null && result == null) {
                    // Initial state - show options
                    Text(
                        text = stringResource(R.string.apply_rule_to_existing_confirm, rule.name),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                        ),
                        shape = RoundedCornerShape(Dimensions.Radius.sm),
                    ){
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text(
                                text = stringResource(R.string.apply_rule_instruction),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.apply_rule_option_all),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                            )
                            Text(
                                text = stringResource(R.string.apply_rule_option_uncategorized),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                            )
                        }
                    }
                } else if (progress != null) {
                    // Processing state - show progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = stringResource(R.string.processing_transactions_format, progress.first, progress.second),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (result != null) {
                    // Result state - show summary
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result.errors.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (result.errors.isEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.completed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = stringResource(R.string.transactions_processed_format, result.totalProcessed),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(R.string.transactions_updated_format, result.totalUpdated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )

                        if (result.totalDeleted > 0) {
                            Text(
                                text = stringResource(R.string.transactions_blocked_format, result.totalDeleted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (result.errors.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.errors_count_format, result.errors.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (progress == null && result == null) {
                DialogConfirmButton(
                    text = stringResource(R.string.all),
                    onClick = onApplyToAll
                )
            } else if (result != null) {
                // Done - show close button
                DialogConfirmButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss
                )
            }
        },
        dismissButton = {
            if (progress == null && result == null) {
                DialogDismissButton(stringResource(R.string.uncategorized), onApplyToUncategorized)
            }
        },
        containerColor = CashiroDialogDefaults.containerColor
    )
}