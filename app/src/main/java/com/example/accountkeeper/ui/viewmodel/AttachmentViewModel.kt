package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.repository.AttachmentRepository
import com.example.accountkeeper.data.repository.BillFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AttachmentViewModel @Inject constructor(
    private val repository: AttachmentRepository,
    private val billFileRepository: BillFileRepository
) : ViewModel() {
    val attachments: StateFlow<List<AttachmentEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(attachment: AttachmentEntity) = viewModelScope.launch {
        val file = File(attachment.filePath)
        if (file.exists() && repository.countByFilePath(attachment.filePath) <= 1) file.delete()
        if (attachment.ownerType == AttachmentOwnerType.BILL && attachment.id.startsWith("BILL_")) {
            billFileRepository.deleteById(attachment.id.removePrefix("BILL_"))
        }
        repository.delete(attachment)
    }
}
