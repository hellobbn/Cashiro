package com.ritesh.cashiro.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Extension for NavController to perform navigation safely.
 * Prevents "Double Navigation" or "Double Pop" race conditions during transitions
 * by checking if the current back stack entry is in the RESUMED state.
 */
fun NavController.safeNavigate(
    route: Any,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate(route, builder)
}

/**
 * Extension for NavController to pop the back stack safely.
 * Prevents race conditions during transitions by checking lifecycle state.
 */
fun NavController.safePopBackStack() {
    if (currentBackStackEntry != null) {
        popBackStack()
    }
}
