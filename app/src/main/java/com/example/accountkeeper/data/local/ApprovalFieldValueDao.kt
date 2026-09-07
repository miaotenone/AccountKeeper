package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.accountkeeper.data.model.ApprovalFieldValue

@Dao
interface ApprovalFieldValueDao {
    @Query("SELECT * FROM approval_field_values WHERE approvalId = :approvalId") suspend fun getForApproval(approvalId: Long): List<ApprovalFieldValue>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<ApprovalFieldValue>)
    @Query("DELETE FROM approval_field_values WHERE approvalId = :approvalId") suspend fun deleteForApproval(approvalId: Long)
    @Query("SELECT * FROM approval_field_values") suspend fun getAllList(): List<ApprovalFieldValue>
    @Query("DELETE FROM approval_field_values WHERE fieldId = :fieldId") suspend fun deleteForField(fieldId: Long)
    @Query("DELETE FROM approval_field_values") suspend fun deleteAll()
}