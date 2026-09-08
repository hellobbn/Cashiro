package com.ritesh.cashiro.presentation.effects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Lightweight visibility feedback. The historical name is retained for existing call sites;
 * visibility changes no longer allocate blur layers, bitmaps, or RenderScript resources.
 * Explicit enter/exit transitions (for example, expanding a form) remain supported.
 */
@Composable
fun BlurredAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(tween(120)),
    exit: ExitTransition = fadeOut(tween(90)),
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = enter, exit = exit, content = content)
}
