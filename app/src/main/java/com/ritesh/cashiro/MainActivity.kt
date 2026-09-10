package com.ritesh.cashiro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import com.ritesh.cashiro.data.manager.NotificationScheduler
import androidx.lifecycle.lifecycleScope
import com.ritesh.cashiro.data.currency.model.CurrencySymbols
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountDetailViewModel
import com.ritesh.cashiro.presentation.ui.features.accounts.ManageAccountsViewModel
import com.ritesh.cashiro.presentation.ui.features.add.AddViewModel
import com.ritesh.cashiro.presentation.ui.features.analytics.AnalyticsViewModel
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetViewModel
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesViewModel
import com.ritesh.cashiro.presentation.ui.features.home.HomeViewModel
import com.ritesh.cashiro.presentation.ui.features.onboarding.OnBoardingViewModel
import com.ritesh.cashiro.presentation.ui.features.profile.ProfileViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.notifications.NotificationViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesViewModel
import com.ritesh.cashiro.presentation.ui.features.spotlight.SpotlightViewModel
import com.ritesh.cashiro.presentation.ui.features.subscriptions.SubscriptionsViewModel
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionDetailViewModel
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_ADD_TRANSACTION = "com.ritesh.cashiro.action.ADD_TRANSACTION"
        const val ACTION_ADD_SUBSCRIPTION = "com.ritesh.cashiro.action.ADD_SUBSCRIPTION"
        const val ACTION_ADD_TRANSFER = "com.ritesh.cashiro.action.ADD_TRANSFER"
    }

    private val themeViewModel: ThemeViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by viewModels()
    
    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    // Transaction ID to edit when launched from notification
    var editTransactionId by mutableStateOf<Long?>(null)
        private set

    // Initial tab to show in Add Screen (0 for Transaction, 1 for Subscription)
    var addTransactionTab by mutableStateOf<Int?>(null)
        private set

    // Initial transaction type to pre-select
    var addTransactionType by mutableStateOf<String?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen until the theme settings are loaded
        splashScreen.setKeepOnScreenCondition {
            !themeViewModel.themeUiState.value.isLoaded
        }
        enableEdgeToEdge()
        requestHighestRefreshRate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
//            window.isStatusBarContrastEnforced = false
        }

        // Handle intent if activity is launched from notification or shortcut/tile
        handleIntent(intent)

        // Schedule daily reminders
        lifecycleScope.launch {
            notificationScheduler.scheduleDailyReminder()
        }

        // Load custom currencies into CurrencySymbols
        lifecycleScope.launch {
            userPreferencesRepository.customCurrencies.collect { customCurrencies ->
                val customMap = customCurrencies.associate { it.code to it.symbol }
                CurrencySymbols.setCustomSymbols(customMap)
            }
        }

        setContent {
            CashiroApp(
                editTransactionId = editTransactionId,
                onEditComplete = { editTransactionId = null },
                addTransactionTab = addTransactionTab,
                addTransactionType = addTransactionType,
                onAddComplete = { 
                    addTransactionTab = null
                    addTransactionType = null
                },
                appLockViewModel = appLockViewModel,
                themeViewModel = themeViewModel,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle intent when activity is already running
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_ADD_TRANSACTION -> {
                addTransactionTab = 0
            }
            ACTION_ADD_SUBSCRIPTION -> {
                addTransactionTab = 1
            }
            ACTION_ADD_TRANSFER -> {
                addTransactionTab = 0
                addTransactionType = "TRANSFER"
            }
        }
    }

    /**
     * Pin the window to the display mode with the highest refresh rate at the current resolution.
     * On Android 15+/16 the adaptive refresh rate machinery lets Compose vote a lower rate for
     * mostly static content, which reads as a choppy home screen on 90/120 Hz panels. An explicit
     * app-preferred display mode is a stronger vote than those per-layer hints.
     */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else @Suppress("DEPRECATION") windowManager.defaultDisplay
        val current = display?.mode ?: return
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (best.modeId == current.modeId && best.refreshRate <= current.refreshRate) return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
