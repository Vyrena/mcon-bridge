package com.vyrena.mconbridge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GameEntryEntity::class], version = 1, exportSchema = true)
@TypeConverters(RoomConverters::class)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        fun create(context: Context): BridgeDatabase = Room.databaseBuilder(
            context,
            BridgeDatabase::class.java,
            "mcon-bridge.db",
        ).build()
    }
}
