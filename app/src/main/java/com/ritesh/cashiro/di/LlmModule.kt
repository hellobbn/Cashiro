package com.ritesh.cashiro.di

import com.ritesh.cashiro.data.service.NoOpLlmService
import com.ritesh.cashiro.domain.service.LlmService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {
    @Binds
    @Singleton
    abstract fun bindLlmService(impl: NoOpLlmService): LlmService
}
