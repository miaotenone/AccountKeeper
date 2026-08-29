package com.example.accountkeeper.data.repository

import androidx.room.withTransaction
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.local.BudgetApprovalDao
import com.example.accountkeeper.data.local.BudgetDao
import com.example.accountkeeper.data.local.BudgetMonthDao
import com.example.accountkeeper.data.local.CategoryDao
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.BudgetMonth
import com.example.accountkeeper.data.model.TransactionType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BudgetApprovalRepository @Inject constructor(
    private val database: AppDatabase,
    private val approvalDao: BudgetApprovalDao,
    private val budgetDao: BudgetDao,
    private val budgetMonthDao: BudgetMonthDao,
    private val categoryDao: CategoryDao
) {
    fun getAll(): Flow<List<BudgetApprovalRequest>> = approvalDao.getAll()

    suspend fun getById(id: Long): BudgetApprovalRequest? = approvalDao.getById(id)

    suspend fun submit(request: BudgetApprovalRequest): Long {
        validate(request)
        val normalized = request.copy(
            id = 0,
            status = BudgetApprovalStatus.PENDING,
            decisionNote = "",
            decidedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return approvalDao.insert(normalized)
    }

    suspend fun resubmit(request: BudgetApprovalRequest) {
        val existing = approvalDao.getById(request.id) ?: error("Approval request not found")
        check(existing.status == BudgetApprovalStatus.WITHDRAWN) { "Only withdrawn requests can be resubmitted" }
        validate(request)
        approvalDao.update(
            request.copy(
                status = BudgetApprovalStatus.PENDING,
                decisionNote = "",
                decidedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun withdraw(id: Long) {
        val existing = approvalDao.getById(id) ?: error("Approval request not found")
        check(existing.status == BudgetApprovalStatus.PENDING) { "Only pending requests can be withdrawn" }
        approvalDao.update(
            existing.copy(
                status = BudgetApprovalStatus.WITHDRAWN,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun approve(id: Long, decisionNote: String = "") {
        approveOrReject(id, BudgetApprovalStatus.APPROVED, decisionNote, applyBudget = true)
    }

    suspend fun reject(id: Long, decisionNote: String = "") {
        approveOrReject(id, BudgetApprovalStatus.REJECTED, decisionNote, applyBudget = false)
    }

    private suspend fun approveOrReject(id: Long, status: BudgetApprovalStatus, decisionNote: String, applyBudget: Boolean) {
        database.withTransaction {
            val existing = approvalDao.getById(id) ?: error("Approval request not found")
            check(existing.status == BudgetApprovalStatus.PENDING) { "Only pending requests can be decided" }
            val now = System.currentTimeMillis()
            approvalDao.update(
                existing.copy(
                    status = status,
                    decisionNote = decisionNote,
                    decidedAt = now,
                    updatedAt = now
                )
            )
            if (applyBudget) applyBudgetChange(existing, now)
        }
    }

    private suspend fun applyBudgetChange(request: BudgetApprovalRequest, now: Long) {
        val existing = if (request.categoryId == null) {
            budgetDao.getTotalByPeriod(request.monthKey, request.periodType)
        } else {
            budgetDao.getByMonthAndPeriodList(request.monthKey, request.periodType)
                .firstOrNull { it.categoryId == request.categoryId }
        }
        val amount = when (request.type) {
            BudgetApprovalType.BUDGET_ADJUSTMENT -> request.amount
            BudgetApprovalType.PURCHASE_BUDGET -> (existing?.amount ?: 0.0) + request.amount
        }
        val budget = existing?.copy(amount = amount, updatedAt = now)
            ?: Budget(
                monthKey = request.monthKey,
                categoryId = request.categoryId,
                amount = amount,
                periodType = request.periodType,
                createdAt = now,
                updatedAt = now
            )
        if (existing == null) budgetDao.insert(budget) else budgetDao.update(budget)
        budgetMonthDao.insert(BudgetMonth(monthKey = request.monthKey, initializedAt = now))
    }

    private suspend fun validate(request: BudgetApprovalRequest) {
        require(request.amount > 0.0) { "Budget approval amount must be positive" }
        require(
            request.type != BudgetApprovalType.PURCHASE_BUDGET || request.categoryId != null
        ) { "Purchase budget requests require a category" }
        request.categoryId?.let { categoryId ->
            require(categoryDao.getById(categoryId)?.type == TransactionType.EXPENSE) {
                "Approval category must be an expense category"
            }
        }
    }
}
