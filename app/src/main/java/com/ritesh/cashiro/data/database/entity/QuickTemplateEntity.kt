package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * A user-defined quick-add template: one tap on the Add Transaction screen pre-fills the form
 * with these values. [amount] is only applied when [prefillAmount] is true.
 */
@Entity(tableName = "quick_templates")
data class QuickTemplateEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "merchant_name") val merchantName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "subcategory") val subcategory: String? = null,
    @ColumnInfo(name = "transaction_type") val transactionType: TransactionType = TransactionType.EXPENSE,
    @ColumnInfo(name = "amount") val amount: BigDecimal? = null,
    @ColumnInfo(name = "prefill_amount", defaultValue = "0") val prefillAmount: Boolean = false,
    @ColumnInfo(name = "bank_name") val bankName: String? = null,
    @ColumnInfo(name = "account_last4") val accountLast4: String? = null,
    @ColumnInfo(name = "currency") val currency: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
