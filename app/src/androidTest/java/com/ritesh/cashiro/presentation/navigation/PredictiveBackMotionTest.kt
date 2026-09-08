package com.ritesh.cashiro.presentation.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PredictiveBackMotionTest {
    @get:Rule val rule = createComposeRule()
    @Test fun cancelThenCommitLtr() = exercise(LayoutDirection.Ltr, BackEventCompat.EDGE_LEFT)
    @Test fun cancelThenCommitRtl() = exercise(LayoutDirection.Rtl, BackEventCompat.EDGE_RIGHT)

    private fun exercise(direction: LayoutDirection, edge: Int) {
        lateinit var activity: ComponentActivity
        lateinit var nav: NavHostController
        rule.setContent {
            activity = LocalActivity.current as ComponentActivity
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                CashiroTheme(dynamicColor = false) {
                    nav = rememberNavController()
                    NavHost(
                        nav, startDestination = "root",
                        enterTransition = CashiroTransitions.horizontalSlideEnter,
                        exitTransition = CashiroTransitions.horizontalSlideExit,
                        popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                        popExitTransition = CashiroTransitions.horizontalSlidePopExit
                    ) {
                        composable("root") { Text("Root") }
                        composable("detail") { Text("Detail") }
                    }
                }
            }
        }
        rule.runOnIdle { nav.navigate("detail") }
        rule.waitForIdle()
        fun preview() {
            rule.runOnIdle {
                activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, edge))
                activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(120f, 300f, 0.6f, edge))
            }
            rule.waitForIdle()
        }
        preview()
        rule.runOnIdle {
            assertEquals("detail", nav.currentDestination?.route)
            activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals("detail", nav.currentDestination?.route) }
        preview()
        rule.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals("root", nav.currentDestination?.route) }
    }
}
