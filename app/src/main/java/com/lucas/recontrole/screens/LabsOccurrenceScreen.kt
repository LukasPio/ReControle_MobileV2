package com.lucas.recontrole.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
import com.lucas.recontrole.dtos.OccurrenceDTO
import com.lucas.recontrole.logic.base64ToBitmap
import com.lucas.recontrole.logic.deleteOccurrence
import com.lucas.recontrole.logic.getOccurrences
import com.lucas.recontrole.logic.listenOccurrences
import com.lucas.recontrole.logic.saveOccurrence

@Composable
fun LabOccurrencesScreen(
    navController: NavController,
    labName: String
) {
    val context = LocalContext.current
    var occurrencesState by remember { mutableStateOf<List<OccurrenceDTO>>(emptyList()) }
    var finishedLoading by remember { mutableStateOf(false) }

    var shouldShowModal by remember { mutableStateOf(false) }
    var shouldShowDialog by remember { mutableStateOf(false) }
    var shouldShowOccurrenceModal by remember { mutableStateOf(false) }

    var dialogText by remember { mutableStateOf("") }
    var occurrenceModalContent by remember { mutableStateOf(OccurrenceDTO()) }

    var occurrencePhotoBase64 by remember { mutableStateOf("") }
    var occurrenceLocal by remember { mutableStateOf(labName) }
    var occurrenceDescription by remember { mutableStateOf("") }
    var occurrenceCategory by remember { mutableStateOf("") }

    var reloadTrigger by remember { mutableIntStateOf(0) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf("") }

    // Carregar ocorrências e filtrar por laboratório
    LaunchedEffect(reloadTrigger) {
        finishedLoading = false
        getOccurrences(context = context, forceRefresh = reloadTrigger > 0) { occurrences ->
            occurrencesState = occurrences.filter { it.local == labName }
            finishedLoading = true
        }
    }

    // Listener para atualizações em tempo real
    LaunchedEffect(Unit) {
        listenOccurrences(context) { updatedList ->
            occurrencesState = updatedList.filter { it.local == labName }
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
                    title = labName,
                    navController = navController,
                    onSync = { reloadTrigger++ }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Loading state
            if (!finishedLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Carregando tickets...",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            // Empty state
            if (finishedLoading && occurrencesState.isEmpty()) {
                item {
                    Text(
                        "Nenhum ticket encontrado para este laboratório!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 50.dp, start = 16.dp, end = 16.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Lista de ocorrências
            if (finishedLoading) {
                items(occurrencesState) { occurrence ->
                    val bitmap = remember(occurrence.imgBase64) {
                        base64ToBitmap(occurrence.imgBase64)
                    }
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

        // Botão FAB para adicionar nova ocorrência
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

        // Error Dialog
        if (shouldShowDialog) {
            ErrorDialog(
                { shouldShowDialog = false },
                { shouldShowDialog = false },
                dialogText
            )
        }

        // Modal de detalhes da ocorrência
        if (shouldShowOccurrenceModal) {
            FullScreenModal(
                onDismiss = { shouldShowOccurrenceModal = false },
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Detalhes da Ocorrência:",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                            Text(text = occurrenceModalContent.local)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Descrição:",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp
                        )
                        Text(text = occurrenceModalContent.description)

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

        // Confirmação de deleção
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                    itemToDelete = ""
                },
                title = { Text("Confirmar deleção") },
                text = {
                    Text("Tem certeza que deseja apagar esta ocorrência? Esta ação não pode ser desfeita.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Log.d("LabOccurrencesScreen", "Confirmando deleção: $itemToDelete")
                            showDeleteConfirmation = false

                            deleteOccurrence(
                                context = context,
                                occurrenceId = itemToDelete,
                                onSuccess = {
                                    Log.d("LabOccurrencesScreen", "Deleção bem-sucedida")
                                    shouldShowOccurrenceModal = false
                                    reloadTrigger++
                                    itemToDelete = ""
                                },
                                onError = { errorMessage ->
                                    Log.e("LabOccurrencesScreen", "Erro: $errorMessage")
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

        // Modal de criar nova ocorrência
        if (shouldShowModal) {
            FullScreenModal(
                onDismiss = { shouldShowModal = false },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 700.dp)
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
                            placeholder = "Local da ocorrência",
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
                            onValueChange = { occurrenceCategory = it }
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
                                occurrenceLocal = labName
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