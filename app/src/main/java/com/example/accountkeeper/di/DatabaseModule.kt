package com.example.accountkeeper.di

import android.content.Context
import androidx.room.Room
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.local.AssetDao
import com.example.accountkeeper.data.local.BudgetDao
import com.example.accountkeeper.data.local.BudgetApprovalDao
import com.example.accountkeeper.data.local.BudgetMonthDao
import com.example.accountkeeper.data.local.CategoryDao
import com.example.accountkeeper.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "account_keeper_db")
            .addMigrations(
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
                AppDatabase.MIGRATION_13_14
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS budgets_total_insert_guard BEFORE INSERT ON budgets WHEN NEW.categoryId IS NULL AND EXISTS (SELECT 1 FROM budgets WHERE monthKey = NEW.monthKey AND categoryId IS NULL) BEGIN SELECT RAISE(ABORT, 'duplicate monthly total budget'); END")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS budgets_total_update_guard BEFORE UPDATE OF monthKey, categoryId ON budgets WHEN NEW.categoryId IS NULL AND EXISTS (SELECT 1 FROM budgets WHERE monthKey = NEW.monthKey AND categoryId IS NULL AND id != NEW.id) BEGIN SELECT RAISE(ABORT, 'duplicate monthly total budget'); END")
                    AppDatabase.createBudgetValidationTriggers(db)
                }
            })
            .build()

    @Provides fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()
    @Provides fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()
    @Provides fun provideAssetDao(database: AppDatabase): AssetDao = database.assetDao()
    @Provides fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()
    @Provides fun provideBudgetMonthDao(database: AppDatabase): BudgetMonthDao = database.budgetMonthDao()
    @Provides fun provideBudgetApprovalDao(database: AppDatabase): BudgetApprovalDao = database.budgetApprovalDao()
}
