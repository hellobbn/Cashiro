package com.ritesh.cashiro.presentation.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.NavigationBarStyle
import dev.chrisbanes.haze.HazeState

/** Kept for API compatibility; the standard bar has no attached FAB. */
data class FabConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit = {},
    val dropdownContent: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null
)

/**
 * Standard Material 3 [NavigationBar] with default colours and indicator.
 *
 * The previous floating toolbar (translucent, blurred, expanding toggle buttons) is gone: it
 * changed size and shape on selection and its tap handling was unreliable during transitions.
 * [navigationBarStyle], [hidePill], [blurEffects], [hazeState] and [fabConfig] are accepted so
 * call sites need not change, but only the standard bar is rendered.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun CashiroBottomNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    currentDestination: NavDestination?,
    navigationBarStyle: NavigationBarStyle,
    hideLabels: Boolean,
    hidePill: Boolean,
    blurEffects: Boolean,
    visible: Boolean,
    hazeState: HazeState = remember { HazeState() },
    fabConfig: FabConfig? = null
) {
    val navigationItems = listOf(BottomNavItem.Home, BottomNavItem.Analytics, BottomNavItem.Transactions)
    val view = LocalView.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        NavigationBar {
            navigationItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route?.contains(item.destinationType.qualifiedName ?: "") == true
                } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        navController.selectMainTab(item)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.titleRes)
                        )
                    },
                    label = if (hideLabels) null else {
                        { Text(text = stringResource(item.titleRes), maxLines = 1) }
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}

private fun NavHostController.selectMainTab(item: BottomNavItem) {
    // Keep an already selected root page; contextual routes still reset below.
    if (isCurrentMainTabRoot(item)) return
    MainTabTiming.request(item.route)
    val startDestId = graph.findStartDestination().id
    if (item.destination == Home) {
        popBackStack(Home, inclusive = false, saveState = true)
    } else if (isCurrentMainTab(item)) {
        safeNavigate(item.destination) {
            popUpTo(item.destinationType) { inclusive = true }
            launchSingleTop = true
        }
    } else {
        safeNavigate(item.destination) {
            popUpTo(startDestId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/** Do not confuse a filtered/search transaction route with the default main tab. */
private fun NavHostController.isCurrentMainTabRoot(item: BottomNavItem): Boolean {
    val entry = currentBackStackEntry ?: return false
    val routeName = item.destinationType.qualifiedName ?: return false
    if (entry.destination.hierarchy.none { it.route?.contains(routeName) == true }) return false
    return when (item) {
        BottomNavItem.Transactions -> runCatching {
            entry.toRoute<Transactions>() == Transactions()
        }.getOrDefault(false)
        else -> true
    }
}

/** Read live navigation state: a rapid tap may arrive before the selected UI recomposes. */
private fun NavHostController.isCurrentMainTab(item: BottomNavItem): Boolean {
    val name = item.destinationType.qualifiedName ?: return false
    return currentDestination?.hierarchy?.any { it.route?.contains(name) == true } == true
}
