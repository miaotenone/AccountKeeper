package com.example.accountkeeper

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.accountkeeper.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end upgrade regression tests for the "crash on launch after update" bug.
 *
 * A user database created by an older release (1.0.0 -> v1, 1.5.1 -> v5) must open
 * with the current [AppDatabase] and pass Room's schema validation. The app opens
 * the database during `MainActivity.onCreate` (SettingsViewModel -> AppDatabase),
 * so any failure here is exactly the launch crash users reported.
 *
 * These tests deliberately go through [Room] instead of calling `Migration.migrate`
 * by hand, because only Room's open helper performs the schema validation that was
 * silently broken.
 */
@RunWith(AndroidJUnit4::class)
class FullUpgradeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createDatabaseAtVersion(version: Int, createSchema: (SupportSQLiteDatabase) -> Unit) {
        val name = "upgrade-test-v$version"
        context.deleteDatabase(name)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = createSchema(db)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase.close()
    }

    private fun openAndValidate(version: Int) {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "upgrade-test-v$version")
            .addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .build()
        db.openHelper.writableDatabase
        db.query("SELECT COUNT(*) FROM transactions", null).use { cursor ->
            assertEquals(1, cursor.count)
        }
        db.close()
    }

    @Test
    fun upgradeFromV1_opensAndValidates() {
        createDatabaseAtVersion(1) { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transactions` (" +
                    "`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                    "`date` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, " +
                    "`source` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            db.execSQL("INSERT INTO categories (id, name, type, isDefault) VALUES (1, '餐饮', 'EXPENSE', 1)")
            db.execSQL("INSERT INTO transactions (id, type, amount, date, categoryId, note, source) VALUES (1, 'EXPENSE', 25.0, 1700000000000, 1, '午饭', 'manual')")
        }
        openAndValidate(1)
    }

    @Test
    fun upgradeFromV5_opensAndValidates() {
        createDatabaseAtVersion(5) { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                    "`isPositiveAsset` INTEGER NOT NULL DEFAULT 1)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transactions` (" +
                    "`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                    "`date` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, " +
                    "`source` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `assets` (" +
                    "`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `amount` REAL NOT NULL, " +
                    "`status` TEXT NOT NULL, `categoryId` INTEGER, " +
                    "`targetPerson` TEXT NOT NULL, `targetAccount` TEXT NOT NULL, " +
                    "`note` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, " +
                    "`attachments` TEXT NOT NULL DEFAULT '', " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_categoryId` ON `assets` (`categoryId`)")
            db.execSQL("INSERT INTO categories (id, name, type, isDefault, isPositiveAsset) VALUES (1, '餐饮', 'EXPENSE', 1, 1)")
            db.execSQL("INSERT INTO transactions (id, type, amount, date, categoryId, note, source) VALUES (1, 'EXPENSE', 25.0, 1700000000000, 1, '午饭', 'manual')")
            db.execSQL("INSERT INTO assets (id, date, amount, status, categoryId, targetPerson, targetAccount, note, isCompleted, attachments, createdAt, updatedAt) VALUES (1, 1700000000000, 500.0, 'OWNED', NULL, '', '', '', 0, '', 1700000000000, 1700000000000)")
        }
        openAndValidate(5)
    }

    companion object {
        private val ALL_MIGRATIONS = listOf(
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
            AppDatabase.MIGRATION_20_21,
            AppDatabase.MIGRATION_21_22,
            AppDatabase.MIGRATION_22_23,
            AppDatabase.MIGRATION_23_24,
            AppDatabase.MIGRATION_24_25,
            AppDatabase.MIGRATION_25_26,
            AppDatabase.MIGRATION_26_27,
            AppDatabase.MIGRATION_27_28
        )
    }
}
