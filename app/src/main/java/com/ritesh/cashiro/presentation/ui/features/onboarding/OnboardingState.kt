package com.ritesh.cashiro.presentation.ui.features.onboarding

import androidx.work.WorkInfo
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.ui.features.profile.EditProfileState

data class OnBoardingUiState(
    val currentStep: Int = 1,
    val hasPermission: Boolean = false,
    val hasSkippedPermission: Boolean = false,
    val showRationale: Boolean = false,
    val profileState: EditProfileState = EditProfileState(),
    val isScanning: Boolean = false,
    val scanWorkInfo: WorkInfo? = null,
    val accounts: List<AccountBalanceEntity> = emptyList(),
    val mainAccountKey: String? = null,
    val selectedAccountsForMerge: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val onboardingFinished: Boolean = false,
    val manualAccountName: String = "",
    val manualAccountBalance: String = "",
    val manualAccountLast4: String = "",
    val selectedCurrency: String = "CNY", // Default currency
    val showCurrencyBottomSheet: Boolean = false,
    val permissionSubStep: Int = 0 // 0: SMS, 1: Notification
)
