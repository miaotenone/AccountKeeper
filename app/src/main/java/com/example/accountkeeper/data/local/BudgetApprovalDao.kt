package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetApprovalDao {
    @Query("SELECT * FROM budget_approval_requests ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BudgetApprovalRequest>>

    @Query("SELECT * FROM budget_approval_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BudgetApprovalRequest?

    @Insert
    suspend fun insert(request: BudgetApprovalRequest): Long

    @Update
    suspend fun update(request: BudgetApprovalRequest)

    @Query("DELETE FROM budget_approval_requests")
    suspend fun deleteAll()
}
