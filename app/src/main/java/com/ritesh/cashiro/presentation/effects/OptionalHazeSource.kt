package com.ritesh.cashiro.presentation.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Do not capture scrollable content into a graphics layer when backdrop blur is disabled. */
@Composable
fun Modifier.optionalHazeSource(
    state: HazeState,
    enabled: Boolean = LocalBlurEffects.current,
): Modifier = if (enabled) hazeSource(state = state) else this
