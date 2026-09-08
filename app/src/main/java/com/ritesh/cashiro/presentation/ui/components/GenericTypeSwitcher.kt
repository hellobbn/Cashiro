package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.ui.theme.Dimensions

@Composable
fun GenericTypeSwitcher(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    val themeColors = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()

    BoxWithConstraints(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(Dimensions.Radius.md))
            .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
            .padding(6.dp)
    ) {
        val maxWidth = maxWidth
        val indicatorWidth = maxWidth / options.size
        val indicatorOffset = animateDpAsState(
            targetValue = indicatorWidth * selectedIndex,
            animationSpec = tween(durationMillis = 150),
            label = "Indicator offset"
        )

        // Animated Indicator
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(indicatorOffset.value.roundToPx(), 0) }
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkTheme) themeColors.surface else Color.White)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, text ->
                TypeButton(
                    text = text,
                    isSelected = selectedIndex == index,
                    onClick = { onIndexChange(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.inverseSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
