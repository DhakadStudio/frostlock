package com.focusflow.app.di

import android.content.Context
import androidx.room.Room
import com.focusflow.app.data.local.datastore.SettingsDataStore
import com.focusflow.app.data.local.db.AppDatabase
import com.focusflow.app.data.local.db.ProfileDao
import com.focusflow.app.data.local.db.SessionDao
import com.focusflow.app.data.local.db.StreakDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "focusflow_db"
        ).addMigrations(AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStreakDao(database: AppDatabase): StreakDao {
        return database.streakDao()
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}
