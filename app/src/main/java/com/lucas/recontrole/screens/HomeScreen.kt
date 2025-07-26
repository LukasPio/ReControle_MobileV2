package com.lucas.recontrole.screens

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import com.lucas.recontrole.components.FullScreenModal
import com.lucas.recontrole.components.GenericInputField
import com.lucas.recontrole.components.OccurrenceCard
import com.lucas.recontrole.dtos.OccurrenceDTO

@Composable
fun HomeScreen(navController: NavController) {
    var occurrencesState by remember { mutableStateOf<List<OccurrenceDTO>>(emptyList())}
    var finishedLoading by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }

    var occurrenceLocal by remember { mutableStateOf("") }
    var occurrenceDescription by remember { mutableStateOf("") }
    var occurrenceImage by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
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
                if (finishedLoading) {
                    if (occurrencesState.isEmpty()) {
                        Text(
                            "Parece que você ainda não abriu nenhum ticket!",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 50.dp, start = 8.dp, end = 8.dp),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    else {
                        Spacer(Modifier.height(20.dp))
                        occurrencesState.forEach { occurrence ->
                            OccurrenceCard(
                                occurrence.title,
                                "Local X",
                                occurrence.status,
                            )
                        }
                    }
                }
                else {
                        Text(
                            "Carregando tickets...",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 50.dp),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)

                        CircularProgressIndicator(modifier = Modifier
                            .size(60.dp)
                            .padding(top = 40.dp))
                    }
            }
        }

        Image(
            painter = painterResource(R.drawable.add_circle_24px),
            contentDescription = "Add circle icon",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(110.dp)
                .padding(16.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(radius = 50.dp)
                ){
                    showModal = true
                }
        )

        if (showModal) {
            FullScreenModal(
                onDismiss = {showModal = false},
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .fillMaxHeight(0.5f)
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
                            onTextChange = {it -> if(it.length <= Constants.MAX_OCCURRENCE_LOCAL_LENGTH) occurrenceLocal = it},
                            icon = R.drawable.baseline_location_pin_24,
                            iconDescription = "Local icon",
                            singleLine = true,
                        )
                        Spacer(Modifier.height(12.dp))
                        GenericInputField(
                            title = "Descrição",
                            placeholder = "Insira uma breve descrição",
                            text = occurrenceDescription,
                            onTextChange = {it -> if(it.length <= Constants.MAX_OCCURRENCE_DESCRIPTION_LENGTH) occurrenceDescription = it},
                            icon = R.drawable.outline_text_24,
                            iconDescription = "Text icon",
                            singleLine = false
                        )
                    }
                }
            )
        }
    }
}

fun getOccurrences(onResult: (List<OccurrenceDTO>) -> Unit) {
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
                    val imgUrl = content.child("img_url").getValue(String::class.java) ?: ""
                    val dto = OccurrenceDTO(
                        id = child.key ?: "",
                        description = texto,
                        title = titulo,
                        status = when(status) {
                            "red" -> Status.PENDENT
                            "yellow" -> Status.ON_PROGRESS
                            "green" -> Status.FINISHED
                            else -> Status.PENDENT
                        },
                        imgUrl = imgUrl,
                        author = userId.orEmpty()
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