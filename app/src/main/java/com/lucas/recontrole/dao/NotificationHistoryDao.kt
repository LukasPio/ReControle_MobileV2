package com.lucas.recontrole.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucas.recontrole.notification.NotificationHistoryEntity

@Dao
interface NotificationHistoryDao {

    @Query("SELECT * FROM notification_history WHERE occurrenceId = :occurrenceId LIMIT 1")
    suspend fun findByOccurrenceId(occurrenceId: String): NotificationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationHistoryEntity)

    @Query("DELETE FROM notification_history WHERE occurrenceId = :occurrenceId")
    suspend fun delete(occurrenceId: String)
}