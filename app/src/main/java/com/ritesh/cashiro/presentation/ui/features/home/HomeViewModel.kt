package com.ritesh.cashiro.presentation.ui.features.home

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.utils.sumOfBigDecimal
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.manager.InAppReviewManager
import com.ritesh.cashiro.data.manager.InAppUpdateManager
import com.ritesh.cashiro.data.preferences.HomeWidget
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.brokerage.BrokerageRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.domain.usecase.excludingHidden
import com.ritesh.cashiro.domain.usecase.netWorthIn
import com.ritesh.cashiro.presentation.ui.features.accounts.investmentSnapshotsOrEmpty
import com.ritesh.cashiro.domain.model.PersonInfo
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    private val inAppUpdateManager: InAppUpdateManager,
    private val inAppReviewManager: InAppReviewManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val lendBorrowRepository: com.ritesh.cashiro.data.repository.LendBorrowRepository,
    private val brokerageRepository: BrokerageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()

    private val _homeWidgets = MutableStateFlow<List<HomeWidgetUiModel>>(emptyList())
    val homeWidgets: StateFlow<List<HomeWidgetUiModel>> = _homeWidgets.asStateFlow()

    val accountOverviewInNetworth: StateFlow<Boolean> = userPreferencesRepository.accountOverviewInNetworth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val accountsMap = accountBalanceRepository.getAllLatestBalances()
        .map { accountList ->
            accountList.associateBy { "${it.bankName}_${it.accountLast4}" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Store currency breakdown maps for quick access when switching currencies
    private var currentMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()
    private var lastMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()
    private var currentYearBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> =
        emptyMap()

    private val baseCurrency = currencyRepository.effectiveBaseCurrencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CNY")

    private val _selectedCurrency = MutableStateFlow<String?>(null)
    private val selectedCurrencyCombined = combine(
        baseCurrency,
        _selectedCurrency
    ) { base, selected ->
        selected ?: base
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CNY")

    init {
        viewModelScope.launch { runCatching { brokerageRepository.load() } }
        loadHomeData()
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { preferences ->
                _uiState.value = _uiState.value.copy(
                    userName = preferences.userName,
                    profileImageUri = preferences.profileImageUri?.toUri(),
                    profileBackgroundColor = Color(preferences.profileBackgroundColor),
                    bannerImageUri = preferences.bannerImageUri?.toUri(),
                    showBannerImage = preferences.showBannerImage
                )
            }
        }

        viewModelScope.launch {
            combine(
                userPreferencesRepository.homeWidgetsOrder,
                userPreferencesRepository.hiddenHomeWidgets
            ) { order, hidden ->
                val allWidgets = HomeWidget.entries.toMutableList()
                
                // Construct the final list based on saved order
                val orderedWidgets = mutableListOf<HomeWidgetUiModel>()
                
                // Net Worth (Always first)
                orderedWidgets.add(HomeWidgetUiModel(HomeWidget.NETWORTH_SUMMARY, true))
                
                //ordered widgets
                order.forEach { widget ->
                     if (widget != HomeWidget.NETWORTH_SUMMARY) { // Prevent duplicates just in case
                         orderedWidgets.add(HomeWidgetUiModel(widget, !hidden.contains(widget)))
                         allWidgets.remove(widget)
                     }
                }
                allWidgets.remove(HomeWidget.NETWORTH_SUMMARY)

                // any remaining widgets (newly added ones in enum)
                allWidgets.sortedBy { it.defaultOrder }.forEach { widget ->
                    orderedWidgets.add(HomeWidgetUiModel(widget, !hidden.contains(widget)))
                }
                
                orderedWidgets
            }.collect { widgets ->
                _homeWidgets.value = widgets
            }
        }
    }

    private fun loadHomeData() {
        // Observe base currency changes
        viewModelScope.launch {
            baseCurrency.collect { mainAccountCurrency ->
                val prevCurrency = _uiState.value.selectedCurrency
                if (mainAccountCurrency != prevCurrency) {
                    _uiState.update { it.copy(
                        selectedCurrency = mainAccountCurrency,
                        baseCurrency = mainAccountCurrency
                    ) }
                    _selectedCurrency.value = mainAccountCurrency
                    // Recalculate breakdown totals and account balances for the new currency
                    val availableCurrencies = _uiState.value.availableCurrencies
                    if (availableCurrencies.isNotEmpty()) {
                        updateUIStateForCurrency(mainAccountCurrency, availableCurrencies)
                    }
                }
            }
        }

        viewModelScope.launch {
            // Load current month breakdown by currency
            combine(
                transactionRepository.getCurrentMonthBreakdownByCurrency(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { breakdownByCurrency, selectedCurrency, _ ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.CURRENT_MONTH, selectedCurrency = selectedCurrency)
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load account balances and react to currency changes
            combine(
                accountBalanceRepository.getAllLatestBalances(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger,
                brokerageRepository.connections
            ) { allBalances, selectedCurrency, _, connections ->
                // Get hidden accounts from SharedPreferences
                val hiddenAccounts =
                    sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

                // Filter out hidden accounts
                val balances = allBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                // Keep zero-balance accounts discoverable in the grouped account list.
                val regularAccounts =
                    balances.filter { !it.isCreditCard }
                val creditCards = balances.filter { it.isCreditCard }

                // Account loading completed
                Log.d("HomeViewModel", "Loaded ${balances.size} account(s)")

                // Check if we have multiple currencies and refresh exchange rates if needed
                val accountCurrencies = regularAccounts.map { it.currency }.distinct()
                val hasMultipleCurrencies = accountCurrencies.size > 1

                if (hasMultipleCurrencies && accountCurrencies.isNotEmpty()) {
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                val totalBalanceInSelectedCurrency = balances.netWorthIn(
                    selectedCurrency,
                    currencyConversionService,
                    investmentSnapshots = connections.investmentSnapshotsOrEmpty()
                )

                var totalAvailableCreditInSelectedCurrency = BigDecimal.ZERO
                for (card in creditCards) {
                    val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                    val amt = if (card.currency == selectedCurrency) {
                        availableInCardCurrency
                    } else {
                        currencyConversionService.convertAmount(
                            amount = availableInCardCurrency,
                            fromCurrency = card.currency,
                            toCurrency = selectedCurrency
                        )
                    }
                    totalAvailableCreditInSelectedCurrency = totalAvailableCreditInSelectedCurrency.add(amt)
                }

                _uiState.update { 
                    it.copy(
                        accountBalances = regularAccounts,
                        creditCards = creditCards,
                        totalBalance = totalBalanceInSelectedCurrency,
                        totalAvailableCredit = totalAvailableCreditInSelectedCurrency,
                        selectedCurrency = selectedCurrency
                    )
                }
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load current month transactions by type (currency-filtered)
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

            combine(
                transactionRepository.getTransactionsBetweenDates(
                    startDate = startOfMonth,
                    endDate = endOfMonth
                ),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { transactions, selectedCurrency, _ ->
                var creditCardTotal = BigDecimal.ZERO
                var transferTotal = BigDecimal.ZERO
                var investmentTotal = BigDecimal.ZERO

                transactions.forEach { tx ->
                    val convertedAmount = if (tx.currency == selectedCurrency) {
                        tx.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = tx.amount,
                            fromCurrency = tx.currency,
                            toCurrency = selectedCurrency
                        ) ?: tx.amount
                    }

                    when (tx.transactionType) {
                        TransactionType.CREDIT -> creditCardTotal += convertedAmount
                        TransactionType.TRANSFER -> transferTotal += convertedAmount
                        TransactionType.INVESTMENT -> investmentTotal += convertedAmount
                        else -> {}
                    }
                }

                _uiState.update { it.copy(
                    currentMonthCreditCard = creditCardTotal,
                    currentMonthTransfer = transferTotal,
                    currentMonthInvestment = investmentTotal
                ) }
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load heatmap data (last 26 weeks)
            val endOfHeatmap = LocalDate.now()
            val startOfHeatmap = endOfHeatmap.minusWeeks(26).with(java.time.DayOfWeek.MONDAY)
            
            transactionRepository.getTransactionsBetweenDates(
                startDate = startOfHeatmap,
                endDate = endOfHeatmap
            ).map { transactions ->
                transactions.groupBy { it.dateTime.toLocalDate() }
                    .mapValues { it.value.size }
            }.flowOn(Dispatchers.Default).collect { heatmap ->
                _uiState.update { it.copy(transactionHeatmap = heatmap) }
            }
        }

        viewModelScope.launch {
            // Load last month breakdown by currency
            combine(
                transactionRepository.getLastMonthBreakdownByCurrency(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { breakdownByCurrency, selectedCurrency, _ ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.LAST_MONTH, selectedCurrency = selectedCurrency)
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load current year breakdown by currency
            combine(
                transactionRepository.getCurrentYearBreakdownByCurrency(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { breakdownByCurrency, selectedCurrency, _ ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, period = FinancialPeriod.CURRENT_YEAR, selectedCurrency = selectedCurrency)
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load recent transactions (last 3) and react to base currency changes
            combine(
                transactionRepository.getRecentTransactions(limit = 3),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { transactions, selectedCurrency, _ ->
                // Calculate converted amounts for shown transactions if transaction currency differs from selected currency
                val converted = transactions
                    .filter { it.currency != selectedCurrency }
                    .associate { tx ->
                        tx.id to (currencyConversionService.convertAmount(tx.amount, tx.currency, selectedCurrency) ?: tx.amount)
                    }
                
                _uiState.update { it.copy(
                    recentTransactions = transactions,
                    convertedAmounts = converted,
                    isLoading = false
                ) }
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load all active subscriptions and react to currency changes
            combine(
                subscriptionRepository.getActiveSubscriptions(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { subscriptions, targetCurrency, _ ->
                // Check if we need to refresh rates for subscription currencies
                val subscriptionCurrencies = subscriptions.map { it.currency }.distinct()
                if (subscriptionCurrencies.any { it != targetCurrency }) {
                    currencyConversionService.refreshExchangeRatesForAccount(subscriptionCurrencies + targetCurrency)
                }

                var totalAmount = BigDecimal.ZERO
                for (subscription in subscriptions) {
                    val amt = if (subscription.currency == targetCurrency) {
                        subscription.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = subscription.amount,
                            fromCurrency = subscription.currency,
                            toCurrency = targetCurrency
                        )
                    }
                    totalAmount = totalAmount.add(amt)
                }

                _uiState.update {
                    it.copy(
                        upcomingSubscriptions = subscriptions,
                        upcomingSubscriptionsTotal = totalAmount,
                        upcomingSubscriptionsCurrency = targetCurrency
                    )
                }
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load active budgets for current month and react to currency changes
            val yearMonth = YearMonth.now()
            combine(
                budgetRepository.getBudgetsWithSpendingForMonth(yearMonth.year, yearMonth.monthValue),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { budgets, targetCurrency, _ ->
                // Convert budgets to match the selected main currency for display
                val convertedBudgets = budgets.map { budgetWithSpending ->
                    if (budgetWithSpending.budget.currency != targetCurrency) {
                        val convertedAmount = currencyConversionService.convertAmount(
                            budgetWithSpending.budget.amount,
                            budgetWithSpending.budget.currency,
                            targetCurrency
                        ) ?: budgetWithSpending.budget.amount

                        val convertedSpending = currencyConversionService.convertAmount(
                            budgetWithSpending.currentSpending,
                            budgetWithSpending.budget.currency,
                            targetCurrency
                        ) ?: budgetWithSpending.currentSpending

                        budgetWithSpending.copy(
                            budget = budgetWithSpending.budget.copy(
                                amount = convertedAmount,
                                currency = targetCurrency
                            ),
                            currentSpending = convertedSpending
                        )
                    } else {
                        budgetWithSpending
                    }
                }
                
                _uiState.update { 
                    it.copy(activeBudgets = convertedBudgets)
                }
            }.flowOn(Dispatchers.Default).collectLatest { }
        }

        viewModelScope.launch {
            // Load portfolio balance history for the last 180 days, reactive to currency changes
            val endDate = LocalDateTime.now()
            val startDate = endDate.minusDays(180)

            combine(
                accountBalanceRepository.getAllBalances(),
                selectedCurrencyCombined,
                currencyConversionService.rateChangeTrigger
            ) { allBalances, selectedCurrency, _ ->
                allBalances to selectedCurrency
            }.collectLatest { (allBalances, selectedCurrency) ->
                // Group balances by date to calculate daily totals
                val dailyPortfolioHistory = allBalances
                    .filter { it.timestamp.isAfter(startDate) }
                    .groupBy { it.timestamp.toLocalDate() }
                    .mapValues { (_, balances) ->
                        // For each day, keep only the latest balance for each unique account
                        val latestBalancesPerAccount = balances
                            .groupBy { "${it.bankName}_${it.accountLast4}" }
                            .mapValues { (_, accountBalances) ->
                                accountBalances.maxByOrNull { it.timestamp }
                            }

                        var dayTotal = BigDecimal.ZERO
                        for (account in latestBalancesPerAccount.values.filterNotNull()) {
                            val balanceValue = if (account.isCreditCard) account.balance.negate() else account.balance
                            val amt = if (account.currency == selectedCurrency) {
                                balanceValue
                            } else {
                                currencyConversionService.convertAmount(
                                    amount = balanceValue,
                                    fromCurrency = account.currency,
                                    toCurrency = selectedCurrency
                                )
                            }
                            dayTotal = dayTotal.add(amt)
                        }
                        dayTotal
                    }
                    .toSortedMap()
                    .map { (date, total) ->
                        BalancePoint(
                            timestamp = date.atStartOfDay(),
                            balance = total,
                            currency = selectedCurrency
                        )
                    }

                _uiState.update { it.copy(
                    balanceHistory = dailyPortfolioHistory
                ) }
            }
        }

        viewModelScope.launch {
            lendBorrowRepository.getSummary().collect { summary ->
                _uiState.update { it.copy(lendBorrowSummary = summary) }
            }
        }

        viewModelScope.launch {
            combine(
                lendBorrowRepository.getAllTransactions(),
                lendBorrowRepository.getPersons()
            ) { lbTransactions, persons ->
                val personMap = persons.associateBy { it.id }
                lbTransactions
                    .filter { it.transactionId != null }
                    .associate { lb ->
                        val person = personMap[lb.personId]
                        lb.transactionId!! to PersonInfo(
                            name = person?.name ?: lb.title,
                            color = person?.color ?: "#4CAF50",
                            avatar = person?.avatar
                        )
                    }
            }.collect { mapping ->
                _uiState.update { it.copy(transactionPersonMapping = mapping) }
            }
        }
    }

    private fun calculateMonthlyChange() {
        val currentExpenses = _uiState.value.currentMonthExpenses
        val lastExpenses = _uiState.value.lastMonthExpenses
        val currentTotal = _uiState.value.currentMonthTotal
        val lastTotal = _uiState.value.lastMonthTotal

        // Calculate expense change for simple comparison
        val expenseChange = currentExpenses - lastExpenses
        val totalChange = currentTotal - lastTotal

        val monthlyChangePercent = if (lastTotal != BigDecimal.ZERO) {
            ((totalChange.toDouble() / lastTotal.toDouble()) * 100).toInt()
        } else if (totalChange != BigDecimal.ZERO) {
            100 // Assume 100% growth if starting from zero
        } else {
            0
        }

        _uiState.value = _uiState.value.copy(
            monthlyChange = totalChange,
            monthlyChangePercent = monthlyChangePercent
        )
    }

    fun refreshHiddenAccounts() {
        viewModelScope.launch {
            // Force re-read of hidden accounts from SharedPreferences
            val hiddenAccounts =
                sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

            // Re-fetch all accounts and filter
            accountBalanceRepository.getAllLatestBalances().first().let { allBalances: List<AccountBalanceEntity> ->
                val visibleBalances: List<AccountBalanceEntity> = allBalances.excludingHidden(hiddenAccounts)

                // Keep zero-balance accounts discoverable in the grouped account list.
                val regularAccounts: List<AccountBalanceEntity> =
                    visibleBalances.filter { !it.isCreditCard }
                val creditCards: List<AccountBalanceEntity> = visibleBalances.filter { it.isCreditCard }

                val selectedCurrency = _uiState.value.selectedCurrency

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    accountBalances = regularAccounts,
                    creditCards = creditCards,
                    // Same rule as the steady-state path: converted, and credit cards are debt.
                    totalBalance = visibleBalances.netWorthIn(
                        selectedCurrency,
                        currencyConversionService,
                        investmentSnapshots = brokerageRepository.connections.value.investmentSnapshotsOrEmpty()
                    ),
                    totalAvailableCredit = creditCards.sumOfBigDecimal { card: AccountBalanceEntity ->
                        // Available = Credit Limit - Outstanding Balance
                        (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                    }
                )
            }
        }
    }

    fun refreshAccountBalances() {
        viewModelScope.launch {
            // Force refresh the account balances by fetching once (prevents memory leaks)
            val allBalances = accountBalanceRepository.getAllLatestBalances().first()
            val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

            // Filter out hidden accounts
            val balances = allBalances.filter { account ->
                val key = "${account.bankName}_${account.accountLast4}"
                !hiddenAccounts.contains(key)
            }
            val regularAccounts = balances.filter { !it.isCreditCard }
            val creditCards = balances.filter { it.isCreditCard }

            val accountCurrencies = regularAccounts.map { it.currency }.distinct()
            val creditCardCurrencies = creditCards.map { it.currency }.distinct()
            val allAccountCurrencies = (accountCurrencies + creditCardCurrencies).distinct()
            val hasMultipleCurrencies = allAccountCurrencies.size > 1

            if (hasMultipleCurrencies && allAccountCurrencies.isNotEmpty()) {
                currencyConversionService.refreshExchangeRatesForAccount(allAccountCurrencies)
            }

            val currentAvailableCurrencies = _uiState.value.availableCurrencies.toSet()
            val updatedAvailableCurrencies = (currentAvailableCurrencies + allAccountCurrencies)
                .sortedWith { a, b ->
                    when {
                        a == b -> 0
                        a == "CNY" -> -1
                        b == "CNY" -> 1
                        else -> a.compareTo(b)
                    }
                }

            val selectedCurrency = _selectedCurrency.value ?: baseCurrency.value
            val totalBalanceInSelectedCurrency = balances.netWorthIn(
                selectedCurrency,
                currencyConversionService,
                investmentSnapshots = brokerageRepository.connections.value.investmentSnapshotsOrEmpty()
            )
            var totalAvailableCreditInSelectedCurrency = BigDecimal.ZERO
            for (card in creditCards) {
                val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                val amt = if (card.currency == selectedCurrency) availableInCardCurrency
                else currencyConversionService.convertAmount(availableInCardCurrency, card.currency, selectedCurrency)
                totalAvailableCreditInSelectedCurrency = totalAvailableCreditInSelectedCurrency.add(amt)
            }

            _uiState.update { 
                it.copy(
                    accountBalances = regularAccounts,
                    creditCards = creditCards,
                    totalBalance = totalBalanceInSelectedCurrency,
                    totalAvailableCredit = totalAvailableCreditInSelectedCurrency,
                    availableCurrencies = updatedAvailableCurrencies,
                    selectedCurrency = selectedCurrency
                )
            }
        }
    }

    fun hideBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = false)
    }

    /**
     * Checks for app updates using Google Play In-App Updates.
     * Should be called with the current activity context.
     * @param activity The activity context
     * @param snackbarHostState Optional SnackbarHostState for showing restart prompt
     * @param scope Optional CoroutineScope for launching the snackbar
     */
    fun checkForAppUpdate(
        activity: ComponentActivity,
        snackbarHostState: SnackbarHostState? = null,
        scope: CoroutineScope? = null
    ) {
        inAppUpdateManager.checkForUpdate(activity, snackbarHostState, scope)
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }

    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransaction(transaction)
        }
    }

    fun clearDeletedTransaction() {
        _deletedTransaction.value = null
    }

    /**
     * Checks if eligible for in-app review and shows if appropriate.
     * Should be called with the current activity context.
     */
    fun checkForInAppReview(activity: ComponentActivity) {
        viewModelScope.launch {
            // Get current transaction count as additional eligibility factor
            val transactionCount = transactionRepository.getAllTransactions().first().size
            inAppReviewManager.checkAndShowReviewIfEligible(activity, transactionCount)
        }
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    private suspend fun updateBreakdownForSelectedCurrency(
        breakdownByCurrency: Map<String, TransactionRepository.MonthlyBreakdown>,
        period: FinancialPeriod,
        selectedCurrency: String
    ) {
        // Store the breakdown map for later use when switching currencies
        when (period) {
            FinancialPeriod.CURRENT_MONTH -> currentMonthBreakdownMap = breakdownByCurrency
            FinancialPeriod.LAST_MONTH -> lastMonthBreakdownMap = breakdownByCurrency
            FinancialPeriod.CURRENT_YEAR -> currentYearBreakdownMap = breakdownByCurrency
        }

        // Update available currencies from all stored data and include the effective base currency
        val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
        val allCurrencies = (currentMonthBreakdownMap.keys + lastMonthBreakdownMap.keys + currentYearBreakdownMap.keys + effectiveCurrency).distinct()
        val availableCurrencies = allCurrencies.sortedWith { a, b ->
            when {
                a == b -> 0
                a == "CNY" -> -1 // CNY first
                b == "CNY" -> 1
                else -> a.compareTo(b) // Alphabetical for others
            }
        }

        // Update UI state with values for selected currency
        updateUIStateForCurrency(selectedCurrency, availableCurrencies)
    }

    private suspend fun calculateAggregatedBreakdown(
        selectedCurrency: String,
        breakdownMap: Map<String, TransactionRepository.MonthlyBreakdown>
    ): TransactionRepository.MonthlyBreakdown {
        var total = BigDecimal.ZERO
        var income = BigDecimal.ZERO
        var expenses = BigDecimal.ZERO

        breakdownMap.forEach { (currency, breakdown) ->
            if (currency == selectedCurrency) {
                total += breakdown.total
                income += breakdown.income
                expenses += breakdown.expenses
            } else {
                total += currencyConversionService.convertAmount(breakdown.total, currency, selectedCurrency)
                income += currencyConversionService.convertAmount(breakdown.income, currency, selectedCurrency)
                expenses += currencyConversionService.convertAmount(breakdown.expenses, currency, selectedCurrency)
            }
        }
        return TransactionRepository.MonthlyBreakdown(total, income, expenses)
    }

    private suspend fun updateUIStateForCurrency(selectedCurrency: String, availableCurrencies: List<String>) {
        // Calculate aggregated breakdown across all currencies by converting to selected currency
        val currentBreakdown = calculateAggregatedBreakdown(selectedCurrency, currentMonthBreakdownMap)
        val lastBreakdown = calculateAggregatedBreakdown(selectedCurrency, lastMonthBreakdownMap)
        val currentYearBreakdown = calculateAggregatedBreakdown(selectedCurrency, currentYearBreakdownMap)

        _uiState.value = _uiState.value.copy(
            currentMonthTotal = currentBreakdown.total,
            currentYearTotal = currentYearBreakdown.total,
            currentMonthIncome = currentBreakdown.income,
            currentMonthExpenses = currentBreakdown.expenses,
            currentYearExpenses = currentYearBreakdown.expenses,
            lastMonthTotal = lastBreakdown.total,
            lastMonthIncome = lastBreakdown.income,
            lastMonthExpenses = lastBreakdown.expenses,
            selectedCurrency = selectedCurrency,
            availableCurrencies = availableCurrencies
        )
        calculateMonthlyChange()
    }

    private enum class FinancialPeriod {
        CURRENT_MONTH,
        LAST_MONTH,
        CURRENT_YEAR
    }

    fun toggleBannerImage() {
        viewModelScope.launch {
            userPreferencesRepository.updateShowBannerImage(!_uiState.value.showBannerImage)
        }
    }

    fun setAccountOverviewInNetworth(embedded: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateAccountOverviewInNetworth(embedded) }
    }

    fun resetWidgetsLayout() {
        saveOrderJob?.cancel()
        viewModelScope.launch { userPreferencesRepository.resetHomeWidgetsLayout() }
    }

    fun toggleHomeWidgetVisibility(widget: HomeWidget, visible: Boolean) {
        viewModelScope.launch {
            val currentHidden = userPreferencesRepository.hiddenHomeWidgets.first().toMutableSet()
            if (visible) {
                currentHidden.remove(widget)
            } else {
                currentHidden.add(widget)
            }
            userPreferencesRepository.updateHiddenHomeWidgets(currentHidden)
        }
    }

    private var saveOrderJob: kotlinx.coroutines.Job? = null

    fun updateWidgetsOrder(widgets: List<HomeWidget>) {
        saveOrderJob?.cancel()
        saveOrderJob = viewModelScope.launch {
            // Debounce writes to avoid rapid DataStore updates and UI jank
            kotlinx.coroutines.delay(500)
            userPreferencesRepository.updateHomeWidgetsOrder(widgets)
        }
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.cleanup()
    }
}
