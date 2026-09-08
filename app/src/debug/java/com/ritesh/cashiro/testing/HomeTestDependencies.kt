package com.ritesh.cashiro.testing

import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only access for real Home startup tests; never included in release builds. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeTestDependencies {
    fun database(): CashiroDatabase
    fun preferences(): UserPreferencesRepository
}
