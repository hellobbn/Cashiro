package com.ritesh.cashiro.di

import com.ritesh.cashiro.data.brokerage.*
import com.ritesh.cashiro.domain.brokerage.BrokerageProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BrokerageModule {
    @Binds abstract fun store(impl: EncryptedBrokerageStore): BrokerageStore
    @Binds @IntoSet abstract fun ibkr(impl: IbkrFlexProvider): BrokerageProvider
}
