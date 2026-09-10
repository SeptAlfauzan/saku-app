package com.septaalfauzan.saku.data.entity

import androidx.room3.Entity
import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.ParserKeyword

@Entity(
    tableName = "parser_keywords",
    primaryKeys = ["packageName", "keywordType", "keyword"],
)
data class ParserKeywordEntity(
    val packageName: String,
    val keywordType: String,
    val keyword: String,
)

fun ParserKeywordEntity.toDomain(): ParserKeyword = ParserKeyword(
    packageName = packageName,
    keywordType = KeywordType.valueOf(keywordType),
    keyword = keyword,
)

fun ParserKeyword.toEntity(): ParserKeywordEntity = ParserKeywordEntity(
    packageName = packageName,
    keywordType = keywordType.name,
    keyword = keyword,
)
