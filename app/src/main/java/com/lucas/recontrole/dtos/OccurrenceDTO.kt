package com.lucas.recontrole.dtos

import android.graphics.Bitmap
import com.lucas.recontrole.Status

data class OccurrenceDTO(
    val id: String = "",
    val description: String = "",
    val status: Status = Status.PENDENT,
    val imgBase64: String = "",
    val author: String = "",
    val local: String = "",
    val category: String = ""
)
