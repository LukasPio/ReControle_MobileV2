package com.lucas.recontrole.dtos

import com.lucas.recontrole.Status

data class OccurrenceDTO(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: Status = Status.PENDENT,
    val imgUrl: String = "",
    val author: String = ""
)
