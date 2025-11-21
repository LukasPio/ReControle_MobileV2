package com.lucas.recontrole.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey val occurrenceId: String,
    val lastNotifiedStatus: String, // "red", "yellow", "green"
    val timestamp: Long
)