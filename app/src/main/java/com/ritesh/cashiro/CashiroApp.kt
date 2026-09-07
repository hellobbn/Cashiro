package com.ritesh.cashiro

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ritesh.cashiro.presentation.navigation.AppLock
import com.ritesh.cashiro.presentation.navigation.Home
import com.ritesh.cashiro.presentation.navigation.CashiroNavHost
import com.ritesh.cashiro.presentation.navigation.OnBoarding
import com.ritesh.cashiro.presentation.navigation.Settings
import com.ritesh.cashiro.presentation.navigation.AddTransaction
import com.ritesh.cashiro.presentation.navigation.TransactionDetail
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import com.ritesh.cashiro.presentation.ui.components.GitHubUpdateHost
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel

@Composable
fun CashiroApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    editTransactionId: Long? = null,
    onEditComplete: () -> Unit = {},
    addTransactionTab: Int? = null,
    addTransactionType: String? = null,
    onAddComplete: () -> Unit = {}
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appLockViewModel.refreshLockState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!themeUiState.isLoaded) return

    val startDestination = remember {
        if (themeUiState.isOnboardingFinished) Home else OnBoarding
    }

    LaunchedEffect(appLockUiState.isLocked, appLockUiState.isLockEnabled) {
        if (appLockUiState.isLocked && appLockUiState.isLockEnabled) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != AppLock::class.qualifiedName &&
                currentRoute != Settings::class.qualifiedName) {
                navController.navigate(AppLock) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
    
    LaunchedEffect(editTransactionId) {
        editTransactionId?.let { transactionId ->
            navController.navigate(TransactionDetail(transactionId))
        }
    }

    LaunchedEffect(addTransactionTab, addTransactionType) {
        addTransactionTab?.let { tab ->
            navController.navigate(AddTransaction(initialTab = tab, type = addTransactionType))
            onAddComplete()
        }
    }

    CashiroTheme(
        darkTheme = darkTheme,
        themeStyle = themeUiState.themeStyle,
        // The visible ThemeStyle choice is authoritative; old backups can contain a
        // contradictory legacy dynamic-color flag. CashiroTheme defaults to enabled.
        isAmoledMode = themeUiState.isAmoledMode,
        accentColor = themeUiState.accentColor,
        appFont = themeUiState.appFont,
        blurEffects = themeUiState.blurEffects
    ) {
        Box {
            CashiroNavHost(
                navController = navController,
                startDestination = startDestination,
                onEditComplete = onEditComplete
            )
            GitHubUpdateHost()
        }
    }
}
