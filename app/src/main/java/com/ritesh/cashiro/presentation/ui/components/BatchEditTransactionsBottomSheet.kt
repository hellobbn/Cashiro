package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.icons.Calendar
import com.ritesh.cashiro.presentation.ui.icons.DocumentText2
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Folder2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ReceiptItem
import com.ritesh.cashiro.presentation.ui.icons.VideoTime
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditTransactionsBottomSheet(
    selectedIdsCount: Int,
    categories: Map<String, CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    onDismiss: () -> Unit,
    onApply: (
        newDate: LocalDate?,
        newTime: LocalTime?,
        newCategory: String?,
        newSubcategory: String?,
        newAmount: BigDecimal?,
        newNote: String?
    ) -> Unit,
    blurEffects: Boolean = false,
    hazeState: HazeState = remember { HazeState() }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var updateDate by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var updateTime by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }

    var updateCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedSubcategory by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }

    var updateAmount by remember { mutableStateOf(false) }
    var selectedAmount by remember { mutableStateOf<BigDecimal?>(null) }
    var showNumberPad by remember { mutableStateOf(false) }

    var updateNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    val hasAnySelection = updateDate || updateTime || updateCategory || updateAmount || updateNote
    val lazyListState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content)
                    .padding(bottom = 0.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Edit2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.batch_edit_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.batch_edit_updating_format, selectedIdsCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    DashedLine(
                        modifier = Modifier,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

//                item {
//                    Text(
//                        text = stringResource(R.string.batch_edit_toggle_hint),
//                        style = MaterialTheme.typography.labelLarge,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }

                // Date Card
                item {
                    BatchEditOptionCard(
                        title = stringResource(R.string.batch_edit_date),
                        icon = Iconax.Calendar,
                        checked = updateDate,
                        onCheckedChange = {
                            updateDate = it
                            if (it && selectedDate == LocalDate.now()) {
                                showDatePicker = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showDatePicker = true }) {
                                Text(stringResource(R.string.batch_edit_pick_date))
                            }
                        }
                    }
                }

                // Time Card
                item {
                    BatchEditOptionCard(
                        title = stringResource(R.string.batch_edit_time),
                        icon = Iconax.VideoTime,
                        checked = updateTime,
                        onCheckedChange = {
                            updateTime = it
                            if (it) {
                                showTimePicker = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                .clickable { showTimePicker = true }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showTimePicker = true }) {
                                Text(stringResource(R.string.batch_edit_pick_time))
                            }
                        }
                    }
                }

                // Category Card
                item {
                    BatchEditOptionCard(
                        title = stringResource(R.string.batch_edit_category_subcategory),
                        icon = Iconax.Folder2,
                        checked = updateCategory,
                        onCheckedChange = {
                            updateCategory = it
                            if (it && selectedCategory == null) {
                                showCategorySheet = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                .clickable { showCategorySheet = true }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedCategory?.name ?: stringResource(R.string.batch_edit_select_category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedSubcategory != null) {
                                    Text(
                                        text = selectedSubcategory?.name ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TextButton(onClick = { showCategorySheet = true }) {
                                Text(stringResource(R.string.batch_edit_choose))
                            }
                        }
                    }
                }

                // Amount Card
                item {
                    BatchEditOptionCard(
                        title = stringResource(R.string.batch_edit_amount),
                        icon = Iconax.ReceiptItem,
                        checked = updateAmount,
                        onCheckedChange = {
                            updateAmount = it
                            if (it && selectedAmount == null) {
                                showNumberPad = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                .clickable { showNumberPad = true }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAmount?.let { CurrencyFormatter.formatAmount(it) }
                                    ?: "0.00",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedAmount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showNumberPad = true }) {
                                Text(stringResource(R.string.batch_edit_set_amount))
                            }
                        }
                    }
                }

                // Note Card
                item {
                    BatchEditOptionCard(
                        title = stringResource(R.string.batch_edit_note_description),
                        icon = Iconax.DocumentText2,
                        checked = updateNote,
                        onCheckedChange = { updateNote = it }
                    ) {
                        TextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text(stringResource(R.string.batch_edit_note_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }


            }
            // Action Buttons at Bottom
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
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Apply Button
                Button(
                    onClick = {
                        onApply(
                            if (updateDate) selectedDate else null,
                            if (updateTime) selectedTime else null,
                            if (updateCategory) selectedCategory?.name else null,
                            if (updateCategory) selectedSubcategory?.name else null,
                            if (updateAmount) selectedAmount else null,
                            if (updateNote) noteText else null
                        )
                        onDismiss()
                    },
                    enabled = hasAnySelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (hasAnySelection) stringResource(R.string.batch_edit_apply_format, selectedIdsCount) else stringResource(R.string.batch_edit_check_options),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        TimePicker(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            },
            timePickerState = timePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Category Selection Sheet
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategorySelectionSheet(
                categories = categories.values.toList(),
                subcategoriesMap = subcategoriesMap,
                onSelectionComplete = { category, subcategory ->
                    selectedCategory = category
                    selectedSubcategory = subcategory
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false }
            )
        }
    }

    // NumberPad for Amount
    if (showNumberPad) {
        ModalBottomSheet(
            onDismissRequest = { showNumberPad = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                initialValue = selectedAmount?.toPlainString() ?: "0",
                onDone = { newAmount: String ->
                    selectedAmount = newAmount.toBigDecimalOrNull()
                    showNumberPad = false
                },
                title = stringResource(R.string.batch_edit_set_batch_amount)
            )
        }
    }
}

@Composable
private fun BatchEditOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (checked) 1.5.dp else 1.dp,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
            )
            .padding(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (checked) {
                Box(modifier = Modifier.padding(top = Spacing.xs)) {
                    content()
                }
            }
        }
    }
}
