package com.ritesh.cashiro.data.preferences

enum class HomeWidget(val titleRes: Int, val defaultOrder: Int) {
    NETWORTH_SUMMARY(com.ritesh.cashiro.R.string.home_widget_net_worth, 0),
    LOANS(com.ritesh.cashiro.R.string.home_widget_loans, 1),
    // Shown under the net worth from its button; kept so saved layouts still parse
    ACCOUNT_CAROUSEL(com.ritesh.cashiro.R.string.home_widget_accounts, 2),
    UPCOMING_SUBSCRIPTIONS(com.ritesh.cashiro.R.string.home_widget_subscriptions, 3),
    RECENT_TRANSACTIONS(com.ritesh.cashiro.R.string.home_widget_recent, 4),
    BUDGET_CAROUSEL(com.ritesh.cashiro.R.string.home_widget_budgets, 5),
    TRANSACTION_HEATMAP(com.ritesh.cashiro.R.string.home_widget_heatmap, 6);
    
    companion object {
        fun fromName(name: String): HomeWidget? {
            return entries.find { it.name == name }
        }
    }
}
