package com.example.uami.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.uami.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New Firebase Cloud Messaging Token: $token")
        
        // Guardar el token en preferencias compartidas para que sea fácil consultarlo o recuperarlo
        val prefs = getSharedPreferences("uami_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_MESSAGE", "Message received from: ${remoteMessage.from}")

        // 1. Verificar si trae notificación en el payload estándar
        remoteMessage.notification?.let {
            Log.d("FCM_MESSAGE", "Notification Title: ${it.title}, Body: ${it.body}")
            sendNotification(it.title ?: "Uami", it.body ?: "")
            return
        }

        // 2. Verificar si trae datos en el payload de data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM_MESSAGE", "Data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Uami"
            val body = remoteMessage.data["body"] ?: ""
            if (body.isNotEmpty()) {
                sendNotification(title, body)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val channelId = "uami_push_notifications"
        
        // Usar ícono nativo por defecto para garantizar que se renderice sin problemas de recursos
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Canal de Notificaciones Uami",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones inteligentes de recetas y nutrición Uami"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
