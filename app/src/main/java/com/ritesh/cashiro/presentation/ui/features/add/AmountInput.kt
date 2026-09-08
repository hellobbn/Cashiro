package com.ritesh.cashiro.presentation.ui.features.add

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmountInput(
    amount: String,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountFontSize: Dp = 50.dp,
    contentAlignment: Alignment = Alignment.Center,
    enabled: Boolean = true,
) {
    // Amount input container
    Box(
        contentAlignment = contentAlignment,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencySymbol,
                fontSize = amountFontSize.value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (amount.isEmpty() || amount == "0") MaterialTheme.colorScheme.inverseSurface.copy(
                    0.5f
                ) else MaterialTheme.colorScheme.inverseSurface
            )
            AnimatedCounterText(
                amount = amount,
                fontSize = amountFontSize.value.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AnimatedCounterText(
    amount: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    maxLines: Int = 1,
    fontWeight: FontWeight? = FontWeight.Normal,
    @Suppress("UNUSED_PARAMETER") animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    ),
    textStyle: TextStyle = TextStyle(
        textMotion = TextMotion.Static,
        lineBreak = LineBreak.Simple,
        textAlign = TextAlign.Start,
    ),
    specialKeys: Set<Char> = setOf('+', '-', '*', '/', '(', ')', '%', '×', '÷'),
    onAnimationComplete: () -> Unit = {},
    enableDynamicSizing: Boolean = true
) {
    // Never count through intermediate money values: format once per input, retaining
    // decimal precision and avoiding per-frame text measurement/font-size changes.
    val displayText = remember(amount, specialKeys) { formatCounterAmount(amount, specialKeys) }
    val completion = androidx.compose.runtime.rememberUpdatedState(onAnimationComplete)
    LaunchedEffect(amount) { completion.value() }

    // Calculate dynamic font size based on display text length
    val dynamicFontSize = remember(displayText, enableDynamicSizing, fontSize) {
        if (enableDynamicSizing) {
            calculateDynamicFontSizeForAnimatedCounterText(displayText)
        } else {
            fontSize.value.toInt()
        }
    }

    Text(
        text = " $displayText",
        modifier = modifier,
        style = textStyle,
        fontSize = dynamicFontSize.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        color = if (amount.isEmpty() || amount == "0")
            MaterialTheme.colorScheme.inverseSurface.copy(0.5f)
        else
            MaterialTheme.colorScheme.inverseSurface
    )
}

internal fun formatCounterAmount(amount: String, specialKeys: Set<Char>): String {
    if (amount.isBlank() || amount.any { it in specialKeys }) return "0"
    val value = amount.toBigDecimalOrNull()?.stripTrailingZeros() ?: return "0"
    val plain = value.toPlainString()
    val integer = plain.substringBefore('.')
    val sign = if (integer.startsWith('-')) "-" else ""
    val grouped = sign + integer.removePrefix("-").reversed().chunked(3).joinToString(",").reversed()
    return if ('.' in plain) "$grouped.${plain.substringAfter('.')}" else grouped
}

// Calculate dynamic font size based on amount length
fun calculateDynamicFontSizeForAnimatedCounterText(amount: String): Int {
    // Remove commas and spaces for length calculation
    val cleanAmount = amount.replace(",", "").replace(" ", "")
    val length = cleanAmount.length

    return when {
        length <= 4 -> 50
        length <= 6 -> 45
        length <= 8 -> 38
        length <= 10 -> 32
        length <= 12 -> 26
        length <= 15 -> 22
        length <= 18 -> 18
        else -> 14
    }
}
