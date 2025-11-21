package com.lucas.recontrole.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lucas.recontrole.notification.OccurrenceMonitorWorker
import java.util.concurrent.TimeUnit

object NotificationManager {
    private const val WORK_NAME = "occurrence_monitor_work"

    fun startMonitoring(context: Context) {
        // Criar canal de notificação
        NotificationService.createNotificationChannel(context)

        // Configurar Worker periódico (verifica a cada 15 minutos)
        val workRequest = PeriodicWorkRequestBuilder<OccurrenceMonitorWorker>(
            15, TimeUnit.MINUTES // Intervalo mínimo permitido pelo Android
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Mantém o trabalho existente se já estiver rodando
            workRequest
        )
    }

    fun stopMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}