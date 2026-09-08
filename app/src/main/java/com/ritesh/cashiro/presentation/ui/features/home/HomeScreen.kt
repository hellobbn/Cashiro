package com.ritesh.cashiro.presentation.ui.features.home

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.preferences.HomeWidget
import com.ritesh.cashiro.utils.capitalizeFirst
import com.ritesh.cashiro.presentation.navigation.AccountDetail
import com.ritesh.cashiro.presentation.navigation.NotificationSettings
import com.ritesh.cashiro.presentation.navigation.safeNavigate
import com.ritesh.cashiro.presentation.ui.components.AccountBalanceRow
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountSectionSummary
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountSectionToggle
import com.ritesh.cashiro.presentation.ui.features.accounts.buildAccountSections
import com.ritesh.cashiro.presentation.ui.features.accounts.listKey
import com.ritesh.cashiro.presentation.ui.components.BalanceCard
import com.ritesh.cashiro.presentation.ui.components.BudgetCarousel
import com.ritesh.cashiro.presentation.ui.components.CurrencySelectionBottomSheet
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.GreetingCard
import com.ritesh.cashiro.presentation.ui.components.HeatmapWidget
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.SubscriptionIconsStack
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.icons.Convertshape2
import com.ritesh.cashiro.presentation.ui.icons.Gallery
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ReceiptItem
import com.ritesh.cashiro.presentation.ui.icons.RefreshCircle
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.icons.Setting2
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.bottomFade
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.presentation.ui.components.LendBorrowCard
import com.ritesh.cashiro.presentation.ui.features.lendborrow.LendBorrowFilter

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToBudgets: (Long?) -> Unit = {},
    onNavigateToBudgetHistory: (Long) -> Unit = {},
    onNavigateToLendBorrow: (String?) -> Unit = { _ -> },
    onTransactionClick: (Long, String) -> Unit = { _, _ -> },
    animatedContentScope: AnimatedContentScope? = null,
) {

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val deletedTransaction by homeViewModel.deletedTransaction.collectAsState()
    val categoriesMap by homeViewModel.categoriesMap.collectAsStateWithLifecycle()
    val subcategoriesMap by homeViewModel.subcategoriesMap.collectAsStateWithLifecycle()
    val accountsMap by homeViewModel.accountsMap.collectAsStateWithLifecycle()
    val homeWidgets by homeViewModel.homeWidgets.collectAsStateWithLifecycle()
    val overviewViewModel: com.ritesh.cashiro.presentation.ui.features.accounts.AccountOverviewViewModel = hiltViewModel()
    val overviewItems by overviewViewModel.items.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.accountBalances, uiState.creditCards, uiState.selectedCurrency) {
        overviewViewModel.update(uiState.accountBalances + uiState.creditCards, uiState.selectedCurrency)
    }
    val showOverview = homeWidgets.any { it.widget == HomeWidget.ACCOUNT_CAROUSEL && it.isVisible }
    val hasNetworth = homeWidgets.any { it.widget == HomeWidget.NETWORTH_SUMMARY && it.isVisible }
    val openCategory: (com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory) -> Unit = { category ->
        if (category == com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory.INVESTMENTS)
            navController.safeNavigate(com.ritesh.cashiro.presentation.navigation.Investments)
        else navController.safeNavigate(com.ritesh.cashiro.presentation.navigation.AccountCategoryRoute(category.name))
    }
    val activity = LocalActivity.current
    val hazeStateBanner = remember { HazeState()}
    val blurEffects = themeUiState.blurEffects

    val snackbarHostState = remember { SnackbarHostState() }
    
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    
    
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, context.getString(R.string.press_back_again_to_close), Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()

    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showEditWidgetsSheet by remember { mutableStateOf(false) }

    // Haptic feedback
    val view = LocalView.current


    LaunchedEffect(Unit) {
        homeViewModel.onHomeVisible(
            activity = activity as? ComponentActivity,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    // ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        homeViewModel.refreshHiddenAccounts()
        onDispose {}
    }

    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            homeViewModel.clearDeletedTransaction()

            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.transaction_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already
                    // cleared
                    homeViewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }

    // Clear snackbar when navigating away
    DisposableEffect(Unit) { onDispose { snackbarHostState.currentSnackbarData?.dismiss() } }

    val lazyListState = rememberLazyListState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                GreetingCard(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(vertical = 12.dp),
                    userName = uiState.userName,
                    profileImageUri = uiState.profileImageUri,
                    profileBackgroundColor = uiState.profileBackgroundColor,
                    onProfileClick = onNavigateToSettings,
                    onNotificationClick = { navController.safeNavigate(NotificationSettings) },
                    onMoreClick = { showMoreBottomSheet = true }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            )
        }

    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Banner Image Background
                AnimatedVisibility(
                    visible = uiState.showBannerImage,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (uiState.bannerImageUri != null) {
                            AsyncImage(
                                model = uiState.bannerImageUri,
                                contentDescription = stringResource(R.string.banner),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(hazeStateBanner)
                                    .alpha(0.5f)
                                    .bottomFade(0.4f)
                                    .hazeSource(hazeStateBanner),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.banner_bg_image),
                                contentDescription = stringResource(R.string.banner),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.5f)
                                    .bottomFade(0.4f)
                                    .hazeSource(hazeStateBanner),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            HomeContentList(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                homeWidgets.forEach { widgetModel ->
                    if (widgetModel.isVisible) {
                        when (widgetModel.widget) {
                            HomeWidget.NETWORTH_SUMMARY -> {
                                item(key = "net_worth") {
                                    NetworthSummaryCards(
                                        uiState = uiState,
                                        onCurrencySelected = {
                                            homeViewModel.selectCurrency(it)
                                        },
                                        blurEffects = blurEffects && uiState.showBannerImage,
                                        hazeState = hazeStateBanner,
                                        overviewItems = if (showOverview) overviewItems else null,
                                        onOpenCategory = openCategory
                                    )
                                }
                            }
                            HomeWidget.LOANS -> {
                                item(key = "loans") {
                                    SharedTransitionLayout {
                                        LendBorrowCard(
                                            summary = uiState.lendBorrowSummary,
                                            onClick = { onNavigateToLendBorrow(null) },
                                            onLentClick = { onNavigateToLendBorrow(LendBorrowFilter.YOU_GET.name) },
                                            onBorrowedClick = { onNavigateToLendBorrow(LendBorrowFilter.YOU_OWE.name) },
                                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                                            currency = uiState.baseCurrency,
                                            blurEffects = blurEffects && uiState.showBannerImage,
                                            hazeState = hazeStateBanner,
                                            animatedContentScope = null
                                        )
                                    }
                                }
                            }
                            HomeWidget.TRANSACTION_HEATMAP -> {
                                item(key = "transaction_heatmap") {
                                    HeatmapWidget(
                                        data = uiState.transactionHeatmap,
                                        blurEffects = blurEffects && uiState.showBannerImage,
                                        hazeState = hazeStateBanner
                                    )
                                }
                            }
                            HomeWidget.BUDGET_CAROUSEL -> {
                                if (uiState.activeBudgets.isNotEmpty()) {
                                    item(key = "budget_carousel") {
                                        var lastBudgetClickTime by remember { mutableLongStateOf(0L) }
                                        SharedTransitionLayout {
                                            BudgetCarousel(
                                                budgets = uiState.activeBudgets,
                                                onBudgetClick = {
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - lastBudgetClickTime > 500) {
                                                        lastBudgetClickTime = currentTime
                                                        onNavigateToBudgets(it)
                                                    }
                                                },
                                                onEditClick = {
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - lastBudgetClickTime > 500) {
                                                        lastBudgetClickTime = currentTime
                                                        onNavigateToBudgets(it)
                                                    }
                                                },
                                                onHistoryClick = onNavigateToBudgetHistory,
                                                animatedVisibilityScope = null,
                                            )
                                        }
                                    }
                                }
                            }
                            HomeWidget.ACCOUNT_CAROUSEL -> {
                                if (!hasNetworth) item(key = "account_overview") {
                                    com.ritesh.cashiro.presentation.ui.features.accounts.AccountOverviewList(
                                        overviewItems, openCategory, Modifier.padding(horizontal = Dimensions.Padding.content))
                                }
                            }
                            HomeWidget.UPCOMING_SUBSCRIPTIONS -> {
                                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                                    item(key = "upcoming_subscriptions") {
                                        val cardModifier = Modifier.padding(
                                            start = Dimensions.Padding.content,
                                            end = Dimensions.Padding.content,
                                        )

                                        UpcomingSubscriptionsCard(
                                            subscriptions = uiState.upcomingSubscriptions,
                                            totalAmount = uiState.upcomingSubscriptionsTotal,
                                            currency = uiState.upcomingSubscriptionsCurrency,
                                            categoriesMap = categoriesMap,
                                            subcategoriesMap = subcategoriesMap,
                                            onClick = onNavigateToSubscriptions,
                                            blurEffects = blurEffects && uiState.showBannerImage,
                                            hazeState = hazeStateBanner,
                                            modifier = cardModifier
                                        )
                                    }
                                }
                            }
                            HomeWidget.RECENT_TRANSACTIONS -> {
                                item(key = "recent_transactions") {
                                    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    Column {
                                        Surface(
                                            modifier = Modifier
                                                .padding(horizontal = Spacing.md)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                                .then(
                                                    if (blurEffects && uiState.showBannerImage) Modifier.hazeEffect(
                                                        state = hazeStateBanner,
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
                                            shape = RoundedCornerShape(Dimensions.Radius.lg),
                                            color = if (blurEffects && uiState.showBannerImage) MaterialTheme.colorScheme.surface.copy(0.5f)
                                            else MaterialTheme.colorScheme.surface,
                                            contentColor = Color.Transparent,
                                        ) {
                                            Column {
                                                SectionHeader(
                                                    title = stringResource(R.string.recent),
                                                    action = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Search button
                                                            TextButton(
                                                                onClick = onNavigateToTransactionsWithSearch,
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        imageVector = Iconax.Search,
                                                                        contentDescription = stringResource(R.string.search_transactions),
                                                                        modifier = Modifier.size(Dimensions.Icon.small),
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(stringResource(R.string.search))
                                                                }

                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.padding(
                                                        start = 16.dp,
                                                        end = 8.dp,
                                                    )
                                                )

                                                if (uiState.isLoading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(Dimensions.Component.minTouchTarget * 2),
                                                        contentAlignment = Alignment.Center
                                                    ) { LoadingCircle() }
                                                } else if (uiState.recentTransactions.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(Dimensions.Padding.card),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            ) {
                                                            Icon(
                                                                imageVector = Iconax.ReceiptItem,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(48.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.height(Spacing.md))
                                                            Text(
                                                                text = stringResource(R.string.no_transactions_yet),
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    uiState.recentTransactions.forEachIndexed { index, transaction ->
                                                        val categoryEntity = categoriesMap[transaction.category]
                                                        val subcategoryEntity =
                                                            if (categoryEntity != null && transaction.subcategory != null) {
                                                                subcategoriesMap[transaction.subcategory]
                                                            } else null
                                                        val position = ListItemPosition.from(
                                                            index,
                                                            uiState.recentTransactions.size
                                                        )

                                                        val accountEntity = accountsMap["${transaction.bankName}_${transaction.accountNumber}"]
                                                        TransactionItem(
                                                            transaction = transaction,
                                                            categoryEntity = categoryEntity,
                                                            subcategoryEntity = subcategoryEntity,
                                                            accountIconResId = accountEntity?.iconResId ?: 0,
                                                            accountIconName = accountEntity?.iconName,
                                                            accountColorHex = accountEntity?.color,
                                                            convertedAmount = uiState.convertedAmounts[transaction.id],
                                                            mainCurrency = uiState.baseCurrency,
                                                            onClick = {
                                                                onTransactionClick(
                                                                    transaction.id,
                                                                    "transaction_${transaction.id}"
                                                                )
                                                            },
                                                            shape = position.toShape(),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            animatedContentScope = animatedContentScope,
                                                            sharedElementKey = "transaction_${transaction.id}",
                                                            linkedLoanPersonName = uiState.transactionPersonMapping[transaction.id]?.name,
                                                            linkedLoanPersonColor = uiState.transactionPersonMapping[transaction.id]?.color,
                                                            linkedLoanPersonAvatar = uiState.transactionPersonMapping[transaction.id]?.avatar
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(Spacing.sm))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextButton(
                                                onClick = onNavigateToTransactions,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                                    .then(
                                                        if (blurEffects && uiState.showBannerImage) Modifier.hazeEffect(
                                                            state = hazeStateBanner,
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
                                                    )
                                                    .height(26.dp),
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    containerColor = if (blurEffects && uiState.showBannerImage)
                                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)
                                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.view_all),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    modifier = Modifier.padding(horizontal = Spacing.md)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item{
                    Spacer(Modifier.height(200.dp)) //Extra space for better scroll
                }
            }

            // More Options BottomSheet
            if (showMoreBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.more_options),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = Spacing.sm).fillMaxWidth()
                        )



                        // Edit Widgets Option
                        ListItem(
                            headline = { Text(stringResource(R.string.edit_widgets)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Convertshape2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                showEditWidgetsSheet = true
                            },
                            shape = ListItemPosition.Top.toShape()
                        )

                        // Settings Option
                        ListItem(
                            headline = { Text(stringResource(R.string.settings)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Setting2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                onNavigateToSettings()
                            },
                            shape = ListItemPosition.Middle.toShape()
                        )



                        // Banner Image Toggle
                        PreferenceSwitch(
                            title = stringResource(R.string.show_banner_image),
                            leadingIcon = {
                                Icon(
                                    imageVector = Iconax.Gallery,
                                    contentDescription = null,
                                )
                            },
                            checked = uiState.showBannerImage,
                            onCheckedChange = { homeViewModel.toggleBannerImage() },
                            isLast = true
                        )
                    }
                }
            }

            // Breakdown Dialog
            if (uiState.showBreakdownDialog) {
                BreakdownDialog(
                    currentMonthIncome = uiState.currentMonthIncome,
                    currentMonthExpenses = uiState.currentMonthExpenses,
                    currentMonthTotal = uiState.currentMonthTotal,
                    lastMonthIncome = uiState.lastMonthIncome,
                    lastMonthExpenses = uiState.lastMonthExpenses,
                    lastMonthTotal = uiState.lastMonthTotal,
                    onDismiss = { homeViewModel.hideBreakdownDialog() }
                )
            }

            // Edit Widgets Sheet
            if (showEditWidgetsSheet) {
                EditWidgetsSheet(
                    onDismissRequest = { showEditWidgetsSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    widgets = homeWidgets,
                    onToggleVisibility = homeViewModel::toggleHomeWidgetVisibility,
                    onReorder = homeViewModel::updateWidgetsOrder
                )
            }
        }
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().capitalizeFirst()} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().capitalizeFirst()} 1-${now.dayOfMonth}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.calculation_breakdown),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = currentMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = currentMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = lastMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = lastMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.breakdown_formula_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs())}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color =
                if (isIncome) {
                    if (!isSystemInDarkTheme()) income_light else income_dark
                } else {
                    if (!isSystemInDarkTheme()) expense_light else expense_dark
                }
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun UpcomingSubscriptionsCard(
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    currency: String,
    categoriesMap: Map<String, CategoryEntity> = emptyMap(),
    subcategoriesMap: Map<String, SubcategoryEntity> = emptyMap(),
    onClick: () -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.Radius.xxl))
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
        shape = RoundedCornerShape(Dimensions.Radius.xxl),
        colors = CardDefaults.cardColors(
            containerColor = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscriptions_count_format, subscriptions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = Dimensions.Alpha.subtitle
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text =
                            CurrencyFormatter.formatCurrency(
                                totalAmount,
                                currency
                            ).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =
                            stringResource(R.string.per_month).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = Dimensions.Alpha.subtitle
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

            }

            SubscriptionIconsStack(
                subscriptions = subscriptions,
                iconSize = 38.dp,
                modifier = Modifier.padding(end = Spacing.sm),
                borderColor = MaterialTheme.colorScheme.surfaceContainerLow,
                categoriesMap = categoriesMap,
                subcategoriesMap = subcategoriesMap
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworthSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() },
    overviewItems: List<com.ritesh.cashiro.presentation.ui.features.accounts.AccountOverviewItem>? = null,
    onOpenCategory: (com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory) -> Unit = {},
) {
    var showCurrencySheet by remember { mutableStateOf(false) }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrency = uiState.selectedCurrency,
            availableCurrencies = uiState.availableCurrencies,
            onCurrencySelected = {
                onCurrencySelected(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    val context = LocalContext.current
    val abbreviatedName = remember(uiState.userName) {
        if (uiState.userName.contains(" ")) {
            uiState.userName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it[0] }
                .joinToString("")
                .uppercase()
        } else if (uiState.userName.length > 4) {
            uiState.userName.filter { it !in "aeiouAEIOU" }.take(4).uppercase().ifEmpty { 
                uiState.userName.take(4).uppercase() 
            }
        } else {
            uiState.userName.uppercase()
        }
    }

    val dateRangeLabel = remember(uiState.balanceHistory) {
        if (uiState.balanceHistory.size < 2) ""
        else {
            val start = uiState.balanceHistory.first().timestamp.toLocalDate()
            val end = uiState.balanceHistory.last().timestamp.toLocalDate()
            val days = ChronoUnit.DAYS.between(start, end)
            context.getString(R.string.last_days_format, days)
        }
    }

    val balanceContent: @Composable (Boolean) -> Unit = { embedded ->

        BalanceCard(
            totalBalance = uiState.totalBalance,
            monthlyChange = uiState.monthlyChange,
            monthlyChangePercent = uiState.monthlyChangePercent,
            currency = uiState.selectedCurrency,
            abbreviatedName = abbreviatedName,
            userName = uiState.userName,
            balanceHistory = uiState.balanceHistory,
            dateRangeLabel = dateRangeLabel,
            thisMonthValue = CurrencyFormatter.formatCurrency(uiState.currentMonthTotal, uiState.selectedCurrency),
            thisYearValue = CurrencyFormatter.formatCurrency(uiState.currentYearTotal, uiState.selectedCurrency),
            availableCurrenciesCount = uiState.availableCurrencies.size,
            onCurrencyClick = { showCurrencySheet = true },
            blurEffects = blurEffects && !embedded,
            embedded = embedded,
            hazeState = hazeState,
            modifier = if (embedded) Modifier else Modifier.padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
            )
        )
    }
    if (overviewItems != null) {
        com.ritesh.cashiro.presentation.ui.features.accounts.AccountOverviewPanel(
            overviewItems, onOpenCategory, Modifier.padding(horizontal = Dimensions.Padding.content)
        ) { balanceContent(true) }
    } else balanceContent(false)
}





