package com.septaalfauzan.saku.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Long,
    val currency: String,
    val merchant: String?,
    val categoryId: String?,
    val description: String?,
    val source: String,
    val sourcePackage: String?,
    val status: String = TransactionStatus.CONFIRMED.name,
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    currency = currency,
    merchant = merchant,
    categoryId = categoryId,
    description = description,
    source = TransactionSource.valueOf(source),
    sourcePackage = sourcePackage,
    occurredAt = Instant.fromEpochMilliseconds(occurredAtMillis),
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
    updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    currency = currency,
    merchant = merchant,
    categoryId = categoryId,
    description = description,
    source = source.name,
    sourcePackage = sourcePackage,
    status = TransactionStatus.CONFIRMED.name,
    occurredAtMillis = occurredAt.toEpochMilliseconds(),
    createdAtMillis = createdAt.toEpochMilliseconds(),
    updatedAtMillis = updatedAt.toEpochMilliseconds(),
)
