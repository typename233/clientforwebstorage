package com.example.clientforwebstorage.ui.files

data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val size: Long = 0,
    val extension: String? = null,
    val updatedAt: String
)

enum class FileType {
    FILE,
    FOLDER
}
