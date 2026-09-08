package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.presentation.ui.theme.Spacing

@Composable
fun SubtitleTag(
    text: String,
    color: Color,
    alpha: Float = 0.2f,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val tagModifier = modifier
        .background(color.copy(alpha = alpha), RoundedCornerShape(Spacing.lg))
        .padding(horizontal = Spacing.sm, vertical = 2.dp)
    // These non-interactive labels need neither Surface's content/elevation providers nor
    // an extra Row when there is no icon. Keep the same rounded fill, padding and text.
    if (icon == null) {
        SubtitleTagText(text, tagModifier)
    } else {
        Row(
            modifier = tagModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            SubtitleTagText(text)
        }
    }
}

@Composable
private fun SubtitleTagText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
