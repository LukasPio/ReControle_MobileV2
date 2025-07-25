package com.lucas.recontrole.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.lucas.recontrole.R
import com.lucas.recontrole.Status
import com.lucas.recontrole.components.AppTopBar
import com.lucas.recontrole.components.OccurrenceCard
import com.lucas.recontrole.dtos.OccurrenceDTO

@Composable
fun HomeScreen(navController: NavController) {
    val occurrencesState = remember { mutableStateOf<List<OccurrenceDTO>>(emptyList())}
    val finishedLoading = remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        getOccurrences { occurrences ->
            occurrencesState.value = occurrences
            finishedLoading.value = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                AppTopBar(onAvatarIconClick = {}, onMoreVertIconClick = {}, title = "Tickets")
            }
            item {
                if (finishedLoading.value) {
                    if (occurrencesState.value.isEmpty()) {
                        Text(
                            "Parece que você ainda não abriu nenhum ticket!",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 50.dp, start = 8.dp, end = 8.dp),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    else {
                        Spacer(Modifier.height(20.dp))
                        occurrencesState.value.forEach { occurrence ->
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
                .padding(16.dp)
        )
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