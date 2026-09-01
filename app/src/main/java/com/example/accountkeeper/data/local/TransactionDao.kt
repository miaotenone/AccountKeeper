package com.example.accountkeeper.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsPaged(): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("""
        SELECT t.* FROM transactions t
        LEFT JOIN assets a ON a.transactionId = t.id
        WHERE t.type = 'EXPENSE'
          AND t.categoryId = :categoryId
          AND t.date >= :startDate AND t.date < :endDate
          AND a.id IS NULL
        ORDER BY t.date DESC
    """)
    suspend fun getAvailableExpenseTransactions(categoryId: Long, startDate: Long, endDate: Long): List<Transaction>

    @Query("UPDATE transactions SET categoryId = :newId WHERE categoryId = :oldId")
    suspend fun updateTransactionCategory(oldId: Long, newId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransactions(transactions: List<Transaction>)

    @Query("""
        SELECT t.* FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.note LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%'
        ORDER BY t.date DESC
    """)
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Query("""
        SELECT t.* FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.note LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%'
        ORDER BY t.date DESC
    """)
    fun searchTransactionsPaged(query: String): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions
        WHERE categoryId = :categoryId
        AND date >= :startTime
        AND date < :endTime
        ORDER BY date DESC
    """)
    fun getByCategoryAndTimePaged(categoryId: Long, startTime: Long, endTime: Long): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getByTimeRangePaged(startDate: Long, endDate: Long): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startDate AND date < :endDate
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY date DESC
    """)
    fun getFilteredPaged(startDate: Long, endDate: Long, categoryId: Long?): PagingSource<Int, Transaction>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME' AND date >= :startDate AND date < :endDate")
    fun getIncomeBetween(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND date >= :startDate AND date < :endDate")
    fun getExpenseBetween(startDate: Long, endDate: Long): Flow<Double>
}
