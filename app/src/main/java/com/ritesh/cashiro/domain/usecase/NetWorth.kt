package com.ritesh.cashiro.domain.usecase

import android.content.Context
import com.ritesh.cashiro.data.currency.CurrencyConversionService
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
    allowNetwork: Boolean = true
): BigDecimal {
    val (creditCards, assets) = partition { it.isCreditCard }
    return assets.convertedTotal(targetCurrency, conversionService, allowNetwork) -
        creditCards.convertedTotal(targetCurrency, conversionService, allowNetwork) +
        investmentSnapshots.convertedTotal(targetCurrency, conversionService, allowNetwork)
}

/** Total of [AccountBalanceEntity.balance] expressed in [targetCurrency]. */
suspend fun List<AccountBalanceEntity>.convertedTotal(
    targetCurrency: String,
    conversionService: CurrencyConversionService,
    allowNetwork: Boolean = true
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
                toCurrency = targetCurrency,
                allowNetwork = allowNetwork
            )
        }
    }
    return total
}

private suspend fun Map<String, BigDecimal>.convertedTotal(
    targetCurrency: String,
    conversionService: CurrencyConversionService,
    allowNetwork: Boolean = true
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
                toCurrency = targetCurrency,
                allowNetwork = allowNetwork
            )
        }
    }
    return total
}
