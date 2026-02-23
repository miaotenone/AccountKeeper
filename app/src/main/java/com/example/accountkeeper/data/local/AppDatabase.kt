package com.example.accountkeeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction

@Database(entities = [Transaction::class, Category::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

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
    }
}
