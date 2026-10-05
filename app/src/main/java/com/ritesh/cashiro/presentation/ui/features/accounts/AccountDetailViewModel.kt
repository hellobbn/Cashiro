package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.LendBorrowRepository
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.LendBorrowPerson
import com.ritesh.cashiro.domain.model.LendBorrowTransactionItem
import com.ritesh.cashiro.domain.model.PersonInfo
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    transactionLookupsSource: com.ritesh.cashiro.presentation.common.TransactionLookupsSource
) : ViewModel() {
    
    private val bankName: String = savedStateHandle.get<String>("bankName") ?: ""
    private val accountLast4: String = savedStateHandle.get<String>("accountLast4") ?: ""
    
    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    // Category, account and lend/borrow person lookups for the transaction rows
    val lookups = transactionLookupsSource.lookups
    
    private val _selectedDateRange = MutableStateFlow(DateRange.LAST_30_DAYS)
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    // The account's currencies (main first) and the one being looked at; null: the main one
    private val pickedCurrency = MutableStateFlow<String?>(null)
    private val currencies: Flow<List<String>> = combine(
        accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4),
        accountBalanceRepository.observePocketBalances()
    ) { account, pockets ->
        val own = pockets.filter { it.accountId == account?.accountId }.map { it.currency }
        listOfNotNull(account?.currency) + own.filter { it != account?.currency }
    }
    private val shownCurrency: Flow<String?> = combine(currencies, pickedCurrency) { all, picked ->
        // Only an account holding several currencies is narrowed to one
        if (all.size > 1) picked?.takeIf { it in all } ?: all.first() else null
    }

    fun selectCurrency(currency: String) {
        pickedCurrency.value = currency
    }
    
    init {
        loadAccountData()
        observeTransactions()
        observeBalanceHistory()
        observeBalanceChartData()
    }
    
    private fun loadAccountData() {
        _uiState.update { it.copy(
            bankName = bankName,
            accountLast4 = accountLast4,
            isLoading = true
        ) }
    }
    
    private fun observeTransactions() {
        viewModelScope.launch {
            combine(
                selectedDateRange,
                transactionRepository.getTransactionsByAccount(bankName, accountLast4),
                currencyRepository.effectiveBaseCurrencyCode,
                accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4),
                currencyConversionService.rateChangeTrigger,
                currencies,
                shownCurrency
            ) { args: Array<Any?> ->
                val dateRange = args[0] as DateRange
                val mainCurrency = args[2] as String
                val latestBalance = args[3] as AccountBalanceEntity?
                val accountCurrencies = args[5] as List<String>
                val shown = args[6] as String?
                // One currency of a multi-currency account: what moved in or out of it
                val allTransactions = (args[1] as List<TransactionEntity>).filter { tx ->
                    if (shown == null) return@filter true
                    val receives = tx.transactionType == TransactionType.TRANSFER &&
                        (tx.toAccountId?.let { it == latestBalance?.accountId } ?: (tx.toAccount == accountLast4)) &&
                        !(tx.toAccountId == tx.accountId && tx.currency == shown)
                    if (receives) (tx.toCurrency ?: tx.currency) == shown else tx.currency == shown
                }

                val (startDate, endDate) = getDateRangeValues(dateRange)

                val filteredTransactions = if (dateRange == DateRange.ALL_TIME) {
                    allTransactions
                } else {
                    allTransactions.filter { transaction ->
                        transaction.dateTime.isAfter(startDate) &&
                        transaction.dateTime.isBefore(endDate)
                    }
                }

                val accountPrimaryCurrency = latestBalance?.currency ?: getPrimaryCurrencyForAccount(bankName)
                val hasMultipleCurrencies = filteredTransactions.map { it.currency }.distinct().size > 1

                // Refresh exchange rates if we have multiple currencies
                if (hasMultipleCurrencies) {
                    val accountCurrencies = filteredTransactions.map { it.currency }.distinct()
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                // Calculate total income and expenses converted to the effective base currency
                var totalIncome = BigDecimal.ZERO
                var totalExpenses = BigDecimal.ZERO

                filteredTransactions.forEach { transaction ->
                    val convertedAmount = if (transaction.currency != mainCurrency) {
                        currencyConversionService.convertAmount(
                            amount = transaction.amount,
                            fromCurrency = transaction.currency,
                            toCurrency = mainCurrency
                        ) ?: transaction.amount
                    } else {
                        transaction.amount
                    }

                    if (transaction.transactionType == TransactionType.INCOME || transaction.transactionType == TransactionType.BORROWED) {
                        totalIncome += convertedAmount
                    } else if (transaction.transactionType == TransactionType.TRANSFER) {
                        val isSender = transaction.bankName == bankName && 
                            (transaction.accountNumber == accountLast4 || transaction.fromAccount == accountLast4)
                        val isReceiver = !isSender && transaction.toAccount == accountLast4

                        if (isReceiver) {
                            totalIncome += convertedAmount
                        } else if (isSender) {
                            totalExpenses += convertedAmount
                        }
                    } else {
                        totalExpenses += convertedAmount
                    }
                }

                // Rates known now; missing ones arrive through rateChangeTrigger
                val conversions = currencyConversionService.convert(filteredTransactions, mainCurrency)

                _uiState.update { state ->
                    state.copy(
                        transactions = filteredTransactions,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        netBalance = totalIncome - totalExpenses,
                        primaryCurrency = accountPrimaryCurrency,
                        baseCurrency = mainCurrency,
                        hasMultipleCurrencies = hasMultipleCurrencies,
                        conversions = conversions,
                        currencies = accountCurrencies,
                        selectedCurrency = shown,
                        isLoading = false
                    )
                }
            }.collectLatest { }
        }
    }
    
    private fun observeBalanceHistory() {
        viewModelScope.launch {
            accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4)
                .collect { latestBalance ->
                    _uiState.update { state ->
                        state.copy(currentBalance = latestBalance)
                    }
                }
        }
        
        viewModelScope.launch {
            combine(selectedDateRange, shownCurrency) { range, shown -> range to shown }.flatMapLatest { (dateRange, shown) ->
                val (startDate, endDate) = getDateRangeValues(dateRange)
                accountBalanceRepository.getBalanceHistory(
                    bankName, 
                    accountLast4,
                    startDate,
                    endDate
                ).map { rows -> if (shown == null) rows else rows.filter { it.currency == shown } }
            }.collect { balanceHistory ->
                _uiState.update { state ->
                    state.copy(balanceHistory = balanceHistory)
                }
            }
        }
    }
    
    private fun observeBalanceChartData() {
        viewModelScope.launch {
            combine(selectedDateRange, currencyConversionService.rateChangeTrigger, shownCurrency) { dateRange, _, shown -> dateRange to shown }
                .flatMapLatest { (dateRange, shown) ->
                val (startDate, endDate) = getDateRangeValues(dateRange)

                val chartStartDate = when (dateRange) {
                    DateRange.LAST_7_DAYS -> endDate.minusDays(14)
                    DateRange.LAST_30_DAYS -> endDate.minusMonths(2)
                    DateRange.LAST_3_MONTHS -> endDate.minusMonths(4)
                    DateRange.LAST_6_MONTHS -> endDate.minusMonths(8)
                    DateRange.LAST_YEAR -> endDate.minusMonths(15)
                    DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
                }

                accountBalanceRepository.getBalanceHistory(
                    bankName,
                    accountLast4,
                    chartStartDate,
                    endDate
                ).map { rows -> if (shown == null) rows else rows.filter { it.currency == shown } }
            }.collect { balanceHistory ->
                val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()

                val chartData = balanceHistory.map { entity ->
                    val convertedBalance = if (entity.currency != effectiveCurrency) {
                        currencyConversionService.convertAmount(
                            entity.balance,
                            entity.currency,
                            effectiveCurrency
                        ) ?: entity.balance
                    } else entity.balance

                    BalancePoint(
                        timestamp = entity.timestamp,
                        balance = convertedBalance,
                        currency = effectiveCurrency
                    )
                }

                _uiState.update { state ->
                    state.copy(balanceChartData = chartData)
                }
            }
        }
    }
    
    fun selectDateRange(dateRange: DateRange) {
        _selectedDateRange.value = dateRange
    }
    
    private fun getDateRangeValues(dateRange: DateRange): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when (dateRange) {
            DateRange.LAST_7_DAYS -> endDate.minusDays(7)
            DateRange.LAST_30_DAYS -> endDate.minusDays(30)
            DateRange.LAST_3_MONTHS -> endDate.minusMonths(3)
            DateRange.LAST_6_MONTHS -> endDate.minusMonths(6)
            DateRange.LAST_YEAR -> endDate.minusYears(1)
            DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
        }
        return startDate to endDate
    }

    private fun getPrimaryCurrencyForAccount(bankName: String): String {
        return CurrencyFormatter.getBankBaseCurrency(bankName)
    }
}