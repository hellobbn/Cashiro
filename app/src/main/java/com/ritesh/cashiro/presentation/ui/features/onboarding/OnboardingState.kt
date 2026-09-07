package com.ritesh.cashiro.presentation.ui.features.onboarding

import com.ritesh.cashiro.presentation.ui.features.profile.EditProfileState

enum class OnboardingStep { WELCOME, ACCOUNT, PROFILE, NOTIFICATIONS }

data class OnBoardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val profileState: EditProfileState = EditProfileState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val onboardingFinished: Boolean = false,
    val hasSavedAccount: Boolean = false,
    val manualAccountName: String = "",
    val manualAccountBalance: String = "",
    val manualAccountLast4: String = "",
    val selectedCurrency: String = "CNY",
    val showCurrencyBottomSheet: Boolean = false
) {
    val canSaveAccount: Boolean
        get() = manualAccountName.isNotBlank() &&
            manualAccountBalance.toBigDecimalOrNull() != null &&
            manualAccountLast4.length == 4
}
