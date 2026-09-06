package com.septaalfauzan.saku.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.septaalfauzan.saku.di.AndroidContextHolder

actual fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(
        context = AndroidContextHolder.applicationContext,
        name = DATABASE_NAME,
    )
