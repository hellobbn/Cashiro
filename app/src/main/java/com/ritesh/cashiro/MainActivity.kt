package com.ritesh.cashiro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.ritesh.cashiro.data.repository.AccountHoldingsSource
import com.ritesh.cashiro.data.repository.LocalAccountHoldings
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
import com.ritesh.cashiro.presentation.ui.features.ai.AiShareInbox
import androidx.core.content.IntentCompat
import kotlin.getValue

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_ADD_TRANSACTION = "com.ritesh.cashiro.action.ADD_TRANSACTION"
        const val ACTION_ADD_SUBSCRIPTION = "com.ritesh.cashiro.action.ADD_SUBSCRIPTION"
        const val ACTION_ADD_TRANSFER = "com.ritesh.cashiro.action.ADD_TRANSFER"
        /** With [ACTION_ADD_TRANSACTION]: the quick template to fill the form with. */
        const val EXTRA_TEMPLATE_ID = "com.ritesh.cashiro.extra.TEMPLATE_ID"
    }

    private val themeViewModel: ThemeViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by viewModels()
    
    @Inject
    lateinit var accountHoldingsSource: AccountHoldingsSource

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var aiShareInbox: AiShareInbox

    // Files were shared to Cashiro: open AI bookkeeping
    var openAiAssistant by mutableStateOf(false)
        private set

    // Transaction ID to edit when launched from notification
    var editTransactionId by mutableStateOf<Long?>(null)
        private set

    // Initial tab to show in Add Screen (0 for Transaction, 1 for Subscription)
    var addTransactionTab by mutableStateOf<Int?>(null)
        private set

    // Initial transaction type to pre-select
    var addTransactionType by mutableStateOf<String?>(null)
        private set

    // Quick template to fill the Add form with (launcher shortcut or home-screen widget)
    var addTemplateId by mutableStateOf<Long?>(null)
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

        // Handle the launch intent once: a recreated activity (rotation, fold, theme) carries the
        // same intent, and handling it again would reopen screens and copy shared files again
        if (savedInstanceState == null) handleIntent(intent)

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
            val holdings by accountHoldingsSource.holdings.collectAsState()
            CompositionLocalProvider(LocalAccountHoldings provides holdings) {
            CashiroApp(
                editTransactionId = editTransactionId,
                onEditComplete = { editTransactionId = null },
                addTransactionTab = addTransactionTab,
                addTransactionType = addTransactionType,
                addTemplateId = addTemplateId,
                openAiAssistant = openAiAssistant,
                onAiAssistantOpened = { openAiAssistant = false },
                onAddComplete = { 
                    addTransactionTab = null
                    addTransactionType = null
                    addTemplateId = null
                },
                appLockViewModel = appLockViewModel,
                themeViewModel = themeViewModel,
            )
            }
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
                addTemplateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, 0L).takeIf { it > 0 }
                addTransactionTab = 0
            }
            ACTION_ADD_SUBSCRIPTION -> {
                addTransactionTab = 1
            }
            ACTION_ADD_TRANSFER -> {
                addTransactionTab = 0
                addTransactionType = "TRANSFER"
            }
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                val uris = sharedUris(intent)
                if (uris.isNotEmpty()) {
                    // Copy now: the permission to read them ends with this activity
                    lifecycleScope.launch { aiShareInbox.add(uris) }
                    openAiAssistant = true
                }
            }
        }
    }

    private fun sharedUris(intent: Intent): List<Uri> {
        val streams = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        }
        if (streams.isNotEmpty()) return streams
        // Shared text (an SMS, a copied bill) has no file: keep it as one
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return emptyList()
        val file = java.io.File(cacheDir, "shared-text-${System.currentTimeMillis()}.txt").apply { writeText(text) }
        return listOf(Uri.fromFile(file))
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
