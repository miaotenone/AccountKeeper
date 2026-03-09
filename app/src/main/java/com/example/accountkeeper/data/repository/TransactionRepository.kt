package com.example.accountkeeper.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.accountkeeper.data.local.TransactionDao
import com.example.accountkeeper.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    companion object {
        private const val PAGE_SIZE = 30
    }

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    fun getAllTransactionsPaged(): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { transactionDao.getAllTransactionsPaged() }
    ).flow
    
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsBetween(startDate, endDate)
    fun searchTransactions(query: String): Flow<List<Transaction>> = transactionDao.searchTransactions(query)
    
    fun searchTransactionsPaged(query: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { transactionDao.searchTransactionsPaged(query) }
    ).flow
    
    fun getByCategoryAndTimePaged(categoryId: Long, startTime: Long, endTime: Long): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { transactionDao.getByCategoryAndTimePaged(categoryId, startTime, endTime) }
    ).flow
    
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    suspend fun updateTransactionCategory(oldId: Long, newId: Long) = transactionDao.updateTransactionCategory(oldId, newId)
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactions(transactions: List<Transaction>) = transactionDao.deleteTransactions(transactions)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
}
