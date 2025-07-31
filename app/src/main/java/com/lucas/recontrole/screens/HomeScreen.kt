package com.lucas.recontrole.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
import com.lucas.recontrole.db.AppDatabase
import com.lucas.recontrole.dtos.OccurrenceDTO
import com.lucas.recontrole.model.OccurrenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var occurrencesState by remember { mutableStateOf<List<OccurrenceDTO>>(emptyList()) }
    var finishedLoading by remember { mutableStateOf(false) }

    var shouldShowModal by remember { mutableStateOf(false) }
    var shouldShowDialog by remember { mutableStateOf(false) }
    var shouldShowOccurrenceModal by remember { mutableStateOf(false) }

    var dialogText by remember { mutableStateOf("") }
    var occurrenceModalContent by remember { mutableStateOf(OccurrenceDTO()) }

    var occurrencePhotoBase64 by remember { mutableStateOf("") }
    var occurrenceLocal by remember { mutableStateOf("") }
    var occurrenceDescription by remember { mutableStateOf("") }

    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(reloadTrigger) {
        finishedLoading = false
        getOccurrences(context = context, forceRefresh = reloadTrigger > 0) { occurrences ->
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
                    navController = navController,
                    onSync = { reloadTrigger++ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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

            if (finishedLoading) {
                items(occurrencesState) { occurrence ->
                    val bitmap = remember(occurrence.imgBase64) {
                        base64ToBitmap(occurrence.imgBase64)
                    }

                    if (bitmap == null) {
                        Log.d("RUNTIME", "Image of ${occurrence.id} is null")
                        return@items
                    }

                    OccurrenceCard(
                        occurrenceDTO = occurrence, // Passa o objeto completo
                        image = bitmap, // Usa o bitmap da ocorrência atual
                        onClick = { clickedOccurrence, img ->
                            // Agora você tem acesso completo à ocorrência clicada
                            occurrenceModalContent = clickedOccurrence
                            shouldShowOccurrenceModal = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
                    shouldShowModal = true
                }
        )

        if (shouldShowDialog) {
            ErrorDialog(
                { shouldShowDialog = false },
                { shouldShowDialog = false },
                dialogText
            )
        }

        if (shouldShowOccurrenceModal) {
            FullScreenModal(
                onDismiss = { shouldShowOccurrenceModal = false },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(400.dp, 600.dp)
                    ) {
                        Text(
                            text = "Detalhes da Ocorrência:",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mostrar a imagem
                        val modalBitmap = remember(occurrenceModalContent.imgBase64) {
                            base64ToBitmap(occurrenceModalContent.imgBase64)
                        }


                        modalBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Imagem da ocorrência",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Text(
                                text = "Local:",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = occurrenceModalContent.local
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Descrição:",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp
                        )
                        Text(
                            text = occurrenceModalContent.description,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Text(
                                text = "Status:",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when (occurrenceModalContent.status) {
                                    Status.PENDENT -> "Pendente"
                                    Status.ON_PROGRESS -> "Em andamento"
                                    Status.FINISHED -> "Concluído"
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                deleteOccurrence(
                                    context = context,
                                    occurrenceId = occurrenceModalContent.id,
                                    onSuccess = {
                                        shouldShowOccurrenceModal = false
                                        reloadTrigger++
                                    },
                                    onError = { errorMessage ->
                                        shouldShowOccurrenceModal = false
                                        dialogText = errorMessage
                                        shouldShowDialog = true
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apagar ocorrência")
                        }
                    }
                }
            )
        }

        if (shouldShowModal) {
            FullScreenModal(
                onDismiss = {
                    shouldShowModal = false
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
                                    context,
                                    OccurrenceDTO(
                                        local = occurrenceLocal,
                                        description = occurrenceDescription,
                                        imgBase64 = occurrencePhotoBase64
                                    ),
                                    onSuccess = { reloadTrigger++ }
                                )

                                shouldShowModal = false
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

private fun deleteOccurrence(
    context: Context,
    occurrenceId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        onError("Usuário não autenticado.")
        return
    }

    val reportRef = FirebaseDatabase.getInstance()
        .getReference("reports")
        .child(occurrenceId)
        .child("content")

    // Delete lógico: adiciona campo 'deleted' com timestamp
    val deleteData = mapOf(
        "deleted" to true,
        "deletedAt" to System.currentTimeMillis(),
        "deletedBy" to userId
    )

    reportRef.updateChildren(deleteData)
        .addOnSuccessListener {
            // Remove do cache local
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val dao = db.occurrenceDao()
                dao.deleteById(occurrenceId)

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Erro ao deletar ocorrência: ${e.message}")
            onError("Erro ao deletar ocorrência: ${e.message}")
        }
}

private fun saveOccurrence(
    context: Context,
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
        "text" to occurrenceDTO.description,
        "status" to "red",
        "img_url" to occurrenceDTO.imgBase64,
        "local" to occurrenceDTO.local
    )

    val newRef = reportsRef.push()
    val newId = newRef.key ?: ""

    newRef.child("content").setValue(occurrenceData)
        .addOnSuccessListener {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val dao = db.occurrenceDao()
                dao.insertAll(
                    listOf(
                        OccurrenceEntity(
                            id = newId,
                            description = occurrenceDTO.description,
                            imgBase64 = occurrenceDTO.imgBase64,
                            local = occurrenceDTO.local,
                            author = userId,
                            status = "red"
                        )
                    )
                )
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Erro ao salvar ocorrência: ${e.message}")
        }
}


fun getOccurrences(
    context: Context,
    forceRefresh: Boolean = false,
    onResult: (List<OccurrenceDTO>) -> Unit
) {
    val db = AppDatabase.getDatabase(context)
    val dao = db.occurrenceDao()

    CoroutineScope(Dispatchers.IO).launch {
        if (!forceRefresh) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) {
                val mapped = cached.map { it.toDTO() }
                withContext(Dispatchers.Main) {
                    onResult(mapped)
                }
                return@launch
            }
        }

        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) return@launch

        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")
        reportsRef.orderByChild("content/autor")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val occurrences = mutableListOf<OccurrenceDTO>()
                    val entities = mutableListOf<OccurrenceEntity>()

                    for (child in snapshot.children) {
                        val content = child.child("content")

                        // Pula itens deletados
                        val isDeleted = content.child("deleted").getValue(Boolean::class.java) ?: false
                        if (isDeleted) continue

                        // resto do código permanece igual...
                        val dto = OccurrenceDTO(
                            id = child.key ?: "",
                            description = content.child("text").getValue(String::class.java) ?: "",
                            imgBase64 = content.child("img_url").getValue(String::class.java) ?: "",
                            local = content.child("local").getValue(String::class.java) ?: "",
                            author = userId,
                            status = when (content.child("status").getValue(String::class.java)) {
                                "yellow" -> Status.ON_PROGRESS
                                "green" -> Status.FINISHED
                                else -> Status.PENDENT
                            }
                        )

                        occurrences.add(dto)
                        entities.add(dto.toEntity())
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        dao.clearAll()
                        dao.insertAll(entities)
                    }

                    onResult(occurrences)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro ao buscar: ${error.message}")
                }
            })
    }
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

fun OccurrenceEntity.toDTO() = OccurrenceDTO(
    id = id,
    description = description,
    imgBase64 = imgBase64,
    local = local,
    author = author,
    status = when (status) {
        "red" -> Status.PENDENT
        "yellow" -> Status.ON_PROGRESS
        "green" -> Status.FINISHED
        else -> Status.PENDENT
    }
)

fun OccurrenceDTO.toEntity() = OccurrenceEntity(
    id = id,
    description = description,
    status = when (status) {
        Status.PENDENT -> "red"
        Status.ON_PROGRESS -> "yellow"
        Status.FINISHED -> "green"
    },
    imgBase64 = imgBase64,
    local = local,
    author = author
)
