package com.example.netdisk.ui.files

data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String? = null,
    val extension: String? = null,
    val updatedAt: String
)

enum class FileType {
    FILE,
    FOLDER
}
