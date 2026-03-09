package com.example.accountkeeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction

@Database(entities = [Transaction::class, Category::class, Asset::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assetDao(): AssetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove AUTOINCREMENT by re-creating the table
                database.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `date` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                database.execSQL("INSERT INTO `transactions_new` (`id`, `type`, `amount`, `date`, `categoryId`, `note`, `source`) SELECT `id`, `type`, `amount`, `date`, `categoryId`, `note`, `source` FROM `transactions`")
                database.execSQL("DROP TABLE `transactions`")
                database.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create assets table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `assets` (
                        `id` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `targetPerson` TEXT NOT NULL,
                        `targetAccount` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_categoryId` ON `assets` (`categoryId`)")
                
                // Add ASSET type to categories
                database.execSQL("UPDATE categories SET type = 'ASSET' WHERE type = 'ASSET'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isPositiveAsset column to categories table (default true = 1)
                database.execSQL("ALTER TABLE `categories` ADD COLUMN `isPositiveAsset` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add attachments column to assets table
                database.execSQL("ALTER TABLE `assets` ADD COLUMN `attachments` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
