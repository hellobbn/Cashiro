package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.domain.model.LendBorrowSummary
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * Home dashboard widget for the Ledger feature. Shows only the lent/borrowed
 * summary tiles. Tapping a tile opens the Ledger screen with the corresponding
 * filter pre-selected; tapping the card opens it without a filter.
 */
@OptIn(ExperimentalHazeApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.LendBorrowCard(
    summary: LendBorrowSummary,
    onClick: () -> Unit,
    onLentClick: () -> Unit,
    onBorrowedClick: () -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "CNY",
    blurEffects: Boolean = false,
    hazeState: HazeState = remember { HazeState() },
    animatedContentScope: AnimatedContentScope? = null
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    CashiroCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.Radius.xl))
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
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
        onClick = onClick,
        shape = RoundedCornerShape(Dimensions.Radius.xl),
        colors = CardDefaults.cardColors(
            containerColor = if (blurEffects) containerColor.copy(alpha = Dimensions.Alpha.surface)
            else containerColor
        ),
        contentPadding = Dimensions.Padding.content
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            LoanSummaryItem(
                title = stringResource(R.string.total_lent),
                amount = summary.totalLentRemaining,
                persons = summary.lentPersons,
                currency = currency,
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ArrowUpward,
                onClick = onLentClick,
                animatedContentScope = animatedContentScope,
                sharedElementKey = LoanSharedElementKeys.LENT,
                modifier = Modifier.weight(1f)
            )
            LoanSummaryItem(
                title = stringResource(R.string.total_borrowed),
                amount = summary.totalBorrowedRemaining,
                persons = summary.borrowedPersons,
                currency = currency,
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.ArrowDownward,
                onClick = onBorrowedClick,
                animatedContentScope = animatedContentScope,
                sharedElementKey = LoanSharedElementKeys.BORROWED,
                modifier = Modifier.weight(1f)
            )
        }
    }
}