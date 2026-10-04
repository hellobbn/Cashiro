package com.ritesh.cashiro.presentation.ui.features.ai

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.ai.AiConfig
import com.ritesh.cashiro.data.ai.AiProtocol
import com.ritesh.cashiro.data.ai.LedgerChange
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DialogConfirmButton
import com.ritesh.cashiro.presentation.ui.components.DialogDismissButton
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.net.URI
import java.time.format.DateTimeFormatter

/**
 * AI bookkeeping: files shared to Cashiro (or picked here) and a request in plain words go to the
 * user's own cloud model, which proposes additions, edits and deletions; nothing is saved until
 * the user ticks what to keep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiAssistantViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val defaultRequest = stringResource(R.string.ai_default_request)
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFiles(uris)
    }

    // Back from a review or a running request returns to the request first
    BackHandler(enabled = state.phase !is AiPhase.Compose) {
        when (state.phase) {
            is AiPhase.Running -> viewModel.cancel()
            else -> viewModel.startOver()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.ai_assistant_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        },
        bottomBar = {
            AiBottomBar(
                state = state,
                onAnalyze = { viewModel.analyze(defaultRequest) },
                onCancel = viewModel::cancel,
                onSave = viewModel::save,
                onStartOver = viewModel::startOver,
                onUndo = viewModel::undo,
                onDone = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + padding.calculateTopPadding(),
                bottom = Dimensions.Padding.content + padding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            state.error?.let { error ->
                item(key = "error") { ErrorCard(error, onDismiss = viewModel::dismissError) }
            }
            when (val phase = state.phase) {
                AiPhase.Compose -> {
                    item(key = "provider") { ProviderCard(state.config, onSave = viewModel::saveConfig) }
                    item(key = "files") {
                        FilesSection(
                            names = state.attachments.map { it.name to it.isImage },
                            onAdd = { pickFiles.launch(arrayOf("image/*", "application/pdf", "text/*")) },
                            onRemove = { index -> viewModel.removeAttachment(state.attachments[index]) }
                        )
                    }
                    item(key = "request") {
                        OutlinedTextField(
                            value = state.request,
                            onValueChange = viewModel::setRequest,
                            placeholder = { Text(stringResource(R.string.ai_request_hint)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.config.isConfigured) {
                        item(key = "privacy") {
                            Text(
                                text = stringResource(R.string.ai_privacy_note, host(state.config.baseUrl)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is AiPhase.Running -> item(key = "running") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.ai_running, phase.proposed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AiPhase.Review, is AiPhase.Saved -> {
                    if (state.summary.isNotBlank()) {
                        item(key = "summary") { Text(state.summary, style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (state.review.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                stringResource(R.string.ai_review_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(state.review, key = { index, _ -> "change_$index" }) { index, item ->
                        ChangeRow(
                            change = item.change,
                            selected = item.selected,
                            enabled = state.phase is AiPhase.Review,
                            onToggle = { viewModel.toggle(index) }
                        )
                    }
                }
            }
        }
    }

    state.passwordFor?.let { name ->
        PasswordDialog(name = name, onResult = viewModel::submitPassword)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AiBottomBar(
    state: AiAssistantUiState,
    onAnalyze: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onStartOver: () -> Unit,
    onUndo: () -> Unit,
    onDone: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (val phase = state.phase) {
                AiPhase.Compose -> Button(
                    onClick = onAnalyze,
                    shapes = ButtonDefaults.shapes(),
                    enabled = state.config.isConfigured && (state.attachments.isNotEmpty() || state.request.isNotBlank())
                ) { Text(stringResource(R.string.ai_analyze)) }
                is AiPhase.Running -> OutlinedButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.ai_cancel))
                }
                AiPhase.Review -> {
                    TextButton(onClick = onStartOver, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.ai_start_over))
                    }
                    val count = state.review.count { it.selected }
                    Button(onClick = onSave, enabled = count > 0, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.ai_save_selected, count))
                    }
                }
                is AiPhase.Saved -> {
                    val count = phase.applied.addedIds.size + phase.applied.updated.size + phase.applied.deleted.size
                    Text(
                        text = stringResource(R.string.ai_saved, count),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onUndo, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ai_undo)) }
                    Button(onClick = onDone, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ai_done)) }
                }
            }
        }
    }
}

/** The provider in one line once it is set up; the form until then, or when changing it. */
@Composable
private fun ProviderCard(config: AiConfig, onSave: (AiConfig) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ai_provider),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (config.isConfigured) "${config.model} · ${host(config.baseUrl)}"
                        else stringResource(R.string.ai_not_configured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (config.isConfigured && !editing) {
                    TextButton(onClick = { editing = true }) { Text(stringResource(R.string.ai_edit)) }
                }
            }
            if (!config.isConfigured || editing) {
                ProviderForm(config) {
                    onSave(it)
                    editing = false
                }
            }
        }
    }
}

/** Shortcuts for the provider form: each fills the protocol, address and a model. */
private enum class AiPreset(val labelRes: Int, val protocol: AiProtocol, val baseUrl: String, val model: String) {
    CLAUDE(R.string.ai_preset_claude, AiProtocol.ANTHROPIC, AiProtocol.ANTHROPIC.defaultBaseUrl, AiProtocol.ANTHROPIC.defaultModel),
    // One key for Claude, Gemini, GPT, DeepSeek, Qwen…; model ids are "vendor/model"
    OPENROUTER(R.string.ai_preset_openrouter, AiProtocol.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1", ""),
    CUSTOM(R.string.ai_preset_custom, AiProtocol.OPENAI_COMPATIBLE, "", "");

    companion object {
        fun of(config: AiConfig): AiPreset = when {
            config.protocol == AiProtocol.ANTHROPIC -> CLAUDE
            host(config.baseUrl) == "openrouter.ai" -> OPENROUTER
            else -> CUSTOM
        }
    }
}

@Composable
private fun ProviderForm(config: AiConfig, onSave: (AiConfig) -> Unit) {
    var preset by rememberSaveable { mutableStateOf(AiPreset.of(config)) }
    var baseUrl by rememberSaveable { mutableStateOf(config.baseUrl) }
    var model by rememberSaveable { mutableStateOf(config.model) }
    var apiKey by rememberSaveable { mutableStateOf(config.apiKey) }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AiPreset.entries.forEachIndexed { index, p ->
            SegmentedButton(
                selected = preset == p,
                onClick = {
                    if (preset != p) {
                        preset = p
                        baseUrl = p.baseUrl
                        model = p.model
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index, AiPreset.entries.size)
            ) { Text(stringResource(p.labelRes), maxLines = 1) }
        }
    }
    OutlinedTextField(
        value = baseUrl, onValueChange = { baseUrl = it }, singleLine = true,
        label = { Text(stringResource(R.string.ai_base_url)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = model, onValueChange = { model = it }, singleLine = true,
        label = { Text(stringResource(R.string.ai_model)) },
        placeholder = if (preset == AiPreset.OPENROUTER) {
            { Text("anthropic/claude-opus-5.5") }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = apiKey, onValueChange = { apiKey = it }, singleLine = true,
        label = { Text(stringResource(R.string.ai_api_key)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = { onSave(AiConfig(preset.protocol, baseUrl, model, apiKey)) },
            enabled = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
        ) { Text(stringResource(R.string.ai_save)) }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilesSection(names: List<Pair<String, Boolean>>, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ai_files),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAdd) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(stringResource(R.string.ai_add_files))
            }
        }
        if (names.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                names.forEachIndexed { index, (name, isImage) ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(index) },
                        label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(
                                if (isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                }
            }
        }
    }
}

/** One proposed change: what kind, the transaction as it would be, and a box to keep it. */
@Composable
private fun ChangeRow(change: LedgerChange, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val datePattern = stringResource(R.string.ai_date_pattern)
    val formatter = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.xs, end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (change) {
                    is LedgerChange.Add -> with(change.draft) {
                        Line(
                            kind = stringResource(R.string.ai_change_add),
                            title = merchant.ifBlank { category },
                            amount = signed(amount, type, currency)
                        )
                        Detail(listOfNotNull(
                            dateTime.format(formatter),
                            listOfNotNull(category, subcategory).joinToString(" · "),
                            account?.let { "${it.bankName} ${it.accountLast4}" },
                            toAccount?.let { "→ ${it.bankName} ${it.accountLast4}" },
                            notes
                        ).joinToString("  "))
                        if (possibleDuplicate != null) {
                            Text(
                                text = stringResource(R.string.ai_possible_duplicate) + ": " +
                                    describe(possibleDuplicate, formatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is LedgerChange.Update -> {
                        Line(
                            kind = stringResource(R.string.ai_change_update),
                            title = change.after.merchantName,
                            amount = signed(change.after.amount, change.after.transactionType, change.after.currency)
                        )
                        val b = change.before
                        val a = change.after
                        Detail(listOfNotNull(
                            a.dateTime.format(formatter),
                            "${b.merchantName} → ${a.merchantName}".takeIf { b.merchantName != a.merchantName },
                            "${listOfNotNull(b.category, b.subcategory).joinToString(" · ")} → " +
                                listOfNotNull(a.category, a.subcategory).joinToString(" · ")
                                    .takeIf { b.category != a.category || b.subcategory != a.subcategory },
                            "${b.description.orEmpty()} → ${a.description.orEmpty()}".takeIf { b.description != a.description }
                        ).joinToString("  "))
                    }
                    is LedgerChange.Delete -> {
                        Line(
                            kind = stringResource(R.string.ai_change_delete),
                            title = change.transaction.merchantName,
                            amount = signed(change.transaction.amount, change.transaction.transactionType, change.transaction.currency),
                            struck = true
                        )
                        Detail(listOfNotNull(
                            change.transaction.dateTime.format(formatter),
                            change.transaction.category,
                            change.reason
                        ).joinToString("  "))
                    }
                }
            }
        }
    }
}

@Composable
private fun Line(kind: String, title: String, amount: String, struck: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = kind,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (struck) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (struck) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun Detail(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.padding(start = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.md)
            )
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = null) }
        }
    }
}

@Composable
private fun PasswordDialog(name: String, onResult: (String?) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(stringResource(R.string.ai_pdf_password_title, name)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text(stringResource(R.string.ai_pdf_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        },
        confirmButton = {
            DialogConfirmButton(stringResource(R.string.ai_unlock), onClick = { onResult(password) }, enabled = password.isNotEmpty())
        },
        dismissButton = { DialogDismissButton(stringResource(R.string.ai_cancel), onClick = { onResult(null) }) }
    )
}

private fun signed(amount: BigDecimal, type: TransactionType, currency: String): String {
    val sign = when (type) {
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> ""
        else -> "−"
    }
    return sign + CurrencyFormatter.formatCurrency(amount, currency)
}

private fun describe(t: TransactionEntity, formatter: DateTimeFormatter) =
    "${t.dateTime.format(formatter)} ${t.merchantName}"

private fun host(url: String): String = runCatching { URI(url).host }.getOrNull() ?: url
