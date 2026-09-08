package com.ritesh.cashiro.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }

// A single bounded transform for the remaining explicit shared-element flows.
object SharedTransitionTransforms {
    val smooth: BoundsTransform = BoundsTransform { _, _ -> tween(220) }
    val snappy: BoundsTransform = BoundsTransform { _, _ -> tween(160) }
    val quick: BoundsTransform = BoundsTransform { _, _ -> tween(120) }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedBounds(
    key: Any,
    boundsTransform: BoundsTransform = SharedTransitionTransforms.smooth
) =
    with(LocalSharedTransitionScope.current) {
        val animatedContentScope = LocalAnimatedContentScope.current
        return@with if (this != null && animatedContentScope != null) {
            sharedBounds(
                rememberSharedContentState(key),
                animatedContentScope,
                boundsTransform = boundsTransform
            ).skipToLookaheadSize()
        } else this@sharedBounds
    }

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElement(
    key: Any,
    boundsTransform: BoundsTransform = SharedTransitionTransforms.smooth
) =
    with(LocalSharedTransitionScope.current) {
        val animatedContentScope = LocalAnimatedContentScope.current
        return@with if (this != null && animatedContentScope != null) {
            sharedElement(
                rememberSharedContentState(key),
                animatedContentScope,
                boundsTransform = boundsTransform
            )
        } else this@sharedElement
    }
