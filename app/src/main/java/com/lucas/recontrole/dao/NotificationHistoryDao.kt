package com.lucas.recontrole.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucas.recontrole.notification.NotificationHistoryEntity

@Dao
interface NotificationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationHistoryEntity)

    @Query("SELECT * FROM notification_history WHERE occurrenceId = :occurrenceId ORDER BY notifiedAt DESC LIMIT 1")
    suspend fun getLastNotificationForOccurrence(occurrenceId: String): NotificationHistoryEntity?

    @Query("DELETE FROM notification_history WHERE notifiedAt < :timestamp")
    suspend fun deleteOldNotifications(timestamp: Long)

    @Query("SELECT COUNT(*) FROM notification_history WHERE occurrenceId = :occurrenceId AND newStatus = :newStatus")
    suspend fun countNotificationsWithStatus(occurrenceId: String, newStatus: String): Int
}