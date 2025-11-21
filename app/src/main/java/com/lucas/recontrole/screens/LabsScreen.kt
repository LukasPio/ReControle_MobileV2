package com.lucas.recontrole.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lucas.recontrole.components.AppTopBar
import com.lucas.recontrole.dtos.OccurrenceDTO
import com.lucas.recontrole.logic.getOccurrences
import com.lucas.recontrole.logic.listenOccurrences

data class LabWithCount(
    val labName: String,
    val occurrenceCount: Int
)

@Composable
fun LabsScreen(navController: NavController) {
    val context = LocalContext.current
    var occurrencesState by remember { mutableStateOf<List<OccurrenceDTO>>(emptyList()) }
    var finishedLoading by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    // Carregar ocorrências
    LaunchedEffect(reloadTrigger) {
        finishedLoading = false
        getOccurrences(context = context, forceRefresh = reloadTrigger > 0) { occurrences ->
            occurrencesState = occurrences
            finishedLoading = true
        }
    }

    // Listener para atualizações em tempo real
    LaunchedEffect(Unit) {
        listenOccurrences(context) { updatedList ->
            occurrencesState = updatedList
            finishedLoading = true
        }
    }

    // Agrupar ocorrências por laboratório e contar
    val labsWithCount = remember(occurrencesState) {
        occurrencesState
            .groupBy { it.local }
            .map { (lab, occurrences) ->
                LabWithCount(lab, occurrences.size)
            }
            .sortedByDescending { it.occurrenceCount }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                AppTopBar(
                    title = "Laboratórios",
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
                            "Carregando laboratórios...",
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
            if (finishedLoading && labsWithCount.isEmpty()) {
                item {
                    Text(
                        "Nenhum laboratório com ocorrências encontrado!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 50.dp, start = 16.dp, end = 16.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Lista de laboratórios
            if (finishedLoading) {
                items(labsWithCount) { lab ->
                    LabCard(
                        labWithCount = lab,
                        onClick = {
                            navController.navigate("labOccurrences/${lab.labName}")
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun LabCard(
    labWithCount: LabWithCount,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(100.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = labWithCount.labName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${labWithCount.occurrenceCount} ocorrência${if (labWithCount.occurrenceCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Badge com número de ocorrências
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = labWithCount.occurrenceCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}