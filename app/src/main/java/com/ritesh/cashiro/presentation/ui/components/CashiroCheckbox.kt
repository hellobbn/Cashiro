package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CashiroCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    // Bulk selection should not launch three spring animations for every visible row.
    val backgroundColor = if (checked) checkedColor else Color.Transparent
    val borderColor = if (checked) checkedColor else uncheckedColor

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCheckedChange(!checked) }
                    )
                } else Modifier
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = checkmarkColor,
            modifier = Modifier
                .size(16.dp)
        )
    }
}
