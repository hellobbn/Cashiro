package com.ritesh.cashiro.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ritesh.cashiro.data.ai.AiConfig
import com.ritesh.cashiro.data.ai.AiProtocol
import com.ritesh.cashiro.data.ai.AiSettings
import com.ritesh.cashiro.data.brokerage.BrokerageRepository
import com.ritesh.cashiro.data.brokerage.BrokerageStore
import com.ritesh.cashiro.data.brokerage.SavedBrokerConnection
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.LendBorrowPersonEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.icons.MerchantIconStore
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Merging a backup (or a sync snapshot) again adds nothing: no copies of transactions, balance rows or people. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupMergeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = LocalDateTime.of(2026, 9, 1, 9, 0)
    private val databases = mutableListOf<CashiroDatabase>()

    private class MemoryStore : BrokerageStore {
        override fun read() = emptyList<SavedBrokerConnection>()
        override fun write(connections: List<SavedBrokerConnection>) = Unit
    }

    private fun database() = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java)
        .allowMainThreadQueries().build().also { databases += it }

    @After fun tearDown() = databases.forEach { it.close() }

    private val prefs = UserPreferencesRepository(context)
    private val brokerage = BrokerageRepository(MemoryStore(), emptySet()) { 0L }
    private val icons = MerchantIconStore(context)
    private val ai = AiSettings(context)

    private fun exporter(db: CashiroDatabase, settings: AiSettings = ai) = BackupExporter(context, db, prefs, brokerage, icons, settings)
    private fun importer(db: CashiroDatabase, settings: AiSettings = ai) = BackupImporter(context, db, prefs, brokerage, icons, settings)

    private suspend fun CashiroDatabase.fill() {
        accountBalanceDao().insertBalance(
            AccountBalanceEntity(bankName = "Bank", accountLast4 = "1234", balance = BigDecimal("1000"),
                timestamp = start, sourceType = "MANUAL", currency = "CNY")
        )
        (1..3).forEach { i ->
            val at = start.plusDays(i.toLong())
            val id = transactionDao().insertTransaction(
                TransactionEntity(amount = BigDecimal("10"), merchantName = "Shop $i", category = "Shopping",
                    transactionType = TransactionType.EXPENSE, dateTime = at, bankName = "Bank", accountNumber = "1234",
                    transactionHash = "hash-$i", currency = "CNY")
            )
            accountBalanceDao().insertBalance(
                AccountBalanceEntity(bankName = "Bank", accountLast4 = "1234", balance = BigDecimal(1000 - 10 * i),
                    timestamp = at, transactionId = id, sourceType = "TRANSACTION", currency = "CNY")
            )
        }
        lendBorrowDao().insertPerson(LendBorrowPersonEntity(name = "Alex"))
        accountBalanceDao().linkTransactionsToAccounts()
    }

    private suspend fun CashiroDatabase.counts() = listOf(
        transactionDao().getAllTransactions().first().size,
        accountBalanceDao().getAllBalances().first().size,
        accountDao().getAccounts().size,
        lendBorrowDao().getAllPersons().first().size
    )

    private suspend fun backupOf(db: CashiroDatabase, config: BackupConfiguration = BackupConfiguration()): Uri {
        val result = exporter(db).exportBackup(config)
        assertTrue("export failed: $result", result is ExportResult.Success)
        return Uri.fromFile((result as ExportResult.Success).file)
    }

    @Test fun mergingTheSameBackupTwiceAddsNothingTheSecondTime() = runTest {
        val source = database().apply { fill() }
        val backup = backupOf(source)
        val target = database()

        assertTrue(importer(target).importBackup(backup, ImportStrategy.MERGE) is ImportResult.Success)
        val once = target.counts()
        assertEquals(source.counts(), once)

        assertTrue(importer(target).importBackup(backup, ImportStrategy.MERGE) is ImportResult.Success)
        assertEquals(once, target.counts())
    }

    @Test fun mergingADevicesOwnSnapshotChangesNothing() = runTest {
        val db = database().apply { fill() }
        val before = db.counts()
        val latest = db.accountBalanceDao().getAllBalances().first().maxBy { it.timestamp }.balance
        assertTrue(importer(db).importBackup(backupOf(db), ImportStrategy.MERGE) is ImportResult.Success)
        assertEquals(before, db.counts())
        assertEquals(latest, db.accountBalanceDao().getAllBalances().first().maxBy { it.timestamp }.balance)
    }

    private fun entries(uri: Uri): Set<String> = java.util.zip.ZipInputStream(java.io.File(uri.path!!).inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.map { it.name }.toSet()
    }

    @Test fun theAiKeyIsInABackupOnlyWhenAskedFor() = runTest {
        ai.save(AiConfig(AiProtocol.ANTHROPIC, AiProtocol.ANTHROPIC.defaultBaseUrl, "model", "sk-test"))
        val db = database()
        assertTrue(BackupExporter.AI_ENTRY !in entries(backupOf(db)))
        assertTrue(BackupExporter.AI_ENTRY in entries(backupOf(db, BackupConfiguration(includeAiKey = true))))
    }

    @Test fun aRestoredKeyFillsAnEmptySettingButNeverReplacesOne() = runTest {
        ai.save(AiConfig(AiProtocol.OPENAI_COMPATIBLE, "https://example.com/v1", "model", "sk-backup"))
        val backup = backupOf(database(), BackupConfiguration(includeAiKey = true))

        val empty = AiSettings(context).apply { save(AiConfig()) }
        importer(database(), empty).importBackup(backup, ImportStrategy.MERGE)
        assertEquals("sk-backup", empty.config.value.apiKey)
        assertEquals(AiProtocol.OPENAI_COMPATIBLE, empty.config.value.protocol)

        val own = AiSettings(context).apply { save(AiConfig(apiKey = "sk-mine")) }
        importer(database(), own).importBackup(backup, ImportStrategy.MERGE)
        assertEquals("sk-mine", own.config.value.apiKey)
    }
}
