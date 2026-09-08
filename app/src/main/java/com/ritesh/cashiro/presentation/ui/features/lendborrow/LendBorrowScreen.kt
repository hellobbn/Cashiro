package com.ritesh.cashiro.presentation.ui.features.lendborrow

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.domain.model.LendBorrowPerson
import com.ritesh.cashiro.domain.model.LendBorrowSummary
import com.ritesh.cashiro.domain.model.PersonCategory
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import androidx.compose.foundation.gestures.ScrollableDefaults
import com.ritesh.cashiro.presentation.ui.components.CashiroCheckbox
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DeleteMultiplePersonsDialog
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.LoanBalanceContent
import com.ritesh.cashiro.presentation.ui.components.SearchBarBox
import com.ritesh.cashiro.presentation.ui.components.SubtitleTag
import com.ritesh.cashiro.presentation.ui.components.listSingleItemShape
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Balance
import com.ritesh.cashiro.presentation.ui.icons.CloseCircle
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.ritesh.cashiro.presentation.effects.optionalHazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.LendBorrowScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPersonDetail: (Long) -> Unit,
    viewModel: LendBorrowViewModel = hiltViewModel(),
    animatedContentScope: AnimatedContentScope? = null,
    blurEffects: Boolean = LocalBlurEffects.current
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var searchInput by remember { mutableStateOf(TextFieldValue(text = uiState.searchQuery)) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != searchInput.text) {
            searchInput = searchInput.copy(text = uiState.searchQuery)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    var showFloatingLabel by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            showFloatingLabel = firstVisibleItem == 0
        }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.toggleSelectionMode()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = if (uiState.isSelectionMode) {
                    stringResource(R.string.items_selected_format, uiState.selectedPersonIds.size)
                } else {
                    stringResource(R.string.lend_borrow_title)
                },
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = {
                    if (uiState.isSelectionMode) {
                        NavigationContent { viewModel.toggleSelectionMode() }
                    } else {
                        NavigationContent(onNavigateBack)
                    }
                },
                actionContent = {
                    BlurredAnimatedVisibility(uiState.isSelectionMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            val allSelected = uiState.selectedPersonIds.size == uiState.filteredPersons.size &&
                                    uiState.filteredPersons.isNotEmpty()
                            IconButton(
                                onClick = {
                                    if (allSelected) viewModel.clearSelection()
                                    else viewModel.selectAllPersons()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                    contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                enabled = uiState.selectedPersonIds.isNotEmpty(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if (uiState.selectedPersonIds.isNotEmpty())
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Iconax.Bag,
                                    contentDescription = stringResource(R.string.delete_selected),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                blurEffects = blurEffects
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                val fabContainerColor = MaterialTheme.colorScheme.primaryContainer
                val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddTransactionSheet(true) },
                    expanded = showFloatingLabel,
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_record)) },
                    text = { Text(text = stringResource(R.string.add_record)) },
                    shape = if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large,
                    modifier = Modifier.then(
                        if (blurEffects) Modifier
                            .clip(if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large)
                            .hazeEffect(
                                state = hazeState,
                                block = fun HazeEffectScope.() {
                                    inputScale = HazeInputScale.Auto
                                    style = HazeDefaults.style(
                                        backgroundColor = Color.Transparent,
                                        tint = HazeDefaults.tint(fabContainerColor),
                                        blurRadius = 20.dp,
                                        noiseFactor = -1f,
                                    )
                                }
                            ) else Modifier
                    ),
                    containerColor = fabContainerColor,
                    contentColor = fabContentColor
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .optionalHazeSource(state = hazeState),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                item {
                    LendBorrowSummaryHeader(
                        summary = uiState.summary,
                        currency = uiState.baseCurrency,
                        animatedContentScope = animatedContentScope
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.md - 1.5.dp))
                    val filters = listOf(
                        LendBorrowFilter.ALL,
                        LendBorrowFilter.YOU_GET,
                        LendBorrowFilter.YOU_OWE,
                        LendBorrowFilter.OVERDUE
                    )
                    GenericTypeSwitcher(
                        selectedIndex = filters.indexOf(uiState.selectedFilter).coerceIn(0, filters.lastIndex),
                        onIndexChange = { index -> viewModel.onFilterSelected(filters[index]) },
                        options = filters.map { filter ->
                            when (filter) {
                                LendBorrowFilter.ALL -> stringResource(R.string.type_all)
                                LendBorrowFilter.YOU_GET -> stringResource(R.string.lend_borrow_filter_lent)
                                LendBorrowFilter.YOU_OWE -> stringResource(R.string.lend_borrow_filter_owed)
                                LendBorrowFilter.OVERDUE -> stringResource(R.string.overdue)
                                LendBorrowFilter.SETTLED -> stringResource(R.string.settled_tag)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.md - 1.5.dp))
                    SearchBarBox(
                        searchQuery = searchInput,
                        onSearchQueryChange = {
                            searchInput = it
                            viewModel.onSearchQueryChanged(it.text)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.search),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Iconax.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchInput.text.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchInput = TextFieldValue("")
                                        viewModel.onSearchQueryChanged("")
                                    }) {
                                        Icon(
                                            imageVector = Iconax.CloseCircle,
                                            contentDescription = stringResource(R.string.clear_search)
                                        )
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showCategoryMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = stringResource(R.string.category_filter),
                                            tint = if (uiState.selectedCategory != null) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showCategoryMenu,
                                        onDismissRequest = { showCategoryMenu = false },
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.category_all)) },
                                            onClick = {
                                                viewModel.onCategorySelected(null)
                                                showCategoryMenu = false
                                            }
                                        )
                                        HorizontalDivider(
                                            thickness = 1.5.dp,
                                            color = MaterialTheme.colorScheme.surface
                                        )
                                        PersonCategory.entries.forEachIndexed { index, category ->
                                            DropdownMenuItem(
                                                text = { Text(categoryLabel(category)) },
                                                onClick = {
                                                    viewModel.onCategorySelected(category)
                                                    showCategoryMenu = false
                                                }
                                            )
                                            if (index < PersonCategory.entries.size - 1) {
                                                HorizontalDivider(
                                                    thickness = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.surface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.md - 1.5.dp))
                }

                if (uiState.filteredPersons.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.Padding.empty),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Iconax.Balance,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = stringResource(R.string.no_persons_yet),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = stringResource(R.string.no_persons_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(uiState.filteredPersons, key = { _, it -> it.id }) { index, person ->
                        val position = ListItemPosition.from(index, uiState.filteredPersons.size)
                        PersonListItemCard(
                            person = person,
                            onClick = { onNavigateToPersonDetail(person.id) },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                if (!uiState.isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.selectPersonSet(setOf(person.id))
                                } else {
                                    viewModel.togglePersonSelection(person.id)
                                }
                            },
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedPersonIds.contains(person.id),
                            onSelectionToggle = { viewModel.togglePersonSelection(person.id) },
                            currency = uiState.baseCurrency,
                            animatedContentScope = animatedContentScope,
                            sharedElementKey = "person_avatar_${person.id}",
                            shape = position.toShape()
                        )
                    }
                }
            }
        }

        if (uiState.showAddTransactionSheet) {
            AddEditLendBorrowTransactionSheet(
                personsList = uiState.persons,
                initialPerson = uiState.selectedPersonForTx,
                accounts = uiState.accounts,
                categories = uiState.categories,
                attachmentService = viewModel.attachmentService,
                onDismiss = { viewModel.showAddTransactionSheet(false) },
                onAddPerson = { name, phone, notes, color, avatar, category ->
                    viewModel.addPerson(name, phone, notes, color, avatar, category)
                },
                onSave = { personId, type, amount, title, dueDate, accountId, category, merchant, attachments ->
                    viewModel.addTransaction(personId, type, amount, title, dueDate, accountId, category, merchant, attachments)
                }
            )
        }

        if (showDeleteConfirmation) {
            DeleteMultiplePersonsDialog(
                onDelete = {
                    val count = uiState.selectedPersonIds.size
                    viewModel.deleteSelectedPersons()
                    showDeleteConfirmation = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.persons_deleted_format, count),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onDismiss = { showDeleteConfirmation = false },
                selectedPersonIds = uiState.selectedPersonIds,
                blurEffects = blurEffects,
                hazeState = hazeState
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.LendBorrowSummaryHeader(
    summary: LendBorrowSummary,
    currency: String,
    animatedContentScope: AnimatedContentScope? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card)
        ) {
            LoanBalanceContent(
                summary = summary,
                currency = currency,
                animatedContentScope = animatedContentScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PersonListItemCard(
    person: LendBorrowPerson,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    currency: String,
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementKey: String? = null,
    shape: CornerBasedShape = listSingleItemShape
) {
    val isDark = isSystemInDarkTheme()
    val colorInt = try {
        android.graphics.Color.parseColor(person.color)
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#4CAF50")
    }

    val isGet = person.netBalance > BigDecimal.ZERO
    val isOwe = person.netBalance < BigDecimal.ZERO
    val statusText = when {
        isGet -> stringResource(R.string.gets_amount, CurrencyFormatter.formatCurrency(person.netBalance, currency))
        isOwe -> stringResource(R.string.owes_amount, CurrencyFormatter.formatCurrency(person.netBalance.abs(), currency))
        else -> stringResource(R.string.settled_tag)
    }

    val statusColor = when {
        isGet -> if (!isDark) income_light else income_dark
        isOwe -> if (!isDark) expense_light else expense_dark
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sharedModifier = if (animatedContentScope != null && sharedElementKey != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = sharedElementKey),
            animatedVisibilityScope = animatedContentScope,
            boundsTransform = { _, _ ->
                spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        )
    } else {
        Modifier
    }

    ListItem(
        headline = {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = 1)
            )
        },
        supporting = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.basicMarquee(iterations = 1)
            ) {
                SubtitleTag(
                    text = categoryLabel(person.category ?: PersonCategory.OTHER),
                    color = categoryColor(person.category ?: PersonCategory.OTHER)
                )
                if (person.hasOverdue) {
                    SubtitleTag(
                        text = stringResource(R.string.overdue_tag),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        leading = {
            BlurredAnimatedVisibility(isSelectionMode) {
                CashiroCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle?.invoke() },
                    modifier = Modifier.size(40.dp)
                )
            }
            BlurredAnimatedVisibility(!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .then(sharedModifier)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt)),
                    contentAlignment = Alignment.Center
                ) {
                    if (person.avatar != null) {
                        AsyncImage(
                            model = person.avatar,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = person.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        trailing = {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        },
        onClick = {
            if (isSelectionMode) {
                onSelectionToggle?.invoke()
            } else {
                onClick()
            }
        },
        onLongClick = onLongClick,
        shape = shape,
        listColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        padding = PaddingValues(0.dp)
    )
}

@Composable
fun categoryLabel(category: PersonCategory): String = when (category) {
    PersonCategory.FRIEND -> stringResource(R.string.category_friends)
    PersonCategory.FAMILY -> stringResource(R.string.category_family)
    PersonCategory.COLLEAGUE -> stringResource(R.string.category_colleagues)
    PersonCategory.OTHER -> stringResource(R.string.category_other)
}

@Composable
fun categoryColor(category: PersonCategory): Color = when (category) {
    PersonCategory.FRIEND -> Color(0xFF6D7CFF)
    PersonCategory.FAMILY -> Color(0xFFF06292)
    PersonCategory.COLLEAGUE -> Color(0xFF26A69A)
    PersonCategory.OTHER -> Color(0xFF9E9E9E)
}
