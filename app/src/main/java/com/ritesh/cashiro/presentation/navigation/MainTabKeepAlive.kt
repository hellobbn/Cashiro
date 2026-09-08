package com.ritesh.cashiro.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ritesh.cashiro.presentation.ui.features.analytics.AnalyticsScreen
import com.ritesh.cashiro.presentation.ui.features.home.HomeScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsViewModel

/**
 * Keeps visited bottom-nav screens composed. NavHost otherwise disposes each
 * tab on leave, so coming back rebuilds Home/Analytics/Transactions from scratch
 * (~200ms on a Fold with a full bookkeeping dataset).
 */
@Composable
internal fun MainTabKeepAlive(
    currentTag: String?,
    navController: NavHostController,
    transactionsViewModel: TransactionsViewModel,
    blurEffects: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    var visitedHome by remember { mutableStateOf(currentTag == "home") }
    var visitedAnalytics by remember { mutableStateOf(currentTag == "analytics") }
    var visitedTransactions by remember { mutableStateOf(currentTag == "transactions") }
    var lastTag by remember { mutableStateOf(currentTag) }
    if (currentTag == "home") visitedHome = true
    if (currentTag == "analytics") visitedAnalytics = true
    if (currentTag == "transactions") visitedTransactions = true
    if (currentTag != null) lastTag = currentTag
    val shownTag = currentTag ?: lastTag

    // Compose parked tabs after Home has painted so the first tap is a warm switch.
    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        delay(400)
        visitedAnalytics = true
        delay(350)
        visitedTransactions = true
    }

    val onNavigateToSettings = remember(navController) {
        { navController.safeNavigate(Settings) }
    }
    val onNavigateToTransactions = remember(navController) {
        { navController.safeNavigate(Transactions()) }
    }
    val onNavigateToTransactionsWithSearch = remember(navController) {
        { navController.safeNavigate(Transactions(focusSearch = true)) }
    }
    val onNavigateToSubscriptions = remember(navController) {
        { navController.safeNavigate(Subscriptions) }
    }
    val onNavigateToBudgets = remember(navController) {
        { id: Long? ->
            if (id != null) {
                navController.safeNavigate(
                    BudgetDetail(budgetId = id, sharedElementKey = "budget_card_$id")
                )
            } else {
                navController.safeNavigate(Budgets())
            }
        }
    }
    val onNavigateToBudgetHistory = remember(navController) {
        { id: Long -> navController.safeNavigate(BudgetHistory(id)) }
    }
    val onNavigateToLendBorrow = remember(navController) {
        { filter: String? -> navController.safeNavigate(LendBorrow(filter)) }
    }
    val onTransactionClick = remember(navController) {
        { transactionId: Long, key: String ->
            navController.safeNavigate(TransactionDetail(transactionId, key))
        }
    }
    val onNavigateBack = remember(navController) {
        { navController.safePopBackStack() }
    }
    val onAnalyticsToTransactions = remember(navController) {
        { category: String?, merchant: String?, period: String?, currency: String? ->
            navController.safeNavigate(
                Transactions(
                    category = category,
                    merchant = merchant,
                    period = period,
                    currency = currency
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (currentTag != null && blurEffects) Modifier.hazeSource(hazeState) else Modifier)
    ) {
        if (visitedHome) {
            KeptTab(visible = shownTag == "home") {
                HomeScreen(
                    navController = navController,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToTransactionsWithSearch = onNavigateToTransactionsWithSearch,
                    onNavigateToSubscriptions = onNavigateToSubscriptions,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToBudgetHistory = onNavigateToBudgetHistory,
                    onNavigateToLendBorrow = onNavigateToLendBorrow,
                    onTransactionClick = onTransactionClick,
                )
            }
        }
        if (visitedAnalytics) {
            KeptTab(visible = shownTag == "analytics") {
                AnalyticsScreen(
                    onNavigateToTransactions = onAnalyticsToTransactions,
                    blurEffects = blurEffects,
                )
            }
        }
        if (visitedTransactions) {
            KeptTab(visible = shownTag == "transactions") {
                TransactionsScreen(
                    transactionsViewModel = transactionsViewModel,
                    onNavigateBack = onNavigateBack,
                    onTransactionClick = onTransactionClick,
                    onNavigateToSettings = onNavigateToSettings,
                    blurEffects = blurEffects,
                )
            }
        }
    }
}

@Composable
private fun KeptTab(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        contentReady = true
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .parkIf(!visible)
    ) {
        if (contentReady) content()
    }
}

/**
 * Keep a stable layout modifier so showing/hiding a tab only changes placement,
 * not the modifier chain (which would remeasure the whole screen).
 */
@Composable
private fun Modifier.parkIf(parked: Boolean): Modifier {
    val parkedState = remember { mutableStateOf(parked) }
    parkedState.value = parked
    return remember {
        parkLayout(parkedState)
    }
}

private fun Modifier.parkLayout(parkedState: MutableState<Boolean>): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        if (parkedState.value) {
            layout(0, 0) {}
        } else {
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
