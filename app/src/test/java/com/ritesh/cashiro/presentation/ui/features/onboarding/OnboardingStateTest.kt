package com.ritesh.cashiro.presentation.ui.features.onboarding

import org.junit.Assert.*
import org.junit.Test

class OnboardingStateTest {
    @Test fun `new install offers welcome without a required account`() {
        val state = OnBoardingUiState()
        assertEquals(OnboardingStep.WELCOME, state.step)
        assertFalse(state.hasSavedAccount)
        assertFalse(state.onboardingFinished)
        assertFalse(state.isLoading)
    }
    @Test fun `manual entry rejects incomplete or malformed amounts`() {
        val state = OnBoardingUiState(manualAccountName = "Bank", manualAccountLast4 = "1234")
        assertFalse(state.canSaveAccount)
        assertFalse(state.copy(manualAccountBalance = ".").canSaveAccount)
        assertFalse(state.copy(manualAccountBalance = "12.50", manualAccountLast4 = "12").canSaveAccount)
        assertTrue(state.copy(manualAccountBalance = "0").canSaveAccount)
        assertTrue(state.copy(manualAccountBalance = "12.50").canSaveAccount)
    }
    @Test fun `onboarding has no tracking or scan step`() {
        assertEquals(listOf("WELCOME", "ACCOUNT", "PROFILE", "NOTIFICATIONS"), OnboardingStep.entries.map { it.name })
    }
}
