package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.accountkeeper.data.model.BudgetMonth

@Dao
interface BudgetMonthDao {
    @Query("SELECT EXISTS(SELECT 1 FROM budget_months WHERE monthKey = :monthKey)")
    suspend fun exists(monthKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(month: BudgetMonth)
}
