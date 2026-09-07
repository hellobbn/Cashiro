package com.ritesh.cashiro.presentation.navigation

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.toRoute
import com.ritesh.cashiro.BuildConfig
import kotlinx.coroutines.flow.first

internal fun NavBackStackEntry.mainTabTag(): String? {
    fun matches(name: String?) = name != null && destination.hierarchy.any { it.route?.contains(name) == true }
    return when {
        matches(Home::class.qualifiedName) -> "home"
        matches(Analytics::class.qualifiedName) -> "analytics"
        matches(Transactions::class.qualifiedName) && runCatching { toRoute<Transactions>() == Transactions() }.getOrDefault(false) -> "transactions"
        else -> null
    }
}

/** Peer tabs use a brief interruptible fade; contextual/detail routes keep their old motion. */
internal object MainTabMotion {
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isPeerSwitch() =
        initialState.mainTabTag() != null && targetState.mainTabTag() != null

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isPeerSwitch()) fadeIn(tween(150)) else CashiroTransitions.verticalSlideEnter(this)
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isPeerSwitch()) fadeOut(tween(150)) else CashiroTransitions.verticalSlideExit(this)
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isPeerSwitch()) fadeIn(tween(150)) else CashiroTransitions.verticalSlidePopEnter(this)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isPeerSwitch()) fadeOut(tween(150)) else CashiroTransitions.verticalSlidePopExit(this)
    }
}

/** Local debug-only timing, from accepted tab request to the target entry becoming RESUMED. */
internal object MainTabTiming {
    private data class Pending(val id: Int, val tab: String, val start: Long)
    private var sequence = 0
    private var pending: Pending? = null
    fun request(tab: String) {
        if (!BuildConfig.DEBUG) return
        pending?.let { Log.d("CashiroTabPerf", "SUPERSEDED|${it.id}|${it.tab}") }
        val p = Pending(++sequence, tab, SystemClock.elapsedRealtimeNanos())
        pending = p
        Log.d("CashiroTabPerf", "REQUEST|${p.id}|${p.tab}|${p.start}")
    }
    fun settled(tab: String) {
        if (!BuildConfig.DEBUG) return
        val p = pending ?: return
        if (p.tab != tab) return
        Log.d("CashiroTabPerf", "SETTLED|${p.id}|$tab|${(SystemClock.elapsedRealtimeNanos() - p.start) / 1_000_000.0}")
        pending = null
    }
}

@Composable
internal fun ObserveMainTabSettled(entry: NavBackStackEntry?) {
    if (!BuildConfig.DEBUG) return
    LaunchedEffect(entry?.id) {
        val current = entry ?: return@LaunchedEffect
        val tab = current.mainTabTag() ?: return@LaunchedEffect
        current.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
        MainTabTiming.settled(tab)
    }
}
