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
import androidx.compose.runtime.mutableIntStateOf
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
import com.lucas.recontrole.components.OccurrenceObjectDropdown
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
    var occurrenceCategory by remember {mutableStateOf("")}

    var reloadTrigger by remember { mutableIntStateOf(0) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf("") }

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
                        "Parece que ainda não existe nenhum ticket por aqui, tente sincronizar!",
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
                                itemToDelete = occurrenceModalContent.id
                                showDeleteConfirmation = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
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

        if (showDeleteConfirmation) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                    itemToDelete = ""
                },
                title = {
                    Text("Confirmar deleção")
                },
                text = {
                    Text("Tem certeza que deseja apagar esta ocorrência? Esta ação não pode ser desfeita.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Log.d("HomeScreen", "Confirmando deleção do item: $itemToDelete")
                            showDeleteConfirmation = false

                            deleteOccurrence(
                                context = context,
                                occurrenceId = itemToDelete,
                                onSuccess = {
                                    Log.d("HomeScreen", "Deleção bem-sucedida, fechando modal e recarregando")
                                    shouldShowOccurrenceModal = false
                                    reloadTrigger++
                                    itemToDelete = ""
                                },
                                onError = { errorMessage ->
                                    Log.e("HomeScreen", "Erro na deleção: $errorMessage")
                                    shouldShowOccurrenceModal = false
                                    dialogText = errorMessage
                                    shouldShowDialog = true
                                    itemToDelete = ""
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Confirmar", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            itemToDelete = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        )
                    ) {
                        Text("Cancelar", color = Color.White)
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
                        OccurrenceObjectDropdown(
                            onValueChange = {occurrenceCategory = it}
                        )
                        Spacer(Modifier.height(12.dp))
                        PhotoModal { occurrencePhotoBase64 = it }
                        Spacer(Modifier.height(16.dp))
                        SubmitButton(
                            "Enviar ticket",
                            {
                                if (occurrenceLocal.isEmpty() || occurrenceDescription.isEmpty() || occurrenceCategory.isEmpty()) {
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
                                        imgBase64 = occurrencePhotoBase64,
                                        category = occurrenceCategory
                                    ),
                                    onSuccess = { reloadTrigger++ }
                                )

                                shouldShowModal = false
                                occurrenceLocal = ""
                                occurrenceDescription = ""
                                occurrencePhotoBase64 = ""
                                occurrenceCategory = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}

// FUNÇÃO DE DELETAR CORRIGIDA
private fun deleteOccurrence(
    context: Context,
    occurrenceId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    Log.d("DeleteOccurrence", "Iniciando deleção do item: $occurrenceId")

    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        Log.e("DeleteOccurrence", "Usuário não autenticado")
        onError("Usuário não autenticado.")
        return
    }

    if (occurrenceId.isEmpty()) {
        Log.e("DeleteOccurrence", "ID da ocorrência está vazio")
        onError("ID da ocorrência inválido.")
        return
    }

    try {
        // Primeiro, tentar deletar do Firebase
        val reportRef = FirebaseDatabase.getInstance()
            .getReference("reports")
            .child(occurrenceId)
            .child("content")

        Log.d("DeleteOccurrence", "Tentando deletar do Firebase: reports/$occurrenceId/content")

        // Verificar se o item existe antes de tentar deletar
        reportRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.w("DeleteOccurrence", "Item não existe no Firebase: $occurrenceId")
                onError("Item não encontrado.")
                return@addOnSuccessListener
            }

            // Verificar se o usuário é o autor
            val autor = snapshot.child("autor").getValue(String::class.java)
            if (autor != userId) {
                Log.w("DeleteOccurrence", "Usuário $userId tentando deletar item de $autor")
                onError("Você só pode deletar suas próprias ocorrências.")
                return@addOnSuccessListener
            }

            // Delete lógico: adiciona campo 'deleted' com timestamp
            val deleteData = mapOf(
                "deleted" to true,
                "deletedAt" to System.currentTimeMillis(),
                "deletedBy" to userId
            )

            Log.d("DeleteOccurrence", "Aplicando delete lógico...")
            reportRef.updateChildren(deleteData)
                .addOnSuccessListener {
                    Log.d("DeleteOccurrence", "Delete no Firebase bem-sucedido")

                    // Remove do cache local
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val dao = db.occurrenceDao()

                            // Verificar se existe no banco local antes de deletar
                            val existingItem = dao.findById(occurrenceId)
                            if (existingItem != null) {
                                dao.deleteById(occurrenceId)
                                Log.d("DeleteOccurrence", "Item removido do cache local")
                            } else {
                                Log.w("DeleteOccurrence", "Item não estava no cache local")
                            }

                            withContext(Dispatchers.Main) {
                                Log.d("DeleteOccurrence", "Deleção concluída com sucesso")
                                onSuccess()
                            }

                        } catch (e: Exception) {
                            Log.e("DeleteOccurrence", "Erro ao remover do cache local: ${e.message}")
                            // Mesmo com erro no cache, se deletou do Firebase é sucesso
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteOccurrence", "Erro ao deletar do Firebase: ${e.message}")
                    onError("Erro ao deletar do servidor: ${e.message}")
                }

        }.addOnFailureListener { e ->
            Log.e("DeleteOccurrence", "Erro ao verificar existência do item: ${e.message}")
            onError("Erro ao acessar o servidor: ${e.message}")
        }

    } catch (e: Exception) {
        Log.e("DeleteOccurrence", "Erro fatal na função de deletar: ${e.message}")
        onError("Erro inesperado: ${e.message}")
    }
}

private fun saveOccurrence(
    context: Context,
    occurrenceDTO: OccurrenceDTO,
    onSuccess: () -> Unit,
    onError: ((String) -> Unit)? = null
) {
    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        Log.e("Firebase", "Usuário não autenticado.")
        onError?.invoke("Usuário não autenticado.")
        return
    }

    try {
        // Validar dados antes de salvar
        if (occurrenceDTO.description.isEmpty() ||
            occurrenceDTO.local.isEmpty() ||
            occurrenceDTO.category.isEmpty()) {
            onError?.invoke("Dados obrigatórios não preenchidos")
            return
        }

        // Validar Base64 da imagem
        if (occurrenceDTO.imgBase64.isNotEmpty() && base64ToBitmap(occurrenceDTO.imgBase64) == null) {
            onError?.invoke("Imagem inválida")
            return
        }

        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")
        val occurrenceData = mapOf(
            "autor" to userId,
            "text" to occurrenceDTO.description,
            "status" to "red",
            "img_url" to occurrenceDTO.imgBase64,
            "local" to occurrenceDTO.local,
            "category" to occurrenceDTO.category,
            "timestamp" to System.currentTimeMillis()
        )

        val newRef = reportsRef.push()
        val newId = newRef.key ?: ""

        newRef.child("content").setValue(occurrenceData)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
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
                                    status = "red",
                                    category = occurrenceDTO.category
                                )
                            )
                        )
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        Log.e("Database", "Erro ao salvar localmente: ${e.message}")
                        withContext(Dispatchers.Main) {
                            onSuccess() // Salvo no Firebase, mas erro local não é crítico
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Erro ao salvar ocorrência: ${e.message}")
                onError?.invoke("Erro ao salvar: ${e.message}")
            }

    } catch (e: Exception) {
        Log.e("SaveOccurrence", "Erro fatal ao salvar: ${e.message}")
        onError?.invoke("Erro inesperado: ${e.message}")
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
        try {
            // Se não forçar refresh, tenta carregar do cache primeiro
            if (!forceRefresh) {
                val cached = dao.getAll()
                if (cached.isNotEmpty()) {
                    Log.d("GetOccurrences", "Carregando ${cached.size} items do cache")
                    val mapped = cached.mapNotNull { entity ->
                        try {
                            entity.toDTO()
                        } catch (e: Exception) {
                            Log.e("DataMapping", "Erro ao converter entidade: ${e.message}")
                            null
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onResult(mapped)
                    }
                    return@launch
                }
            }

            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.e("Auth", "Usuário não autenticado")
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
                return@launch
            }

            Log.d("GetOccurrences", "Buscando dados do Firebase para userId: $userId")
            val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

            reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Firebase", "Snapshot recebido com ${snapshot.childrenCount} children")

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val occurrences = mutableListOf<OccurrenceDTO>()
                            val entities = mutableListOf<OccurrenceEntity>()

                            for (child in snapshot.children) {
                                try {
                                    val content = child.child("content")
                                    Log.d("Firebase", "Processando child: ${child.key}")

                                    // Pula itens deletados
                                    val isDeleted = content.child("deleted").getValue(Boolean::class.java) ?: false
                                    if (isDeleted) {
                                        Log.d("Firebase", "Item ${child.key} está deletado, pulando")
                                        continue
                                    }

                                    val id = child.key ?: ""
                                    val description = content.child("text").getValue(String::class.java) ?: ""
                                    val category = content.child("category").getValue(String::class.java) ?: ""
                                    val imgBase64 = content.child("img_url").getValue(String::class.java) ?: ""
                                    val local = content.child("local").getValue(String::class.java) ?: ""
                                    val author = content.child("autor").getValue(String::class.java) ?: ""
                                    val statusString = content.child("status").getValue(String::class.java) ?: "red"

                                    Log.d("Firebase", "Dados do item $id: desc=$description, local=$local, category=$category")

                                    // Verificar se é um item válido
                                    if (id.isEmpty() || description.isEmpty()) {
                                        Log.w("Firebase", "Item $id tem dados inválidos, pulando")
                                        continue
                                    }

                                    val status = when (statusString) {
                                        "yellow" -> Status.ON_PROGRESS
                                        "green" -> Status.FINISHED
                                        else -> Status.PENDENT
                                    }

                                    val dto = OccurrenceDTO(
                                        id = id,
                                        description = description,
                                        category = category,
                                        imgBase64 = imgBase64,
                                        local = local,
                                        author = author,
                                        status = status
                                    )

                                    val entity = OccurrenceEntity(
                                        id = id,
                                        description = description,
                                        status = statusString,
                                        imgBase64 = imgBase64,
                                        local = local,
                                        author = author,
                                        category = category
                                    )

                                    occurrences.add(dto)
                                    entities.add(entity)

                                    Log.d("Firebase", "Item $id adicionado com sucesso")

                                } catch (e: Exception) {
                                    Log.e("Firebase", "Erro ao processar item ${child.key}: ${e.message}")
                                    continue
                                }
                            }

                            Log.d("Firebase", "Total de ${occurrences.size} ocorrências processadas")

                            // Salvar no cache
                            if (entities.isNotEmpty()) {
                                try {
                                    dao.clearAll()
                                    dao.insertAll(entities)
                                    Log.d("Database", "${entities.size} items salvos no cache")
                                } catch (e: Exception) {
                                    Log.e("Database", "Erro ao salvar no cache: ${e.message}")
                                }
                            }

                            withContext(Dispatchers.Main) {
                                Log.d("GetOccurrences", "Retornando ${occurrences.size} ocorrências para a UI")
                                onResult(occurrences)
                            }

                        } catch (e: Exception) {
                            Log.e("Firebase", "Erro geral no processamento: ${e.message}")
                            withContext(Dispatchers.Main) {
                                onResult(emptyList())
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro ao buscar dados: ${error.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(emptyList())
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("GetOccurrences", "Erro fatal: ${e.message}")
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}

private fun base64ToBitmap(base64String: String): Bitmap? {
    return try {
        if (base64String.isEmpty()) return null

        // Limpar o base64 de possíveis prefixos
        val cleanBase64 = base64String.replace("data:image/[^;]*;base64,".toRegex(), "")

        val decodeBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.size)

        if (bitmap == null) {
            Log.e("ImageDecode", "Bitmap decodificado é null para: ${base64String.take(50)}...")
        }

        bitmap
    } catch (e: IllegalArgumentException) {
        Log.e("ImageDecode", "Base64 inválido: ${e.message}")
        null
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
    author = author,
    category = category
)
