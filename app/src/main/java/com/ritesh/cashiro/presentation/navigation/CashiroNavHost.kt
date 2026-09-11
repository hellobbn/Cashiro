package com.ritesh.cashiro.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.ritesh.cashiro.presentation.ui.theme.MotionDurations

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountDetailScreen
import com.ritesh.cashiro.presentation.ui.features.accounts.AddAccountScreen
import com.ritesh.cashiro.presentation.ui.features.accounts.ManageAccountsScreen
import com.ritesh.cashiro.presentation.ui.features.add.AddScreen
import com.ritesh.cashiro.presentation.ui.features.analytics.AnalyticsScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetDetailScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetHistoryScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetsScreen
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesScreen
import com.ritesh.cashiro.presentation.ui.features.contacts.ContactsScreen
import com.ritesh.cashiro.presentation.ui.features.home.HomeScreen
import com.ritesh.cashiro.presentation.ui.features.lendborrow.LendBorrowScreen
import com.ritesh.cashiro.presentation.ui.features.lendborrow.PersonDetailScreen
import com.ritesh.cashiro.presentation.ui.features.onboarding.OnBoardingScreen
import com.ritesh.cashiro.presentation.ui.features.profile.ProfileScreen
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsScreen
import com.ritesh.cashiro.presentation.ui.features.settings.about.AboutScreen
import com.ritesh.cashiro.presentation.ui.features.settings.about.LicensesScreen
import com.ritesh.cashiro.presentation.ui.features.settings.currency.CurrencySettingsScreen
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.AppearanceScreen
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockScreen
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.DataPrivacyScreen
import com.ritesh.cashiro.presentation.ui.features.settings.cloudbackup.BackupSyncScreen
import com.ritesh.cashiro.presentation.ui.features.settings.developer.DeveloperScreen
import com.ritesh.cashiro.presentation.ui.features.settings.notifications.NotificationScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.CreateRuleScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesScreen
import com.ritesh.cashiro.presentation.ui.features.settings.quicktemplates.QuickTemplatesScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.webhooks.WebhookEditorScreen
import com.ritesh.cashiro.presentation.ui.features.settings.webhooks.WebhooksScreen
import com.ritesh.cashiro.presentation.ui.features.subscriptions.SubscriptionsScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.ExportTransactionsDialog
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionDetailScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsViewModel
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.icons.ImportArrow01
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun CashiroNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }

    // Get theme settings for bottom nav style
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeUiState by themeViewModel.themeUiState.collectAsState()

    // Track current destination for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    ObserveMainTabSettled(navBackStackEntry)
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Check if current route is in bottom nav routes
    val showBottomNav = BOTTOM_NAV_ROUTES.any { qualifiedName ->
        currentRoute?.contains(qualifiedName ?: "") == true
    }

    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val view = LocalView.current

    // State for full resync confirmation dialog
    var showExportDialog by remember { mutableStateOf(false) }

    val isHomeScreen = currentRoute?.contains(Home::class.qualifiedName ?: "") == true
    val isAnalyticsScreen = currentRoute?.contains(Analytics::class.qualifiedName ?: "") == true
    val isTransactionsScreen = currentRoute?.contains(Transactions::class.qualifiedName ?: "") == true
    val isAddTransactionScreen = currentRoute?.contains(AddTransaction::class.qualifiedName ?: "") == true
    val isSubscriptionsScreen = currentRoute?.contains(Subscriptions::class.qualifiedName ?: "") == true
    val isBudgetDetailScreen = currentRoute?.contains(BudgetDetail::class.qualifiedName ?: "") == true


    val hazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ordinary navigation must not put every scrolling destination in a shared lookahead layout.
        Box {
            NavHost(
                navController = navController,
                startDestination = stableStartDestination,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            ) {
                // App Lock Screen
                composable<AppLock>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    AppLockScreen(
                        onUnlocked = {
                            navController.safeNavigate(Home) {
                                popUpTo(AppLock) { inclusive = true }
                            }
                        }
                    )
                }

                // Onboarding Screen
                composable<OnBoarding>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    OnBoardingScreen(
                        onOnBoardingComplete = {
                            navController.safeNavigate(Home) {
                                popUpTo(OnBoarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Investments> {
                    com.ritesh.cashiro.presentation.ui.features.investments.InvestmentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onManageManualAccounts = { navController.safeNavigate(AccountCategoryRoute("INVESTMENTS")) }
                    )
                }

                /* BOTTOM NAV SCREENS ---- */
                // Home Screen
                composable<Home>(
                    enterTransition = MainTabMotion.enter,
                    exitTransition = MainTabMotion.exit,
                    popEnterTransition = MainTabMotion.popEnter,
                    popExitTransition = MainTabMotion.popExit
                ) {
                    SharedTransitionLayout {
                        HomeScreen(
                            navController = navController,
                            onNavigateToSettings = { navController.safeNavigate(Settings) },
                            onNavigateToTransactions = { navController.safeNavigate(Transactions()) },
                            onNavigateToTransactionsWithSearch = {
                                navController.safeNavigate(Transactions(focusSearch = true))
                            },
                            onNavigateToSubscriptions = { navController.safeNavigate(Subscriptions) },
                            onNavigateToBudgets = { id ->
                                if (id != null) {
                                    navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = "budget_card_$id"))
                                } else {
                                    navController.safeNavigate(Budgets())
                                }
                            },
                            onNavigateToBudgetHistory = { id ->
                                navController.safeNavigate(BudgetHistory(id))
                            },
                            onNavigateToLendBorrow = { filter -> navController.safeNavigate(LendBorrow(filter)) },
                            onTransactionClick = { transactionId, key ->
                                navController.safeNavigate(TransactionDetail(transactionId, key))
                            },
                            animatedContentScope = this@composable,
                        )
                    }
                }

                // Analytics Screen
                composable<Analytics>(
                    enterTransition = MainTabMotion.enter,
                    exitTransition = MainTabMotion.exit,
                    popEnterTransition = MainTabMotion.popEnter,
                    popExitTransition = MainTabMotion.popExit
                ) {
                    SharedTransitionLayout {
                        AnalyticsScreen(
                            onNavigateToTransactions = { category, merchant, period, currency ->
                                navController.safeNavigate(
                                    Transactions(
                                        category = category,
                                        merchant = merchant,
                                        period = period,
                                        currency = currency
                                    )
                                )
                            },
                            animatedContentScope = this@composable,
                            blurEffects = themeUiState.blurEffects,
                        )
                    }
                }

                /* SETTINGS & SUB-SCREENS ---- */
                composable<Settings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    SettingsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCategories = { navController.safeNavigate(Categories) },
                        onNavigateToManageAccounts = { navController.safeNavigate(ManageAccounts) },
                        onNavigateToRules = { navController.safeNavigate(Rules) },
                        onNavigateToQuickTemplates = { navController.safeNavigate(QuickTemplates) },
                        onNavigateToAppearance = { navController.safeNavigate(Appearance) },
                        onNavigateToProfile = { navController.safeNavigate(Profile) },
                        onNavigateToNotifications = { navController.safeNavigate(NotificationSettings) },
                        onNavigateToWebhooks = { navController.safeNavigate(Webhooks) },
                        onNavigateToBudgets = { navController.safeNavigate(Budgets()) },
                        onNavigateToLendBorrow = { navController.safeNavigate(LendBorrow()) },
                        onNavigateToDataPrivacy = { navController.safeNavigate(DataPrivacy) },
                        onNavigateToCloudBackup = { navController.safeNavigate(CloudBackup) },
                        onNavigateToAbout = { navController.safeNavigate(About) },
                        onNavigateToCurrency = { navController.safeNavigate(CurrencySettings) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Webhooks>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    WebhooksScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToEditor = { profileId ->
                            navController.safeNavigate(WebhookEditor(profileId))
                        }
                    )
                }

                composable<WebhookEditor>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<WebhookEditor>()
                    WebhookEditorScreen(
                        profileId = route.profileId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<About>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    AboutScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToLicenses = { navController.safeNavigate(Licenses) },
                        onNavigateToDeveloper = { navController.safeNavigate(DeveloperOptions) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Licenses>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    LicensesScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<DataPrivacy>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    DataPrivacyScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccounts = { navController.safeNavigate(ManageAccounts) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<CloudBackup>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    BackupSyncScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccounts = { navController.safeNavigate(ManageAccounts) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<DeveloperOptions>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    DeveloperScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<CurrencySettings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    CurrencySettingsScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Profile>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    SharedTransitionLayout {
                        ProfileScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onNavigateToContacts = { navController.safeNavigate(Contacts()) },
                            onNavigateToPerson = { personId ->
                                navController.safeNavigate(
                                    PersonDetail(personId, "person_avatar_$personId")
                                )
                            },
                            animatedContentScope = this@composable
                        )
                    }
                }

                composable<Contacts>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val contactsRoute = backStackEntry.toRoute<Contacts>()
                        ContactsScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onNavigateToPersonDetail = { personId ->
                                navController.safeNavigate(
                                    PersonDetail(personId, "person_avatar_$personId")
                                )
                            },
                            selectedPersonId = contactsRoute.personId,
                            animatedContentScope = this@composable,
                            blurEffects = themeUiState.blurEffects
                        )
                    }
                }

                composable<Appearance>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    AppearanceScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<NotificationSettings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    NotificationScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                composable<Categories>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    CategoriesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }


                composable<AccountCategoryRoute>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { entry ->
                    val selected = com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory.entries.firstOrNull {
                        it.name == entry.toRoute<AccountCategoryRoute>().category
                    } ?: com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory.WALLETS
                    ManageAccountsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountDetail = { name, suffix -> navController.safeNavigate(AccountDetail(name, suffix)) },
                        onNavigateToAddAccount = { cat -> navController.safeNavigate(AddAccount(cat?.name)) },
                        blurEffects = themeUiState.blurEffects,
                        category = selected
                    )
                }

                composable<ManageAccounts>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    ManageAccountsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountDetail = { bankName, last4 ->
                            navController.safeNavigate(AccountDetail(bankName, last4))
                        },
                        onNavigateToAddAccount = { cat -> navController.safeNavigate(AddAccount(cat?.name)) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<AddAccount>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { entry ->
                    val initialCategory = entry.toRoute<AddAccount>().category?.let { name ->
                        com.ritesh.cashiro.presentation.ui.features.accounts.AccountCategory.entries.firstOrNull { it.name == name }
                    }
                    AddAccountScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        initialCategory = initialCategory
                    )
                }

                composable<QuickTemplates>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    QuickTemplatesScreen(onNavigateBack = { navController.safePopBackStack() })
                }

                composable<Rules>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    RulesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCreateRule = { navController.safeNavigate(CreateRule()) },
                        onEditRule = { rule ->
                            navController.safeNavigate(CreateRule(ruleId = rule.id))
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<CreateRule>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val createRuleRoute = backStackEntry.toRoute<CreateRule>()
                    val rulesViewModel: RulesViewModel = hiltViewModel()
                    val rules by rulesViewModel.rules.collectAsState()
                    val existingRule = remember(createRuleRoute.ruleId, rules) {
                        rules.find { it.id == createRuleRoute.ruleId }
                    }

                    CreateRuleScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onSaveRule = { rule ->
                            if (createRuleRoute.ruleId != null) {
                                rulesViewModel.updateRule(rule)
                            } else {
                                rulesViewModel.createRule(rule)
                            }
                            navController.safePopBackStack()
                        },
                        existingRule = existingRule,
                        rulesViewModel = rulesViewModel
                    )
                }

                /* DETAIL SCREENS (with shared transitions) ---- */
                composable<TransactionDetail>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
                        TransactionDetailScreen(
                            transactionId = transactionDetail.transactionId,
                            sharedElementKey = transactionDetail.sharedElementKey,
                            onNavigateBack = {
                                onEditComplete()
                                navController.safePopBackStack()
                            },
                            onNavigateToPersonDetail = { personId ->
                                navController.safeNavigate(PersonDetail(personId, "person_avatar_$personId"))
                            },
                            animatedContentScope = this@composable,
                            blurEffects = themeUiState.blurEffects,
                        )
                    }
                }

                composable<AddTransaction>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    Box(Modifier.fillMaxSize())
                }

                composable<AccountDetail>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val accountDetail = backStackEntry.toRoute<AccountDetail>()
                        AccountDetailScreen(
                            navController = navController,
                            bankName = accountDetail.bankName,
                            accountLast4 = accountDetail.accountLast4,
                            animatedContentScope = this@composable
                        )
                    }
                }

                composable<Subscriptions>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    SharedTransitionLayout {
                        SubscriptionsScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onEditSubscription = { id ->
                                navController.safeNavigate(AddTransaction(initialTab = 1, subscriptionId = id))
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this@composable
                        )
                    }
                }

                composable<Transactions>(
                    enterTransition = MainTabMotion.enter,
                    exitTransition = MainTabMotion.exit,
                    popEnterTransition = MainTabMotion.popEnter,
                    popExitTransition = MainTabMotion.popExit
                ) { backStackEntry ->
                    val transactions = backStackEntry.toRoute<Transactions>()
                    TransactionsScreen(
                        transactionsViewModel = transactionsViewModel,
                        initialCategory = transactions.category,
                        initialMerchant = transactions.merchant,
                        initialPeriod = transactions.period,
                        initialCurrency = transactions.currency,
                        initialType = transactions.type,
                        focusSearch = transactions.focusSearch,
                        onNavigateBack = { navController.safePopBackStack() },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        onNavigateToSettings = {
                            navController.safeNavigate(Settings)
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Budgets>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val budgets = backStackEntry.toRoute<Budgets>()
                        BudgetsScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onBudgetClick = { id, key ->
                                navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = key))
                            },
                            onHistoryClick = { id ->
                                navController.safeNavigate(BudgetHistory(id))
                            },
                            animatedContentScope = this@composable,
                            sharedElementPrefix = budgets.sharedElementPrefix,
                            blurEffects = themeUiState.blurEffects
                        )
                    }
                }

                composable<BudgetDetail>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val budgetDetail = backStackEntry.toRoute<BudgetDetail>()
                        BudgetDetailScreen(
                            budgetId = budgetDetail.budgetId,
                            startDate = budgetDetail.startDate,
                            endDate = budgetDetail.endDate,
                            onNavigateBack = { navController.safePopBackStack() },
                            onNavigateToHistory = { id -> navController.safeNavigate(BudgetHistory(id)) },
                            onTransactionClick = { transactionId, key ->
                                navController.safeNavigate(TransactionDetail(transactionId, key))
                            },
                            animatedContentScope = this@composable,
                            sharedElementKey = budgetDetail.sharedElementKey,
                            blurEffects = themeUiState.blurEffects
                        )
                    }
                }

                composable<BudgetHistory>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val budgetHistory = backStackEntry.toRoute<BudgetHistory>()
                        BudgetHistoryScreen(
                            budgetId = budgetHistory.budgetId,
                            onNavigateBack = { navController.safePopBackStack() },
                            onNavigateToDetail = { id, start, end ->
                                navController.safeNavigate(BudgetDetail(
                                    budgetId = id,
                                    startDate = start?.toString(),
                                    endDate = end?.toString()
                                ))
                            }
                        )
                    }
                }

                composable<LendBorrow>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    SharedTransitionLayout {
                        LendBorrowScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onNavigateToPersonDetail = { personId ->
                                navController.safeNavigate(
                                    PersonDetail(personId, "person_avatar_$personId")
                                )
                            },
                            animatedContentScope = this@composable,
                            blurEffects = themeUiState.blurEffects
                        )
                    }
                }

                composable<PersonDetail>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) { backStackEntry ->
                    SharedTransitionLayout {
                        val personDetail = backStackEntry.toRoute<PersonDetail>()
                        PersonDetailScreen(
                            onNavigateBack = { navController.safePopBackStack() },
                            onTransactionClick = { transactionId, key ->
                                navController.safeNavigate(TransactionDetail(transactionId, key))
                            },
                            sharedElementKey = personDetail.sharedElementKey,
                            animatedContentScope = this@composable
                        )
                    }
                }
            }
        }

        SharedTransitionLayout {
            // Add Screen Overlay - Handled here for shared transition from FAB
            AnimatedVisibility(
                visible = isAddTransactionScreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val addTransaction = if (isAddTransactionScreen) {
                    try { navBackStackEntry?.toRoute<AddTransaction>() ?: AddTransaction() }
                    catch (_: Exception) { AddTransaction() }
                } else AddTransaction()

                AddScreen(
                    onNavigateBack = { navController.safePopBackStack() },
                    animatedVisibilityScope = this@AnimatedVisibility,
                    initialTab = addTransaction.initialTab,
                    subscriptionId = addTransaction.subscriptionId,
                    transactionType = addTransaction.type,
                    blurEffects = themeUiState.blurEffects,
                )
            }

            // FABs Container - Shown on Home, Transactions, Subscriptions, and Budget Detail
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = isHomeScreen || isTransactionsScreen || isSubscriptionsScreen || isBudgetDetailScreen,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimensions.Padding.content)
                        .padding(
                            bottom = if (showBottomNav) 84.dp else 10.dp
                        )
                        .navigationBarsPadding()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        val smallFabContainerColor =  MaterialTheme.colorScheme.tertiaryContainer
                        val smallFabContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        // Secondary FAB (Export)
                        if (isTransactionsScreen) {
                            SmallFloatingActionButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    showExportDialog = true
                                },
                                containerColor = smallFabContainerColor,
                                contentColor = smallFabContentColor,
                            ) {
                                Icon(
                                    imageVector = Iconax.ImportArrow01,
                                    contentDescription = stringResource(R.string.export_transactions),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        val fabContainerColor =  MaterialTheme.colorScheme.primaryContainer
                        val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer

                        // Add FAB
                        FloatingActionButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                val initialTab = if (isSubscriptionsScreen) 1 else 0
                                navController.safeNavigate(AddTransaction(initialTab = initialTab))
                            },
                            modifier = Modifier
                                .then(
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "fab_to_add"),
                                        animatedVisibilityScope = this@AnimatedVisibility,
                                        boundsTransform = { _, _ ->
                                            tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                            contentScale = ContentScale.FillBounds,
                                            alignment = Alignment.Center
                                        )
                                    )
                                        .skipToLookaheadSize()
                                )
                                .clip(MaterialTheme.shapes.large),
                            containerColor = fabContainerColor,
                            contentColor = fabContentColor,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_transaction_subscription_cd),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }



            // Export Transactions Dialog (Only when on TransactionsScreen)
            if (showExportDialog && isTransactionsScreen) {
                // Collected inside the branch so the export list is only observed while the
                // dialog is actually open.
                val transactionsUiState by transactionsViewModel.uiState.collectAsStateWithLifecycle()
                ExportTransactionsDialog(
                    transactions = transactionsUiState.transactions,
                    onDismiss = { showExportDialog = false },
                    blurEffects = themeUiState.blurEffects,
                    hazeState = hazeState
                )
            }
        }

        // Block pointer input while a navigation transition is in progress so a quick tap
        // meant for the destination screen doesn't hit the still-composed outgoing screen.
        // Placed under the navigation bar so tab taps are never swallowed: switching tabs
        // rapidly used to drop taps that landed during the previous tab's fade.
        NavigationTransitionInputBlocker(
            navController = navController,
            isAddTransactionScreen = isAddTransactionScreen
        )

        // Bottom Navigation
        CashiroBottomNavigation(
            navController = navController,
            currentDestination = currentDestination,
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Overlays a transparent full-screen surface that consumes all pointer input while the current
 * destination is not yet RESUMED (i.e. a navigation transition is animating). Shared element
 * transitions hold the outgoing screen composed during the transition, which would otherwise
 * let a quick tap meant for the list hit a clickable element on the screen being popped.
 */
private const val TransitionBlockGraceMillis = 600L

@Composable
private fun NavigationTransitionInputBlocker(
    navController: NavHostController,
    isAddTransactionScreen: Boolean
) {
    val entry by navController.currentBackStackEntryAsState()
    val topState = entry?.lifecycle?.let { lifecycle ->
        lifecycle.currentStateFlow.collectAsStateWithLifecycle(
            initialValue = lifecycle.currentState,
            lifecycle = lifecycle
        ).value
    }
    val inTransition = !isAddTransactionScreen &&
        topState != null && topState != Lifecycle.State.RESUMED
    // Safety valve: a transition interrupted by rapid tab taps can leave the top entry STARTED
    // without ever reaching RESUMED. Blocking must never outlive a real transition, so it ends
    // after a short grace period regardless of lifecycle state.
    var timedOut by remember(entry?.id) { mutableStateOf(false) }
    LaunchedEffect(entry?.id, inTransition) {
        timedOut = false
        if (inTransition) {
            kotlinx.coroutines.delay(TransitionBlockGraceMillis)
            timedOut = true
        }
    }
    val shouldBlock = inTransition && !timedOut
    if (shouldBlock) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
        )
    }
}
