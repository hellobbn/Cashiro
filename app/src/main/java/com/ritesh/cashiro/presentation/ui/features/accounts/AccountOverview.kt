package com.ritesh.cashiro.presentation.ui.features.accounts

import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.brokerage.BrokerConnection
import com.ritesh.cashiro.presentation.common.icons.InstitutionCatalog
import java.math.BigDecimal
import java.math.RoundingMode

enum class AccountCategory(val titleRes: Int) {
    WALLETS(R.string.section_wallets), BANKS(R.string.section_bank_accounts),
    CREDIT_CARDS(R.string.section_credit_cards), INVESTMENTS(R.string.overview_investments)
}

internal fun AccountBalanceEntity.category(): AccountCategory = when {
    isCreditCard -> AccountCategory.CREDIT_CARDS
    isWallet -> AccountCategory.WALLETS
    InstitutionCatalog.find(bankName)?.isBroker == true -> AccountCategory.INVESTMENTS
    else -> AccountCategory.BANKS
}

internal enum class OverviewStatus { READY, LOADING, CONNECT, UNAVAILABLE, MULTIPLE_SOURCES }
internal data class AccountOverviewItem(
    val category: AccountCategory,
    val count: Int = 0,
    val amount: BigDecimal? = null,
    val currency: String = "CNY",
    val status: OverviewStatus = OverviewStatus.LOADING,
    val isSnapshot: Boolean = false,
    val hasSnapshots: Boolean = false,
    val converted: Boolean = false
)

/** A missing rate must never relabel an unconverted amount as the target currency. */
internal suspend fun overviewTotal(
    totals: Map<String, BigDecimal>, currency: String,
    rate: suspend (String, String) -> BigDecimal?
): BigDecimal? {
    var sum = BigDecimal.ZERO
    for ((source, value) in totals) {
        val exchange = if (source == currency) BigDecimal.ONE else rate(source, currency) ?: return null
        sum += value * exchange
    }
    return sum.setScale(2, RoundingMode.HALF_UP)
}

internal suspend fun buildOverview(
    accounts: List<AccountBalanceEntity>, connections: List<BrokerConnection>, currency: String,
    brokerLoaded: Boolean, brokerFailed: Boolean,
    rate: suspend (String, String) -> BigDecimal?
): List<AccountOverviewItem> = AccountCategory.entries.map { category ->
    val members = accounts.filter { it.category() == category }
    val manualTotals = members.groupBy { it.currency }.mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { n, a -> n + a.balance } }
    if (category != AccountCategory.INVESTMENTS) {
        val amount = overviewTotal(manualTotals, currency, rate)
        AccountOverviewItem(category, members.size, amount, currency,
            if (amount == null) OverviewStatus.UNAVAILABLE else OverviewStatus.READY,
            converted = manualTotals.keys.any { it != currency })
    } else {
        val brokerAccounts = connections.flatMap { it.accounts }
        val holdings = brokerAccounts.flatMap { it.holdings }
        val snapshotTotals = holdings.groupBy { it.currency }.mapValues { (_, rows) ->
            rows.fold(BigDecimal.ZERO) { n, h -> n + (h.marketValue?.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
        }
        val status = when {
            brokerFailed -> OverviewStatus.UNAVAILABLE
            !brokerLoaded -> OverviewStatus.LOADING
            members.isNotEmpty() && connections.isNotEmpty() -> OverviewStatus.MULTIPLE_SOURCES
            members.isEmpty() && connections.isEmpty() -> OverviewStatus.CONNECT
            holdings.any { it.marketValue?.toBigDecimalOrNull() == null } -> OverviewStatus.UNAVAILABLE
            else -> OverviewStatus.READY
        }
        // Manual balances may describe the same portfolio: never add snapshots to them implicitly.
        val totals = if (connections.isNotEmpty()) snapshotTotals else manualTotals
        val amount = if (status == OverviewStatus.READY) overviewTotal(totals, currency, rate) else null
        AccountOverviewItem(category, members.size + brokerAccounts.size, amount, currency,
            if (status == OverviewStatus.READY && amount == null) OverviewStatus.UNAVAILABLE else status,
            isSnapshot = connections.isNotEmpty(), hasSnapshots = connections.isNotEmpty(),
            converted = totals.keys.any { it != currency })
    }
}

/** Snapshot portfolios are informational, not implicitly part of the existing net-worth balance. */
internal fun AccountOverviewItem.netWorthContribution(): BigDecimal? = when {
    category == AccountCategory.CREDIT_CARDS -> amount?.negate()
    isSnapshot -> null
    else -> amount
}
