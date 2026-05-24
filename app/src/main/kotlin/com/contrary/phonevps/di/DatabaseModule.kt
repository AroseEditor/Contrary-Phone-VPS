package com.contrary.phonevps.di

import android.content.Context
import androidx.room.Room
import com.contrary.phonevps.data.db.BotDatabase
import com.contrary.phonevps.data.db.BotDao
import com.contrary.phonevps.data.db.LogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BotDatabase =
        Room.databaseBuilder(
            context,
            BotDatabase::class.java,
            "contrary_vps.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBotDao(db: BotDatabase): BotDao = db.botDao()

    @Provides
    fun provideLogDao(db: BotDatabase): LogDao = db.logDao()
}
