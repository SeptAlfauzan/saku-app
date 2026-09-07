package com.septaalfauzan.saku.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
)
