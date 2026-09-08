package com.ritesh.cashiro.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.ritesh.cashiro.presentation.ui.theme.MotionDurations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

// Centralized transition definitions
object CashiroTransitions {
    
    // Horizontal slide transitions for sub-screens
    val horizontalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    val horizontalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 4 },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    val horizontalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    val horizontalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    // Vertical slide transitions
    val verticalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    val verticalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it / 4},
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard))
    }

    val verticalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard))
    }

    val verticalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    // FAB to screen scale transitions
    val fabScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
            )
    }
    
    val fabScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleOut(
                targetScale = 1.1f,
                animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
            )
    }
    
    val fabScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleIn(
                initialScale = 1.1f,
                animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
            )
    }
    
    val fabScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing)
            )
    }
    
    // Scale transitions for detail screens
    val scaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleIn(animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing))
    }
    
    val scaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard)) +
            scaleOut(animationSpec = tween(durationMillis = MotionDurations.standard, easing = FastOutSlowInEasing))
    }
    
    // None transitions - for screens using shared element transitions entirely
    val noneEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
    
    val noneExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(durationMillis = MotionDurations.standard))
    }
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

@Serializable data class AddAccount(val category: String? = null)



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
