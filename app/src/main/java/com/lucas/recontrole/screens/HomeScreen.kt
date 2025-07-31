package com.lucas.recontrole.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lucas.recontrole.Constants
import com.lucas.recontrole.R
import com.lucas.recontrole.Status
import com.lucas.recontrole.components.AppTopBar
import com.lucas.recontrole.components.ErrorDialog
import com.lucas.recontrole.components.FullScreenModal
import com.lucas.recontrole.components.GenericInputField
import com.lucas.recontrole.components.OccurrenceCard
import com.lucas.recontrole.components.PhotoModal
import com.lucas.recontrole.components.SubmitButton
import com.lucas.recontrole.dtos.OccurrenceDTO

@Composable
fun HomeScreen(navController: NavController) {
    var occurrencesState by remember { mutableStateOf<List<OccurrenceDTO>>(emptyList()) }
    var finishedLoading by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }

    var shouldShowDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }

    var occurrencePhotoBase64 by remember { mutableStateOf("") }
    var occurrenceLocal by remember { mutableStateOf("") }
    var occurrenceDescription by remember { mutableStateOf("") }

    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(reloadTrigger) {
        getOccurrences { occurrences ->
            occurrencesState = occurrences
            finishedLoading = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                AppTopBar(
                    title = "Tickets",
                    navController = navController
                )
            }

            item {
                if (!finishedLoading) {
                    Text(
                        "Carregando tickets...",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 50.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(top = 40.dp)
                    )
                }
            }

            if (finishedLoading && occurrencesState.isEmpty()) {
                item {
                    Text(
                        "Parece que você ainda não abriu nenhum ticket!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 50.dp, start = 8.dp, end = 8.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(occurrencesState) { occurrence ->
                val bitmap = remember(occurrence.imgBase64) {
                    base64ToBitmap(occurrence.imgBase64)
                }

                if (bitmap == null) {
                    Log.d("RUNTIME", "Image of ${occurrence.id} is null")
                    return@items
                }

                OccurrenceCard(
                    title = occurrence.title,
                    local = occurrence.local,
                    status = occurrence.status,
                    image = bitmap
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Image(
            painter = painterResource(R.drawable.add_circle_24px),
            contentDescription = "Add circle icon",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(110.dp)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(radius = 50.dp)
                ) {
                    showModal = true
                }
        )

        if (shouldShowDialog) {
            ErrorDialog(
                {shouldShowDialog = false},
                {shouldShowDialog = false},
                dialogText
            )
        }

        if (showModal) {
            FullScreenModal(
                onDismiss = {
                    showModal = false
                },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 600.dp)
                    ) {
                        Text(
                            text = "Insira as informações:",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        GenericInputField(
                            title = "Local",
                            placeholder = "Insira o local da ocorrência",
                            text = occurrenceLocal,
                            onTextChange = {
                                if (it.length <= Constants.MAX_OCCURRENCE_LOCAL_LENGTH)
                                    occurrenceLocal = it
                            },
                            icon = R.drawable.baseline_location_pin_24,
                            iconDescription = "Local icon",
                        )
                        Spacer(Modifier.height(12.dp))
                        GenericInputField(
                            title = "Descrição",
                            placeholder = "Insira uma breve descrição",
                            text = occurrenceDescription,
                            onTextChange = {
                                if (it.length <= Constants.MAX_OCCURRENCE_DESCRIPTION_LENGTH)
                                    occurrenceDescription = it
                            },
                            icon = R.drawable.outline_text_24,
                            iconDescription = "Text icon",
                            singleLine = false,
                        )
                        Spacer(Modifier.height(12.dp))
                        PhotoModal { occurrencePhotoBase64 = it }
                        Spacer(Modifier.height(16.dp))
                        SubmitButton(
                            "Enviar ticket",
                            {
                                if (occurrenceLocal.isEmpty() || occurrenceDescription.isEmpty()) {
                                    dialogText = "Todos os campos devem ser preenchidos"
                                    shouldShowDialog = true
                                    return@SubmitButton
                                }
                                if (occurrencePhotoBase64.isEmpty()) {
                                    dialogText = "Por favor, tire uma foto do problema reportado"
                                    shouldShowDialog = true
                                    return@SubmitButton
                                }
                                saveOccurrence(
                                    OccurrenceDTO(
                                        local = occurrenceLocal,
                                        description = occurrenceDescription,
                                        imgBase64 = occurrencePhotoBase64
                                    ),
                                    onSuccess = { reloadTrigger++ }
                                )
                                showModal = false
                                occurrenceLocal = ""
                                occurrenceDescription = ""
                                occurrencePhotoBase64 = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}


private fun saveOccurrence(
    occurrenceDTO: OccurrenceDTO,
    onSuccess: () -> Unit
) {
    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        Log.e("Firebase", "Usuário não autenticado.")
        return
    }

    val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

    val occurrenceData = mapOf(
        "autor" to userId,
        "title" to occurrenceDTO.title,
        "text" to occurrenceDTO.description,
        "status" to "red",
        "img_url" to occurrenceDTO.imgBase64,
        "local" to occurrenceDTO.local
    )

    val newRef = reportsRef.push()
    newRef.child("content").setValue(occurrenceData)
        .addOnSuccessListener {
            onSuccess()
            Log.d("Firebase", "Ocorrência salva com sucesso.")
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Erro ao salvar ocorrência: ${e.message}")
        }
}


private fun getOccurrences(onResult: (List<OccurrenceDTO>) -> Unit) {
    val userId = Firebase.auth.currentUser?.uid
    val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

    reportsRef
        .orderByChild("content/autor")
        .equalTo(userId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val occurrences = mutableListOf<OccurrenceDTO>()

                for (child in snapshot.children) {
                    val content = child.child("content")

                    val titulo = content.child("title").getValue(String::class.java) ?: ""
                    val texto = content.child("text").getValue(String::class.java) ?: ""
                    val status = content.child("status").getValue(String::class.java) ?: ""
                    val imgBase64 = content.child("img_url").getValue(String::class.java) ?: ""
                    val local = content.child("local").getValue(String::class.java) ?: ""

                    val dto = OccurrenceDTO(
                        id = child.key ?: "",
                        description = texto,
                        title = titulo,
                        status = when (status) {
                            "red" -> Status.PENDENT
                            "yellow" -> Status.ON_PROGRESS
                            "green" -> Status.FINISHED
                            else -> Status.PENDENT
                        },
                        imgBase64 = imgBase64,
                        author = userId.orEmpty(),
                        local = local
                    )

                    occurrences.add(dto)
                }

                onResult(occurrences)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("Firebase", "Erro ao buscar: ${error.message}")
            }
        })
}

private fun base64ToBitmap(base64String: String): Bitmap? {
    return try {
        val decodeBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.size)
    } catch (e: Exception) {
        Log.e("ImageDecode", "Erro ao decodificar imagem: ${e.message}")
        null
    }
}
