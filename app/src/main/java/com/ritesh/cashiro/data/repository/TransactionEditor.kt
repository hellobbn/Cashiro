package com.ritesh.cashiro.data.repository

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves an edited transaction and moves its effect on account balances with it.
 *
 * The old effect is taken back the way deleting the transaction does, and the new one applied
 * the way adding it does, so a changed amount, type, date, account or transfer target all land
 * right, credit cards included (their balance is what is owed). Editing used to adjust balances
 * by a delta that assumed an ordinary account, which ran the wrong way on cards.
 */
@Singleton
class TransactionEditor @Inject constructor(private val database: CashiroDatabase) {

    /**
     * Replaces [original] with [updated]. [newTarget] is the account a transfer now goes to, when
     * the user picked one; otherwise a transfer keeps its current target.
     */
    suspend fun update(original: TransactionEntity, updated: TransactionEntity, newTarget: AccountBalanceEntity? = null) {
        database.withTransaction {
            val balances = database.accountBalanceDao()
            val linked = balances.getBalancesForTransaction(original.id)
            val oldSource = linked.firstOrNull { it.bankName == original.bankName && it.accountLast4 == original.accountNumber }
            val oldTarget = linked.firstOrNull { it !== oldSource }
            val accountChanged = original.bankName != updated.bankName || original.accountNumber != updated.accountNumber

            if (linked.isNotEmpty() && !original.isDeleted) {
                // Recalculates every affected account as if the transaction were gone; its rows
                // then pass the running balance through and can be removed without a trace
                balances.changeTransactionDeletion(listOf(original.id), deleted = true)
            }
            linked.forEach { balances.deleteBalanceById(it.id) }
            database.transactionDao().updateTransaction(updated)

            // A transaction that never moved a balance only starts to once it is given an account
            if (updated.isDeleted || (linked.isEmpty() && !accountChanged)) return@withTransaction
            val bankName = updated.bankName ?: return@withTransaction
            val accountLast4 = updated.accountNumber ?: return@withTransaction
            balances.getLatestBalance(bankName, accountLast4) ?: return@withTransaction

            when (updated.transactionType) {
                TransactionType.BALANCE_UPDATE -> Unit
                TransactionType.TRANSFER -> {
                    val target = newTarget?.let { it.bankName to it.accountLast4 }
                        ?: oldTarget?.let { it.bankName to it.accountLast4 }?.takeIf { it.second == updated.toAccount }
                        ?: return@withTransaction
                    balances.insertTransactionBalance(
                        bankName = bankName, accountLast4 = accountLast4, amount = updated.amount,
                        transactionType = TransactionType.EXPENSE, explicitBalance = null,
                        timestamp = updated.dateTime, transactionId = updated.id, creditLimit = null,
                        isCreditCard = false, smsSource = null, currency = updated.currency
                    )
                    balances.insertTransactionBalance(
                        bankName = target.first, accountLast4 = target.second, amount = updated.toAmount ?: updated.amount,
                        transactionType = TransactionType.INCOME, explicitBalance = null,
                        timestamp = updated.dateTime, transactionId = updated.id, creditLimit = null,
                        isCreditCard = false, smsSource = null, currency = updated.currency
                    )
                }
                else -> {
                    // A balance the bank reported with the transaction still holds if only details changed
                    val reported = oldSource?.takeIf {
                        it.sourceType == SOURCE_TRANSACTION_SMS_BALANCE && !accountChanged &&
                            original.amount.compareTo(updated.amount) == 0 &&
                            original.transactionType == updated.transactionType && original.dateTime == updated.dateTime
                    }?.balance
                    balances.insertTransactionBalance(
                        bankName = bankName, accountLast4 = accountLast4, amount = updated.amount,
                        transactionType = updated.transactionType, explicitBalance = reported,
                        timestamp = updated.dateTime, transactionId = updated.id, creditLimit = null,
                        isCreditCard = false, smsSource = null, currency = updated.currency
                    )
                }
            }
        }
    }

    private companion object {
        const val SOURCE_TRANSACTION_SMS_BALANCE = "TRANSACTION_SMS_BALANCE"
    }
}
