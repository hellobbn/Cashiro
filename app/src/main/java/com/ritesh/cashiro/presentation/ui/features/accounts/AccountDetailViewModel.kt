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

    /** Sets one currency of the account to [balance] (a card's amount owed), as the account list does. */
    fun calibrate(balance: BigDecimal, currency: String) {
        viewModelScope.launch { accountBalanceRepository.calibrate(bankName, accountLast4, balance, currency) }
    }

    init {
        loadAccountData()
        observeTransactions()
        observeBalanceHistory()
        observeBalanceChartData()
        observeCardBill()
    }

    /** A credit card's statement and due dates, and how much of the open statement is still owed. */
    private fun observeCardBill() {
        viewModelScope.launch {
            combine(
                accountBalanceRepository.observeAccounts().map { all -> all.firstOrNull { it.name == bankName && it.last4 == accountLast4 } },
                accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4)
            ) { account, latest -> account to latest }.collectLatest { (account, latest) ->
                val dates = com.ritesh.cashiro.domain.model.CardDates(account?.statementDay, account?.dueDay)
                val status = if (account?.isCreditCard == true && latest != null) cardStatus(dates, latest) else null
                _uiState.update { it.copy(cardDates = dates, cardStatus = status) }
            }
        }
    }

    private suspend fun cardStatus(dates: com.ritesh.cashiro.domain.model.CardDates, latest: AccountBalanceEntity): CardStatus? {
        val cycle = com.ritesh.cashiro.domain.model.CardCycle
        val today = java.time.LocalDate.now()
        val dueDay = dates.dueDay ?: return null
        val statementDay = dates.statementDay ?: return CardStatus(cycle.nextDue(dueDay, today), null, null, null)
        val closing = cycle.lastClosing(statementDay, today)
        val due = cycle.dueAfter(closing, dueDay)
        if (today.isAfter(due)) {
            // That bill is past; the next one is not out yet
            val next = cycle.nextClosing(statementDay, today)
            return CardStatus(cycle.dueAfter(next, dueDay), next, null, null)
        }
        val statement = accountBalanceRepository.getLatestBalanceOnOrBefore(
            bankName, accountLast4, closing.atTime(java.time.LocalTime.MAX), latest.currency
        )?.balance?.coerceAtLeast(BigDecimal.ZERO)
        // Payments since closing bring what is owed below the statement; new spending is the next bill's
        val remaining = statement?.min(latest.balance.coerceAtLeast(BigDecimal.ZERO))
        return CardStatus(due, closing, statement, remaining)
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
                currencyConversionService.rateChangeTrigger
            ) { args: Array<Any?> ->
                val dateRange = args[0] as DateRange
                val mainCurrency = args[2] as String
                val latestBalance = args[3] as AccountBalanceEntity?
                // Every currency the account holds, in one list
                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<TransactionEntity>

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

                // Income and spending in the app's main currency. A transfer counts by the side
                // this account is on, in that side's currency; one between two of its own
                // currencies is neither.
                var totalIncome = BigDecimal.ZERO
                var totalExpenses = BigDecimal.ZERO
                val accountId = latestBalance?.accountId
                suspend fun inMain(amount: BigDecimal, currency: String) =
                    if (currency == mainCurrency) amount
                    else currencyConversionService.convertAmount(amount, currency, mainCurrency)

                filteredTransactions.forEach { tx ->
                    when (tx.transactionType) {
                        TransactionType.INCOME, TransactionType.BORROWED -> totalIncome += inMain(tx.amount, tx.currency)
                        TransactionType.TRANSFER -> {
                            val sends = tx.accountId?.let { it == accountId }
                                ?: (tx.bankName == bankName && (tx.accountNumber == accountLast4 || tx.fromAccount == accountLast4))
                            val receives = tx.toAccountId?.let { it == accountId } ?: (!sends && tx.toAccount == accountLast4)
                            when {
                                sends && receives -> Unit
                                receives -> totalIncome += inMain(tx.toAmount ?: tx.amount, tx.toCurrency ?: tx.currency)
                                sends -> totalExpenses += inMain(tx.amount, tx.currency)
                            }
                        }
                        TransactionType.BALANCE_UPDATE -> Unit
                        else -> totalExpenses += inMain(tx.amount, tx.currency)
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
            selectedDateRange.flatMapLatest { dateRange ->
                val (startDate, endDate) = getDateRangeValues(dateRange)
                accountBalanceRepository.getBalanceHistory(
                    bankName, 
                    accountLast4,
                    startDate,
                    endDate
                )
            }.collect { balanceHistory ->
                _uiState.update { state ->
                    state.copy(balanceHistory = balanceHistory)
                }
            }
        }
    }
    
    private fun observeBalanceChartData() {
        viewModelScope.launch {
            combine(selectedDateRange, currencyConversionService.rateChangeTrigger) { dateRange, _ -> dateRange }
                .flatMapLatest { dateRange ->
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
                )
            }.collect { balanceHistory ->
                val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()

                // The account's total over time: each currency at its latest balance by then,
                // in the main currency at today's rates
                val rates = balanceHistory.map { it.currency }.distinct().associateWith { currency ->
                    if (currency == effectiveCurrency) BigDecimal.ONE
                    else currencyConversionService.getExchangeRate(currency, effectiveCurrency) ?: BigDecimal.ONE
                }
                val latest = linkedMapOf<String, BigDecimal>()
                val chartData = balanceHistory.sortedWith(compareBy({ it.timestamp }, { it.id })).map { entity ->
                    latest[entity.currency] = entity.balance
                    BalancePoint(
                        timestamp = entity.timestamp,
                        balance = latest.entries.fold(BigDecimal.ZERO) { sum, (currency, balance) ->
                            sum + balance.multiply(rates.getValue(currency))
                        },
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