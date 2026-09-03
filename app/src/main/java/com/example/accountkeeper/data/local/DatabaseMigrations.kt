package com.example.accountkeeper.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP INDEX IF EXISTS `index_assets_assetCategoryId`")
            database.execSQL("DROP INDEX IF EXISTS `index_assets_sourceApprovalId`")
            database.execSQL("CREATE TABLE `assets_new` (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `assetCategoryId` INTEGER, `categoryId` INTEGER, `name` TEXT NOT NULL, `specification` TEXT NOT NULL, `quantity` REAL NOT NULL, `purchaseDate` INTEGER, `sourceApprovalId` INTEGER, `transactionId` INTEGER, `targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, `note` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `attachments` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `assetRootType` TEXT NOT NULL, `supplier` TEXT NOT NULL, `location` TEXT NOT NULL, `userOrDepartment` TEXT NOT NULL, `warranty` TEXT NOT NULL, `serviceStartDate` INTEGER, `serviceEndDate` INTEGER, `renewalCycle` TEXT NOT NULL, `accessUrl` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`assetCategoryId`) REFERENCES `asset_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
            database.execSQL("INSERT INTO `assets_new` (`id`,`date`,`amount`,`status`,`assetCategoryId`,`categoryId`,`name`,`specification`,`quantity`,`purchaseDate`,`sourceApprovalId`,`transactionId`,`targetPerson`,`targetAccount`,`note`,`isCompleted`,`attachments`,`createdAt`,`updatedAt`,`assetRootType`,`supplier`,`location`,`userOrDepartment`,`warranty`,`serviceStartDate`,`serviceEndDate`,`renewalCycle`,`accessUrl`) SELECT `id`,`date`,`amount`,`status`,`assetCategoryId`,`categoryId`,`name`,`specification`,`quantity`,`purchaseDate`,`sourceApprovalId`,NULL,`targetPerson`,`targetAccount`,`note`,`isCompleted`,`attachments`,`createdAt`,`updatedAt`,`assetRootType`,`supplier`,`location`,`userOrDepartment`,`warranty`,`serviceStartDate`,`serviceEndDate`,`renewalCycle`,`accessUrl` FROM `assets`")
            database.execSQL("DROP TABLE `assets`")
            database.execSQL("ALTER TABLE `assets_new` RENAME TO `assets`")
            database.execSQL("CREATE INDEX `index_assets_assetCategoryId` ON `assets` (`assetCategoryId`)")
            database.execSQL("CREATE UNIQUE INDEX `index_assets_sourceApprovalId` ON `assets` (`sourceApprovalId`) WHERE `sourceApprovalId` IS NOT NULL")
            database.execSQL("CREATE UNIQUE INDEX `index_assets_transactionId` ON `assets` (`transactionId`) WHERE `transactionId` IS NOT NULL")

            database.execSQL("CREATE TABLE `budget_approval_requests_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `categoryId` INTEGER, `assetCategoryId` INTEGER, `amount` REAL NOT NULL, `purchaseDate` INTEGER, `reason` TEXT NOT NULL, `itemName` TEXT NOT NULL, `specification` TEXT NOT NULL, `quantity` REAL NOT NULL, `attachments` TEXT NOT NULL, `status` TEXT NOT NULL, `decisionNote` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `decidedAt` INTEGER, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`assetCategoryId`) REFERENCES `asset_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
            database.execSQL("INSERT INTO `budget_approval_requests_new` (`id`,`type`,`categoryId`,`assetCategoryId`,`amount`,`purchaseDate`,`reason`,`itemName`,`specification`,`quantity`,`attachments`,`status`,`decisionNote`,`createdAt`,`updatedAt`,`decidedAt`) SELECT `id`,`type`,`categoryId`,`assetCategoryId`,`amount`,`purchaseDate`,`reason`,`itemName`,`specification`,`quantity`,`attachments`,`status`,`decisionNote`,`createdAt`,`updatedAt`,`decidedAt` FROM `budget_approval_requests`")
            database.execSQL("DROP TABLE `budget_approval_requests`")
            database.execSQL("ALTER TABLE `budget_approval_requests_new` RENAME TO `budget_approval_requests`")
            database.execSQL("CREATE INDEX `index_budget_approval_requests_status` ON `budget_approval_requests` (`status`)")
            database.execSQL("CREATE INDEX `index_budget_approval_requests_createdAt` ON `budget_approval_requests` (`createdAt`)")
            database.execSQL("CREATE INDEX `index_budget_approval_requests_categoryId` ON `budget_approval_requests` (`categoryId`)")
            database.execSQL("CREATE INDEX `index_budget_approval_requests_assetCategoryId` ON `budget_approval_requests` (`assetCategoryId`)")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DELETE FROM budget_approval_requests WHERE type = 'BUDGET_ADJUSTMENT'")
        }
    }
}
