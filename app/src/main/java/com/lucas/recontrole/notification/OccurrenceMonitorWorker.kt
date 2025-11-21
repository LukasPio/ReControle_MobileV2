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
import java.util.UUID

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

            checkForStatusChanges(userId)
            Result.success()
        } catch (e: Exception) {
            Log.e("OccurrenceMonitor", "Erro ao monitorar ocorrências: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun checkForStatusChanges(userId: String) {
        val db = AppDatabase.getDatabase(context)
        val occurrenceDao = db.occurrenceDao()
        val notificationDao = db.notificationHistoryDao()

        val cachedOccurrences = occurrenceDao.getAll()
        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

        try {
            val snapshot = reportsRef.get().await()

            for (child in snapshot.children) {
                try {
                    val content = child.child("content")
                    val isDeleted = content.child("deleted").getValue(Boolean::class.java) ?: false

                    if (isDeleted) continue

                    val id = child.key ?: continue
                    val author = content.child("autor").getValue(String::class.java) ?: ""

                    // SÓ NOTIFICAR SE O USUÁRIO CRIOU A OCORRÊNCIA
                    if (author != userId) {
                        continue
                    }

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

                    // Verificar se existe no cache
                    val cachedOccurrence = cachedOccurrences.find { it.id == id }

                    if (cachedOccurrence != null) {
                        val oldStatus = when (cachedOccurrence.status) {
                            "yellow" -> Status.ON_PROGRESS
                            "green" -> Status.FINISHED
                            else -> Status.PENDENT
                        }

                        // Se o status mudou
                        if (oldStatus != newStatus) {
                            Log.d("OccurrenceMonitor", "Status mudou: $id de $oldStatus para $newStatus")

                            // Verificar se já notificamos sobre esta mudança
                            val lastNotification = notificationDao.getLastNotificationForOccurrence(id)

                            val shouldNotify = if (lastNotification == null) {
                                // Primeira mudança, notificar
                                true
                            } else {
                                // Verificar se é uma mudança diferente da última
                                lastNotification.newStatus != statusString
                            }

                            if (shouldNotify) {
                                Log.d("OccurrenceMonitor", "Enviando notificação para: $id")

                                // Enviar notificação
                                NotificationService.sendStatusChangeNotification(
                                    context = context,
                                    occurrenceId = id,
                                    occurrenceCategory = if (category.isNotEmpty()) category else "Ocorrência",
                                    occurrenceLocal = local,
                                    oldStatus = oldStatus,
                                    newStatus = newStatus
                                )

                                // Registrar no histórico para evitar duplicatas
                                val notificationRecord = NotificationHistoryEntity(
                                    id = "${id}_${System.currentTimeMillis()}",
                                    occurrenceId = id,
                                    oldStatus = cachedOccurrence.status,
                                    newStatus = statusString,
                                    notifiedAt = System.currentTimeMillis(),
                                    category = category,
                                    local = local
                                )
                                notificationDao.insert(notificationRecord)

                                // Atualizar status no cache local
                                occurrenceDao.insertAll(
                                    listOf(
                                        cachedOccurrence.copy(status = statusString)
                                    )
                                )
                            } else {
                                Log.d("OccurrenceMonitor", "Notificação duplicada, ignorando")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OccurrenceMonitor", "Erro ao processar ocorrência: ${e.message}")
                }
            }

            // Limpar notificações antigas (mais de 30 dias)
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            notificationDao.deleteOldNotifications(thirtyDaysAgo)

        } catch (e: Exception) {
            Log.e("OccurrenceMonitor", "Erro ao buscar dados do Firebase: ${e.message}")
        }
    }
}