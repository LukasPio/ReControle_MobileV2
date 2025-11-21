package com.lucas.recontrole.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lucas.recontrole.notification.OccurrenceMonitorWorker
import java.util.concurrent.TimeUnit

object NotificationManager {
    private const val WORK_NAME = "occurrence_monitor_work"

    fun startMonitoring(context: Context) {
        try {
            // Criar canal de notificação
            NotificationService.createNotificationChannel(context)

            Log.d("NotificationManager", "Iniciando monitoramento de ocorrências")

            // Configurar constraints para o Worker
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Precisa de conexão
                .setRequiresBatteryNotLow(false) // Rodar mesmo com bateria baixa
                .setRequiresDeviceIdle(false) // Não esperar dispositivo ocioso
                .build()

            // Configurar Worker periódico
            // Android 12+: mínimo 15 minutos
            // Versões antigas: pode ser mais frequente
            val workRequest = PeriodicWorkRequestBuilder<OccurrenceMonitorWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(2, TimeUnit.MINUTES) // Esperar 2 min antes da primeira execução
                .addTag("occurrence_monitoring")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Mantém trabalho existente
                workRequest
            )

            Log.d("NotificationManager", "Worker enfileirado com sucesso")

        } catch (e: Exception) {
            Log.e("NotificationManager", "Erro ao iniciar monitoramento: ${e.message}", e)
        }
    }

    fun stopMonitoring(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("NotificationManager", "Monitoramento parado")
        } catch (e: Exception) {
            Log.e("NotificationManager", "Erro ao parar monitoramento: ${e.message}", e)
        }
    }

    fun isMonitoringActive(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            workInfos.any { !it.state.isFinished }
        } catch (e: Exception) {
            false
        }
    }
}