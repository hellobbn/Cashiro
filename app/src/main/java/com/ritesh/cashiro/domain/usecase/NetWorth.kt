package com.ritesh.cashiro.domain.usecase

import android.content.Context
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.dao.PocketBalance
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.utils.sumOfBigDecimal
import java.math.BigDecimal

private const val ACCOUNT_PREFS = "account_prefs"
private const val HIDDEN_ACCOUNTS_KEY = "hidden_accounts"

/** Identity the account list uses to remember which accounts the user hid. */
fun AccountBalanceEntity.hiddenAccountKey(): String = "${bankName}_$accountLast4"

fun Context.hiddenAccountKeys(): Set<String> =
    getSharedPreferences(ACCOUNT_PREFS, Context.MODE_PRIVATE)
        .getStringSet(HIDDEN_ACCOUNTS_KEY, emptySet())
        .orEmpty()

fun List<AccountBalanceEntity>.excludingHidden(hiddenKeys: Set<String>): List<AccountBalanceEntity> =
    if (hiddenKeys.isEmpty()) this else filterNot { it.hiddenAccountKey() in hiddenKeys }

/**
 * Converts every balance into [targetCurrency] and returns assets minus credit-card debt.
 *
 * Home and Profile have to show the same number, so both go through here instead of
 * summing balances themselves. Callers are responsible for dropping hidden accounts
 * first with [excludingHidden].
 */
suspend fun List<AccountBalanceEntity>.netWorthIn(
    targetCurrency: String,
    conversionService: CurrencyConversionService,
    investmentSnapshots: Map<String, BigDecimal> = emptyMap(),
    // Every currency of every account; an account found here counts each of its currencies
    pockets: List<PocketBalance> = emptyList()
): BigDecimal {
    val (creditCards, assets) = withPockets(pockets).partition { it.isCreditCard }
    return assets.convertedTotal(targetCurrency, conversionService) -
        creditCards.convertedTotal(targetCurrency, conversionService) +
        investmentSnapshots.convertedTotal(targetCurrency, conversionService)
}

/** Each account once per currency it holds (see [PocketBalance]); accounts not in [pockets] as they are. */
fun List<AccountBalanceEntity>.withPockets(pockets: List<PocketBalance>): List<AccountBalanceEntity> {
    if (pockets.isEmpty()) return this
    val byAccount = pockets.groupBy { it.accountId }
    return flatMap { account ->
        account.accountId?.let { byAccount[it] }?.map { account.copy(balance = it.balance, currency = it.currency) }
            ?: listOf(account)
    }
}

/** What a card owes across all its currencies, in its main currency. */
suspend fun AccountBalanceEntity.owedAcrossCurrencies(
    pockets: List<PocketBalance>,
    conversionService: CurrencyConversionService
): BigDecimal {
    val own = pockets.filter { it.accountId == accountId }.takeIf { it.isNotEmpty() } ?: return balance
    var total = BigDecimal.ZERO
    for (pocket in own) total += conversionService.convertAmount(pocket.balance, pocket.currency, currency)
    return total
}

/** Total of [AccountBalanceEntity.balance] expressed in [targetCurrency]. */
suspend fun List<AccountBalanceEntity>.convertedTotal(
    targetCurrency: String,
    conversionService: CurrencyConversionService
): BigDecimal {
    if (none { it.currency != targetCurrency }) {
        return sumOfBigDecimal { it.balance }
    }
    var total = BigDecimal.ZERO
    for (account in this) {
        total += if (account.currency == targetCurrency) {
            account.balance
        } else {
            conversionService.convertAmount(
                amount = account.balance,
                fromCurrency = account.currency,
                toCurrency = targetCurrency
            )
        }
    }
    return total
}

private suspend fun Map<String, BigDecimal>.convertedTotal(
    targetCurrency: String,
    conversionService: CurrencyConversionService
): BigDecimal {
    if (isEmpty()) return BigDecimal.ZERO
    var total = BigDecimal.ZERO
    for ((currency, amount) in this) {
        total += if (currency == targetCurrency) {
            amount
        } else {
            conversionService.convertAmount(
                amount = amount,
                fromCurrency = currency,
                toCurrency = targetCurrency
            )
        }
    }
    return total
}
