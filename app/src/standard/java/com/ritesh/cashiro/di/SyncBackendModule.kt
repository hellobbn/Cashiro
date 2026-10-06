package com.ritesh.cashiro.di

import com.ritesh.cashiro.data.sync.SyncBackend
import com.ritesh.cashiro.data.sync.firebase.FirebaseSyncBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Standard flavor: sync through Firebase (docs/sync.md). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBackendModule {
    @Binds
    abstract fun bindSyncBackend(backend: FirebaseSyncBackend): SyncBackend
}
