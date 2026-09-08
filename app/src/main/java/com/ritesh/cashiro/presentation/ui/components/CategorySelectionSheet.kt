package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.presentation.ui.icons.CloseCircle
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun CategorySelectionSheet(
    categories: List<CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    onSelectionComplete: (CategoryEntity, SubcategoryEntity?) -> Unit,
    onDismiss: () -> Unit
) {

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    
    // Filter categories based on search
    val filteredCategories = remember(categories, subcategoriesMap, searchQuery.text) {
        if (searchQuery.text.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                val categoryMatches = category.name.contains(searchQuery.text, ignoreCase = true)
                val subcategoriesMatch = subcategoriesMap[category.id]?.any {
                    it.name.contains(searchQuery.text, ignoreCase = true)
                } == true
                categoryMatches || subcategoriesMatch
            }
        }
    }

    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    // Auto-expand categories that have matching subcategories when searching
    LaunchedEffect(searchQuery.text) {
        if (searchQuery.text.isNotBlank()) {
            filteredCategories.forEach { category ->
                val hasMatchingSubcategory = subcategoriesMap[category.id]?.any {
                    it.name.contains(searchQuery.text, ignoreCase = true)
                } == true
                if (hasMatchingSubcategory) {
                    expandedStates[category.id] = true
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search Bar
        Box(modifier = Modifier.padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)) {
             SearchBarBox(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                 label = {
                     Text(text = stringResource(com.ritesh.cashiro.R.string.search))
                 },
                leadingIcon = {},
                trailingIcon = if (searchQuery.text.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(Iconax.CloseCircle, contentDescription = "Clear search")
                        }
                    }
                } else { {} }
            )
        }

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No categories found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.sm,
                        bottom = 0.dp
                    )
                    .clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(
                    items = filteredCategories,
                    key = { it.id },
                    contentType = { "category" }
                ) { category ->
                    val subs = subcategoriesMap[category.id] ?: emptyList()
                    
                    val isExpanded = expandedStates[category.id] == true
                    
                    val displayedSubcategories = if (searchQuery.text.isNotBlank()) {
                        // When searching, show only matching subcategories OR all if category matches
                        val categoryMatches = category.name.contains(searchQuery.text, ignoreCase = true)
                        if (categoryMatches) {
                            subs
                        } else {
                            subs.filter { it.name.contains(searchQuery.text, ignoreCase = true) }
                        }
                    } else if (isExpanded) {
                        subs
                    } else {
                        emptyList()
                    }

                    CategoryItem(
                            category = category,
                            subcategories = displayedSubcategories,
                            onClick = {
                                if (subs.isNotEmpty()) {
                                    if (isExpanded) {
                                        // If already expanded, select the category itself
                                        onSelectionComplete(category, null)
                                    } else {
                                        // Otherwise, expand to show subcategories
                                        expandedStates[category.id] = true
                                    }
                                } else {
                                    onSelectionComplete(category, null)
                                }
                            },
                            onAddSubcategory = {},
                            onEditSubcategory = { sub ->
                                onSelectionComplete(category, sub)
                            },
                            showAddSubcategoryButton = false
                        )

                }
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}
