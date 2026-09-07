package com.ritesh.cashiro.presentation.ui.features.onboarding

import com.ritesh.cashiro.presentation.common.icons.InstitutionCatalog
import android.Manifest
import android.content.Context
import android.util.Log
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.manager.SmsScanManager
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class OnBoardingViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val smsScanManager: SmsScanManager,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> = _uiState.asStateFlow()

    init {
        checkPermissionStatus()
        observeUserPreferences()
        observeScanWorkInfo()
        observeAccounts()
        loadMainAccount()
    }

    private fun checkPermissionStatus() {
        val hasPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                        PackageManager.PERMISSION_GRANTED

        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    private fun observeUserPreferences() {
        userPreferencesRepository
                .userPreferences
                .onEach { prefs ->
                    _uiState.update {
                        it.copy(
                                hasSkippedPermission = prefs.hasSkippedSmsPermission,
                                profileState =
                                        it.profileState.copy(
                                                editedUserName = prefs.userName,
                                                editedProfileImageUri =
                                                        prefs.profileImageUri?.let {
                                                            Uri.parse(it)
                                                        },
                                                editedProfileBackgroundColor =
                                                        if (prefs.profileBackgroundColor != 0)
                                                                Color(prefs.profileBackgroundColor)
                                                        else Color.Transparent,
                                                editedBannerImageUri =
                                                        prefs.bannerImageUri?.let { Uri.parse(it) }
                                        )
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    fun nextStep() {
        _uiState.update { state ->
            val next = when (state.currentStep) {
                1 -> 2 // Welcome -> SMS
                2 -> 3 // SMS -> Notification
                3 -> 4 // Notification -> Account Phase (Sync/Manual)
                4 -> 5 // Account Phase -> Profile
                else -> state.currentStep
            }
            state.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val prev = when (state.currentStep) {
                5 -> 4 // Profile -> Account Phase
                4 -> 3 // Account Phase -> Notification
                3 -> 2 // Notification -> SMS
                2 -> 1 // SMS -> Welcome
                else -> if (state.currentStep > 1) state.currentStep - 1 else 1
            }
            state.copy(currentStep = prev)
        }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(profileState = it.profileState.copy(editedUserName = name, hasChanges = true))
        }
    }

    fun onProfileImageChange(uri: Uri?) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(editedProfileImageUri = uri, hasChanges = true)
            )
        }
    }

    fun onBackgroundColorChange(color: Color) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(
                                    editedProfileBackgroundColor = color,
                                    hasChanges = true
                            )
            )
        }
    }

    fun onBannerImageChange(uri: Uri?) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(editedBannerImageUri = uri, hasChanges = true)
            )
        }
    }

    fun saveProfile() {
        val profile = _uiState.value.profileState
        viewModelScope.launch {
            userPreferencesRepository.updateUserName(profile.editedUserName)
            userPreferencesRepository.updateProfileImageUri(
                    profile.editedProfileImageUri?.toString()
            )
            userPreferencesRepository.updateProfileBackgroundColor(
                    profile.editedProfileBackgroundColor.toArgb()
            )
            userPreferencesRepository.updateBannerImageUri(profile.editedBannerImageUri?.toString())
            finishOnboarding()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            viewModelScope.launch { userPreferencesRepository.updateSkippedSmsPermission(false) }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(showRationale = true) }
    }

    fun skipSmsPermission() {
        nextStep()
        viewModelScope.launch { userPreferencesRepository.updateSkippedSmsPermission(true) }
    }

    fun toggleCurrencyBottomSheet(show: Boolean) {
        _uiState.update { it.copy(showCurrencyBottomSheet = show) }
    }

    fun updateSelectedCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    private fun observeScanWorkInfo() {
        smsScanManager.getSmsScanWorkInfo().asFlow()
            .onEach { workInfos ->
                val workInfo = workInfos?.firstOrNull()
                _uiState.update { it.copy(scanWorkInfo = workInfo) }
                
                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                    _uiState.update { it.copy(isScanning = false) }
                    // Check accounts after sync
                    checkAccountsAfterSync()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountBalanceRepository.getAllLatestBalances()
            .onEach { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
                
                // If only one account exists and no main account is set, auto-set it
                if (accounts.size == 1 && _uiState.value.mainAccountKey == null) {
                    val account = accounts.first()
                    setAsMainAccount(account.bankName, account.accountLast4)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMainAccount() {
        val main = sharedPrefs.getString("main_account", null)
        _uiState.update { it.copy(mainAccountKey = main) }
    }

    private fun checkAccountsAfterSync() {
        viewModelScope.launch {
            val accounts = accountBalanceRepository.getAllLatestBalances().first()
            if (accounts.size == 1) {
                // If only one account, set as main and proceed to profile
                val account = accounts.first()
                setAsMainAccount(account.bankName, account.accountLast4)
                // Persist this account's currency as the app-wide base currency
                userPreferencesRepository.updateBaseCurrency(account.currency)
                
                // Also update the in-built Cash wallet to match the main account's currency
                val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
                if (cashWallet != null && cashWallet.currency != account.currency) {
                    accountBalanceRepository.insertBalance(
                        cashWallet.copy(
                            id = 0,
                            currency = account.currency,
                            timestamp = LocalDateTime.now(),
                            sourceType = "MAIN_ACCOUNT_SYNC"
                        )
                    )
                }
                nextStep()
            } else if (accounts.size > 1) {
                // If multiple accounts, move to next step (handled in OnBoardingScreen based on results)

            } else {
                // If no accounts found, the UI will show manual option
                _uiState.update { it.copy(hasSkippedPermission = true) }
            }
        }
    }

    fun skipSync() {
        // Move to manual entry within Stage 4
        _uiState.update { it.copy(hasSkippedPermission = true) }
    }

    fun updateManualAccountName(name: String) {
        _uiState.update { it.copy(manualAccountName = name) }
    }

    fun updateManualAccountBalance(balance: String) {
        if (balance.isEmpty() || balance.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(manualAccountBalance = balance) }
        }
    }

    fun updateManualAccountLast4(last4: String) {
        if (last4.length <= 4) {
            _uiState.update { it.copy(manualAccountLast4 = last4) }
        }
    }

    fun saveManualAccount() {
        val state = _uiState.value
        if (state.manualAccountName.isBlank() || 
            state.manualAccountBalance.isBlank() || 
            state.manualAccountLast4.length != 4) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val balance = BigDecimal(state.manualAccountBalance)
            val institution = InstitutionCatalog.find(state.manualAccountName)
            accountBalanceRepository.insertBalance(
                AccountBalanceEntity(
                    bankName = state.manualAccountName,
                    accountLast4 = state.manualAccountLast4,
                    balance = balance,
                    timestamp = LocalDateTime.now(),
                    sourceType = "MANUAL",
                    iconResId = institution?.iconResId ?: com.ritesh.cashiro.R.drawable.type_finance_dollar_banknote,
                    iconName = institution?.iconName ?: "type_finance_dollar_banknote",
                    color = institution?.color ?: "#33B5E5",
                    currency = state.selectedCurrency
                )
            )
            
            setAsMainAccount(state.manualAccountName, state.manualAccountLast4)
            // Persist the user-selected currency as the app-wide base currency
            userPreferencesRepository.updateBaseCurrency(state.selectedCurrency)
            
            // Also update the in-built Cash wallet to match the manual account's currency
            val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
            if (cashWallet != null && cashWallet.currency != state.selectedCurrency) {
                accountBalanceRepository.insertBalance(
                    cashWallet.copy(
                        id = 0,
                        currency = state.selectedCurrency,
                        timestamp = LocalDateTime.now(),
                        sourceType = "MAIN_ACCOUNT_SYNC"
                    )
                )
            }
            nextStep()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun startSmsScan() {
        _uiState.update { it.copy(isScanning = true) }
        smsScanManager.startSmsLoggingScan()
    }

    fun setAsMainAccount(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        sharedPrefs.edit { putString("main_account", key) }
        _uiState.update { it.copy(mainAccountKey = key) }
    }

    fun toggleAccountSelectionForMerge(key: String) {
        _uiState.update { state ->
            val current = state.selectedAccountsForMerge
            val new = if (current.contains(key)) current - key else current + key
            state.copy(selectedAccountsForMerge = new)
        }
    }

    fun mergeSelectedAccounts(targetAccount: AccountBalanceEntity) {
        val selectedKeys = _uiState.value.selectedAccountsForMerge
        val sourceAccounts = _uiState.value.accounts.filter { 
            val key = "${it.bankName}_${it.accountLast4}"
            selectedKeys.contains(key) && key != "${targetAccount.bankName}_${targetAccount.accountLast4}"
        }

        if (sourceAccounts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Calculate aggregated balance
                val totalBalance = sourceAccounts.fold(targetAccount.balance) { acc, source ->
                    acc + source.balance
                }

                // Reassign transactions
                sourceAccounts.forEach { source ->
                    transactionRepository.updateAccountForTransactions(
                        oldBankName = source.bankName,
                        oldAccountNumber = source.accountLast4,
                        newBankName = targetAccount.bankName,
                        newAccountNumber = targetAccount.accountLast4
                    )
                }

                // Update target account balance with aggregated total
                accountBalanceRepository.insertBalance(
                    targetAccount.copy(
                        id = 0, // Insert new record
                        balance = totalBalance,
                        timestamp = java.time.LocalDateTime.now(),
                        sourceType = "MERGE"
                    )
                )

                // Delete source accounts
                sourceAccounts.forEach { source ->
                    accountBalanceRepository.deleteAccount(source.bankName, source.accountLast4)
                }
            } catch (e: Exception) {
                Log.e("OnBoardingViewModel", "Error merging accounts", e)
            } finally {
                _uiState.update { it.copy(isLoading = false, selectedAccountsForMerge = emptySet()) }
            }
        }
    }

    fun finishOnboarding() {
        // Complete onboarding
        viewModelScope.launch {
            userPreferencesRepository.markScanTutorialShown()
            _uiState.update { it.copy(onboardingFinished = true) }
        }
    }
}

