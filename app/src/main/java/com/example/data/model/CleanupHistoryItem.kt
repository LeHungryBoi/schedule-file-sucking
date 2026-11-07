package com.example.data.model

data class CleanupHistoryItem(
    val id: String,
    val timestamp: Long,
    val filesDeleted: Int,
    val bytesReclaimed: Long,
    val isAutomated: Boolean
)
