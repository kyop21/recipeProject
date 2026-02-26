package com.example.recipeproject.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecipeEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 → v2 : memo 컬럼 추가
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
            }
        }

        // v2 → v3 : iceMemo/hotMemo 추가 + name 인덱스 unique로 교체
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN iceMemo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recipes ADD COLUMN hotMemo TEXT NOT NULL DEFAULT ''")

                // 기존 index_recipes_name 이 non-unique로 남아있을 수 있어서 교체
                db.execSQL("DROP INDEX IF EXISTS index_recipes_name")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recipes_name ON recipes(name)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}