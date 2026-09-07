package com.example.accountkeeper.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.accountkeeper.data.model.AssetCategoryDefaults
import com.example.accountkeeper.data.model.AssetDirection

/** SQL seed shared by fresh installations and migrations. */
object AssetCategorySeed {
    fun insertDefaults(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        AssetDirection.entries.forEach { direction ->
            AssetCategoryDefaults.categoryNames(direction).forEach { name ->
                if (!exists(db, name, direction)) insert(db, name, direction, now)
            }
        }
        insertDefaultFields(db, now)
    }

    private fun insertDefaultFields(db: SupportSQLiteDatabase, now: Long) {
        db.query("SELECT id, name, isDefault FROM asset_categories").use { categories ->
            while (categories.moveToNext()) {
                val categoryId = categories.getLong(0)
                val name = categories.getString(1)
                val isDefault = categories.getInt(2) != 0
                if (!isDefault) continue
                AssetCategoryDefaults.fieldTemplatesFor(name, true).forEachIndexed { index, template ->
                    db.execSQL(
                        "INSERT INTO asset_field_definitions (assetCategoryId,name,type,required,options,sortOrder,createdAt,updatedAt) SELECT ?,?,?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM asset_field_definitions WHERE assetCategoryId=? AND name=?)",
                        arrayOf(categoryId, template.name, template.type.name, 0, template.options, index, now, now, categoryId, template.name)
                    )
                }
            }
        }
    }

    private fun insert(db: SupportSQLiteDatabase, name: String, direction: AssetDirection, now: Long): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("direction", direction.name)
            put("isDefault", 1)
            put("createdAt", now)
            put("updatedAt", now)
        }
        return db.insert("asset_categories", SQLiteDatabase.CONFLICT_IGNORE, values)
    }

    private fun exists(db: SupportSQLiteDatabase, name: String, direction: AssetDirection): Boolean =
        db.query(
            "SELECT 1 FROM asset_categories WHERE name = ? AND direction = ? LIMIT 1",
            arrayOf(name, direction.name)
        ).use { it.moveToFirst() }
}
