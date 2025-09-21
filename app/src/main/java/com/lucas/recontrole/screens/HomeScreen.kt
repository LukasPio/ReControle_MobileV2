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
import androidx.compose.foundation.lazy.LazyRow
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
import com.lucas.recontrole.logic.base64ToBitmap
import com.lucas.recontrole.logic.deleteOccurrence
import com.lucas.recontrole.logic.getOccurrences
import com.lucas.recontrole.logic.saveOccurrence
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

    var selectedLab by remember { mutableStateOf<String?>(null) } // Lab selecionado para filtro

    var shouldShowModal by remember { mutableStateOf(false) }
    var shouldShowDialog by remember { mutableStateOf(false) }
    var shouldShowOccurrenceModal by remember { mutableStateOf(false) }

    var dialogText by remember { mutableStateOf("") }
    var occurrenceModalContent by remember { mutableStateOf(OccurrenceDTO()) }

    var occurrencePhotoBase64 by remember { mutableStateOf("") }
    var occurrenceLocal by remember { mutableStateOf("") }
    var occurrenceDescription by remember { mutableStateOf("") }
    var occurrenceCategory by remember { mutableStateOf("") }

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

    // Filtrar ocorrências pelo lab selecionado
    val filteredOccurrences = remember(occurrencesState, selectedLab) {
        if (selectedLab.isNullOrEmpty()) occurrencesState
        else occurrencesState.filter { it.local == selectedLab }
    }

    // Obter lista de laboratórios únicos
    val labsList = remember(occurrencesState) {
        occurrencesState.map { it.local }.distinct().sorted()
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

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Row de laboratórios
            if (labsList.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(labsList) { lab ->
                            val isSelected = lab == selectedLab
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedLab = if (isSelected) null else lab
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(text = lab, color = if (isSelected) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }

            // Se ainda carregando
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

            // Nenhuma ocorrência
            if (finishedLoading && filteredOccurrences.isEmpty()) {
                selectedLab = null
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

            // Listar ocorrências filtradas
            if (finishedLoading) {
                items(filteredOccurrences) { occurrence ->
                    val bitmap = remember(occurrence.imgBase64) { base64ToBitmap(occurrence.imgBase64) }
                    if (bitmap != null) {
                        OccurrenceCard(
                            occurrenceDTO = occurrence,
                            image = bitmap,
                            onClick = { clickedOccurrence, _ ->
                                occurrenceModalContent = clickedOccurrence
                                shouldShowOccurrenceModal = true
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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