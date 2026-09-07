package com.septaalfauzan.saku.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.septaalfauzan.saku.domain.model.NotificationSource

@Entity(tableName = "notification_sources")
data class NotificationSourceEntity(
    @PrimaryKey val packageName: String,
    val providerId: String,
    val enabled: Boolean,
)

fun NotificationSourceEntity.toDomain(): NotificationSource = NotificationSource(
    packageName = packageName,
    providerId = providerId,
    enabled = enabled,
)

fun NotificationSource.toEntity(): NotificationSourceEntity = NotificationSourceEntity(
    packageName = packageName,
    providerId = providerId,
    enabled = enabled,
)
