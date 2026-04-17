package com.familytracker.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.familytracker.app.ui.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "FamilyTracker"
        val body = message.notification?.body ?: ""
        val fromUid = message.data["fromUid"] ?: ""
        val requestId = message.data["requestId"] ?: ""

        showLocationRequestNotification(title, body, fromUid, requestId)
    }

    private fun showLocationRequestNotification(
        title: String, body: String, fromUid: String, requestId: String
    ) {
        val channelId = "request_channel"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Location Requests",
                NotificationManager.IMPORTANCE_HIGH)
        )

        val allowIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("action", "allow")
            putExtra("requestId", requestId)
            putExtra("fromUid", fromUid)
        }
        val allowPending = PendingIntent.getActivity(this, 0, allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val denyIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("action", "deny")
            putExtra("requestId", requestId)
        }
        val denyPending = PendingIntent.getActivity(this, 1, denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(0, "Allow", allowPending)
            .addAction(0, "Deny", denyPending)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
