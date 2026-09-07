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

    @Query("SELECT * FROM budget_approval_requests WHERE (:startAt IS NULL OR createdAt >= :startAt) AND (:endAt IS NULL OR createdAt < :endAt) AND (:categoryId IS NULL OR categoryId = :categoryId) AND (:assetCategoryId IS NULL OR assetCategoryId = :assetCategoryId) ORDER BY createdAt DESC")
    fun search(startAt: Long?, endAt: Long?, categoryId: Long?, assetCategoryId: Long?): Flow<List<BudgetApprovalRequest>>

    @Insert
    suspend fun insert(request: BudgetApprovalRequest): Long

    @Update
    suspend fun update(request: BudgetApprovalRequest)

    @Query("DELETE FROM budget_approval_requests")
    suspend fun deleteAll()
}