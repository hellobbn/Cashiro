package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.common.icons.InstitutionCatalog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.common.icons.IconProvider
import com.ritesh.cashiro.utils.formatBalance
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountCarousel(
    modifier: Modifier = Modifier,
    bankAccounts: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    animatedContentScope: AnimatedVisibilityScope? = null,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val totalAccounts = bankAccounts.size + creditCards.size
    val allAccounts = bankAccounts + creditCards
    
    val isTransitioning = animatedContentScope?.transition?.let { 
        it.currentState != it.targetState 
    } ?: false

    if (totalAccounts == 1) {
        // Single account
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val account = allAccounts.first()
            AccountCarouselCard(
                bankName = account.bankName,
                accountLast4 = account.accountLast4,
                balance = account.formatBalance(),
                subtitle = when {
                    account.isWallet -> stringResource(R.string.type_wallet)
                    creditCards.contains(account) -> stringResource(R.string.type_credit_card)
                    InstitutionCatalog.find(account.bankName)?.isBroker == true -> stringResource(R.string.type_investment_account)
                    else -> stringResource(R.string.type_savings_account)
                },
                onClick = { onAccountClick(account.bankName, account.accountLast4) },
                animatedContentScope = animatedContentScope,
                isWallet = account.isWallet,
                iconResId = account.iconResId,
                iconName = account.iconName,
                color = account.color,
                blurEffects = blurEffects,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        // Multiple accounts - show as carousel with snapping
        val pagerState = rememberPagerState(pageCount = { allAccounts.size })
        
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp,
            userScrollEnabled = !isTransitioning
        ) { page ->
            val account = allAccounts[page]
            AccountCarouselCard(
                bankName = account.bankName,
                accountLast4 = account.accountLast4,
                balance = account.formatBalance(),
                subtitle = when {
                    account.isWallet -> stringResource(R.string.type_wallet)
                    creditCards.contains(account) -> stringResource(R.string.type_credit_card)
                    InstitutionCatalog.find(account.bankName)?.isBroker == true -> stringResource(R.string.type_investment_account)
                    else -> stringResource(R.string.type_savings_account)
                },
                onClick = { onAccountClick(account.bankName, account.accountLast4) },
                animatedContentScope = animatedContentScope,
                isWallet = account.isWallet,
                iconResId = account.iconResId,
                iconName = account.iconName,
                color = account.color,
                blurEffects = blurEffects,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun SharedTransitionScope.AccountCarouselCard(
    bankName: String,
    accountLast4: String,
    balance: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedContentScope: AnimatedVisibilityScope? = null,
    isWallet: Boolean = false,
    iconResId: Int = 0,
    iconName: String? = null,
    color: String? = null,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .then(
                if (animatedContentScope != null) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "account_${bankName}_${accountLast4}"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                    )
                        .skipToLookaheadSize()
                } else Modifier
            )
            .clip(RoundedCornerShape(28.dp))
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
        color = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val iconResource = remember(bankName, iconResId, iconName) {
                IconProvider.getIconForTransaction(
                    context = context,
                    merchantName = bankName,
                    accountIconResId = iconResId,
                    accountIconName = iconName
                )
            }

            TiledScrollingIconBackground(
                iconResource = iconResource,
                opacity = 0.05f,
                iconSize = 56.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(110.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                BrandIcon(
                    merchantName = bankName,
                    size = 48.dp,
                    showBackground = true,
                    accountIconResId = iconResId,
                    accountIconName = iconName,
                    accountColorHex = color
                )


                Column {
                    Text(
                        text = if (isWallet) bankName.uppercase() else "${bankName.uppercase()} ••$accountLast4",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = balance,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.view_details),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
