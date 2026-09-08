package com.ritesh.cashiro.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

/** One short, seekable directional transition; NavHost owns predictive-back progress/cancellation. */
object CashiroTransitions {
    private const val DURATION_MS = 220

    val horizontalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION_MS))
    }
    val horizontalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION_MS)) { it / 4 }
    }
    val horizontalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION_MS)) { it / 4 }
    }
    val horizontalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION_MS))
    }

    // Keep route call sites compatible, with a single consistent spatial model instead of
    // mixing full-height slides, bouncy zooms, and alpha compositing for ordinary screens.
    val verticalSlideEnter = horizontalSlideEnter
    val verticalSlideExit = horizontalSlideExit
    val verticalSlidePopEnter = horizontalSlidePopEnter
    val verticalSlidePopExit = horizontalSlidePopExit
    val fabScaleEnter = horizontalSlideEnter
    val fabScaleExit = horizontalSlideExit
    val fabScalePopEnter = horizontalSlidePopEnter
    val fabScalePopExit = horizontalSlidePopExit
    val scaleEnter = horizontalSlideEnter
    val scaleExit = horizontalSlideExit

    val noneEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    val noneExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
}

@Serializable object AppLock

@Serializable object OnBoarding

@Serializable object Home

@Serializable
data class Transactions(
    val category: String? = null,
    val merchant: String? = null,
    val period: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val focusSearch: Boolean = false
)


@Serializable object Settings
@Serializable object CurrencySettings
@Serializable object Subscriptions
@Serializable object Categories

@Serializable object Analytics


@Serializable data class TransactionDetail(val transactionId: Long, val sharedElementKey: String? = null)

@Serializable data class AddTransaction(val initialTab: Int = 0, val subscriptionId: Long? = null, val type: String? = null)

@Serializable data class AccountDetail(val bankName: String, val accountLast4: String)


@Serializable object Faq

@Serializable object Rules

@Serializable data class CreateRule(val ruleId: String? = null)

@Serializable object Appearance

@Serializable object Investments

@Serializable object ManageAccounts

@Serializable object Profile

@Serializable data class Contacts(
    val personId: Long? = null,
    val sharedElementKey: String? = null
)


@Serializable object DataPrivacy
@Serializable object CloudBackup


@Serializable object NotificationSettings
@Serializable object Webhooks
@Serializable data class WebhookEditor(val profileId: String? = null)

@Serializable data class Budgets(val sharedElementPrefix: Long? = null)

@Serializable data class BudgetDetail(
    val budgetId: Long, 
    val sharedElementKey: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable data class BudgetHistory(val budgetId: Long)

@Serializable object DeveloperOptions

@Serializable object AddAccount



@Serializable object About

@Serializable object Licenses

@Serializable data class LendBorrow(val filter: String? = null)

@Serializable data class PersonDetail(
    val personId: Long,
    val sharedElementKey: String? = null
)

// Routes where bottom navigation should be visible
val BOTTOM_NAV_ROUTES = setOf(
    Home::class.qualifiedName,
    Analytics::class.qualifiedName,
    Transactions::class.qualifiedName
)

@Serializable data class AccountCategoryRoute(val category: String)
