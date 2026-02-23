package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.TransactionDao
import com.example.accountkeeper.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsBetween(startDate, endDate)
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    suspend fun updateTransactionCategory(oldId: Long, newId: Long) = transactionDao.updateTransactionCategory(oldId, newId)
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
}
