package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CustomTitleTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    extraInfoCard: @Composable () -> Unit = {},
    hazeState: HazeState = remember { HazeState() },
    blurEffects: Boolean = LocalBlurEffects.current,
    showTitleInLargeBar: Boolean = true
) {
    // collapsedFraction changes every scroll frame. Passing it as a lambda keeps the read in
    // the draw phase, so scrolling repaints the bar instead of recomposing this whole subtree.
    val hasLargeBar = scrollBehaviorLarge != scrollBehaviorSmall
    val collapsedFraction = { scrollBehaviorLarge.state.collapsedFraction }
    val smallBarFraction = if (hasLargeBar) collapsedFraction else ({ 1f })
    val isCollapsed by remember(scrollBehaviorLarge, hasLargeBar) {
        derivedStateOf { !hasLargeBar || scrollBehaviorLarge.state.collapsedFraction > 0.01f }
    }

    // LargeTopAppBar
    if (hasLargeBar) {
        LargerTopAppBar(
            scrollBehaviorLarge = scrollBehaviorLarge,
            title = title,
            hasBackButton = hasBackButton,
            collapsedFraction = collapsedFraction,
            actionContent = actionContent,
            navigationContent = navigationContent,
            extraInfoCard = extraInfoCard,
            hazeState = hazeState,
            blurEffects = blurEffects,
            themeColors = MaterialTheme.colorScheme,
            showTitleInLargeBar = showTitleInLargeBar
        )
    }

    // Regular TopAppBar
    RegularTopAppBar(
        scrollBehaviorSmall = scrollBehaviorSmall,
        title = title,
        hasBackButton = hasBackButton,
        hasActionButton = hasActionButton,
        actionContent = actionContent,
        navigationContent = navigationContent,
        collapsedFraction = smallBarFraction,
        isCollapsed = isCollapsed,
        modifier = modifier,
        hazeState = hazeState,
        blurEffects = blurEffects,
        showTitle = showTitleInLargeBar
    )

}


@Composable
private fun Modifier.titleOffsetModifier(
    hasBackButton: Boolean,
    hasActionButton: Boolean = false,
    isHomeScreen: Boolean = false,
): Modifier {
    // Define the target offset based on conditions
    val targetOffsetX = when {
        hasBackButton && hasActionButton-> 0.dp
        isHomeScreen-> (0).dp
        hasBackButton -> (-26).dp
        else -> (-10).dp
    }

    // Convert to pixels for animation
    val density = LocalDensity.current
    val targetOffsetXPx = with(density) { targetOffsetX.toPx() }

    // Apply offset directly as a float value instead of rounding to Int
    return this
        .fillMaxWidth()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                // Use the exact float value for positioning
                placeable.placeRelative(x = targetOffsetXPx.toInt(), y = 0)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun LargerTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    collapsedFraction: () -> Float,
    extraInfoCard: @Composable () -> Unit = {},
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    hazeState: HazeState,
    blurEffects: Boolean = false,
    themeColors: ColorScheme,
    showTitleInLargeBar: Boolean = true
    ){
    LargeTopAppBar(
        title = {
            TitleForLargeTopAppBar(
                title = title,
                modifier = modifier ,
                extraInfoCard = extraInfoCard,
                showTitle = showTitleInLargeBar
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =  Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        navigationIcon = {
            NavigationForLargeTopAppBar(
                hasBackButton = hasBackButton,
                navigationContent = navigationContent,
                isHomeScreen = title == "Cashiro"
            )
        },
        actions = {
            ActionForLargeTopAppBar(
                actionContent = actionContent,
                isHomeScreen = title == "Cashiro"
            )
        },
        collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight,
        expandedHeight = if (title == "Cashiro") 150.dp else 110.dp,
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehaviorLarge,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        inputScale = HazeInputScale.Auto
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = tint(backgroundColor),
                            blurRadius = 10.dp,
                            noiseFactor = -1f,
                        )
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                ) else Modifier
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.background,
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .graphicsLayer { alpha = 1f - collapsedFraction() }
    )
}

@Composable
private fun TitleForLargeTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    extraInfoCard: @Composable () -> Unit = {},
    showTitle: Boolean = true
){
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        BlurredAnimatedVisibility(
            visible = title != "Cashiro" && showTitle,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp)
            )
        }
        extraInfoCard()
    }
}

@Composable
private fun NavigationForLargeTopAppBar(
    hasBackButton: Boolean = false,
    isHomeScreen: Boolean = false,
    navigationContent: @Composable () -> Unit = {},
){
    BlurredAnimatedVisibility(
        visible = hasBackButton && !isHomeScreen,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        navigationContent()
    }
}

@Composable
private fun ActionForLargeTopAppBar(
    actionContent: @Composable () -> Unit = {},
    isHomeScreen: Boolean = false,
){
    BlurredAnimatedVisibility(
        visible = !isHomeScreen,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        actionContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun RegularTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    collapsedFraction: () -> Float,
    isCollapsed: Boolean,
    hazeState: HazeState,
    blurEffects: Boolean = false,
    showTitle: Boolean = true
){
    BlurredAnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val isHomeScreen = title == "Cashiro"

        TopAppBar(
            title = {
                BlurredAnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.titleOffsetModifier(
                            hasBackButton = hasBackButton,
                            hasActionButton = hasActionButton,
                            isHomeScreen = title == "Cashiro",
                        )
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            navigationIcon = {
                BlurredAnimatedVisibility(
                    visible = hasBackButton || isHomeScreen,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    navigationContent()
                }
            },
            actions = {
                actionContent()
            },
            scrollBehavior = scrollBehaviorSmall,
            windowInsets = WindowInsets(0.dp),
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            inputScale = HazeInputScale.Auto
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = 10.dp,
                                noiseFactor = -1f,
                            )
                            progressive =
                                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                        }
                    ) else Modifier
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .graphicsLayer { alpha = collapsedFraction() }
        )
    }
}

