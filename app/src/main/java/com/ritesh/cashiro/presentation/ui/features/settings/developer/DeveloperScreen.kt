package com.ritesh.cashiro.presentation.ui.features.settings.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsViewModel
import com.ritesh.cashiro.presentation.ui.icons.Glass
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.NotificationBing
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.grey_dark
import com.ritesh.cashiro.presentation.ui.theme.grey_light
import androidx.compose.material.icons.rounded.Webhook
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isWebhookModeEnabled by
            settingsViewModel.isWebhookModeEnabled.collectAsStateWithLifecycle(
                initialValue = false
            )

    val isTestNotificationAlertsEnabled by
        settingsViewModel.isTestNotificationAlertsEnabled.collectAsStateWithLifecycle(
            initialValue = false
        )

    val isSampleDataSeeded by
        settingsViewModel.isSampleDataSeeded.collectAsStateWithLifecycle(
            initialValue = false
        )

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    LaunchedEffect(uiState.seedMessage) {
        uiState.seedMessage?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearSeedMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
                CustomTitleTopAppBar(
                title = stringResource(R.string.developer_options),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                PreferenceSwitch(
                    title = stringResource(R.string.webhook_mode),
                    subtitle = stringResource(R.string.webhook_mode_desc),
                    checked = isWebhookModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleWebhookMode(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Webhook,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isFirst = true
                )

                PreferenceSwitch(
                    title = stringResource(R.string.test_notification_alerts),
                    subtitle = stringResource(R.string.test_notification_alerts_desc),
                    checked = isTestNotificationAlertsEnabled,
                    onCheckedChange = { settingsViewModel.toggleTestNotificationAlerts(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Iconax.NotificationBing,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isLast = false
                )

                PreferenceSwitch(
                    title = stringResource(R.string.sample_data),
                    subtitle = stringResource(R.string.sample_data_desc),
                    checked = isSampleDataSeeded,
                    onCheckedChange = { if (!uiState.isSeeding) settingsViewModel.toggleSampleData(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSeeding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = grey_dark
                                )
                            } else {
                                Icon(
                                    Iconax.Glass,
                                    contentDescription = null,
                                    tint = grey_dark
                                )
                            }
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isLast = true
                )
            }
        }
    }
}
