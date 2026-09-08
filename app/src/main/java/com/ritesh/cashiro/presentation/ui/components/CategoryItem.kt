package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.presentation.ui.features.categories.SubcategoryRow
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryItem(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    onClick: (() -> Unit)?,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (SubcategoryEntity) -> Unit,
    showAddSubcategoryButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val showAddButton = showAddSubcategoryButton && subcategories.isEmpty()

    CashiroCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with Icon
                CategoryChip(
                    category = category,
                    onClick = onClick,
                    showText = true,
                    modifier = Modifier.weight(1f)
                )

                // Subcategory Add/Toggle
                BlurredAnimatedVisibility(showAddButton) {
                    IconButton(
                        onClick = onAddSubcategory,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.largeIncreased
                    ) {
                        Icon(
                            Icons.Rounded.AddCircle,
                            contentDescription = "Add Subcategory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Subcategory Row (horizontal chips)
            BlurredAnimatedVisibility(subcategories.isNotEmpty()) {
                SubcategoryRow(
                        subcategories = subcategories,
                        onSubcategoryClick = onEditSubcategory,
                        onAddClick = onAddSubcategory,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                        showAddButton = showAddSubcategoryButton
                )
            }
            BlurredAnimatedVisibility(subcategories.isEmpty()) {
                // Show add button if no subcategories (and allowed)
                 if (showAddSubcategoryButton) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                 }
            }
        }
    }
}
