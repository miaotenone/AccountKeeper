package com.example.accountkeeper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.accountkeeper.utils.BackupManager
import com.example.accountkeeper.utils.ZipBackupData
import com.example.accountkeeper.utils.AssetData
import com.example.accountkeeper.utils.AssetTypeData
import com.example.accountkeeper.utils.BillFileData
import com.example.accountkeeper.utils.BudgetData
import com.example.accountkeeper.utils.TransactionData
import com.example.accountkeeper.utils.ApprovalData
import com.example.accountkeeper.utils.AssetCategoryData
import com.example.accountkeeper.utils.AttachmentData
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentType
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.data.model.AttachmentOwnerType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ZipRoundtripTest {

    private lateinit var context: Context
    private lateinit var backupManager: BackupManager

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        backupManager = BackupManager(context)
    }

    private fun createTestZipFile(data: ZipBackupData, name: String): File {
        val tempFile = File(context.cacheDir, "${name}_${System.currentTimeMillis()}.zip")
        ZipOutputStream(tempFile.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(json.encodeToString(data).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
        return tempFile
    }

    private fun createRawZipFile(entries: List<Pair<String, ByteArray>>, name: String): File {
        val tempFile = File(context.cacheDir, "${name}_${System.currentTimeMillis()}.zip")
        ZipOutputStream(tempFile.outputStream()).use { zipOut ->
            for ((entryName, content) in entries) {
                zipOut.putNextEntry(ZipEntry(entryName))
                zipOut.write(content)
                zipOut.closeEntry()
            }
        }
        return tempFile
    }

    private fun assetData(
        id: Long, date: Long = 1700000000000, amount: Double, categoryName: String?,
        assetTypeId: Long, targetPerson: String, targetAccount: String, note: String
    ) = AssetData(
        id = id, date = date, amount = amount, status = "OWNED", categoryName = categoryName,
        targetPerson = targetPerson, targetAccount = targetAccount, note = note,
        isCompleted = false, attachments = emptyList(), createdAt = 1700000000000,
        updatedAt = 1700000000000, assetTypeId = assetTypeId
    )

    @Test
    fun automaticBackupChain_keepsCurrentBaseAndReverseDeltas() {
        backupManager.clearAllDeltaBackups()
        val first = listOf(Transaction(1, TransactionType.EXPENSE, 10.0, 1, null, "before"))
        val second = listOf(Transaction(1, TransactionType.EXPENSE, 20.0, 2, null, "after"))
        val third = listOf(Transaction(1, TransactionType.EXPENSE, 30.0, 3, null, "latest"))
        val categories = emptyMap<Long, String>()

        assertNotNull(backupManager.createBaseBackup(first, emptyList(), categories))
        assertTrue(backupManager.createDeltaBackup(first, emptyList(), second, emptyList(), categories, maxKeep = 2))
        assertTrue(backupManager.createDeltaBackup(second, emptyList(), third, emptyList(), categories, maxKeep = 1))

        val latest = backupManager.restoreToStep(0)
        assertTrue(latest.success)
        assertEquals("latest", latest.transactions.single().note)

        val previous = backupManager.restoreToStep(1)
        assertTrue(previous.success)
        assertEquals("after", previous.transactions.single().note)
        assertEquals(1, backupManager.getDeltaBackupSteps().size)
    }

    @Test
    fun automaticBackup_deltaStepRestoresHistoricalAttachmentFiles() {
        backupManager.clearAllDeltaBackups()
        val categories = emptyMap<Long, String>()
        val first = listOf(Transaction(1, TransactionType.EXPENSE, 10.0, 1, null, "before"))
        val second = listOf(Transaction(1, TransactionType.EXPENSE, 20.0, 2, null, "after"))
        val attachmentFile = File(context.cacheDir, "delta_attach_${System.currentTimeMillis()}.txt").apply { writeText("historical payload") }
        val previousAttachment = AttachmentData(
            id = "asset-1-receipt",
            ownerType = AttachmentOwnerType.ASSET.name,
            ownerId = 1,
            fileName = attachmentFile.name,
            archiveFileName = "asset-1-receipt_${attachmentFile.name}",
            mimeType = "text/plain",
            fileSize = attachmentFile.length(),
            sha256 = backupManager.getFileSha256(attachmentFile),
            createdAt = 1,
            filePath = attachmentFile.absolutePath
        )
        val previousAttachmentEntity = AttachmentEntity(
            id = previousAttachment.id,
            ownerType = AttachmentOwnerType.ASSET,
            ownerId = 1,
            fileName = attachmentFile.name,
            filePath = attachmentFile.absolutePath,
            mimeType = "text/plain",
            fileSize = attachmentFile.length(),
            sha256 = previousAttachment.sha256,
            createdAt = 1
        )

        assertNotNull(backupManager.createBaseBackup(first, emptyList(), categories))
        assertTrue(
            backupManager.createDeltaBackup(
                previousTransactions = first,
                previousAssets = emptyList(),
                currentTransactions = second,
                currentAssets = emptyList(),
                categoryMap = categories,
                previousAttachments = listOf(previousAttachment),
                attachments = listOf(previousAttachmentEntity)
            )
        )

        val previous = backupManager.restoreToStep(1)
        assertTrue(previous.success)
        assertEquals("before", previous.transactions.single().note)
        assertEquals("manifest attachment count", 1, previous.attachments.size)
        assertTrue(previous.attachmentFiles.containsKey("asset-1-receipt"))
        assertTrue(previous.attachmentFiles["asset-1-receipt"]!!.exists())
        assertTrue(backupManager.getFileSha256(previous.attachmentFiles["asset-1-receipt"]!!) == previousAttachment.sha256)
    }

    @Test
    fun roundtrip_emptyData() {
        val original = ZipBackupData(
            transactions = emptyList(),
            assets = emptyList(),
            assetTypes = emptyList(),
            budgets = emptyList(),
            version = 1
        )

        val zipFile = createTestZipFile(original, "empty")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(0, result.transactions.size)
        assertEquals(0, result.assets.size)
        assertEquals(0, result.assetTypes.size)
        assertEquals(0, result.budgets.size)
    }

    @Test
    fun roundtrip_transactionsOnly() {
        val original = ZipBackupData(
            transactions = listOf(
                TransactionData(1, 1700000000000, "Income", 5000.0, "Salary", "Monthly salary"),
                TransactionData(2, 1700100000000, "Expense", 200.0, "Food", "Lunch")
            ),
            assets = emptyList(),
            version = 1
        )

        val zipFile = createTestZipFile(original, "tx")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(2, result.transactions.size)
        assertEquals("Salary", result.transactions[0].categoryName)
        assertEquals(5000.0, result.transactions[0].amount, 0.01)
        assertEquals("Expense", result.transactions[1].type)
    }

    @Test
    fun roundtrip_assetsWithTypes() {
        val original = ZipBackupData(
            transactions = emptyList(),
            assets = listOf(
                assetData(id = 1, amount = 100000.0, categoryName = "House", assetTypeId = 1L, targetPerson = "Me", targetAccount = "My House", note = "Home")
            ),
            assetTypes = listOf(
                AssetTypeData(1, "Real Estate", 1700000000000, 1700000000000),
                AssetTypeData(2, "Virtual Assets", 1700000000000, 1700000000000)
            ),
            version = 1
        )

        val zipFile = createTestZipFile(original, "assets")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.assets.size)
        assertEquals(100000.0, result.assets[0].amount, 0.01)
        assertEquals(1L, result.assets[0].assetTypeId)
        assertEquals(2, result.assetTypes.size)
        assertEquals("Real Estate", result.assetTypes[0].name)
        assertEquals("Virtual Assets", result.assetTypes[1].name)
    }

    @Test
    fun roundtrip_budgets() {
        val original = ZipBackupData(
            transactions = emptyList(),
            assets = emptyList(),
            budgets = listOf(
                BudgetData("2025-01", "Food", 2000.0, 1700000000000, 1700000000000),
                BudgetData("2025-01", null, 10000.0, 1700000000000, 1700000000000)
            ),
            version = 1
        )

        val zipFile = createTestZipFile(original, "budgets")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(2, result.budgets.size)
        assertEquals("Food", result.budgets[0].categoryName)
        assertEquals(2000.0, result.budgets[0].amount, 0.01)
        assertNull(result.budgets[1].categoryName)
        assertEquals(10000.0, result.budgets[1].amount, 0.01)
    }

    @Test
    fun roundtrip_fullData() {
        val original = ZipBackupData(
            transactions = listOf(
                TransactionData(1, 1700000000000, "Income", 5000.0, "Salary", "Monthly salary")
            ),
            assets = listOf(
                assetData(id = 1, amount = 100000.0, categoryName = "House", assetTypeId = 1L, targetPerson = "Me", targetAccount = "My House", note = "Home")
            ),
            assetTypes = listOf(
                AssetTypeData(1, "Real Estate", 1700000000000, 1700000000000)
            ),
            budgets = listOf(
                BudgetData("2025-01", "Food", 2000.0, 1700000000000, 1700000000000)
            ),
            version = 1
        )

        val zipFile = createTestZipFile(original, "full")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.assets.size)
        assertEquals(1, result.assetTypes.size)
        assertEquals(1, result.budgets.size)
    }

    @Test
    fun archiveManifest_preservesCategoriesApprovalsAndAttachments() {
        val original = ZipBackupData(
            assetCategories = listOf(AssetCategoryData(10, "Hardware", "PHYSICAL", null, true, 1, 2), AssetCategoryData(11, "Laptop", "PHYSICAL", 10, false, 3, 4)),
            approvals = listOf(ApprovalData(id = 7, type = "PURCHASE_BUDGET", categoryName = "Office", assetCategoryId = 11, amount = 3000.0, purchaseDate = 1, reason = "Need", itemName = "ThinkPad", specification = "14-inch", quantity = 1.0, attachments = "[]", status = "PENDING", decisionNote = "", createdAt = 1, updatedAt = 2)),
            attachments = listOf(AttachmentData("approval-7", "APPROVAL", 7, "quote.pdf", "approval-7_quote.pdf", "application/pdf", 10, "hash", 5)),
            version = 2
        )
        val result = backupManager.readZipBackupFromFile(createTestZipFile(original, "extended_manifest"))
        assertTrue(result.success)
        assertEquals(2, result.assetCategories.size)
        assertEquals(10L, result.assetCategories[1].parentCategoryId)
        assertEquals("ThinkPad", result.approvals.single().itemName)
        assertEquals("APPROVAL", result.attachments.single().ownerType)
    }

    @Test
    fun billFiles_areExtractedWithMetadata() {
        val archiveName = "bill-1_receipt.pdf"
        val data = ZipBackupData(
            billFiles = listOf(BillFileData(
                id = "bill-1",
                fileName = "receipt.pdf",
                archiveFileName = archiveName,
                mimeType = "application/pdf",
                fileSize = 7L,
                sha256 = "deadbeef",
                createdAt = 1700000000000
            )),
            version = 1
        )
        val zipFile = createRawZipFile(
            listOf(
                "data.json" to json.encodeToString(data).toByteArray(Charsets.UTF_8),
                "bills/$archiveName" to "payload".toByteArray()
            ),
            "bill_files"
        )
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.billFiles.size)
        assertEquals(archiveName, result.billFiles.single().archiveFileName)
        assertTrue(result.billArchiveFiles[archiveName]!!.exists())
    }

    @Test
    fun attachmentFiles_withUnderscoreIdsAreMappedFromManifest() {
        val archiveName = "ASSET_12_quote-1_quote.pdf"
        val data = ZipBackupData(
            attachments = listOf(
                AttachmentData(
                    id = "ASSET_12_quote-1",
                    ownerType = "ASSET",
                    ownerId = 12,
                    fileName = "quote.pdf",
                    archiveFileName = archiveName,
                    mimeType = "application/pdf",
                    fileSize = 7,
                    sha256 = "hash",
                    createdAt = 1
                )
            ),
            version = 2
        )
        val zipFile = createRawZipFile(
            listOf(
                "data.json" to json.encodeToString(data).toByteArray(Charsets.UTF_8),
                "attachments/$archiveName" to "payload".toByteArray()
            ),
            "attachment_underscore_ids"
        )

        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertTrue(result.attachmentFiles.containsKey("ASSET_12_quote-1"))
        assertTrue(result.attachmentFiles["ASSET_12_quote-1"]!!.exists())
    }

    @Test
    fun transactionAttachments_areIncludedInManifestAndFiles() {
        val attachmentFile = File(context.cacheDir, "tx_receipt_${System.currentTimeMillis()}.txt").apply { writeText("receipt") }
        val attachment = Attachment(
            id = "tx_attach_1",
            fileName = "receipt.txt",
            filePath = attachmentFile.absolutePath,
            fileType = AttachmentType.TEXT,
            fileSize = attachmentFile.length(),
            mimeType = "text/plain",
            createdAt = 1
        )
        val transaction = Transaction(
            id = 77,
            type = TransactionType.EXPENSE,
            amount = 12.0,
            date = 1,
            categoryId = null,
            note = "with attachment",
            attachments = AttachmentConverter.toJson(listOf(attachment))
        )
        val zipFile = File(context.cacheDir, "tx_attachment_${System.currentTimeMillis()}.zip")

        assertTrue(
            backupManager.exportZipToFile(
                file = zipFile,
                transactions = listOf(transaction),
                assets = emptyList(),
                categoryMap = emptyMap()
            )
        )

        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals("with attachment", result.transactions.single().note)
        assertEquals("receipt.txt", result.transactions.single().attachments.single().fileName)
        assertTrue(result.attachmentFiles.containsKey("tx_attach_1"))
    }

    @Test
    fun oldFormat_noAssetTypesField_parsesWithDefaults() {

        val oldJson = """
            {
                "transactions": [{"id":1,"date":1700000000000,"type":"Income","amount":5000.0,"categoryName":"Salary","note":"test"}],
                "assets": [{"id":1,"date":1700000000000,"amount":100.0,"status":"OWNED","categoryName":null,"targetPerson":"","targetAccount":"","note":"","isCompleted":false,"attachments":[],"createdAt":1700000000000,"updatedAt":1700000000000}],
                "version": 1
            }
        """.trimIndent()

        val zipFile = createRawZipFile(listOf("data.json" to oldJson.toByteArray(Charsets.UTF_8)), "old_format")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.assets.size)
        assertEquals(2L, result.assets[0].assetTypeId)
        assertEquals(0, result.assetTypes.size)
    }

    @Test
    fun oldFormat_noBudgetsField_parsesWithDefaults() {
        val oldJson = """
            {
                "transactions": [],
                "assets": [],
                "version": 1
            }
        """.trimIndent()

        val zipFile = createRawZipFile(listOf("data.json" to oldJson.toByteArray(Charsets.UTF_8)), "old_no_budgets")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(0, result.budgets.size)
    }

    @Test
    fun invalidZip_returnsFailure() {
        val tempFile = File(context.cacheDir, "invalid_${System.currentTimeMillis()}.zip")
        tempFile.writeText("this is not a zip file")
        val result = backupManager.readZipBackupFromFile(tempFile)

        assertFalse(result.success)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun zipMissingDataJson_returnsEmpty() {
        val zipFile = createRawZipFile(listOf("other_file.txt" to "hello".toByteArray()), "no_data_json")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.assets.isEmpty())
    }

    @Test
    fun chineseCharacters_roundtripCorrectly() {
        val original = ZipBackupData(
            transactions = listOf(
                TransactionData(1, 1700000000000, "Income", 5000.0, "工资", "测试中文")
            ),
            assets = listOf(
                assetData(id = 1, amount = 100000.0, categoryName = "房子", assetTypeId = 1L, targetPerson = "张三", targetAccount = "我的账户", note = "备注")
            ),
            assetTypes = listOf(
                AssetTypeData(1, "实物资产", 1700000000000, 1700000000000)
            ),
            version = 1
        )

        val zipFile = createTestZipFile(original, "chinese")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals("工资", result.transactions[0].categoryName)
        assertEquals("测试中文", result.transactions[0].note)
        assertEquals("房子", result.assets[0].categoryName)
        assertEquals("张三", result.assets[0].targetPerson)
        assertEquals("实物资产", result.assetTypes[0].name)
    }

    @Test
    fun largeDataset_roundtripCorrectly() {
        val transactions = (1L..100L).map { i ->
            TransactionData(i, 1700000000000 + i * 86400000, if (i % 2 == 0L) "Income" else "Expense", i * 100.0, "Category$i", "Note$i")
        }
        val assets = (1L..50L).map { i ->
            assetData(id = i, date = 1700000000000 + i * 86400000, amount = i * 1000.0, categoryName = "Type${i % 3}", assetTypeId = i % 3 + 1, targetPerson = "Person$i", targetAccount = "Account$i", note = "Asset$i")
        }

        val original = ZipBackupData(
            transactions = transactions,
            assets = assets,
            assetTypes = listOf(AssetTypeData(1, "Type1", 1700000000000, 1700000000000), AssetTypeData(2, "Type2", 1700000000000, 1700000000000), AssetTypeData(3, "Type3", 1700000000000, 1700000000000)),
            version = 1
        )

        val zipFile = createTestZipFile(original, "large")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(100, result.transactions.size)
        assertEquals(50, result.assets.size)
        assertEquals(3, result.assetTypes.size)
    }

    @Test
    fun oldFormat_assetWithNoAssetTypeId_defaultsToVirtualType() {
        val oldJson = """
            {
                "transactions": [],
                "assets": [{"id":1,"date":1700000000000,"amount":500.0,"status":"OWNED","categoryName":"House","targetPerson":"","targetAccount":"","note":"","isCompleted":false,"attachments":[],"createdAt":1700000000000,"updatedAt":1700000000000}],
                "version": 1
            }
        """.trimIndent()

        val zipFile = createRawZipFile(listOf("data.json" to oldJson.toByteArray(Charsets.UTF_8)), "old_no_assetTypeId")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.assets.size)
        assertEquals(2L, result.assets[0].assetTypeId)
    }

    @Test
    fun oldFormat_noBudgetsField_parsesEmptyBudgets() {
        val oldJson = """
            {
                "transactions": [],
                "assets": [],
                "version": 1
            }
        """.trimIndent()

        val zipFile = createRawZipFile(listOf("data.json" to oldJson.toByteArray(Charsets.UTF_8)), "old_empty_budgets")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(0, result.budgets.size)
    }

    @Test
    fun attachmentFiles_areExtractedFromZip() {
        val attachmentContent = "fake image content".toByteArray()
        val tempFile = File(context.cacheDir, "attach_test_${System.currentTimeMillis()}.zip")
        ZipOutputStream(tempFile.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(json.encodeToString(ZipBackupData(
                transactions = listOf(TransactionData(1, 1700000000000, "Income", 100.0, "Salary", "test")),
                assets = emptyList(),
                version = 1
            )).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("attachments/123_receipt.jpg"))
            zipOut.write(attachmentContent)
            zipOut.closeEntry()
        }

        val result = backupManager.readZipBackupFromFile(tempFile)

        assertTrue(result.success)
        assertEquals(1, result.attachmentFiles.size)
        assertTrue(result.attachmentFiles.containsKey("123"))
        assertTrue(result.attachmentFiles["123"]!!.exists())
    }

    @Test
    fun mixedOldAndNewData_roundtripCorrectly() {
        val oldJson = """
            {
                "transactions": [{"id":1,"date":1700000000000,"type":"Income","amount":5000.0,"categoryName":"Salary","note":"test"}],
                "assets": [{"id":1,"date":1700000000000,"amount":100.0,"status":"OWNED","categoryName":null,"targetPerson":"","targetAccount":"","note":"","isCompleted":false,"attachments":[],"createdAt":1700000000000,"updatedAt":1700000000000}],
                "version": 1
            }
        """.trimIndent()

        val zipFile = createRawZipFile(listOf("data.json" to oldJson.toByteArray(Charsets.UTF_8)), "mixed_old_new")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.assets.size)
        assertEquals(2L, result.assets[0].assetTypeId)
        assertEquals(0, result.assetTypes.size)
        assertEquals(0, result.budgets.size)
    }

    @Test
    fun newFormat_withAllFields_roundtripCorrectly() {
        val original = ZipBackupData(
            transactions = listOf(TransactionData(1, 1700000000000, "Expense", 100.0, "Food", "lunch")),
            assets = listOf(assetData(id = 1, amount = 50000.0, categoryName = "Savings", assetTypeId = 1L, targetPerson = "Me", targetAccount = "Bank", note = "note")),
            assetTypes = listOf(AssetTypeData(1, "Real", 1700000000000, 1700000000000)),
            budgets = listOf(BudgetData("2025-06", "Food", 500.0, 1700000000000, 1700000000000)),
            version = 1
        )

        val zipFile = createTestZipFile(original, "new_full")
        val result = backupManager.readZipBackupFromFile(zipFile)

        assertTrue(result.success)
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.assets.size)
        assertEquals(1, result.assetTypes.size)
        assertEquals(1, result.budgets.size)
        assertEquals("Food", result.budgets[0].categoryName)
    }

    @Test
    fun independentAttachment_roundtripsWithRealFile() {
        val content = "independent attachment payload"
        val source = File(context.cacheDir, "independent_${System.currentTimeMillis()}.txt").apply { writeText(content) }
        val entity = AttachmentEntity(
            id = "asset-77-receipt",
            ownerType = AttachmentOwnerType.ASSET,
            ownerId = 77,
            fileName = source.name,
            filePath = source.absolutePath,
            mimeType = "text/plain",
            fileSize = source.length(),
            sha256 = backupManager.getFileSha256(source),
            createdAt = 10
        )
        val zip = File(context.cacheDir, "independent_zip_${System.currentTimeMillis()}.zip")
        assertTrue(
            backupManager.exportZipToFile(
                file = zip,
                transactions = emptyList(),
                assets = emptyList(),
                categoryMap = emptyMap(),
                attachments = listOf(entity)
            )
        )
        val result = backupManager.readZipBackupFromFile(zip)
        assertTrue(result.success)
        assertEquals("asset-77-receipt", result.attachments.single().id)
        val restored = result.attachmentFiles["asset-77-receipt"]
        assertNotNull(restored)
        assertEquals(backupManager.getFileSha256(restored!!), entity.sha256)
        zip.delete()
        source.delete()
    }

    @Test
    fun duplicatedLegacyAndIndependentAttachment_writesSingleArchiveEntry() {
        val content = "shared payload"
        val source = File(context.cacheDir, "shared_${System.currentTimeMillis()}.txt").apply { writeText(content) }
        val legacy = Attachment(
            id = "shared-1",
            fileName = source.name,
            filePath = source.absolutePath,
            fileType = AttachmentType.TEXT,
            fileSize = source.length(),
            mimeType = "text/plain",
            createdAt = 1
        )
        val asset = Asset(
            id = 3,
            date = 1,
            amount = 10.0,
            status = AssetStatus.OWNED,
            name = "Desk",
            targetPerson = "",
            targetAccount = "",
            note = "",
            attachments = AttachmentConverter.toJson(listOf(legacy))
        )
        val entity = AttachmentEntity(
            id = "shared-1",
            ownerType = AttachmentOwnerType.ASSET,
            ownerId = 3,
            fileName = source.name,
            filePath = source.absolutePath,
            mimeType = "text/plain",
            fileSize = source.length(),
            sha256 = backupManager.getFileSha256(source),
            createdAt = 1
        )
        val zip = File(context.cacheDir, "duplicate_zip_${System.currentTimeMillis()}.zip")
        assertTrue(
            backupManager.exportZipToFile(zip, emptyList(), listOf(asset), emptyMap(), attachments = listOf(entity))
        )
        ZipFile(zip).use { zipFile ->
            val attachmentEntries = zipFile.entries().asSequence().filter { it.name.startsWith("attachments/") }.count()
            assertEquals(1, attachmentEntries)
        }
        zip.delete()
        source.delete()
    }

    @Test
    fun restoreCopies_areTrackedForRollback() {
        val source = File(context.cacheDir, "rollback_${System.currentTimeMillis()}.txt").apply { writeText("payload") }
        val created = mutableListOf<File>()
        val copied = backupManager.copyAttachmentToInternalStorage("rollback-id", source, "rollback.txt", created)
        assertNotNull(copied)
        assertTrue(created.contains(File(copied!!.filePath)))
        val billSource = File(context.cacheDir, "rollback_bill_${System.currentTimeMillis()}.csv").apply { writeText("bill") }
        val billDest = backupManager.copyBillFileToInternalStorage(billSource, "bill.csv", created)
        assertNotNull(billDest)
        assertTrue(created.contains(billDest!!))
        created.filter { it.exists() }.forEach { it.delete() }
    }
}
