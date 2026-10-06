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
                .allowMainThreadQueries()
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
            .addMigrations(*CashiroDatabase.MIGRATIONS).allowMainThreadQueries().build()
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
