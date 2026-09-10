package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class KeywordType { EXPENSE, INCOME, MERCHANT }

@Serializable
data class ParserKeyword(
    val packageName: String,
    val keywordType: KeywordType,
    val keyword: String,
)
