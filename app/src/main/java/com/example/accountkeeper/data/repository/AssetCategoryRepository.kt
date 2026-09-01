package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.AssetCategoryDao
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AssetCategoryRepository @Inject constructor(
    private val dao: AssetCategoryDao
) {
    fun getAll(): Flow<List<AssetCategoryEntity>> = dao.getAll()
    fun getByRootType(rootType: AssetRootType): Flow<List<AssetCategoryEntity>> = dao.getByRootType(rootType)
    fun getChildrenByRootType(rootType: AssetRootType): Flow<List<AssetCategoryEntity>> = dao.getChildrenByRootType(rootType)
    suspend fun getRootCategory(rootType: AssetRootType): AssetCategoryEntity? = dao.getRootCategory(rootType)
    suspend fun getById(id: Long): AssetCategoryEntity? = dao.getById(id)
    suspend fun insert(entity: AssetCategoryEntity): Long = dao.insert(entity)
    suspend fun update(entity: AssetCategoryEntity) = dao.update(entity)
    suspend fun delete(entity: AssetCategoryEntity) = dao.delete(entity)
    suspend fun deleteAll() = dao.deleteAll()

    suspend fun ensureDefaults() {
        val now = System.currentTimeMillis()
        AssetRootType.entries.forEach { rootType ->
            val root = dao.getRootCategory(rootType) ?: run {
                val id = dao.insert(
                    AssetCategoryEntity(
                        name = rootType.name,
                        rootType = rootType,
                        parentCategoryId = null,
                        isDefault = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                dao.getById(id) ?: AssetCategoryEntity(id = id, name = rootType.name, rootType = rootType, isDefault = true, createdAt = now, updatedAt = now)
            }
            if (dao.getChildrenByRootTypeList(rootType).isEmpty()) {
                val defaultChildName = when (rootType) {
                    AssetRootType.PHYSICAL -> "Equipment"
                    AssetRootType.VIRTUAL -> "Digital Service"
                }
                dao.insert(
                    AssetCategoryEntity(
                        name = defaultChildName,
                        rootType = rootType,
                        parentCategoryId = root.id,
                        isDefault = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
