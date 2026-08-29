package com.example.accountkeeper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.accountkeeper.utils.BackupManager
import com.example.accountkeeper.utils.ZipBackupData
import com.example.accountkeeper.utils.AssetData
import com.example.accountkeeper.utils.AssetTypeData
import com.example.accountkeeper.utils.BudgetData
import com.example.accountkeeper.utils.TransactionData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
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
                AssetData(1, 1700000000000, 100000.0, "OWNED", "House", 1L, "Me", "My House", "Home", false, emptyList(), 1700000000000, 1700000000000)
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
                AssetData(1, 1700000000000, 100000.0, "OWNED", "House", 1L, "Me", "My House", "Home", false, emptyList(), 1700000000000, 1700000000000)
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
                AssetData(1, 1700000000000, 100000.0, "OWNED", "房子", 1L, "张三", "我的账户", "备注", false, emptyList(), 1700000000000, 1700000000000)
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
            AssetData(i, 1700000000000 + i * 86400000, i * 1000.0, "OWNED", "Type${i % 3}", i % 3 + 1, "Person$i", "Account$i", "Asset$i", false, emptyList(), 1700000000000, 1700000000000)
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
            assets = listOf(AssetData(1, 1700000000000, 50000.0, "OWNED", "Savings", 1L, "Me", "Bank", "note", false, emptyList(), 1700000000000, 1700000000000)),
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
}
