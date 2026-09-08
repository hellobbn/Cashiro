package com.ritesh.cashiro.presentation.ui.features.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.domain.brokerage.BrokerageAccount
import com.ritesh.cashiro.domain.brokerage.BrokerageError
import com.ritesh.cashiro.domain.brokerage.Holding
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun InvestmentsShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        headline = {
            Text(
                text = stringResource(R.string.investments_title),
                fontWeight = FontWeight.Medium
            )
        },
        supporting = { Text(stringResource(R.string.investments_shortcut)) },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        trailing = {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        },
        onClick = onClick,
        shape = ListItemPosition.Single.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@Composable
fun InvestmentsScreen(
    onNavigateBack: () -> Unit,
    onManageManualAccounts: () -> Unit = {},
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val manualAccounts by viewModel.manualAccounts.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var connecting by remember { mutableStateOf(false) }
    var disconnecting by remember { mutableStateOf<BrokerConnection?>(null) }

    InvestmentsContent(
        connections = connections,
        busy = busy,
        loaded = loaded,
        error = error,
        onBack = onNavigateBack,
        onConnect = { viewModel.clearError(); connecting = true },
        onRefresh = viewModel::refresh,
        onDisconnect = { disconnecting = it },
        onRetry = viewModel::reload,
        manualAccounts = manualAccounts,
        onManageManualAccounts = onManageManualAccounts
    )
    if (connecting) {
        IbkrConnectDialog(
            busy = busy,
            error = error,
            onDismiss = { viewModel.cancelConnection(); connecting = false; viewModel.clearError() },
            onConnect = { label, token, query -> viewModel.connect(label, token, query) { connecting = false } }
        )
    }
    disconnecting?.let { connection ->
        AlertDialog(
            onDismissRequest = { disconnecting = null },
            title = { Text(stringResource(R.string.investments_disconnect)) },
            text = { Text(stringResource(R.string.investments_disconnect_hint)) },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { viewModel.disconnect(connection.id); disconnecting = null }
                ) { Text(stringResource(R.string.investments_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { disconnecting = null }) {
                    Text(stringResource(R.string.investments_cancel))
                }
            },
            shape = MaterialTheme.shapes.large
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
internal fun InvestmentsContent(
    connections: List<BrokerConnection>,
    busy: Boolean,
    loaded: Boolean,
    error: BrokerageError?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: (String) -> Unit,
    onDisconnect: (BrokerConnection) -> Unit,
    onRetry: () -> Unit,
    manualAccounts: List<AccountBalanceEntity> = emptyList(),
    onManageManualAccounts: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.overview_investments),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + padding.calculateTopPadding()
                )
                .testTag("investments_list"),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (busy) {
                item(key = "sync_progress") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.investments_syncing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (error != null) {
                item(key = "broker_error") { InvestmentError(error) }
            }
            if (!loaded && !busy) {
                item(key = "retry") {
                    ListItem(
                        headline = { Text(stringResource(R.string.investments_retry), fontWeight = FontWeight.Medium) },
                        onClick = onRetry,
                        shape = ListItemPosition.Single.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }
            if (manualAccounts.isNotEmpty()) {
                item(key = "manual_header") {
                    SectionHeader(title = stringResource(R.string.overview_manual_investments))
                }
                item(key = "manual_investment_summary") {
                    ManualInvestmentSummary(manualAccounts, onManageManualAccounts)
                }
            }
            if (loaded && connections.isEmpty()) {
                item(key = "empty") {
                    ListItem(
                        headline = {
                            Text(stringResource(R.string.investments_empty_title), fontWeight = FontWeight.Medium)
                        },
                        supporting = { Text(stringResource(R.string.investments_empty_body)) },
                        shape = ListItemPosition.Single.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }
            if (loaded) {
                item(key = "connect") {
                    ListItem(
                        modifier = Modifier
                            .testTag("connect_broker")
                            .then(if (busy) Modifier.semantics { disabled() } else Modifier),
                        headline = {
                            Text(stringResource(R.string.investments_connect_ibkr), fontWeight = FontWeight.Medium)
                        },
                        supporting = { Text(stringResource(R.string.investments_report_hint)) },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        },
                        trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                        onClick = if (busy) null else onConnect,
                        shape = ListItemPosition.Single.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }
            }
            connections.forEach { connection ->
                connectionSection(connection, busy, onRefresh, onDisconnect)
            }
            if (manualAccounts.isNotEmpty() && connections.isNotEmpty()) {
                item(key = "source_note") {
                    Text(
                        stringResource(R.string.overview_source_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualInvestmentSummary(
    accounts: List<AccountBalanceEntity>,
    onManageManualAccounts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        val totals = accounts.groupBy { it.currency }.toSortedMap()
        totals.entries.forEachIndexed { index, (currency, rows) ->
            val position = ListItemPosition.from(index, totals.size)
            ListItem(
                headline = {
                    Text(
                        CurrencyFormatter.formatCurrency(
                            rows.fold(BigDecimal.ZERO) { n, a -> n + a.balance },
                            currency
                        ),
                        fontWeight = FontWeight.Medium
                    )
                },
                supporting = { Text(stringResource(R.string.overview_count, rows.size) + " · " + currency) },
                trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                onClick = onManageManualAccounts,
                shape = position.toShape(),
                padding = PaddingValues(0.dp)
            )
        }
    }
}

private fun LazyListScope.connectionSection(
    connection: BrokerConnection,
    busy: Boolean,
    onRefresh: (String) -> Unit,
    onDisconnect: (BrokerConnection) -> Unit
) {
    item(key = "connection_header_${connection.id}") {
        SectionHeader(title = connection.label)
    }
    item(key = "connection_${connection.id}") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            ListItem(
                headline = {
                    Text(stringResource(R.string.investments_synced, formattedSyncTime(connection.syncedAt)))
                },
                supporting = {
                    val asOf = connection.accounts.map { it.asOf }.distinct().joinToString()
                    if (asOf.isNotEmpty()) {
                        Text(stringResource(R.string.investments_as_of, asOf))
                    }
                },
                shape = ListItemPosition.Top.toShape(),
                padding = PaddingValues(0.dp)
            )
            ListItem(
                headline = { Text(stringResource(R.string.investments_refresh), fontWeight = FontWeight.Medium) },
                leading = {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                },
                onClick = if (busy) null else ({ onRefresh(connection.id) }),
                shape = ListItemPosition.Middle.toShape(),
                padding = PaddingValues(0.dp)
            )
            ListItem(
                headline = { Text(stringResource(R.string.investments_disconnect), fontWeight = FontWeight.Medium) },
                leading = {
                    Icon(Icons.Rounded.LinkOff, contentDescription = null)
                },
                onClick = if (busy) null else ({ onDisconnect(connection) }),
                shape = ListItemPosition.Bottom.toShape(),
                padding = PaddingValues(0.dp)
            )
        }
    }
    connection.accounts.forEach { account ->
        item(key = "account_${connection.id}_${account.accountId}") {
            SectionHeader(title = account.accountId)
        }
        item(key = "account_totals_${connection.id}_${account.accountId}") {
            AccountTotals(account)
        }
        val holdings = account.holdings
        if (holdings.isEmpty()) {
            item(key = "no_positions_${connection.id}_${account.accountId}") {
                ListItem(
                    headline = { Text(stringResource(R.string.investments_no_positions)) },
                    shape = ListItemPosition.Single.toShape(),
                    padding = PaddingValues(0.dp)
                )
            }
        } else {
            itemsIndexed(
                holdings,
                key = { _, holding ->
                    "holding_${connection.id}_${account.accountId}_${holding.instrumentId}_${holding.currency}_${holding.model}"
                }
            ) { index, holding ->
                HoldingRow(holding, ListItemPosition.from(index, holdings.size))
            }
        }
    }
}

@Composable
private fun AccountTotals(account: BrokerageAccount) {
    val currencies = (account.holdings.map { it.currency } + account.cashBalances.map { it.currency })
        .toSortedSet()
    val cashByCurrency = account.cashBalances.associateBy { it.currency }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        if (currencies.isEmpty()) {
            ListItem(
                headline = { Text(stringResource(R.string.investments_as_of, account.asOf)) },
                supporting = { Text(stringResource(R.string.investments_no_positions)) },
                shape = ListItemPosition.Single.toShape(),
                padding = PaddingValues(0.dp)
            )
            return
        }
        currencies.forEachIndexed { index, currency ->
            val holdings = account.holdings.filter { it.currency == currency }
            val cash = cashByCurrency[currency]
            val holdingsTotal = sumDecimals(holdings.map { it.marketValue })
            val cashAmount = cash?.endingCash?.toBigDecimalOrNull()
            val snapshot = when {
                holdingsTotal != null && cashAmount != null -> holdingsTotal + cashAmount
                holdingsTotal != null && cash == null -> holdingsTotal
                else -> null
            }
            val supporting = buildString {
                append(stringResource(R.string.investments_market_total, currency, decimal(holdingsTotal?.toPlainString())))
                if (cashAmount != null) {
                    append('\n')
                    append(
                        if (cashAmount.signum() < 0) {
                            stringResource(R.string.investments_margin_debit, currency, decimal(cashAmount.abs().toPlainString()))
                        } else {
                            stringResource(R.string.investments_cash, currency, decimal(cash.endingCash))
                        }
                    )
                } else {
                    append('\n')
                    append(stringResource(R.string.investments_cash_missing))
                }
            }
            ListItem(
                headline = {
                    Text(
                        text = stringResource(
                            R.string.investments_snapshot_total,
                            currency,
                            decimal(snapshot?.toPlainString())
                        ),
                        fontWeight = FontWeight.Medium
                    )
                },
                supporting = { Text(supporting) },
                shape = ListItemPosition.from(index, currencies.size).toShape(),
                padding = PaddingValues(0.dp)
            )
        }
    }
}

@Composable
private fun HoldingRow(holding: Holding, position: ListItemPosition) {
    val dark = isSystemInDarkTheme()
    val pnl = holding.unrealizedPnl?.toBigDecimalOrNull()
    val pnlColor = when {
        pnl == null -> MaterialTheme.colorScheme.onSurfaceVariant
        pnl.signum() >= 0 -> if (dark) income_dark else income_light
        else -> if (dark) expense_dark else expense_light
    }
    ListItem(
        headline = { Text(holding.symbol, fontWeight = FontWeight.Medium) },
        supporting = {
            val detail = buildString {
                append(holding.description)
                if (holding.model.isNotBlank()) {
                    append(" · ")
                    append(holding.model)
                }
                append('\n')
                append(stringResource(R.string.investments_quantity, holding.quantity))
            }
            Text(detail)
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.investments_value, holding.currency, decimal(holding.marketValue)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.investments_pnl, holding.currency, decimal(holding.unrealizedPnl)),
                    style = MaterialTheme.typography.bodySmall,
                    color = pnlColor
                )
            }
        },
        shape = position.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
internal fun IbkrConnectDialog(
    busy: Boolean,
    error: BrokerageError?,
    onDismiss: () -> Unit,
    onConnect: (String, String, String) -> Unit
) {
    // Intentionally remember, not rememberSaveable: credentials never enter saved instance state.
    var label by remember { mutableStateOf("IBKR") }
    var token by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)
    ) {
        Scaffold(
            topBar = {
                CustomTitleTopAppBar(
                    title = stringResource(R.string.investments_connect_ibkr),
                    scrollBehaviorSmall = scrollBehavior,
                    scrollBehaviorLarge = scrollBehavior,
                    hazeState = hazeState,
                    hasBackButton = true,
                    navigationContent = { NavigationContent(onDismiss) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    stringResource(R.string.investments_setup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.investments_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    uriHandler.openUri(
                        "https://www.interactivebrokers.com/docs/web-api/flex-web-service/client-portal-configuration"
                    )
                }) {
                    Text(stringResource(R.string.investments_help))
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(60) },
                    label = { Text(stringResource(R.string.investments_label)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim().take(128) },
                    label = { Text("Flex Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("flex_token"),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.trim().take(32) },
                    label = { Text("Query ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("flex_query"),
                    shape = MaterialTheme.shapes.large
                )
                if (error != null) InvestmentError(error)
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.investments_syncing))
                }
                Button(
                    onClick = { onConnect(label, token, query) },
                    enabled = !busy && token.matches(Regex("[0-9]{6,128}")) && query.matches(Regex("[0-9]{1,32}")),
                    modifier = Modifier.fillMaxWidth().testTag("save_broker"),
                    shape = MaterialTheme.shapes.large
                ) {
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
    Text(
        stringResource(resource),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("broker_error")
    )
}

private fun formattedSyncTime(syncedAt: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(syncedAt))

private fun sumDecimals(values: List<String?>): BigDecimal? {
    if (values.isEmpty()) return BigDecimal.ZERO
    if (values.any { it == null }) return null
    return values.fold(BigDecimal.ZERO) { n, v -> n + v!!.toBigDecimal() }
}

private fun decimal(value: String?): String = value?.let {
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(it.toBigDecimal())
} ?: "—"
