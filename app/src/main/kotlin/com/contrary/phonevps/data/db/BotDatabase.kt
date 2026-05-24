package com.contrary.phonevps.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.contrary.phonevps.data.model.BotConfig
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel

class Converters {
    @TypeConverter
    fun fromLogLevel(level: LogLevel): String = level.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel = LogLevel.valueOf(value)
}

@Database(
    entities = [BotConfig::class, LogEntry::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BotDatabase : RoomDatabase() {
    abstract fun botDao(): BotDao
    abstract fun logDao(): LogDao
}
