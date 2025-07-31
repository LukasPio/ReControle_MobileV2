package com.lucas.recontrole.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.recontrole.Status

@Composable
fun OccurrenceCard(
    title: String,
    local: String,
    status: Status,
    image: Bitmap
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
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "Photo of an occurrence",
        )
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

private fun base64ToBitmap(base64String: String): Bitmap {
    val decodeBytes = Base64.decode(base64String, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.size)
}