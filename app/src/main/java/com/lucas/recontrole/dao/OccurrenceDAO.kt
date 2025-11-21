package com.lucas.recontrole.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucas.recontrole.model.OccurrenceEntity

@Dao
interface OccurrenceDao {
    @Query("SELECT * FROM occurrences")
    suspend fun getAll(): List<OccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(occurrences: List<OccurrenceEntity>)

    @Query("DELETE FROM occurrences")
    suspend fun clearAll()
    @Query("DELETE FROM occurrences WHERE id = :id")
    fun deleteById(id: String)
    @Query("SELECT * FROM occurrences WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OccurrenceEntity?
}
