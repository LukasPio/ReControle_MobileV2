package com.lucas.recontrole.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lucas.recontrole.MainActivity
import com.lucas.recontrole.R
import com.lucas.recontrole.Status

object NotificationService {
    private const val CHANNEL_ID = "occurrence_status_channel"
    private const val CHANNEL_NAME = "Status de Ocorrências"
    private const val CHANNEL_DESCRIPTION = "Notificações sobre mudanças no status das ocorrências"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendStatusChangeNotification(
        context: Context,
        occurrenceId: String,
        occurrenceCategory: String,
        occurrenceLocal: String,
        oldStatus: Status,
        newStatus: Status
    ) {
        // Intent para abrir o app ao clicar na notificação
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("occurrenceId", occurrenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            occurrenceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (newStatus) {
            Status.PENDENT -> "Pendente"
            Status.ON_PROGRESS -> "Em Andamento"
            Status.FINISHED -> "Concluído"
        }

        val title = "Ocorrência atualizada"
        val message = "$occurrenceCategory ($occurrenceLocal) está: $statusText"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_warning_24) // Use um ícone do seu projeto
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(occurrenceId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permissão de notificação não concedida
            android.util.Log.e("NotificationService", "Permissão negada: ${e.message}")
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissão não necessária em versões antigas
        }
    }
}