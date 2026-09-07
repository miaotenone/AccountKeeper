package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.AssetCategoryDao
import com.example.accountkeeper.data.local.AssetDao
import com.example.accountkeeper.data.local.AssetFieldDefinitionDao
import com.example.accountkeeper.data.model.AssetCategoryDefaults
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.AssetFieldDefinition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AssetCategoryRepository @Inject constructor(
    private val dao: AssetCategoryDao,
    private val assetDao: AssetDao,
    private val fieldDao: AssetFieldDefinitionDao
) {
    fun getAll(): Flow<List<AssetCategoryEntity>> = dao.getAll()
    fun getByDirection(direction: AssetDirection): Flow<List<AssetCategoryEntity>> = dao.getByDirection(direction)
    suspend fun getById(id: Long): AssetCategoryEntity? = dao.getById(id)
    suspend fun insert(entity: AssetCategoryEntity): Long {
        val id = dao.insert(entity)
        seedFields(entity.copy(id = id))
        return id
    }
    suspend fun update(entity: AssetCategoryEntity) = dao.update(entity)
    suspend fun delete(entity: AssetCategoryEntity) = dao.delete(entity)
    suspend fun deleteAll() = dao.deleteAll()

    suspend fun ensureDefaults() {
        val now = System.currentTimeMillis()
        AssetDirection.entries.forEach { direction ->
            AssetCategoryDefaults.categoryNames(direction).forEach { name ->
                if (dao.getByNameAndDirection(name, direction) == null) {
                    dao.insert(AssetCategoryEntity(name = name, direction = direction, isDefault = true, createdAt = now, updatedAt = now))
                }
            }
        }
        for (category in dao.getAllList()) { seedFields(category) }
    }

    suspend fun setCategoryDirection(category: AssetCategoryEntity, targetDirection: AssetDirection) {
        if (category.direction == targetDirection) return
        val now = System.currentTimeMillis()
        dao.update(category.copy(direction = targetDirection, updatedAt = now))
        assetDao.updateDirectionForCategory(category.id, targetDirection.name, now)
    }

    private suspend fun seedFields(category: AssetCategoryEntity) {
        val existingNames = fieldDao.getForCategoryList(category.id).map { it.name.trim() }.toSet()
        AssetCategoryDefaults.fieldTemplatesFor(category.name, category.isDefault)
            .filterNot { it.name in existingNames }
            .forEachIndexed { index, template ->
                fieldDao.insert(
                    AssetFieldDefinition(
                        assetCategoryId = category.id,
                        name = template.name,
                        type = template.type,
                        options = template.options,
                        sortOrder = existingNames.size + index
                    )
                )
            }
    }
}
