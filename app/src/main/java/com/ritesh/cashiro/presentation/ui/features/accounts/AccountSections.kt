package com.ritesh.cashiro.presentation.ui.features.accounts

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import java.math.BigDecimal

internal enum class AccountSectionKind { WALLETS, BANKS, CREDIT_CARDS, INVESTMENTS }

internal data class AccountSection(
    val kind: AccountSectionKind,
    val accounts: List<AccountBalanceEntity>,
    // Never add balances in different currencies or mix assets with card debt.
    val totals: Map<String, BigDecimal>
) {
    val collapsedPreviewCount: Int
        get() = 0

    fun visibleAccounts(expanded: Boolean): List<AccountBalanceEntity> =
        if (expanded) accounts else accounts.take(collapsedPreviewCount)

    fun showFooterToggle(expanded: Boolean): Boolean =
        expanded && accounts.isNotEmpty()
}

internal data class AccountSections(
    val visible: List<AccountSection>,
    val hidden: List<AccountBalanceEntity>
)

internal fun AccountBalanceEntity.listKey(): String =
    "account:${bankName.length}:$bankName:$accountLast4"

internal fun buildAccountSections(
    accounts: List<AccountBalanceEntity>,
    hiddenKeys: Set<String>
): AccountSections {
    val (hidden, visible) = accounts.partition {
        "${it.bankName}_${it.accountLast4}" in hiddenKeys
    }
    return AccountSections(
        visible = AccountSectionKind.entries.map { kind ->
            val members = visible.filter {
                when (kind) {
                    AccountSectionKind.WALLETS -> it.isWallet && !it.isCreditCard
                    AccountSectionKind.BANKS -> !it.isWallet && !it.isCreditCard && it.category() != AccountCategory.INVESTMENTS
                    AccountSectionKind.CREDIT_CARDS -> it.isCreditCard
                    AccountSectionKind.INVESTMENTS -> it.category() == AccountCategory.INVESTMENTS
                }
            }
            AccountSection(kind, members, members.groupBy { it.currency }.toSortedMap().mapValues { (_, group) ->
                group.fold(BigDecimal.ZERO) { total, account -> total + account.balance }
            })
        },
        hidden = hidden
    )
}
