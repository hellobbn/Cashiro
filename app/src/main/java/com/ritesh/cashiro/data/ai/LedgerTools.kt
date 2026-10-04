package com.ritesh.cashiro.data.ai

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.domain.usecase.AddTransactionUseCase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** A transaction the model wants to add, resolved against the user's accounts and categories. */
data class TransactionDraft(
    val dateTime: LocalDateTime,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val merchant: String,
    val category: String,
    val subcategory: String?,
    val account: AccountBalanceEntity?,
    val toAccount: AccountBalanceEntity?,
    val notes: String?,
    // An existing transaction with the same amount around the same day, on the same account
    val possibleDuplicate: TransactionEntity?
)

/** One change the model proposed. Nothing is written until the user approves it. */
sealed interface LedgerChange {
    data class Add(val draft: TransactionDraft) : LedgerChange
    data class Update(val before: TransactionEntity, val after: TransactionEntity) : LedgerChange
    data class Delete(val transaction: TransactionEntity, val reason: String?) : LedgerChange
}

/** What [LedgerTools.apply] did, enough to take it back. */
data class AppliedChanges(
    val addedIds: List<Long>,
    val updated: List<TransactionEntity>,
    val deleted: List<TransactionEntity>
)

/**
 * The ledger as tools for a model: one read tool and three write tools. Writes only queue a
 * [LedgerChange] for review; [apply] commits the ones the user keeps through the same paths the
 * app's own screens use, so balances stay right.
 */
class LedgerTools @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    /** The user's accounts and categories, loaded once per session. */
    class Context internal constructor(
        val accounts: List<AccountBalanceEntity>,
        val categories: List<CategoryEntity>,
        val subcategories: Map<Long, List<SubcategoryEntity>>
    ) {
        // Short stable references the model uses instead of bank names: A1, A2…
        val accountRefs: Map<String, AccountBalanceEntity> =
            accounts.mapIndexed { i, account -> "A${i + 1}" to account }.toMap()

        fun refOf(bankName: String?, last4: String?): String? =
            accountRefs.entries.firstOrNull { it.value.bankName == bankName && it.value.accountLast4 == last4 }?.key
    }

    suspend fun context(): Context = Context(
        accounts = accountBalanceRepository.getAllLatestBalances().first(),
        categories = categoryRepository.getAllCategories().first(),
        subcategories = subcategoryRepository.getAllSubcategories().first().groupBy { it.categoryId }
    )

    /** The accounts and categories as the model sees them, for the system prompt. */
    fun describe(context: Context): String = buildString {
        appendLine("Accounts (ref: name, currency):")
        context.accountRefs.forEach { (ref, a) ->
            val kind = when {
                a.isCreditCard -> ", credit card"
                a.isWallet -> ", wallet"
                else -> ""
            }
            appendLine("- $ref: ${a.bankName} ${a.accountLast4}, ${a.currency}$kind")
        }
        appendLine()
        appendLine("Categories (name: subcategories):")
        context.categories.sortedWith(compareBy({ it.isIncome }, { it.displayOrder })).forEach { c ->
            val subs = context.subcategories[c.id].orEmpty().joinToString(", ") { it.name }
            appendLine("- ${if (c.isIncome) "[income] " else ""}${c.name}${if (subs.isNotEmpty()) ": $subs" else ""}")
        }
    }

    val tools: List<AiTool> = listOf(
        AiTool(
            name = FIND,
            description = "Find the user's existing transactions between two dates (inclusive), optionally " +
                "matching text in merchant or notes, or an exact amount. Use it before adding, to skip " +
                "transactions that are already recorded, and to find the ids of transactions to change.",
            schema = schema(
                required = listOf("from_date", "to_date"),
                "from_date" to str("yyyy-MM-dd"),
                "to_date" to str("yyyy-MM-dd"),
                "text" to str("Optional text to match"),
                "amount" to num("Optional exact amount")
            )
        ),
        AiTool(
            name = ADD,
            description = "Propose new transactions for the user to review (at most 50 per call). Amounts " +
                "are positive; the type gives the direction. A transfer needs to_account.",
            schema = schema(
                required = listOf("transactions"),
                "transactions" to array(
                    schema(
                        required = listOf("date", "amount", "type", "merchant", "category"),
                        "date" to str("yyyy-MM-dd or yyyy-MM-dd HH:mm"),
                        "amount" to num("Positive amount"),
                        "currency" to str("ISO code; defaults to the account's"),
                        "type" to enumOf("EXPENSE", "INCOME", "TRANSFER"),
                        "merchant" to str("Payee or payer as the user would name it"),
                        "category" to str("Exactly one of the user's category names"),
                        "subcategory" to str("One of that category's subcategories, if any fits"),
                        "account" to str("Account ref such as A1"),
                        "to_account" to str("Transfers only: the receiving account ref"),
                        "notes" to str("Optional note")
                    )
                )
            )
        ),
        AiTool(
            name = UPDATE,
            description = "Propose changes to existing transactions: merchant, category, subcategory or " +
                "notes. To change an amount, date, type or account, delete the transaction and add a " +
                "corrected one instead.",
            schema = schema(
                required = listOf("changes"),
                "changes" to array(
                    schema(
                        required = listOf("id"),
                        "id" to int("Transaction id from find_transactions"),
                        "merchant" to str(""),
                        "category" to str(""),
                        "subcategory" to str(""),
                        "notes" to str("")
                    )
                )
            )
        ),
        AiTool(
            name = DELETE,
            description = "Propose deleting transactions. Deleted transactions go to the trash.",
            schema = schema(
                required = listOf("ids"),
                "ids" to arrayOf(int("Transaction id")),
                "reason" to str("Why, shown to the user")
            )
        )
    )

    /** Runs one tool call: reads answer at once, writes add to [queue]. Returns the tool result. */
    suspend fun run(call: AiToolCall, context: Context, queue: MutableList<LedgerChange>): AiToolResult {
        call.input[AiChat.INVALID_ARGUMENTS]?.let {
            return AiToolResult(call.id, "The arguments were not valid JSON.", isError = true)
        }
        return try {
            val text = when (call.name) {
                FIND -> find(call.input, context)
                ADD -> add(call.input, context, queue)
                UPDATE -> update(call.input, context, queue)
                DELETE -> delete(call.input, queue)
                else -> return AiToolResult(call.id, "Unknown tool ${call.name}", isError = true)
            }
            AiToolResult(call.id, text)
        } catch (e: IllegalArgumentException) {
            AiToolResult(call.id, e.message ?: "Invalid input", isError = true)
        }
    }

    private suspend fun find(input: JsonObject, context: Context): String {
        val from = date(input.string("from_date")).atStartOfDay()
        val to = date(input.string("to_date")).atTime(LocalTime.MAX)
        val text = input.string("text")?.lowercase()
        val amount = input.decimal("amount")
        val found = transactionRepository.getTransactionsBetweenDates(from, to).first()
            .asSequence()
            .filter { !it.isDeleted }
            .filter {
                text == null || it.merchantName.lowercase().contains(text) ||
                    it.description.orEmpty().lowercase().contains(text)
            }
            .filter { amount == null || it.amount.compareTo(amount) == 0 }
            .sortedBy { it.dateTime }
            .toList()
        val shown = found.take(MAX_FOUND)
        return buildJsonObject {
            put("count", found.size)
            if (found.size > shown.size) put("note", "Only the first $MAX_FOUND are listed; narrow the range.")
            putJsonArray("transactions") {
                shown.forEach { t ->
                    addJsonObject {
                        put("id", t.id)
                        put("date", t.dateTime.format(DATE_TIME))
                        put("amount", t.amount.toPlainString())
                        put("currency", t.currency)
                        put("type", t.transactionType.name)
                        put("merchant", t.merchantName)
                        put("category", t.category)
                        t.subcategory?.let { put("subcategory", it) }
                        context.refOf(t.bankName, t.accountNumber)?.let { put("account", it) }
                        t.description?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
                    }
                }
            }
        }.toString()
    }

    private suspend fun add(input: JsonObject, context: Context, queue: MutableList<LedgerChange>): String {
        val items = input["transactions"] as? JsonArray ?: throw IllegalArgumentException("transactions is required")
        val errors = mutableListOf<String>()
        var queued = 0
        items.forEachIndexed { index, element ->
            try {
                queue += LedgerChange.Add(draft(element.jsonObject, context))
                queued++
            } catch (e: IllegalArgumentException) {
                errors += "#$index: ${e.message}"
            }
        }
        return result(queued, errors)
    }

    private suspend fun draft(item: JsonObject, context: Context): TransactionDraft {
        val dateText = item.string("date") ?: throw IllegalArgumentException("date is required")
        val dateTime = dateTime(dateText)
        val amount = item.decimal("amount")?.abs()?.takeIf { it.signum() > 0 }
            ?: throw IllegalArgumentException("amount must be a positive number")
        val type = item.string("type")?.let { name -> ALLOWED_TYPES.firstOrNull { it.name == name } }
            ?: throw IllegalArgumentException("type must be EXPENSE, INCOME or TRANSFER")
        val account = item.string("account")?.let {
            context.accountRefs[it] ?: throw IllegalArgumentException("unknown account $it")
        }
        val toAccount = item.string("to_account")?.let {
            context.accountRefs[it] ?: throw IllegalArgumentException("unknown account $it")
        }
        if (type == TransactionType.TRANSFER && (account == null || toAccount == null)) {
            throw IllegalArgumentException("a transfer needs account and to_account")
        }
        val (category, subcategory) = category(item.string("category"), item.string("subcategory"), context)
        val possibleDuplicate = transactionRepository
            .findPotentialDuplicates(amount, dateTime.minusDays(1), dateTime.plusDays(1))
            .firstOrNull { existing ->
                !existing.isDeleted && (account == null || existing.bankName == account.bankName &&
                    existing.accountNumber == account.accountLast4)
            }
        return TransactionDraft(
            dateTime = dateTime,
            amount = amount,
            currency = item.string("currency")?.uppercase() ?: account?.currency ?: DEFAULT_CURRENCY,
            type = type,
            merchant = item.string("merchant").orEmpty().trim(),
            category = category,
            subcategory = subcategory,
            account = account,
            toAccount = toAccount,
            notes = item.string("notes")?.trim()?.takeIf { it.isNotEmpty() },
            possibleDuplicate = possibleDuplicate
        )
    }

    private suspend fun update(input: JsonObject, context: Context, queue: MutableList<LedgerChange>): String {
        val items = input["changes"] as? JsonArray ?: throw IllegalArgumentException("changes is required")
        val errors = mutableListOf<String>()
        var queued = 0
        items.forEachIndexed { index, element ->
            val item = element.jsonObject
            try {
                val id = item.long("id") ?: throw IllegalArgumentException("id is required")
                val before = transactionRepository.getTransactionById(id)?.takeIf { !it.isDeleted }
                    ?: throw IllegalArgumentException("no transaction $id")
                val (category, subcategory) = if (item.string("category") != null || item.string("subcategory") != null) {
                    category(item.string("category") ?: before.category, item.string("subcategory"), context)
                } else before.category to before.subcategory
                val after = before.copy(
                    merchantName = item.string("merchant")?.trim() ?: before.merchantName,
                    category = category,
                    subcategory = subcategory,
                    description = item.string("notes") ?: before.description,
                    updatedAt = LocalDateTime.now()
                )
                if (after.copy(updatedAt = before.updatedAt) != before) {
                    queue.removeAll { it is LedgerChange.Update && it.before.id == id }
                    queue += LedgerChange.Update(before, after)
                    queued++
                }
            } catch (e: IllegalArgumentException) {
                errors += "#$index: ${e.message}"
            }
        }
        return result(queued, errors)
    }

    private suspend fun delete(input: JsonObject, queue: MutableList<LedgerChange>): String {
        val ids = (input["ids"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }
            ?: throw IllegalArgumentException("ids is required")
        val reason = input.string("reason")
        val errors = mutableListOf<String>()
        var queued = 0
        ids.forEach { id ->
            val transaction = transactionRepository.getTransactionById(id)?.takeIf { !it.isDeleted }
            if (transaction == null) {
                errors += "no transaction $id"
            } else if (queue.none { it is LedgerChange.Delete && it.transaction.id == id }) {
                queue += LedgerChange.Delete(transaction, reason)
                queued++
            }
        }
        return result(queued, errors)
    }

    /** Commits approved changes. */
    suspend fun apply(changes: List<LedgerChange>): AppliedChanges {
        val added = mutableListOf<Long>()
        val updated = mutableListOf<TransactionEntity>()
        val deleted = mutableListOf<TransactionEntity>()
        changes.forEach { change ->
            when (change) {
                is LedgerChange.Add -> with(change.draft) {
                    added += addTransactionUseCase.execute(
                        amount = amount,
                        merchant = merchant,
                        category = category,
                        subcategory = subcategory,
                        type = type,
                        date = dateTime,
                        notes = notes,
                        bankName = account?.bankName,
                        accountLast4 = account?.accountLast4,
                        currency = currency,
                        sourceAccountId = account?.id,
                        targetAccountBankName = toAccount?.bankName,
                        targetAccountLast4 = toAccount?.accountLast4,
                        createSubscription = false
                    )
                }
                is LedgerChange.Update -> {
                    // Only descriptive fields change, so balances are untouched
                    transactionRepository.updateTransaction(change.after)
                    updated += change.before
                }
                is LedgerChange.Delete -> {
                    transactionRepository.deleteTransaction(change.transaction)
                    deleted += change.transaction
                }
            }
        }
        return AppliedChanges(added, updated, deleted)
    }

    /** Takes back what [apply] did. */
    suspend fun undo(applied: AppliedChanges) {
        applied.addedIds.forEach { transactionRepository.deleteTransactionById(it, hardDelete = true) }
        applied.updated.forEach { transactionRepository.updateTransaction(it) }
        if (applied.deleted.isNotEmpty()) transactionRepository.undoDeleteTransactions(applied.deleted)
    }

    private fun category(name: String?, subcategory: String?, context: Context): Pair<String, String?> {
        val category = context.categories.firstOrNull { it.name == name }
            ?: context.categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: throw IllegalArgumentException("unknown category $name; use one of the listed names")
        val sub = subcategory?.takeIf { it.isNotBlank() }?.let { wanted ->
            context.subcategories[category.id].orEmpty().firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.name
                ?: throw IllegalArgumentException("unknown subcategory $wanted in ${category.name}")
        }
        return category.name to sub
    }

    private fun result(queued: Int, errors: List<String>) = buildString {
        append("Queued $queued for the user's review.")
        if (errors.isNotEmpty()) append(" Rejected: ").append(errors.joinToString("; ")).append(". Fix and retry those.")
    }

    companion object {
        const val FIND = "find_transactions"
        const val ADD = "add_transactions"
        const val UPDATE = "update_transactions"
        const val DELETE = "delete_transactions"
        private const val MAX_FOUND = 200
        private const val DEFAULT_CURRENCY = "CNY"
        private val ALLOWED_TYPES = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
        private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        private fun date(text: String?): LocalDate = try {
            LocalDate.parse(text?.take(10))
        } catch (e: Exception) {
            throw IllegalArgumentException("dates are yyyy-MM-dd, got $text")
        }

        internal fun dateTime(text: String): LocalDateTime {
            val day = date(text)
            val time = text.drop(10).trim().removePrefix("T").takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalTime.parse(it.take(5)) }.getOrNull() }
            // Statements often give only a day: keep it at noon so time zones never move it to another day
            return day.atTime(time ?: LocalTime.NOON)
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

        private fun JsonObject.long(key: String): Long? =
            (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

        private fun JsonObject.decimal(key: String): BigDecimal? =
            (this[key] as? JsonPrimitive)?.contentOrNull?.replace(",", "")?.toBigDecimalOrNull()

        private fun str(description: String) = buildJsonObject {
            put("type", "string")
            if (description.isNotEmpty()) put("description", description)
        }

        private fun num(description: String) = buildJsonObject {
            put("type", "number")
            put("description", description)
        }

        private fun int(description: String) = buildJsonObject {
            put("type", "integer")
            put("description", description)
        }

        private fun enumOf(vararg values: String) = buildJsonObject {
            put("type", "string")
            put("enum", JsonArray(values.map { JsonPrimitive(it) }))
        }

        private fun array(items: JsonObject) = arrayOf(items)
        private fun arrayOf(items: JsonObject) = buildJsonObject {
            put("type", "array")
            put("items", items)
        }

        private fun schema(required: List<String>, vararg properties: Pair<String, JsonElement>) = buildJsonObject {
            put("type", "object")
            put("properties", JsonObject(properties.toMap()))
            put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        }
    }
}
