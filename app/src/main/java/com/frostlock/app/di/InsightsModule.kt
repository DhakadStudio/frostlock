package com.frostlock.app.di

import com.frostlock.app.insights.Clock
import com.frostlock.app.insights.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InsightsModule {

    @Binds
    @Singleton
    abstract fun bindClock(clock: SystemClock): Clock
}
