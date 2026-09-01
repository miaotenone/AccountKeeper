package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.BillFileDao
import com.example.accountkeeper.data.model.BillFileEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BillFileRepository @Inject constructor(private val dao: BillFileDao) {
    fun getAll(): Flow<List<BillFileEntity>> = dao.getAll()
    suspend fun getById(id: String): BillFileEntity? = dao.getById(id)
    suspend fun getBySha256(sha256: String): BillFileEntity? = dao.getBySha256(sha256)
    suspend fun insert(billFile: BillFileEntity) = dao.insert(billFile)
    suspend fun update(billFile: BillFileEntity) = dao.update(billFile)
    suspend fun delete(billFile: BillFileEntity) = dao.delete(billFile)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
}
