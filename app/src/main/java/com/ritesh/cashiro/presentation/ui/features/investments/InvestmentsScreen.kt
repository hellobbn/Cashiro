package com.ritesh.cashiro.presentation.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.brokerage.BrokerConnection
import com.ritesh.cashiro.domain.brokerage.BrokerageError
import com.ritesh.cashiro.domain.brokerage.Holding
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun InvestmentsShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.investments_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.investments_shortcut), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun InvestmentsScreen(onNavigateBack: () -> Unit, viewModel: InvestmentsViewModel = hiltViewModel()) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var connecting by remember { mutableStateOf(false) }
    var disconnecting by remember { mutableStateOf<BrokerConnection?>(null) }

    InvestmentsContent(connections, busy, loaded, error, onNavigateBack,
        onConnect = { viewModel.clearError(); connecting = true },
        onRefresh = viewModel::refresh, onDisconnect = { disconnecting = it }, onRetry = viewModel::reload)
    if (connecting) {
        IbkrConnectDialog(busy, error, onDismiss = { viewModel.cancelConnection(); connecting = false; viewModel.clearError() },
            onConnect = { label, token, query -> viewModel.connect(label, token, query) { connecting = false } })
    }
    disconnecting?.let { connection ->
        AlertDialog(
            onDismissRequest = { disconnecting = null },
            title = { Text(stringResource(R.string.investments_disconnect)) },
            text = { Text(stringResource(R.string.investments_disconnect_hint)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                viewModel.disconnect(connection.id); disconnecting = null
            }) { Text(stringResource(R.string.investments_disconnect)) } },
            dismissButton = { TextButton(onClick = { disconnecting = null }) { Text(stringResource(R.string.investments_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InvestmentsContent(
    connections: List<BrokerConnection>, busy: Boolean, loaded: Boolean, error: BrokerageError?,
    onBack: () -> Unit, onConnect: () -> Unit, onRefresh: (String) -> Unit,
    onDisconnect: (BrokerConnection) -> Unit, onRetry: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.investments_title)) },
        navigationIcon = { IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.investments_back))
        } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("investments_list"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), overscrollEffect = null) {
            item {
                Text(stringResource(R.string.investments_report_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (busy) item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.investments_syncing), style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) item { InvestmentError(error) }
            if (!loaded && !busy) item {
                OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.investments_retry)) }
            }
            if (loaded && connections.isEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.investments_empty_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.investments_empty_body))
                    }
                }
            }
            if (loaded) item {
                Button(onClick = onConnect, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("connect_broker")) {
                    Text(stringResource(R.string.investments_connect_ibkr))
                }
            }
            connections.forEach { connection ->
                item(key = "connection_${connection.id}") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(connection.label, style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.investments_synced, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(connection.syncedAt))),
                            style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onRefresh(connection.id) }, enabled = !busy) {
                                Text(stringResource(R.string.investments_refresh))
                            }
                            TextButton(onClick = { onDisconnect(connection) }, enabled = !busy) {
                                Text(stringResource(R.string.investments_disconnect))
                            }
                        }
                    }
                }
                connection.accounts.forEach { account ->
                    item(key = "account_${connection.id}_${account.accountId}") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider()
                            Text(account.accountId, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.investments_as_of, account.asOf), style = MaterialTheme.typography.bodySmall)
                            // Never sum different currencies, never label positions as net assets.
                            account.holdings.groupBy { it.currency }.toSortedMap().forEach { (currency, holdings) ->
                                val sum = if (holdings.all { it.marketValue != null }) holdings.fold(BigDecimal.ZERO) { n, h -> n + h.marketValue!!.toBigDecimal() }.toPlainString() else null
                                Text(stringResource(R.string.investments_market_total, currency, decimal(sum)), style = MaterialTheme.typography.titleMedium)
                            }
                            if (account.holdings.isEmpty()) Text(stringResource(R.string.investments_no_positions))
                        }
                    }
                    items(account.holdings, key = { "holding_${connection.id}_${account.accountId}_${it.instrumentId}_${it.currency}_${it.model}" }) { holding ->
                        HoldingCard(holding)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingCard(holding: Holding) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(holding.symbol, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            Text(holding.description, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
            if (holding.model.isNotBlank()) Text(holding.model, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.investments_quantity, holding.quantity), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.investments_value, holding.currency, decimal(holding.marketValue)), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.investments_cost, holding.currency, decimal(holding.costBasis)), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.investments_pnl, holding.currency, decimal(holding.unrealizedPnl)), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IbkrConnectDialog(busy: Boolean, error: BrokerageError?, onDismiss: () -> Unit,
    onConnect: (String, String, String) -> Unit) {
    // Intentionally remember, not rememberSaveable: credentials never enter saved instance state.
    var label by remember { mutableStateOf("IBKR") }
    var token by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.investments_connect_ibkr)) },
            navigationIcon = { IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.investments_back))
            } }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.investments_setup))
                Text(stringResource(R.string.investments_privacy), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { uriHandler.openUri("https://www.interactivebrokers.com/docs/web-api/flex-web-service/client-portal-configuration") }) {
                    Text(stringResource(R.string.investments_help))
                }
                OutlinedTextField(label, { label = it.take(60) }, label = { Text(stringResource(R.string.investments_label)) },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(token, { token = it.trim().take(128) }, label = { Text("Flex Token") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("flex_token"))
                OutlinedTextField(query, { query = it.trim().take(32) }, label = { Text("Query ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("flex_query"))
                if (error != null) InvestmentError(error)
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.investments_syncing))
                }
                Button(onClick = { onConnect(label, token, query) },
                    enabled = !busy && token.matches(Regex("[0-9]{6,128}")) && query.matches(Regex("[0-9]{1,32}")),
                    modifier = Modifier.fillMaxWidth().testTag("save_broker")) {
                    Text(stringResource(R.string.investments_connect_sync))
                }
            }
        }
    }
}

@Composable
private fun InvestmentError(error: BrokerageError) {
    val resource = when (error) {
        BrokerageError.INVALID_CREDENTIALS -> R.string.investments_error_credentials
        BrokerageError.EXPIRED_CREDENTIALS -> R.string.investments_error_expired
        BrokerageError.IP_RESTRICTED -> R.string.investments_error_ip
        BrokerageError.INVALID_QUERY -> R.string.investments_error_query
        BrokerageError.REPORT_NOT_READY -> R.string.investments_error_pending
        BrokerageError.RATE_LIMITED -> R.string.investments_error_rate
        BrokerageError.NETWORK -> R.string.investments_error_network
        BrokerageError.INVALID_REPORT -> R.string.investments_error_report
        BrokerageError.UNSUPPORTED_PROVIDER -> R.string.investments_error_provider
        BrokerageError.STORAGE -> R.string.investments_error_storage
        BrokerageError.DUPLICATE_CONNECTION -> R.string.investments_error_duplicate
    }
    Text(stringResource(resource), color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("broker_error"))
}

private fun decimal(value: String?): String = value?.let {
    NumberFormat.getNumberInstance().apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(it.toBigDecimal())
} ?: "—"
