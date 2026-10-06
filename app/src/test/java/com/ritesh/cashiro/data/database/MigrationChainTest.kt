package com.ritesh.cashiro.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every exported schema, built empty and opened at the current version through the app's
 * migrations: Room fails the open when a step is missing or leaves a table unlike its entity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationChainTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val schemas = File("schemas/com.ritesh.cashiro.data.database.CashiroDatabase")

    private fun create(name: String, version: Int) {
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(File(schemas, "$version.json").readText())
            .jsonObject["database"]!!.jsonObject
        val statements = buildList {
            schema["entities"]!!.jsonArray.forEach { entity ->
                val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
                add(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity.jsonObject["indices"]?.jsonArray?.forEach {
                    add(it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["views"]?.jsonArray?.forEach {
                val view = it.jsonObject["viewName"]!!.jsonPrimitive.content
                add("CREATE VIEW `$view` AS " + it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${VIEW_NAME}", view))
            }
            schema["setupQueries"]!!.jsonArray.forEach { add(it.jsonPrimitive.content) }
        }
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }
            ).build()
        ).apply { writableDatabase; close() }
    }

    @Test fun everyExportedSchemaMigratesToTheCurrentVersion() {
        val versions = schemas.listFiles()!!.mapNotNull { it.nameWithoutExtension.toIntOrNull() }.sorted()
        val current = versions.last()
        assertTrue("schemas from 27 on are exported", versions.first() <= 27)
        val failures = versions.dropLast(1).mapNotNull { version ->
            val name = "chain-$version.db"
            create(name, version)
            val db = Room.databaseBuilder(context, CashiroDatabase::class.java, name)
                .addMigrations(*CashiroDatabase.MIGRATIONS)
                .addCallback(SyncTriggers.Callback).allowMainThreadQueries()
                .build()
            try {
                assertEquals(current, db.openHelper.writableDatabase.version)
                null
            } catch (e: Throwable) {
                "$version: ${e.message?.lineSequence()?.firstOrNull()}"
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
        assertEquals("migrations that failed", emptyList<String>(), failures)
    }

    @Test fun version71GivesEveryRowASyncIdAndCapturesLaterChanges() {
        val name = "sync.db"
        create(name, 70)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(70) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }
            ).build()
        )
        helper.writableDatabase.apply {
            (1..3).forEach {
                execSQL(
                    "INSERT INTO categories (name, color, is_system, is_income, display_order, created_at, updated_at) " +
                        "VALUES ('C$it', '#000000', 0, 0, $it, '2026-09-01T09:00', '2026-09-01T09:00')"
                )
            }
            execSQL(
                "INSERT INTO lend_borrow_persons (name, color, created_at, updated_at) " +
                    "VALUES ('Alex', '#000000', '2026-09-01T09:00', '2026-09-01T09:00')"
            )
        }
        helper.close()
        val before = System.currentTimeMillis()
        val db = Room.databaseBuilder(context, CashiroDatabase::class.java, name)
            .addMigrations(*CashiroDatabase.MIGRATIONS).addCallback(SyncTriggers.Callback).allowMainThreadQueries().build()
        try {
            val sql = db.openHelper.writableDatabase
            fun rows(query: String) = sql.query(query).use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) }
            }
            val categories = rows("SELECT sync_id, sync_updated_at FROM categories ORDER BY id")
            val people = rows("SELECT sync_id, sync_updated_at FROM lend_borrow_persons")
            assertEquals(listOf("C1", "C2", "C3"), rows("SELECT name, id FROM categories ORDER BY id").map { it.first })
            val all = categories + people
            assertEquals(4, all.size)
            assertTrue(all.all { (id, at) -> id.length == 32 && at >= before - 60_000 })
            assertEquals(4, all.map { it.first }.toSet().size)
            // Rows that were there are not queued; the first sync uploads everything anyway
            assertEquals(0, rows("SELECT COUNT(*), 0 FROM sync_outbox").single().first.toInt())
            assertEquals(
                SyncTriggers.TABLES.size * 3,
                rows("SELECT COUNT(*), 0 FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_v%'").single().first.toInt()
            )

            sql.execSQL("UPDATE categories SET color = '#FFFFFF' WHERE name = 'C1'")
            val queued = rows("SELECT sync_id, queued_at FROM sync_outbox WHERE table_name = 'categories' AND op = 'UPSERT'")
            assertEquals(listOf(categories.first().first), queued.map { it.first })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun aSubscriptionsNotesSurviveTheRenameOfTheirColumn() {
        val name = "notes.db"
        create(name, 69)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(69) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }
            ).build()
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO subscriptions (merchant_name, amount, state, sms_body, created_at, updated_at, currency) " +
                "VALUES ('Music', '15', 'ACTIVE', 'family plan', '2026-09-01T09:00', '2026-09-01T09:00', 'CNY')"
        )
        helper.close()
        val db = Room.databaseBuilder(context, CashiroDatabase::class.java, name)
            .addMigrations(*CashiroDatabase.MIGRATIONS).addCallback(SyncTriggers.Callback).allowMainThreadQueries().build()
        try {
            val notes = db.openHelper.writableDatabase.query("SELECT notes FROM subscriptions").use { c ->
                c.moveToFirst(); c.getString(0)
            }
            assertEquals("family plan", notes)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
