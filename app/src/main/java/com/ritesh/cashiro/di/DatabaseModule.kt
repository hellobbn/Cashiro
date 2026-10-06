package com.ritesh.cashiro.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.DatabaseCallback
import com.ritesh.cashiro.data.database.SyncTriggers
import com.ritesh.cashiro.data.database.dao.SyncDao

import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.CardDao
import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.dao.SubcategoryDao
import com.ritesh.cashiro.data.database.dao.SubscriptionDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ritesh.cashiro.R
import com.ritesh.cashiro.utils.IconResolutionUtils

/** Hilt module that provides database-related dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of CashiroDatabase.
     *
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun provideCashiroDatabase(@ApplicationContext context: Context): CashiroDatabase {
        val database =
            Room.databaseBuilder(
                context,
                CashiroDatabase::class.java,
                CashiroDatabase.DATABASE_NAME
            )
                // Add manual migrations here when needed
                .addMigrations(*CashiroDatabase.MIGRATIONS)

                // Enable auto-migrations
                // Room will automatically detect schema changes between versions

                // Sync change capture first, so the seeded rows are captured too (docs/sync.md)
                .addCallback(SyncTriggers.Callback)
                // Add callback to seed default data on first creation
                .addCallback(DatabaseCallback(context))
                .build()

        return database
    }

    /**
     * Provides the TransactionDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return TransactionDao for accessing transaction data
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: CashiroDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: CashiroDatabase): com.ritesh.cashiro.data.database.dao.AccountDao =
        database.accountDao()

    /**
     * Provides the SubscriptionDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return SubscriptionDao for accessing subscription data
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: CashiroDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    /**
     * Provides the CategoryDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return CategoryDao for accessing category data
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: CashiroDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides the AccountBalanceDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return AccountBalanceDao for accessing account balance data
     */
    @Provides
    @Singleton
    fun provideAccountBalanceDao(database: CashiroDatabase): AccountBalanceDao {
        return database.accountBalanceDao()
    }

    /**
     * Provides the CardDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return CardDao for accessing card data
     */
    @Provides
    @Singleton
    fun provideCardDao(database: CashiroDatabase): CardDao {
        return database.cardDao()
    }

    /**
     * Provides the ExchangeRateDao from the database.
     *
     * @param database The CashiroDatabase instance
     * @return ExchangeRateDao for accessing exchange rate data
     */
    @Provides
    @Singleton
    fun provideExchangeRateDao(database: CashiroDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }

    @Provides
    @Singleton
    fun provideSubcategoryDao(database: CashiroDatabase): SubcategoryDao {
        return database.subcategoryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: CashiroDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideLendBorrowDao(database: CashiroDatabase): com.ritesh.cashiro.data.database.dao.LendBorrowDao {
        return database.lendBorrowDao()
    }

    @Provides
    @Singleton
    fun provideQuickTemplateDao(database: CashiroDatabase): com.ritesh.cashiro.data.database.dao.QuickTemplateDao {
        return database.quickTemplateDao()
    }

    @Provides
    @Singleton
    fun provideSyncDao(database: CashiroDatabase): SyncDao = database.syncDao()
}
