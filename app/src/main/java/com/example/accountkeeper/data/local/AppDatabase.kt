package com.example.accountkeeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetMonth
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction

@Database(entities = [Transaction::class, Category::class, Asset::class, Budget::class, BudgetMonth::class, BudgetApprovalRequest::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assetDao(): AssetDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetMonthDao(): BudgetMonthDao
    abstract fun budgetApprovalDao(): BudgetApprovalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `date` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("INSERT INTO `transactions_new` (`id`, `type`, `amount`, `date`, `categoryId`, `note`, `source`) SELECT `id`, `type`, `amount`, `date`, `categoryId`, `note`, `source` FROM `transactions`")
                database.execSQL("DROP TABLE `transactions`")
                database.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `assets` (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `categoryId` INTEGER, `targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, `note` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_categoryId` ON `assets` (`categoryId`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE `categories` ADD COLUMN `isPositiveAsset` INTEGER NOT NULL DEFAULT 1") } }
        val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE `assets` ADD COLUMN `attachments` TEXT NOT NULL DEFAULT ''") } }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `asset_types` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                val now = System.currentTimeMillis()
                database.execSQL("INSERT OR IGNORE INTO `asset_types` (`id`, `name`, `createdAt`, `updatedAt`) VALUES (1, '实物资产', $now, $now)")
                database.execSQL("INSERT OR IGNORE INTO `asset_types` (`id`, `name`, `createdAt`, `updatedAt`) VALUES (2, '虚拟资产', $now, $now)")
                database.execSQL("ALTER TABLE `assets` ADD COLUMN `assetTypeId` INTEGER NOT NULL DEFAULT 2")
                database.execSQL("CREATE TABLE `assets_new` (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `categoryId` INTEGER, `assetTypeId` INTEGER NOT NULL, `targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, `note` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `attachments` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`assetTypeId`) REFERENCES `asset_types`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("INSERT INTO `assets_new` (`id`, `date`, `amount`, `status`, `categoryId`, `assetTypeId`, `targetPerson`, `targetAccount`, `note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt`) SELECT `id`, `date`, `amount`, `status`, `categoryId`, `assetTypeId`, `targetPerson`, `targetAccount`, `note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt` FROM `assets`")
                database.execSQL("DROP TABLE `assets`")
                database.execSQL("ALTER TABLE `assets_new` RENAME TO `assets`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_categoryId` ON `assets` (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_assetTypeId` ON `assets` (`assetTypeId`)")
                database.execSQL("CREATE TABLE `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `monthKey` TEXT NOT NULL, `categoryId` INTEGER, `amount` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_monthKey_categoryId` ON `budgets` (`monthKey`, `categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_budgets_monthKey_categoryId`")
                database.execSQL("DELETE FROM `budgets` WHERE `categoryId` IS NOT NULL AND `id` NOT IN (SELECT MIN(`id`) FROM `budgets` WHERE `categoryId` IS NOT NULL GROUP BY `monthKey`, `categoryId`)")
                database.execSQL("DELETE FROM `budgets` WHERE `categoryId` IS NULL AND `id` NOT IN (SELECT MIN(`id`) FROM `budgets` WHERE `categoryId` IS NULL GROUP BY `monthKey`)")
                database.execSQL("CREATE UNIQUE INDEX `index_budgets_monthKey_categoryId` ON `budgets` (`monthKey`, `categoryId`)")
                database.execSQL("CREATE UNIQUE INDEX `index_budgets_monthKey_total` ON `budgets` (`monthKey`) WHERE `categoryId` IS NULL")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `budget_months` (`monthKey` TEXT NOT NULL, `initializedAt` INTEGER NOT NULL, PRIMARY KEY(`monthKey`))")
                database.execSQL("INSERT OR IGNORE INTO `budget_months` (`monthKey`, `initializedAt`) SELECT DISTINCT `monthKey`, MIN(`createdAt`) FROM `budgets` GROUP BY `monthKey`")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_budgets_monthKey_categoryId`")
                database.execSQL("DROP INDEX IF EXISTS `index_budgets_monthKey_total`")
                database.execSQL("CREATE TABLE `budgets_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `monthKey` TEXT NOT NULL, `categoryId` INTEGER, `amount` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("INSERT INTO `budgets_new` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) SELECT `id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt` FROM `budgets` WHERE `categoryId` IS NULL OR `categoryId` IN (SELECT `id` FROM `categories`)")
                database.execSQL("DROP TABLE `budgets`")
                database.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
                database.execSQL("CREATE UNIQUE INDEX `index_budgets_monthKey_categoryId` ON `budgets` (`monthKey`, `categoryId`)")
                database.execSQL("CREATE INDEX `index_budgets_categoryId` ON `budgets` (`categoryId`)")
                database.execSQL("CREATE UNIQUE INDEX `index_budgets_monthKey_total` ON `budgets` (`monthKey`) WHERE `categoryId` IS NULL")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_budgets_monthKey_total`")
                database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_total_insert_guard` BEFORE INSERT ON `budgets` WHEN NEW.`categoryId` IS NULL AND EXISTS (SELECT 1 FROM `budgets` WHERE `monthKey` = NEW.`monthKey` AND `categoryId` IS NULL) BEGIN SELECT RAISE(ABORT, 'duplicate monthly total budget'); END")
                database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_total_update_guard` BEFORE UPDATE OF `monthKey`, `categoryId` ON `budgets` WHEN NEW.`categoryId` IS NULL AND EXISTS (SELECT 1 FROM `budgets` WHERE `monthKey` = NEW.`monthKey` AND `categoryId` IS NULL AND `id` != NEW.`id`) BEGIN SELECT RAISE(ABORT, 'duplicate monthly total budget'); END")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE `budgets` SET `amount` = 0 WHERE `amount` < 0")
                createBudgetValidationTriggers(database)
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `budgets` ADD COLUMN `periodType` TEXT NOT NULL DEFAULT 'MONTHLY'")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `assets_new` (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `categoryId` INTEGER, `targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, `note` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `attachments` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("INSERT INTO `assets_new` (`id`, `date`, `amount`, `status`, `categoryId`, `targetPerson`, `targetAccount`, `note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt`) SELECT `id`, `date`, `amount`, `status`, `categoryId`, `targetPerson`, `targetAccount`, `note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt` FROM `assets`")
                database.execSQL("DROP TABLE `assets`")
                database.execSQL("ALTER TABLE `assets_new` RENAME TO `assets`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_categoryId` ON `assets` (`categoryId`)")
                database.execSQL("DROP TABLE IF EXISTS `asset_types`")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `budget_approval_requests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `periodType` TEXT NOT NULL, `categoryId` INTEGER, `amount` REAL NOT NULL, `purchaseDate` INTEGER, `reason` TEXT NOT NULL, `attachments` TEXT NOT NULL, `status` TEXT NOT NULL, `decisionNote` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `decidedAt` INTEGER, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_approval_requests_status` ON `budget_approval_requests` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_approval_requests_createdAt` ON `budget_approval_requests` (`createdAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_approval_requests_monthKey_periodType` ON `budget_approval_requests` (`monthKey`, `periodType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_approval_requests_categoryId` ON `budget_approval_requests` (`categoryId`)")
            }
        }

        fun createBudgetValidationTriggers(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_amount_insert_guard` BEFORE INSERT ON `budgets` WHEN NEW.`amount` < 0 BEGIN SELECT RAISE(ABORT, 'budget amount must be non-negative'); END")
            database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_amount_update_guard` BEFORE UPDATE OF `amount` ON `budgets` WHEN NEW.`amount` < 0 BEGIN SELECT RAISE(ABORT, 'budget amount must be non-negative'); END")
            database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_expense_category_insert_guard` BEFORE INSERT ON `budgets` WHEN NEW.`categoryId` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `categories` WHERE `id` = NEW.`categoryId` AND `type` = 'EXPENSE') BEGIN SELECT RAISE(ABORT, 'budget category must be expense'); END")
            database.execSQL("CREATE TRIGGER IF NOT EXISTS `budgets_expense_category_update_guard` BEFORE UPDATE OF `categoryId` ON `budgets` WHEN NEW.`categoryId` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `categories` WHERE `id` = NEW.`categoryId` AND `type` = 'EXPENSE') BEGIN SELECT RAISE(ABORT, 'budget category must be expense'); END")
            database.execSQL("CREATE TRIGGER IF NOT EXISTS `categories_budget_type_guard` BEFORE UPDATE OF `type` ON `categories` WHEN NEW.`type` != 'EXPENSE' AND EXISTS (SELECT 1 FROM `budgets` WHERE `categoryId` = NEW.`id`) BEGIN SELECT RAISE(ABORT, 'budget category must remain expense'); END")
        }
    }
}
