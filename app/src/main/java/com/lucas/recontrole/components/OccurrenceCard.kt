package com.lucas.recontrole.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.recontrole.Status

@Composable
fun OccurrenceCard(
    title: String,
    local: String,
    status: Status,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.padding(8.dp)
            .fillMaxWidth(0.9f)
            .height(200.dp),
    ) {
        Text(
            text = title,
            fontSize = 20.sp
        )
        Text(
            text = local
        )
        Text(
            text = when(status) {
                Status.PENDENT -> "Pendente"
                Status.ON_PROGRESS -> "Em andamento"
                Status.FINISHED -> "Concluído"
            }
        )
    }
}