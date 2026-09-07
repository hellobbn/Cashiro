package com.ritesh.cashiro.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.icons.FavoriteChart
import com.ritesh.cashiro.presentation.ui.icons.Home
import com.ritesh.cashiro.presentation.ui.icons.ReceiptItem
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import kotlin.reflect.KClass
import com.ritesh.cashiro.presentation.navigation.Home as HomeDestination
import com.ritesh.cashiro.presentation.navigation.Analytics as AnalyticsDestination
import com.ritesh.cashiro.presentation.navigation.Transactions as TransactionsDestination

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val destination: Any,
    val destinationType: KClass<*>
) {
    data object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.home_title,
        icon = Iconax.Home,
        destination = HomeDestination,
        destinationType = HomeDestination::class
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        titleRes = R.string.analytics,
        icon = Iconax.FavoriteChart,
        destination = AnalyticsDestination,
        destinationType = AnalyticsDestination::class
    )

    data object Transactions : BottomNavItem(
        route = "transactions",
        titleRes = R.string.transactions,
        icon = Iconax.ReceiptItem,
        destination = TransactionsDestination(),
        destinationType = TransactionsDestination::class
    )
}