package com.example.accountkeeper

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.accountkeeper.data.local.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val DB_NAME = "migration-test-db"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun openDbAtVersion(
        version: Int,
        setup: (SupportSQLiteDatabase) -> Unit = {}
    ): SupportSQLiteDatabase {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper.writableDatabase.also(setup)
    }

    private fun runMigrations(db: SupportSQLiteDatabase, migrations: List<Migration>) {
        migrations.forEach { it.migrate(db) }
    }

    private fun allMigrations(): List<Migration> = listOf(
        AppDatabase.MIGRATION_1_2,
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8,
        AppDatabase.MIGRATION_8_9,
        AppDatabase.MIGRATION_9_10,
        AppDatabase.MIGRATION_10_11,
        AppDatabase.MIGRATION_11_12,
        AppDatabase.MIGRATION_12_13,
        AppDatabase.MIGRATION_13_14,
        AppDatabase.MIGRATION_14_15,
        AppDatabase.MIGRATION_15_16,
        AppDatabase.MIGRATION_16_17,
        AppDatabase.MIGRATION_17_18,
        AppDatabase.MIGRATION_18_19,
        AppDatabase.MIGRATION_19_20,
        AppDatabase.MIGRATION_20_21
    )

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`isDefault` INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions` (" +
                "`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                "`date` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
        )
    }

    private fun createVersion5Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                "`isPositiveAsset` INTEGER NOT NULL DEFAULT 1)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `assets` (" +
                "`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, " +
                "`status` TEXT NOT NULL, `categoryId` INTEGER, " +
                "`targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "`isCompleted` INTEGER NOT NULL, `attachments` TEXT NOT NULL DEFAULT '', " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
        )
    }

    private fun insertVersion5Asset(db: SupportSQLiteDatabase, id: Long = 101L) {
        db.execSQL(
            "INSERT INTO `assets` " +
                "(`id`, `date`, `amount`, `status`, `categoryId`, `targetPerson`, `targetAccount`, " +
                "`note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt`) " +
                "VALUES ($id, 1700000000000, 500.0, 'OWNED', NULL, '', '', '', 0, '', 1700000000000, 1700000000000)"
        )
    }

    private fun createBudgetsTable(db: SupportSQLiteDatabase, withRestrictForeignKey: Boolean = false) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`monthKey` TEXT NOT NULL, `categoryId` INTEGER, `amount` REAL NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL" +
                (if (withRestrictForeignKey) {
                    ", FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT"
                } else {
                    ""
                }) +
                ")"
        )
    }

    private fun createCategoriesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                "`isPositiveAsset` INTEGER NOT NULL DEFAULT 1)"
        )
    }

    private fun createVersion6Schema(db: SupportSQLiteDatabase) {
        createBudgetsTable(db)
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (1, '2025-01', NULL, 100.0, 1, 1)")
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (2, '2025-01', NULL, 200.0, 1, 1)")
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (3, '2025-01', 10, 50.0, 1, 1)")
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (4, '2025-01', 10, 60.0, 1, 1)")
    }

    private fun createVersion7Schema(db: SupportSQLiteDatabase) {
        createBudgetsTable(db)
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (1, '2025-01', NULL, 100.0, 11, 11)")
        db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (2, '2025-02', NULL, 200.0, 22, 22)")
    }

    private fun createVersion9Schema(db: SupportSQLiteDatabase) {
        createCategoriesTable(db)
        createBudgetsTable(db, withRestrictForeignKey = true)
    }

    private fun createVersion10Schema(db: SupportSQLiteDatabase, insertNegativeBudget: Boolean = false) {
        createCategoriesTable(db)
        createBudgetsTable(db, withRestrictForeignKey = true)
        if (insertNegativeBudget) {
            db.execSQL("INSERT INTO `budgets` (`id`, `monthKey`, `categoryId`, `amount`, `createdAt`, `updatedAt`) VALUES (1, '2025-01', NULL, -10.0, 1, 1)")
        }
    }

    @Test
    fun migrateAll_from1To21() {
        val db = openDbAtVersion(1) { createVersion1Schema(it) }
        runMigrations(db, allMigrations())
        assertNotNull(db)
        db.close()
    }

    @Test
    fun migrate5to6_createsAssetTypesTable() {
        val db = openDbAtVersion(5) { createVersion5Schema(it) }
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT * FROM asset_types ORDER BY id")
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals("实物资产", cursor.getString(cursor.getColumnIndexOrThrow("name")))

        cursor.moveToNext()
        assertEquals("虚拟资产", cursor.getString(cursor.getColumnIndexOrThrow("name")))

        cursor.close()
        db.close()
    }

    @Test
    fun migrate5to6_addsAssetTypeIdToAssets() {
        val db = openDbAtVersion(5) { createVersion5Schema(it) }
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("PRAGMA table_info(assets)")
        var hasAssetTypeId = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "assetTypeId") {
                hasAssetTypeId = true
                break
            }
        }
        cursor.close()
        assertTrue("assets table should have assetTypeId column", hasAssetTypeId)

        db.close()
    }

    @Test
    fun migrate5to6_createsBudgetsTable() {
        val db = openDbAtVersion(5) { createVersion5Schema(it) }
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='budgets'")
        assertEquals(1, cursor.count)
        cursor.close()

        db.close()
    }

    @Test
    fun migrate7to8_createsBudgetMonthsTable() {
        val db = openDbAtVersion(7) { createVersion7Schema(it) }
        AppDatabase.MIGRATION_7_8.migrate(db)

        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='budget_months'")
        assertEquals(1, cursor.count)
        cursor.close()

        db.close()
    }

    @Test
    fun migrate10to11_clampsNegativeBudgetAmounts() {
        val db = openDbAtVersion(10) { createVersion10Schema(it, insertNegativeBudget = true) }
        AppDatabase.MIGRATION_10_11.migrate(db)

        val cursor = db.query("SELECT amount FROM budgets WHERE amount < 0")
        assertEquals(0, cursor.count)
        cursor.close()

        db.close()
    }

    @Test
    fun migrate9to10_createsTotalBudgetGuardTriggers() {
        val db = openDbAtVersion(9) { createVersion9Schema(it) }
        AppDatabase.MIGRATION_9_10.migrate(db)

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name='budgets_total_insert_guard'"
        )
        assertEquals(1, cursor.count)
        cursor.close()

        val cursor2 = db.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name='budgets_total_update_guard'"
        )
        assertEquals(1, cursor2.count)
        cursor2.close()

        db.close()
    }

    @Test
    fun migrate10to11_createsBudgetValidationTriggers() {
        val db = openDbAtVersion(10) { createVersion10Schema(it) }
        AppDatabase.MIGRATION_10_11.migrate(db)

        val expectedTriggers = listOf(
            "budgets_amount_insert_guard",
            "budgets_amount_update_guard",
            "budgets_expense_category_insert_guard",
            "budgets_expense_category_update_guard",
            "categories_budget_type_guard"
        )

        for (triggerName in expectedTriggers) {
            val cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='trigger' AND name='$triggerName'"
            )
            assertTrue("Trigger $triggerName should exist", cursor.count == 1)
            cursor.close()
        }

        db.close()
    }

    @Test
    fun migrate6to7_deduplicatesBudgets() {
        val db = openDbAtVersion(6) { createVersion6Schema(it) }
        AppDatabase.MIGRATION_6_7.migrate(db)

        val countCursor = db.query("SELECT COUNT(*) FROM budgets")
        assertTrue(countCursor.moveToFirst())
        assertEquals(2, countCursor.getInt(0))
        countCursor.close()

        val cursor = db.query("PRAGMA index_list('budgets')")
        var foundUniqueIndex = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "index_budgets_monthKey_categoryId") {
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("unique")))
                foundUniqueIndex = true
            }
        }
        cursor.close()
        assertTrue("Unique budget month/category index should exist", foundUniqueIndex)

        db.close()
    }

    @Test
    fun fullMigration_from1To21_preservesSchema() {
        val db = openDbAtVersion(1) { createVersion1Schema(it) }
        runMigrations(db, allMigrations())

        val expectedTables = listOf(
            "transactions", "categories", "assets", "budgets", "budget_months",
            "budget_approval_requests", "attachments", "asset_categories", "bill_files"
        )
        for (table in expectedTables) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'")
            assertTrue("Table $table should exist", cursor.count == 1)
            cursor.close()
        }

        // asset_types table should NOT exist after migration 12->13
        val assetTypesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='asset_types'")
        assertEquals("asset_types table should not exist", 0, assetTypesCursor.count)
        assetTypesCursor.close()

        val expectedIndexes = listOf(
            "index_transactions_categoryId",
            "index_assets_assetCategoryId",
            "index_assets_sourceApprovalId",
            "index_attachments_ownerType_ownerId",
            "index_attachments_sha256",
            "index_asset_categories_rootType",
            "index_bill_files_ownerType",
            "index_budgets_monthKey_categoryId",
            "index_budgets_categoryId"
        )
        for (index in expectedIndexes) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='$index'")
            assertTrue("Index $index should exist", cursor.count == 1)
            cursor.close()
        }

        val attachmentIndexCursor = db.query("PRAGMA index_list('attachments')")
        var foundShaIndex = false
        while (attachmentIndexCursor.moveToNext()) {
            if (attachmentIndexCursor.getString(attachmentIndexCursor.getColumnIndexOrThrow("name")) == "index_attachments_sha256") {
                assertEquals("Attachment sha256 index should allow one file to be linked to multiple owners", 0, attachmentIndexCursor.getInt(attachmentIndexCursor.getColumnIndexOrThrow("unique")))
                foundShaIndex = true
            }
        }
        attachmentIndexCursor.close()
        assertTrue(foundShaIndex)

        val assetForeignKeys = db.query("PRAGMA foreign_key_list('assets')")
        val transactionColumns = db.query("PRAGMA table_info('transactions')")
        val transactionColumnNames = mutableSetOf<String>()
        while (transactionColumns.moveToNext()) {
            transactionColumnNames += transactionColumns.getString(transactionColumns.getColumnIndexOrThrow("name"))
        }
        transactionColumns.close()
        assertTrue("transactions table should keep legacy attachment JSON", transactionColumnNames.contains("attachments"))

        val assetForeignKeyColumns = mutableListOf<String>()
        while (assetForeignKeys.moveToNext()) {
            assetForeignKeyColumns += assetForeignKeys.getString(assetForeignKeys.getColumnIndexOrThrow("from"))
        }
        assetForeignKeys.close()
        assertEquals(listOf("assetCategoryId"), assetForeignKeyColumns)

        val expectedTriggers = listOf(
            "budgets_total_insert_guard",
            "budgets_total_update_guard",
            "budgets_amount_insert_guard",
            "budgets_amount_update_guard",
            "budgets_expense_category_insert_guard",
            "budgets_expense_category_update_guard",
            "categories_budget_type_guard"
        )
        for (trigger in expectedTriggers) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='trigger' AND name='$trigger'")
            assertTrue("Trigger $trigger should exist", cursor.count == 1)
            cursor.close()
        }

        db.close()
    }

    @Test
    fun migration20To21_addsTransactionAttachmentColumn() {
        val db = openDbAtVersion(1) { createVersion1Schema(it) }
        runMigrations(db, allMigrations().dropLast(1))

        AppDatabase.MIGRATION_20_21.migrate(db)

        val transactionColumns = db.query("PRAGMA table_info('transactions')")
        val transactionColumnNames = mutableSetOf<String>()
        while (transactionColumns.moveToNext()) {
            transactionColumnNames += transactionColumns.getString(transactionColumns.getColumnIndexOrThrow("name"))
        }
        transactionColumns.close()
        assertTrue("transactions table should keep legacy attachment JSON", transactionColumnNames.contains("attachments"))
        db.close()
    }

    @Test
    fun migrationFrom15To21_preservesLegacyAssetAndApprovalAttachments() {
        val db = openDbAtVersion(1) { createVersion1Schema(it) }
        val migrations = allMigrations()
        runMigrations(db, migrations.take(14))

        val legacyAttachments = "[{\"id\":\"legacy-attachment\",\"fileName\":\"receipt.pdf\",\"filePath\":\"/files/receipt.pdf\",\"fileType\":\"PDF\",\"fileSize\":12,\"mimeType\":\"application/pdf\"}]"
        db.execSQL("INSERT INTO asset_categories (name, rootType, parentCategoryId, isDefault, createdAt, updatedAt) VALUES ('Equipment', 'PHYSICAL', NULL, 0, 1, 1)")
        db.execSQL("INSERT INTO assets (id, date, amount, status, categoryId, name, specification, quantity, purchaseDate, sourceApprovalId, targetPerson, targetAccount, note, isCompleted, attachments, createdAt, updatedAt, assetRootType, supplier, location, userOrDepartment, warranty, serviceStartDate, serviceEndDate, renewalCycle, accessUrl) VALUES (900, 1, 20.0, 'OWNED', NULL, '', '', 1.0, NULL, NULL, '', '', '', 0, ?, 1, 1, 'PHYSICAL', '', '', '', '', NULL, NULL, '', '')", arrayOf(legacyAttachments))
        db.execSQL("INSERT INTO budget_approval_requests (type, monthKey, periodType, categoryId, assetCategoryId, amount, purchaseDate, reason, reasonDetail, itemName, specification, quantity, attachments, status, decisionNote, createdAt, updatedAt, decidedAt) VALUES ('PURCHASE_BUDGET', '2026-08', 'MONTHLY', NULL, NULL, 20.0, 1, '', '', '', '', 1.0, ?, 'PENDING', '', 1, 1, NULL)", arrayOf(legacyAttachments))

        runMigrations(db, migrations.drop(14))

        val assetCursor = db.query("SELECT attachments, assetCategoryId FROM assets WHERE id = 900")
        assertTrue(assetCursor.moveToFirst())
        assertEquals(legacyAttachments, assetCursor.getString(0))
        assertTrue(assetCursor.isNull(1))
        assetCursor.close()

        val approvalCursor = db.query("SELECT attachments FROM budget_approval_requests")
        assertTrue(approvalCursor.moveToFirst())
        assertEquals(legacyAttachments, approvalCursor.getString(0))
        approvalCursor.close()

        val attachmentTableCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'attachments'")
        assertTrue(attachmentTableCursor.moveToFirst())
        attachmentTableCursor.close()

        val migratedAttachments = db.query("SELECT ownerType, ownerId, fileName FROM attachments ORDER BY ownerType")
        assertEquals(2, migratedAttachments.count)
        assertTrue(migratedAttachments.moveToFirst())
        assertEquals("APPROVAL", migratedAttachments.getString(0))
        assertEquals("receipt.pdf", migratedAttachments.getString(2))
        assertTrue(migratedAttachments.moveToNext())
        assertEquals("ASSET", migratedAttachments.getString(0))
        assertEquals(900L, migratedAttachments.getLong(1))
        migratedAttachments.close()

        db.execSQL("INSERT INTO attachments (id, ownerType, ownerId, fileName, filePath, mimeType, fileSize, sha256, createdAt) VALUES ('same-hash-asset', 'ASSET', 1, 'copy.pdf', '/files/copy.pdf', 'application/pdf', 12, 'same-hash', 1)")
        db.execSQL("INSERT INTO attachments (id, ownerType, ownerId, fileName, filePath, mimeType, fileSize, sha256, createdAt) VALUES ('same-hash-approval', 'APPROVAL', 1, 'copy.pdf', '/files/copy.pdf', 'application/pdf', 12, 'same-hash', 1)")
        val duplicateHashCursor = db.query("SELECT COUNT(*) FROM attachments WHERE sha256 = 'same-hash'")
        assertTrue(duplicateHashCursor.moveToFirst())
        assertEquals(2, duplicateHashCursor.getInt(0))
        duplicateHashCursor.close()
        db.close()
    }


    @Test
    fun migrate5to6_oldAssets_getVirtualAssetTypeId() {
        val db = openDbAtVersion(5) {
            createVersion5Schema(it)
            insertVersion5Asset(it)
        }
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT assetTypeId FROM assets")
        assertTrue(cursor.moveToFirst())
        assertEquals(2L, cursor.getLong(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate5to6_virtualAssetTypeExists() {
        val db = openDbAtVersion(5) { createVersion5Schema(it) }
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT name FROM asset_types WHERE id = 2")
        assertTrue(cursor.moveToFirst())
        assertEquals("虚拟资产", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate7to8_budgetMonths_hasCorrectColumns() {
        val db = openDbAtVersion(7) { createVersion7Schema(it) }
        AppDatabase.MIGRATION_7_8.migrate(db)

        val cursor = db.query("PRAGMA table_info(budget_months)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        assertTrue("budget_months should have monthKey column", columns.contains("monthKey"))
        assertTrue("budget_months should have initializedAt column", columns.contains("initializedAt"))

        db.close()
    }

    @Test
    fun migrate9to10_budgetsForeignKeys_areRestrict() {
        val db = openDbAtVersion(9) { createVersion9Schema(it) }
        AppDatabase.MIGRATION_9_10.migrate(db)

        val cursor = db.query("SELECT sql FROM sqlite_master WHERE type='table' AND name='budgets'")
        assertTrue(cursor.moveToFirst())
        val createSql = cursor.getString(0)
        cursor.close()

        assertTrue("budgets table should have FOREIGN KEY with RESTRICT", createSql.contains("RESTRICT"))

        db.close()
    }

    @Test
    fun migrate10to11_budgetAmounts_areNonNegative() {
        val db = openDbAtVersion(10) { createVersion10Schema(it) }
        AppDatabase.MIGRATION_10_11.migrate(db)

        val cursor = db.query("SELECT amount FROM budgets")
        while (cursor.moveToNext()) {
            val amount = cursor.getDouble(0)
            assertTrue("Budget amount should be non-negative, got $amount", amount >= 0.0)
        }
        cursor.close()
        db.close()
    }

    @Test
    fun migration20To21_allowsMultipleAssetsWithNullSourceApprovalId() {
        val db = openDbAtVersion(1) { createVersion1Schema(it) }
        runMigrations(db, allMigrations().dropLast(1))

        AppDatabase.MIGRATION_20_21.migrate(db)

        db.execSQL(
            "INSERT INTO `assets` (`id`, `date`, `amount`, `status`, `categoryId`, `name`, `specification`, `quantity`, `purchaseDate`, `sourceApprovalId`, `targetPerson`, `targetAccount`, `note`, `isCompleted`, `attachments`, `createdAt`, `updatedAt`, `assetRootType`, `supplier`, `location`, `userOrDepartment`, `warranty`, `serviceStartDate`, `serviceEndDate`, `renewalCycle`, `accessUrl`) VALUES (901, 1, 10.0, 'OWNED', NULL, '', '', 1.0, NULL, NULL, '', '', '', 0, '', 1, 1, 'PHYSICAL', '', '', '', '', NULL, NULL, '', ''), (902, 1, 20.0, 'OWNED', NULL, '', '', 1.0, NULL, NULL, '', '', '', 0, '', 1, 1, 'PHYSICAL', '', '', '', '', NULL, NULL, '', '')"
        )
        val countCursor = db.query("SELECT COUNT(*) FROM assets WHERE sourceApprovalId IS NULL")
        assertTrue(countCursor.moveToFirst())
        assertEquals(2, countCursor.getInt(0))
        countCursor.close()
        db.close()
    }
}
