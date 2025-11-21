package com.lucas.recontrole.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.lucas.recontrole.Status
import com.lucas.recontrole.db.AppDatabase
import com.lucas.recontrole.notifications.NotificationService
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OccurrenceMonitorWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.d("OccurrenceMonitor", "Usuário não autenticado")
                return@withContext Result.success()
            }

            if (!NotificationService.hasNotificationPermission(context)) {
                Log.d("OccurrenceMonitor", "Sem permissão de notificação")
                return@withContext Result.success()
            }

            checkForStatusChanges()
            Result.success()
        } catch (e: Exception) {
            Log.e("OccurrenceMonitor", "Erro ao monitorar ocorrências: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun checkForStatusChanges() {
        val db = AppDatabase.getDatabase(context)
        val dao = db.occurrenceDao()
        val cachedOccurrences = dao.getAll()

        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

        try {
            val snapshot = reportsRef.get().await()

            for (child in snapshot.children) {
                try {
                    val content = child.child("content")
                    val isDeleted = content.child("deleted").getValue(Boolean::class.java) ?: false

                    if (isDeleted) continue

                    val id = child.key ?: continue
                    val statusString = content.child("status").getValue(String::class.java) ?: "red"
                    val category = content.child("category").getValue(String::class.java) ?: ""
                    var local = content.child("local").getValue(String::class.java) ?: ""

                    if (local.isEmpty()) {
                        local = child.child("selected_obj/sel_lab_id").getValue(String::class.java) ?: ""
                    }

                    val newStatus = when (statusString) {
                        "yellow" -> Status.ON_PROGRESS
                        "green" -> Status.FINISHED
                        else -> Status.PENDENT
                    }

                    // Verificar se existe no cache e se mudou de status
                    val cachedOccurrence = cachedOccurrences.find { it.id == id }

                    if (cachedOccurrence != null) {
                        val oldStatus = when (cachedOccurrence.status) {
                            "yellow" -> Status.ON_PROGRESS
                            "green" -> Status.FINISHED
                            else -> Status.PENDENT
                        }

                        // Se o status mudou, enviar notificação
                        if (oldStatus != newStatus) {
                            Log.d("OccurrenceMonitor", "Status mudou: $id de $oldStatus para $newStatus")

                            NotificationService.sendStatusChangeNotification(
                                context = context,
                                occurrenceId = id,
                                occurrenceCategory = if (category.isNotEmpty()) category else "Ocorrência",
                                occurrenceLocal = local,
                                oldStatus = oldStatus,
                                newStatus = newStatus
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OccurrenceMonitor", "Erro ao processar ocorrência: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("OccurrenceMonitor", "Erro ao buscar dados do Firebase: ${e.message}")
        }
    }
}