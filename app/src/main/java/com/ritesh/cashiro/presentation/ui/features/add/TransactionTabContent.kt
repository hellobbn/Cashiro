package com.ritesh.cashiro.presentation.ui.features.add

import androidx.compose.material.icons.rounded.Bolt

import androidx.compose.material3.Switch

import androidx.compose.material3.AssistChip

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.ritesh.cashiro.presentation.ui.components.CashiroModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.features.lendborrow.AddEditPersonSheet
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.components.AccountSelectionSheet
import com.ritesh.cashiro.presentation.ui.components.AttachmentSection
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.presentation.ui.components.DatePicker
import com.ritesh.cashiro.presentation.ui.components.TimePicker
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.icons.Box2
import com.ritesh.cashiro.presentation.ui.icons.Calendar
import com.ritesh.cashiro.presentation.ui.icons.DocumentText2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Shop
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.IconResolutionUtils
import com.ritesh.cashiro.utils.horizontalFadingEdge
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionTabContent(
    viewModel: AddViewModel,
    onSave: () -> Unit,
    isTransitioning: Boolean = false,
    blurEffects: Boolean,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val uiState by viewModel.transactionUiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactionSubcategories by viewModel.transactionSubcategories.collectAsState()
    val transactionAttachments by viewModel.transactionAttachments.collectAsState()
    val persons by viewModel.persons.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedCategoryObj = remember(uiState.category, categories) {
        categories.find { it.name == uiState.category }
    }
    val selectedSubcategoryObj = remember(uiState.subcategory, transactionSubcategories) {
        transactionSubcategories.find { it.name == uiState.subcategory }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showNumberPad by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxSize()
                .imePadding() // Handle keyboard properly
                .verticalScroll(
                    state = scrollState,
                    enabled = !isTransitioning
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick-add templates
            val quickTemplates by viewModel.quickTemplates.collectAsState()
            if (quickTemplates.isNotEmpty()) {
                QuickTemplateRow(
                    templates = quickTemplates,
                    categories = categories,
                    onSelect = viewModel::applyQuickTemplate
                )
            }

            // Amount Input
            AmountInput(
                amount = uiState.amount.ifEmpty { "0" },
                currencySymbol = CurrencyFormatter.getCurrencySymbol(
                    uiState.selectedAccount?.currency ?: "CNY"
                ),
                onClick = {
                    showNumberPad = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Transaction Type Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.transaction_type_required),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.entries.filter { it != TransactionType.BALANCE_UPDATE }.forEach { type ->
                        FilterChip(
                            selected = uiState.transactionType == type,
                            onClick = { viewModel.updateTransactionType(type) },
                            label = {
                                Text(stringResource(type.labelRes))
                            },
                            leadingIcon =
                                if (uiState.transactionType == type) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderWidth = 0.dp,
                                selected = uiState.transactionType == type,
                                enabled = true
                            ),
                        )
                    }
                }
            }
            // Date and Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Date Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(Dimensions.Radius.md)
                        )
                        .padding(8.dp)
                        .clickable(
                            onClick = { showDatePicker = true },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val themeColors = MaterialTheme.colorScheme
                        Icon(
                            imageVector = Iconax.Calendar,
                            contentDescription = stringResource(R.string.date_picker_desc),
                            tint = themeColors.onSurface
                        )
                        Spacer(Modifier.size(8.dp))

                        val dateLabel =
                            uiState.date.format(DateTimeFormatter.ofPattern("dd MMMM"))
                        val yearLabel =
                            uiState.date.format(DateTimeFormatter.ofPattern("yyyy"))
                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = yearLabel,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Start,
                                color = themeColors.primary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = dateLabel,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start,
                                color = themeColors.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }


                // Time Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .clickable { showTimePicker = true },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val hour = if (uiState.date.hour % 12 == 0) 12 else uiState.date.hour % 12
                        val minute = uiState.date.minute
                        val amPm = if (uiState.date.hour < 12) stringResource(R.string.am_lbl) else stringResource(R.string.pm_lbl)

                        Box(modifier = Modifier
                            .padding(5.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                text = String.format("%02d", hour),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(5.dp)
                            )
                        }

                        Text(
                            text = ":",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                        )

                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = String.format("%02d", minute),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(5.dp)
                            )
                        }

                        Box(modifier = Modifier.padding(5.dp)) {
                            Text(
                                text = amPm,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // Due Date (only relevant for Lent / Borrowed loans)
            BlurredAnimatedVisibility(
                uiState.transactionType == TransactionType.LENT ||
                    uiState.transactionType == TransactionType.BORROWED
            ) {
                var showDueDatePicker by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(Dimensions.Radius.md)
                        )
                        .clip(RoundedCornerShape(Dimensions.Radius.md))
                        .clickable { showDueDatePicker = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Iconax.Calendar,
                            contentDescription = null,
                            tint = if (uiState.dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.due_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.dueDate?.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"))
                                    ?: stringResource(R.string.no_due_date),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                        if (uiState.dueDate != null) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.clear_due_date),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        showDueDatePicker = false
                                        viewModel.updateTransactionDueDate(null)
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showDueDatePicker) {
                    val dueDatePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = (uiState.dueDate ?: java.time.LocalDateTime.now())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    )
                    DatePicker(
                        onDismiss = { showDueDatePicker = false },
                        onConfirm = {
                            dueDatePickerState.selectedDateMillis?.let { millis ->
                                viewModel.updateTransactionDueDate(
                                    java.time.Instant.ofEpochMilli(millis)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime()
                                )
                            }
                            showDueDatePicker = false
                        },
                        datePickerState = dueDatePickerState,
                        blurEffects = blurEffects,
                        hazeState = hazeState
                    )
                }
            }

            // Person Selection for Lent / Borrowed
            BlurredAnimatedVisibility(
                uiState.transactionType == TransactionType.LENT ||
                    uiState.transactionType == TransactionType.BORROWED
            ) {
                var showAddPersonSheet by remember { mutableStateOf(false) }
                var lastAddedPersonName by remember { mutableStateOf<String?>(null) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_person),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val personLazyListState = rememberLazyListState()
                    LazyRow(
                        state = personLazyListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalFadingEdge(
                                canScrollBackward = personLazyListState.canScrollBackward,
                                canScrollForward = personLazyListState.canScrollForward
                            )
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddPersonSheet = true }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_person),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.add_new),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(persons, key = { it.id }) { person ->
                            val isSelected = uiState.selectedPersonId == person.id
                            val colorInt = try {
                                android.graphics.Color.parseColor(person.color)
                            } catch (_: Exception) {
                                android.graphics.Color.parseColor("#4CAF50")
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateTransactionPersonId(person.id) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (person.avatar != null) {
                                        AsyncImage(
                                            model = person.avatar,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = person.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(56.dp).padding(top = 4.dp),
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (showAddPersonSheet) {
                    AddEditPersonSheet(
                        attachmentService = viewModel.attachmentService,
                        onDismiss = { showAddPersonSheet = false },
                        onSave = { name, phone, notes, color, avatar, category ->
                            lastAddedPersonName = name
                            viewModel.addPerson(name, phone, notes, color, avatar, category)
                            showAddPersonSheet = false
                        }
                    )
                }

                LaunchedEffect(persons) {
                    val lastAdded = lastAddedPersonName
                    if (lastAdded != null) {
                        persons.firstOrNull { it.name == lastAdded }?.let {
                            viewModel.updateTransactionPersonId(it.id)
                        }
                        lastAddedPersonName = null
                    }
                }
            }

            // Accounts Section
            val accounts by viewModel.accounts.collectAsState()
            var showAccountSheet by remember { mutableStateOf(false) }
            var showTargetAccountSheet by remember { mutableStateOf(false) }

            // Conditional UI based on transaction type
            BlurredAnimatedVisibility(uiState.transactionType == TransactionType.TRANSFER) {
                // Transfer Type UI: Source Account + Exchange Icon + Target Account + Category
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            // Source Account Card
                            Card(
                                onClick = { showAccountSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(0.dp, Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BrandIcon(
                                        merchantName = uiState.selectedAccount?.bankName ?: "",
                                        accountIconResId = uiState.selectedAccount?.iconResId ?: 0,
                                        accountIconName = uiState.selectedAccount?.iconName,
                                        size = 26.dp,
                                        showBackground = false
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.selectedAccount?.bankName ?: stringResource(R.string.select_source_account),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (uiState.selectedAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.selectedAccount != null) {
                                            Text(
                                                text = if (uiState.selectedAccount?.accountLast4 == "wallet") "${uiState.selectedAccount?.accountLast4}" else "••${uiState.selectedAccount?.accountLast4}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Target Account Card
                            Card(
                                onClick = { showTargetAccountSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(0.dp, Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BrandIcon(
                                        merchantName = uiState.targetAccount?.bankName ?: "",
                                        accountIconResId = uiState.targetAccount?.iconResId ?: 0,
                                        accountIconName = uiState.targetAccount?.iconName,
                                        size = 26.dp,
                                        showBackground = false
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.targetAccount?.bankName ?: stringResource(R.string.select_target_account),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (uiState.targetAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.targetAccount != null) {
                                            Text(
                                                text = if (uiState.targetAccount?.accountLast4 == "wallet") "${uiState.targetAccount?.accountLast4}" else "••${uiState.targetAccount?.accountLast4}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        // Exchange Icon
                        Box(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(elevation = 3.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = stringResource(R.string.transfer_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection with full rounded corners
                    val categoryInteractionSource = remember { MutableInteractionSource() }
                    TextField(
                        value = uiState.category,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.category_label), fontWeight = FontWeight.SemiBold) },
                        readOnly = true,
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    interactionSource = categoryInteractionSource,
                                    indication = null
                                ) {
                                    showCategoryMenu = true
                                },
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            val context = LocalContext.current
                            val resolvedResId = remember(selectedCategoryObj) {
                                selectedCategoryObj?.let { cat ->
                                    if (!cat.iconName.isNullOrEmpty()) {
                                        val res = IconResolutionUtils.nameToResId(context, cat.iconName)
                                        if (res != 0) res else cat.iconResId
                                    } else cat.iconResId
                                } ?: 0
                            }

                            if (resolvedResId != 0) {
                                Icon(
                                    painter = painterResource(id = resolvedResId),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Iconax.Box2, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        },
                        isError = uiState.categoryError != null,
                        supportingText = uiState.categoryError?.let { { Text(it) } },
                        enabled = false,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            ),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledIndicatorColor = Color.Transparent,
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            BlurredAnimatedVisibility(uiState.transactionType != TransactionType.TRANSFER){
                // Non-Transfer Type UI: Original layout with connected sections
                Column(
                    modifier = Modifier.animateContentSize().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Card(
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BrandIcon(
                                merchantName = uiState.selectedAccount?.bankName ?: "",
                                accountIconResId = uiState.selectedAccount?.iconResId ?: 0,
                                accountIconName = uiState.selectedAccount?.iconName,
                                size = 26.dp,
                                showBackground = false
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedAccount?.bankName ?: stringResource(R.string.select_account),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                        if (uiState.selectedAccount != null)
                                            MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.selectedAccount != null) {
                                    Text(
                                        text = if (uiState.selectedAccount?.accountLast4 == "wallet") "${uiState.selectedAccount?.accountLast4}" else "••${uiState.selectedAccount?.accountLast4}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Category Selection
                    val categoryInteractionSource = remember { MutableInteractionSource() }
                    TextField(
                        value = uiState.category,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.category_label), fontWeight = FontWeight.SemiBold) },
                        readOnly = true,
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    interactionSource = categoryInteractionSource,
                                    indication = null
                                ) {
                                    showCategoryMenu = true
                                },
                        shape =
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                        leadingIcon = {
                            val context = LocalContext.current
                            val resolvedResId = remember(selectedCategoryObj) {
                                selectedCategoryObj?.let { cat ->
                                    if (!cat.iconName.isNullOrEmpty()) {
                                        val res = IconResolutionUtils.nameToResId(context, cat.iconName)
                                        if (res != 0) res else cat.iconResId
                                    } else cat.iconResId
                                } ?: 0
                            }

                            if (resolvedResId != 0) {
                                Icon(
                                    painter = painterResource(id = resolvedResId),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Iconax.Box2, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        },
                        isError = uiState.categoryError != null,
                        supportingText = uiState.categoryError?.let { { Text(it) } },
                        enabled = false, // Disable typing, handle click above
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledIndicatorColor = Color.Transparent,
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Subcategory Display (Read-only, selected via sheet)
                    if (uiState.subcategory != null) {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        TextField(
                            value = uiState.subcategory ?: stringResource(R.string.none_label),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.subcategory_label)) },
                            leadingIcon = {
                                val context = LocalContext.current
                                val resolvedResId = remember(selectedSubcategoryObj) {
                                    selectedSubcategoryObj?.let { sub ->
                                        if (!sub.iconName.isNullOrEmpty()) {
                                            val res = IconResolutionUtils.nameToResId(context, sub.iconName)
                                            if (res != 0) res else sub.iconResId
                                        } else sub.iconResId
                                    } ?: 0
                                }

                                if (resolvedResId != 0) {
                                    Icon(
                                        painter = painterResource(id = resolvedResId),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = if (selectedSubcategoryObj != null) {
                                val color = try {
                                    Color(selectedSubcategoryObj.color.toColorInt())
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                                TextFieldDefaults.colors(
                                    focusedContainerColor = color.copy(alpha = 0.2f),
                                    unfocusedContainerColor = color.copy(alpha = 0.2f),
                                    disabledContainerColor = color.copy(alpha = 0.2f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    disabledIndicatorColor = Color.Transparent,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }
            }

            // Show subcategory for Transfer type outside the conditional
            if (uiState.transactionType == TransactionType.TRANSFER && uiState.subcategory != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                TextField(
                    value = uiState.subcategory ?: stringResource(R.string.none_label),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.subcategory_label)) },
                    leadingIcon = {
                        val context = LocalContext.current
                        val resolvedResId = remember(selectedSubcategoryObj) {
                            selectedSubcategoryObj?.let { sub ->
                                if (!sub.iconName.isNullOrEmpty()) {
                                    val res = IconResolutionUtils.nameToResId(context, sub.iconName)
                                    if (res != 0) res else sub.iconResId
                                } else sub.iconResId
                            } ?: 0
                        }

                        if (resolvedResId != 0) {
                            Icon(
                                painter = painterResource(id = resolvedResId),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = if (selectedSubcategoryObj != null) {
                        val color = try {
                            Color(selectedSubcategoryObj.color.toColorInt())
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        TextFieldDefaults.colors(
                            focusedContainerColor = color.copy(alpha = 0.2f),
                            unfocusedContainerColor = color.copy(alpha = 0.2f),
                            disabledContainerColor = color.copy(alpha = 0.2f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledIndicatorColor = Color.Transparent,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }


            if (showAccountSheet) {
                CashiroModalBottomSheet(
                    onDismissRequest = { showAccountSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    AccountSelectionSheet(
                        accounts = accounts,
                        selectedAccount = uiState.selectedAccount,
                        onAccountSelected = {
                            viewModel.updateTransactionAccount(it)
                            showAccountSheet = false
                        },
                        isTransitioning = isTransitioning,
                        showNoneOption = false
                    )
                }
            }

            // Target Account BottomSheet (for Transfer type)
            if (showTargetAccountSheet) {
                CashiroModalBottomSheet(
                    onDismissRequest = { showTargetAccountSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    // Filter out the source account from target selection
                    val availableTargetAccounts = accounts.filter { it.id != uiState.selectedAccount?.id }
                    AccountSelectionSheet(
                        accounts = availableTargetAccounts,
                        selectedAccount = uiState.targetAccount,
                        title = stringResource(R.string.select_target_account),
                        onAccountSelected = {
                            viewModel.updateTransactionTargetAccount(it)
                            showTargetAccountSheet = false
                        },
                        isTransitioning = isTransitioning,
                        showNoneOption = false
                    )
                }
            }

            // NumberPad for Amount Input
            if (showNumberPad) {
                CashiroModalBottomSheet(
                    onDismissRequest = { showNumberPad = false },
                    sheetState = sheetState ,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    NumberPad(
                        initialValue = uiState.amount.ifEmpty { "0" },
                        onDone = { newAmount ->
                            viewModel.updateTransactionAmount(newAmount)
                            showNumberPad = false
                        },
                        title = stringResource(R.string.enter_amount)
                    )
                }
            }

            // Category Selection Sheet
            if (showCategoryMenu) {
                val allSubcategories by viewModel.allSubcategories.collectAsState(initial = emptyMap())
                CashiroModalBottomSheet(
                    onDismissRequest = { showCategoryMenu = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    CategorySelectionSheet(
                        categories = categories,
                        subcategoriesMap = allSubcategories,
                        onSelectionComplete = { category, subcategory ->
                            viewModel.updateTransactionCategory(category.name)
                            viewModel.updateTransactionSubcategory(subcategory?.name)
                            showCategoryMenu = false
                        },
                        onDismiss = { showCategoryMenu = false }
                    )
                }
            }


            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                BlurredAnimatedVisibility(
                    uiState.transactionType != TransactionType.LENT &&
                        uiState.transactionType != TransactionType.BORROWED
                ) {
                // Merchant Name Input
                TextField(
                    value = uiState.merchant,
                    onValueChange = viewModel::updateTransactionMerchant,
                    label = { Text(stringResource(R.string.merchant_lbl), fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                    leadingIcon = { Icon(Iconax.Shop, contentDescription = null) },
                    isError = uiState.merchantError != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    ),
                    supportingText = uiState.merchantError?.let { { Text(it) } },
                )
                }

                // Save this entry as a quick-add template (not for transfers / loans)
                if (uiState.transactionType != TransactionType.TRANSFER &&
                    uiState.transactionType != TransactionType.LENT &&
                    uiState.transactionType != TransactionType.BORROWED
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.quick_template_save_toggle),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.saveAsQuickTemplate,
                            onCheckedChange = viewModel::updateSaveAsQuickTemplate
                        )
                    }
                }

                // Notes/Description (Optional)
                TextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateTransactionNotes,
                label = { Text(stringResource(R.string.notes_optional), fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.fillMaxWidth(),
                shape = if (uiState.transactionType == TransactionType.LENT ||
                    uiState.transactionType == TransactionType.BORROWED
                ) RoundedCornerShape(16.dp) else
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    leadingIcon = {
                        Icon(Iconax.DocumentText2, contentDescription = null)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    ),
                )
            }

            // Attachments Section
            AttachmentSection(
                attachments = transactionAttachments,
                attachmentService = viewModel.attachmentService,
                onAddAttachment = viewModel::addTransactionAttachment,
                onRemoveAttachment = viewModel::removeTransactionAttachment,
                onAttachmentClick = { path ->
                    if (viewModel.attachmentService.isUrl(path)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    } else {
                        val uri = viewModel.attachmentService.getAttachmentUri(path)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, viewModel.attachmentService.getAttachmentMimeType(path))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                    }
                },
                isEditable = true
            )

            // Bottom padding for keyboard
            Spacer(modifier = Modifier.height(80.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { viewModel.saveTransaction(onSuccess = onSave) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensions.Padding.content)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                enabled = uiState.isValid && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    uiState.date
                        .toLocalDate()
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
            )

        DatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    viewModel.updateTransactionDate(millis)
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = uiState.date.hour,
                initialMinute = uiState.date.minute
            )

        TimePicker(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.updateTransactionTime(
                    timePickerState.hour,
                    timePickerState.minute
                )
                showTimePicker = false
            },
            timePickerState = timePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}


/** Horizontal row of user-defined quick-add templates; a tap pre-fills the form. */
@Composable
private fun QuickTemplateRow(
    templates: List<com.ritesh.cashiro.data.database.entity.QuickTemplateEntity>,
    categories: List<com.ritesh.cashiro.data.database.entity.CategoryEntity>,
    onSelect: (com.ritesh.cashiro.data.database.entity.QuickTemplateEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.quick_add_templates),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                AssistChip(
                    onClick = { onSelect(template) },
                    label = {
                        val amountText = template.amount
                            ?.takeIf { template.prefillAmount }
                            ?.let { " · " + CurrencyFormatter.formatCurrency(it, template.currency ?: "CNY") }
                            ?: ""
                        Text(template.name + amountText, maxLines = 1)
                    },
                    leadingIcon = {
                        BrandIcon(
                            merchantName = template.merchantName,
                            size = 22.dp,
                            showBackground = false,
                            categoryEntity = categories.find { it.name == template.category },
                            category = template.category,
                            subcategory = template.subcategory
                        )
                    }
                )
            }
        }
    }
}
