package com.septaalfauzan.saku.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val type: String,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    type = TransactionType.valueOf(type),
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    type = type.name,
)
