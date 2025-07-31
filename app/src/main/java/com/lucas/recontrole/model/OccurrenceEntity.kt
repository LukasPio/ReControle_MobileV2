package com.lucas.recontrole.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "occurrences")
data class OccurrenceEntity(
    @PrimaryKey val id: String,
    val description: String,
    val status: String,
    val imgBase64: String,
    val local: String,
    val author: String

)

