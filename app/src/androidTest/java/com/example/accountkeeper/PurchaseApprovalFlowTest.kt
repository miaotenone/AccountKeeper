package com.example.accountkeeper

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.model.AttachmentType
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AttachmentRepository
import com.example.accountkeeper.data.repository.BudgetApprovalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseApprovalFlowTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetApprovalRepository
    private lateinit var attachmentRepository: AttachmentRepository
    private var expenseCategoryId: Long = 0L
    private var assetCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.categoryDao().insertCategory(Category(name = "Office", type = TransactionType.EXPENSE))
        expenseCategoryId = database.categoryDao().getAllCategoriesList().single().id
        assetCategoryId = database.assetCategoryDao().insert(
            AssetCategoryEntity(name = "Laptop", rootType = AssetRootType.PHYSICAL)
        )
        attachmentRepository = AttachmentRepository(database.attachmentDao())
        repository = BudgetApprovalRepository(
            database = database,
            approvalDao = database.budgetApprovalDao(),
            budgetDao = database.budgetDao(),
            budgetMonthDao = database.budgetMonthDao(),
            categoryDao = database.categoryDao(),
            assetCategoryDao = database.assetCategoryDao(),
            assetDao = database.assetDao(),
            attachmentRepository = attachmentRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchaseApproval_requiresPurchaseFields() = runBlocking {
        val request = purchaseRequest().copy(itemName = "")

        val error = expectFailure<IllegalArgumentException> { repository.submit(request) }

        assertTrue(error.message!!.contains("item name"))
    }

    @Test
    fun withdrawnRequest_canBeEditedAndResubmitted() = runBlocking {
        val id = repository.submit(purchaseRequest(itemName = "Old laptop"))
        repository.withdraw(id)

        repository.resubmit(purchaseRequest(itemName = "New laptop").copy(id = id))

        val updated = repository.getById(id)!!
        assertEquals(BudgetApprovalStatus.PENDING, updated.status)
        assertEquals("New laptop", updated.itemName)
        assertEquals("", updated.decisionNote)
    }

    @Test
    fun approvingPurchase_updatesBudgetCreatesOneAssetAndDualWritesAttachments() = runBlocking {
        val attachment = Attachment(
            id = "quote-1",
            fileName = "quote.pdf",
            filePath = "/files/quote.pdf",
            fileType = AttachmentType.PDF,
            fileSize = 20,
            mimeType = "application/pdf",
            createdAt = 1
        )
        val requestId = repository.submit(purchaseRequest(attachments = listOf(attachment)))

        repository.approve(requestId, "ok")

        val approved = repository.getById(requestId)!!
        assertEquals(BudgetApprovalStatus.APPROVED, approved.status)
        assertEquals("ok", approved.decisionNote)

        val assets = database.assetDao().getAllAssets().first()
        assertEquals(1, assets.size)
        val asset = assets.single()
        assertEquals(AssetStatus.IN_PROGRESS, asset.status)
        assertEquals(requestId, asset.sourceApprovalId)
        assertEquals(assetCategoryId, asset.assetCategoryId)
        assertEquals("Laptop Pro", asset.name)

        val approvalAttachments = attachmentRepository.getForOwnerList(AttachmentOwnerType.APPROVAL, requestId)
        val assetAttachments = attachmentRepository.getForOwnerList(AttachmentOwnerType.ASSET, asset.id)
        assertEquals(1, approvalAttachments.size)
        assertEquals(1, assetAttachments.size)
        assertEquals("quote.pdf", assetAttachments.single().fileName)

        expectFailure<IllegalStateException> { repository.approve(requestId, "again") }
        assertEquals(1, database.assetDao().getAllAssets().first().size)
        assertNotNull(database.assetDao().getBySourceApprovalId(requestId))
    }

    private fun purchaseRequest(
        itemName: String = "Laptop Pro",
        attachments: List<Attachment> = emptyList()
    ) = BudgetApprovalRequest(
        type = BudgetApprovalType.PURCHASE_BUDGET,
        categoryId = expenseCategoryId,
        assetCategoryId = assetCategoryId,
        amount = 1200.0,
        purchaseDate = 1785600000000,
        reason = "Replacement",
        itemName = itemName,
        specification = "14 inch",
        quantity = 1.0,
        attachments = AttachmentConverter.toJson(attachments)
    )

    private suspend inline fun <reified T : Throwable> expectFailure(block: suspend () -> Unit): T {
        return try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            if (error is T) error else throw error
        }
    }
}
