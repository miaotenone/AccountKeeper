package com.example.accountkeeper.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.accountkeeper.data.local.TransactionDao
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentOwnerType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val attachmentRepository: AttachmentRepository
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

    fun getByTimeRangePaged(startDate: Long, endDate: Long): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { transactionDao.getByTimeRangePaged(startDate, endDate) }
    ).flow

    fun getFilteredPaged(startDate: Long, endDate: Long, categoryId: Long?): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { transactionDao.getFilteredPaged(startDate, endDate, categoryId) }
    ).flow

    fun getIncomeBetween(startDate: Long, endDate: Long): Flow<Double> = transactionDao.getIncomeBetween(startDate, endDate)
    fun getExpenseBetween(startDate: Long, endDate: Long): Flow<Double> = transactionDao.getExpenseBetween(startDate, endDate)
    
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    suspend fun getAvailableExpenseTransactions(categoryId: Long, startDate: Long, endDate: Long): List<Transaction> = transactionDao.getAvailableExpenseTransactions(categoryId, startDate, endDate)
    suspend fun updateTransactionCategory(oldId: Long, newId: Long) = transactionDao.updateTransactionCategory(oldId, newId)
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        attachmentRepository.replaceForOwner(AttachmentOwnerType.TRANSACTION, transaction.id, AttachmentConverter.fromJson(transaction.attachments))
    }
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
        attachmentRepository.replaceForOwner(AttachmentOwnerType.TRANSACTION, transaction.id, AttachmentConverter.fromJson(transaction.attachments))
    }
    suspend fun deleteTransaction(transaction: Transaction) {
        attachmentRepository.deleteForOwner(AttachmentOwnerType.TRANSACTION, transaction.id)
        transactionDao.deleteTransaction(transaction)
    }
    suspend fun deleteTransactions(transactions: List<Transaction>) {
        transactions.forEach { attachmentRepository.deleteForOwner(AttachmentOwnerType.TRANSACTION, it.id) }
        transactionDao.deleteTransactions(transactions)
    }
    suspend fun deleteAllTransactions() {
        attachmentRepository.deleteForOwnerType(AttachmentOwnerType.TRANSACTION)
        transactionDao.deleteAllTransactions()
    }
}
