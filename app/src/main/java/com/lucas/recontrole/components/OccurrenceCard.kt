package com.lucas.recontrole.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucas.recontrole.Status
import com.lucas.recontrole.dtos.OccurrenceDTO

@Composable
fun OccurrenceCard(
    occurrenceDTO: OccurrenceDTO,
    image: Bitmap,
    onClick: (OccurrenceDTO, Bitmap) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(0.9f)
            .height(130.dp)
            .clickable {
                onClick(occurrenceDTO, image)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // SEMPRE exibir a categoria como título
                Text(
                    text = occurrenceDTO.category.ifEmpty {
                        "Outro"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                // Exibir local e status
                Text(
                    text = occurrenceDTO.local + " - " + when(occurrenceDTO.status) {
                        Status.PENDENT -> "Pendente"
                        Status.ON_PROGRESS -> "Em andamento"
                        Status.FINISHED -> "Concluído"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Ver mais...",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Photo of an occurrence",
                modifier = Modifier
                    .height(100.dp)
                    .width(100.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}