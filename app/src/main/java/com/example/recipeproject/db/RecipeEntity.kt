package com.example.recipeproject.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["chosung"]),
        Index(value = ["name"], unique = true) // ✅ 이름 기준 매칭(업서트/REPLACE) 안정화
    ]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val name: String,
    val chosung: String,

    // ICE
    val iceSteps: String = "",
    val iceToppings: String = "",
    @ColumnInfo(defaultValue = "") val iceMemo: String = "",

    // HOT
    val hotSteps: String = "",
    val hotToppings: String = "",
    @ColumnInfo(defaultValue = "") val hotMemo: String = "",

    // ✅ 기존 DB에 남아있던 컬럼(호환용). 앱에서는 안 써도 됨
    @ColumnInfo(defaultValue = "") val memo: String = "",

    val createdAt: Long = System.currentTimeMillis()
)