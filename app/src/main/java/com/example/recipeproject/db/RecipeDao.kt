package com.example.recipeproject.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity): Long

    // ✅ 엑셀 import(이름 unique 기준 자동 매칭)
    @Upsert
    suspend fun upsertAll(items: List<RecipeEntity>)

    @Query("SELECT * FROM recipes WHERE chosung = :chosung ORDER BY name ASC")
    fun observeByChosung(chosung: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecipeEntity?

    // ✅ export 전체 추출
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    suspend fun getAll(): List<RecipeEntity>

    // ✅ 메모 분리 저장
    @Query("UPDATE recipes SET iceMemo = :memo WHERE id = :id")
    suspend fun updateIceMemo(id: Long, memo: String)

    @Query("UPDATE recipes SET hotMemo = :memo WHERE id = :id")
    suspend fun updateHotMemo(id: Long, memo: String)

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recipes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}