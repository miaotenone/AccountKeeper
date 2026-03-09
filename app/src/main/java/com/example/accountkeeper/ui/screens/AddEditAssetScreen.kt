package com.example.accountkeeper.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentType
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.AppStrings
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// Premium gradient colors
private val LightGradientOwned = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
private val DarkGradientOwned = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
private val LightGradientNotOwned = listOf(Color(0xFF9E9E9E), Color(0xFF616161))
private val DarkGradientNotOwned = listOf(Color(0xFFBDBDBD), Color(0xFF757575))
private val LightGradientInProgress = listOf(Color(0xFFFFC107), Color(0xFFFF8F00))
private val DarkGradientInProgress = listOf(Color(0xFFFFCA28), Color(0xFFFFB300))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    assetId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: AssetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrencySymbol.current
    val categories by categoryViewModel.categories.collectAsState()
    
    val isEditMode = assetId != -1L
    val existingAsset = remember { mutableStateOf<Asset?>(null) }

    // Form state
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedStatus by remember { mutableStateOf(AssetStatus.NONE) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var targetPerson by remember { mutableStateOf("") }
    var targetAccount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }

    // UI state
    var showDatePicker by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    // Attachments state
    val context = LocalContext.current
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Copy file to app's internal storage
            val newAttachment = copyFileToInternalStorage(context, selectedUri)
            if (newAttachment != null) {
                attachments = attachments + newAttachment
            }
        }
    }

    // Load existing asset if in edit mode
    LaunchedEffect(assetId) {
        if (isEditMode) {
            val asset = viewModel.getAssetById(assetId)
            if (asset != null) {
                existingAsset.value = asset
                amount = asset.amount.toString()
                selectedDate = asset.date
                selectedStatus = asset.status
                selectedCategoryId = asset.categoryId
                targetPerson = asset.targetPerson
                targetAccount = asset.targetAccount
                note = asset.note
                isCompleted = asset.isCompleted
                // Load existing attachments
                attachments = AttachmentConverter.fromJson(asset.attachments)
            }
        }
    }

    val assetCategories = categories.filter { it.type == TransactionType.ASSET }
    val selectedCategory = assetCategories.find { it.id == selectedCategoryId }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Status colors and labels
    val statusColor = when (selectedStatus) {
        AssetStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        AssetStatus.OWNED -> Color(0xFF4CAF50)
        AssetStatus.NOT_OWNED -> Color(0xFF9E9E9E)
        AssetStatus.IN_PROGRESS -> Color(0xFFFFC107)
        AssetStatus.TEMPORARILY_WITH_ME -> Color(0xFFFFC107)
        AssetStatus.TEMPORARILY_WITH_OTHERS -> Color(0xFFFFC107)
    }
    
    val statusLabel = when (selectedStatus) {
        AssetStatus.NONE -> strings.none
        AssetStatus.OWNED -> strings.owned
        AssetStatus.NOT_OWNED -> strings.notOwned
        AssetStatus.IN_PROGRESS -> strings.inProgress
        AssetStatus.TEMPORARILY_WITH_ME -> strings.temporarilyWithMe
        AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.temporarilyWithOthers
    }

    // Flow status for top right display
    val flowStatus: String? = when (selectedStatus) {
        AssetStatus.OWNED, AssetStatus.NOT_OWNED -> strings.assetFlowCompleted
        AssetStatus.IN_PROGRESS -> strings.assetFlowInProgress
        AssetStatus.TEMPORARILY_WITH_ME, AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.assetFlowInProgress
        AssetStatus.NONE -> null
    }

    val flowStatusColor = when (selectedStatus) {
        AssetStatus.OWNED, AssetStatus.NOT_OWNED -> Color(0xFF4CAF50)
        AssetStatus.IN_PROGRESS -> Color(0xFFFF9800)
        AssetStatus.TEMPORARILY_WITH_ME, AssetStatus.TEMPORARILY_WITH_OTHERS -> Color(0xFFFF9800)
        AssetStatus.NONE -> Color.Transparent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) strings.editAsset else strings.addAsset,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // Flow status badge in top right
                    if (flowStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = flowStatusColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(flowStatusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    flowStatus,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = flowStatusColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Status Selector with Premium Design
            PremiumAssetStatusSelector(
                selectedStatus = selectedStatus,
                isPositiveCategory = selectedCategory?.isPositiveAsset ?: true,
                onStatusSelected = { selectedStatus = it },
                strings = strings
            )

            // Date Selection with Premium Design
            PremiumAssetDateSelector(
                date = selectedDate,
                onClick = { showDatePicker = true },
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            // Amount Input with Premium Design
            PremiumAssetAmountInput(
                amountText = amount,
                onAmountChange = { 
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = it
                        amountError = null
                    }
                },
                currency = currency,
                strings = strings,
                isError = amountError != null,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection
            Text(
                strings.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    onClick = { categoryExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                strings.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedCategory?.name ?: strings.selectCategory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedCategory != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedCategory != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (selectedCategory.isPositiveAsset)
                                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else
                                            Color(0xFFE53935).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            if (selectedCategory.isPositiveAsset) strings.positiveAsset 
                                            else strings.negativeAsset,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedCategory.isPositiveAsset) 
                                                Color(0xFF4CAF50) 
                                            else 
                                                Color(0xFFE53935),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    if (assetCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "请先在设置中添加资产分类",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = { categoryExpanded = false }
                        )
                    } else {
                        assetCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            category.name,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (category.isPositiveAsset)
                                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            else
                                                Color(0xFFE53935).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                if (category.isPositiveAsset) strings.positiveAsset 
                                                else strings.negativeAsset,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (category.isPositiveAsset) 
                                                    Color(0xFF4CAF50) 
                                                else 
                                                    Color(0xFFE53935),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedCategoryId == category.id) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Additional Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetPerson,
                        onValueChange = { targetPerson = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.targetPerson) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = targetAccount,
                        onValueChange = { targetAccount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.targetAccount) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.note) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Attachments Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.attachments,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("image/*", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv"))
                            }
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = strings.addAttachment,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (attachments.isEmpty()) {
                        Text(
                            strings.noAttachments,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            attachments.forEach { attachment ->
                                AttachmentItem(
                                    attachment = attachment,
                                    onRemove = {
                                        attachments = attachments.filter { it.id != attachment.id }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button with Premium Design
            PremiumAssetSaveButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        amountError = "请输入有效金额"
                        return@PremiumAssetSaveButton
                    }
                    if (selectedCategoryId == null) {
                        return@PremiumAssetSaveButton
                    }

                    val now = System.currentTimeMillis()
                    val asset = Asset(
                        id = if (isEditMode) assetId else now,
                        date = selectedDate,
                        amount = amountValue,
                        status = selectedStatus,
                        categoryId = selectedCategoryId,
                        targetPerson = targetPerson,
                        targetAccount = targetAccount,
                        note = note,
                        isCompleted = isCompleted,
                        attachments = AttachmentConverter.toJson(attachments),
                        createdAt = existingAsset.value?.createdAt ?: now,
                        updatedAt = now
                    )

                    if (isEditMode) {
                        viewModel.updateAsset(asset)
                    } else {
                        viewModel.addAsset(asset)
                    }
                    onNavigateBack()
                },
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDate = it
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text(strings.ok)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(strings.cancel)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Status Picker Dialog
        if (showStatusPicker) {
            val isPositiveCategory = selectedCategory?.isPositiveAsset ?: true
            
            AlertDialog(
                onDismissRequest = { showStatusPicker = false },
                title = { Text(strings.assetStatus, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusOption(
                            label = strings.none,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            isSelected = selectedStatus == AssetStatus.NONE,
                            onClick = {
                                selectedStatus = AssetStatus.NONE
                                showStatusPicker = false
                            }
                        )
                        // Show status options based on category type
                        if (isPositiveCategory) {
                            // Positive asset: OWNED and IN_PROGRESS
                            StatusOption(
                                label = strings.owned,
                                color = Color(0xFF4CAF50),
                                isSelected = selectedStatus == AssetStatus.OWNED,
                                onClick = {
                                    selectedStatus = AssetStatus.OWNED
                                    showStatusPicker = false
                                }
                            )
                        } else {
                            // Negative asset: NOT_OWNED and IN_PROGRESS
                            StatusOption(
                                label = strings.notOwned,
                                color = Color(0xFF9E9E9E),
                                isSelected = selectedStatus == AssetStatus.NOT_OWNED,
                                onClick = {
                                    selectedStatus = AssetStatus.NOT_OWNED
                                    showStatusPicker = false
                                }
                            )
                        }
                        StatusOption(
                            label = strings.inProgress,
                            color = Color(0xFFFFC107),
                            isSelected = selectedStatus == AssetStatus.IN_PROGRESS,
                            onClick = {
                                selectedStatus = AssetStatus.IN_PROGRESS
                                showStatusPicker = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatusPicker = false }) {
                        Text(strings.ok)
                    }
                }
            )
        }
    }
}

/**
 * Copy file from Uri to app's internal storage
 */
private fun copyFileToInternalStorage(context: Context, uri: Uri): Attachment? {
    return try {
        val contentResolver = context.contentResolver
        
        // Get file info
        val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val attachmentType = Attachment.getTypeFromMimeType(mimeType)
        
        // Create unique file name
        val extension = fileName.substringAfterLast(".", "")
        val newFileName = "${System.currentTimeMillis()}_$fileName"
        
        // Create attachments directory
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        
        // Copy file
        val newFile = File(attachmentsDir, newFileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(newFile).use { output ->
                input.copyTo(output)
            }
        }
        
        Attachment(
            id = System.currentTimeMillis().toString(),
            fileName = fileName,
            filePath = newFile.absolutePath,
            fileType = attachmentType,
            fileSize = newFile.length(),
            mimeType = mimeType
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Get file name from Uri
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

@Composable
fun SettingsRow(
    icon: ImageVector?,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showBadge: Boolean = false,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBadge && badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AttachmentItem(
    attachment: Attachment,
    onRemove: () -> Unit
) {
    val icon = when (attachment.fileType) {
        AttachmentType.IMAGE -> Icons.Default.Image
        AttachmentType.EXCEL, AttachmentType.CSV -> Icons.Default.InsertDriveFile
        else -> Icons.Default.AttachFile
    }
    
    val fileSizeText = formatFileSize(attachment.fileSize)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    fileSizeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Format file size to human readable string
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

@Composable
fun StatusOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, CircleShape)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ==================== Premium Style Components ====================

@Composable
fun PremiumAssetStatusSelector(
    selectedStatus: AssetStatus,
    isPositiveCategory: Boolean,
    onStatusSelected: (AssetStatus) -> Unit,
    strings: AppStrings
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // None option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selectedStatus == AssetStatus.NONE) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            )
                        } else Modifier
                    )
                    .clickable { onStatusSelected(AssetStatus.NONE) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    strings.`none`,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedStatus == AssetStatus.NONE) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedStatus == AssetStatus.NONE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Owned/NotOwned option based on category type
            val statusOption = if (isPositiveCategory) AssetStatus.OWNED to strings.owned else AssetStatus.NOT_OWNED to strings.notOwned
            val statusGradient = if (isPositiveCategory) {
                if (isSystemInDarkTheme()) DarkGradientOwned else LightGradientOwned
            } else {
                if (isSystemInDarkTheme()) DarkGradientNotOwned else LightGradientNotOwned
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selectedStatus == statusOption.first) {
                            Modifier.background(Brush.horizontalGradient(statusGradient))
                        } else Modifier
                    )
                    .clickable { onStatusSelected(statusOption.first) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    statusOption.second,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedStatus == statusOption.first) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedStatus == statusOption.first) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // In Progress option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selectedStatus == AssetStatus.IN_PROGRESS) {
                            Modifier.background(
                                if (isSystemInDarkTheme()) Brush.horizontalGradient(DarkGradientInProgress)
                                else Brush.horizontalGradient(LightGradientInProgress)
                            )
                        } else Modifier
                    )
                    .clickable { onStatusSelected(AssetStatus.IN_PROGRESS) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    strings.inProgress,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedStatus == AssetStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedStatus == AssetStatus.IN_PROGRESS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PremiumAssetDateSelector(
    date: Long,
    onClick: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    strings.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    dateFormatter.format(Date(date)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PremiumAssetAmountInput(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    strings: AppStrings,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val cardElevation by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 4.dp,
        label = "cardElevation"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (isError) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                strings.amount,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currency,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(
                            "0.00",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ) 
                    },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    interactionSource = interactionSource,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}

@Composable
fun PremiumAssetSaveButton(
    onClick: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Text(
            strings.save,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
