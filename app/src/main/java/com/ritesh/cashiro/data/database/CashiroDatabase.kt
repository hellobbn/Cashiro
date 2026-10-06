package com.ritesh.cashiro.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritesh.cashiro.data.database.converter.Converters
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.CardDao
import com.ritesh.cashiro.data.database.dao.CategoryDao

import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.dao.SubcategoryDao
import com.ritesh.cashiro.data.database.dao.SubscriptionDao
import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CategoryEntity

import com.ritesh.cashiro.data.database.entity.ExchangeRateEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity

/**
 * The Cashiro Room database.
 *
 * This database stores all financial transaction data locally on the device.
 *
 * @property version Current database version. Increment this when making schema changes.
 * @property entities List of all entities (tables) in the database.
 * @property exportSchema Set to true in production to export schema for version control.
 * @property autoMigrations List of automatic migrations between versions.
 */
@Database(
    entities =
        [
            TransactionEntity::class,
            SubscriptionEntity::class,
            CategoryEntity::class,
            AccountBalanceEntity::class,
            CardEntity::class,
            ExchangeRateEntity::class,
            SubcategoryEntity::class,
            BudgetEntity::class,
            BudgetCategoryLimitEntity::class,
            com.ritesh.cashiro.data.database.entity.LendBorrowPersonEntity::class,
            com.ritesh.cashiro.data.database.entity.LendBorrowTransactionEntity::class,
            com.ritesh.cashiro.data.database.entity.QuickTemplateEntity::class,
            com.ritesh.cashiro.data.database.entity.AccountEntity::class,
            com.ritesh.cashiro.data.database.entity.AccountCurrencyEntity::class
        ],
        version = 69,
    exportSchema = true,
    autoMigrations =
        [
            AutoMigration(from = 27, to = 28),
            AutoMigration(from = 28, to = 29),
            AutoMigration(from = 29, to = 30, spec = Migration29To30::class),
            AutoMigration(from = 30, to = 31),
            AutoMigration(from = 31, to = 32, spec = Migration31To32::class),
            AutoMigration(from = 32, to = 33),
            AutoMigration(from = 33, to = 34),
            AutoMigration(from = 34, to = 35, spec = Migration34To35::class),
            AutoMigration(from = 35, to = 36),
            AutoMigration(from = 36, to = 37),
            AutoMigration(from = 37, to = 38),
            AutoMigration(from = 38, to = 39),
            AutoMigration(from = 39, to = 40),
            AutoMigration(from = 40, to = 41, spec = Migration40To41::class),
            AutoMigration(from = 41, to = 42),
            AutoMigration(from = 42, to = 43),
            AutoMigration(from = 43, to = 44, spec = Migration43To44::class),
            AutoMigration(from = 44, to = 45, spec = Migration44To45::class),
            AutoMigration(from = 45, to = 46, spec = Migration45To46::class),
            AutoMigration(from = 46, to = 47, spec = Migration46To47::class),
            AutoMigration(from = 47, to = 48)
        ]
)
@TypeConverters(Converters::class)
abstract class CashiroDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountBalanceDao(): AccountBalanceDao
    abstract fun accountDao(): com.ritesh.cashiro.data.database.dao.AccountDao
    abstract fun cardDao(): CardDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun lendBorrowDao(): com.ritesh.cashiro.data.database.dao.LendBorrowDao
    abstract fun quickTemplateDao(): com.ritesh.cashiro.data.database.dao.QuickTemplateDao

    companion object {
        const val DATABASE_NAME = "pennywise_database"

        /** Every manual migration, oldest first; the automatic ones are declared above. See Migrations.kt. */
        val MIGRATIONS = arrayOf(
            MIGRATION_12_14,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_48_49,
            MIGRATION_49_50,
            MIGRATION_50_51,
            MIGRATION_51_52,
            MIGRATION_52_53,
            MIGRATION_53_54,
            MIGRATION_54_55,
            MIGRATION_55_56,
            MIGRATION_56_57,
            MIGRATION_57_58,
            MIGRATION_58_59,
            MIGRATION_59_60,
            MIGRATION_60_61,
            MIGRATION_61_62,
            MIGRATION_62_63,
            MIGRATION_63_64,
            MIGRATION_64_65,
            MIGRATION_65_66,
            MIGRATION_66_67,
            MIGRATION_67_68,
            MIGRATION_68_69,
        )
    }
}
