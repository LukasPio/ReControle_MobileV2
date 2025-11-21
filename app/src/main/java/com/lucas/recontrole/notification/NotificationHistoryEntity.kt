package com.lucas.recontrole.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey val id: String, // occurrence_id + timestamp para evitar duplicatas
    val occurrenceId: String,
    val oldStatus: String,
    val newStatus: String,
    val notifiedAt: Long,
    val category: String,
    val local: String
)