package com.weeknday.onething

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OneThingFirebaseMessagingSevice : FirebaseMessagingService() {
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //추가한것
        sendNotification(remoteMessage.data["message"])
    }

    private fun sendNotification(messageBody: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "CHERI"
            // String description = getString(R.string.channel_description);
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("cheri", name, importance)
            // channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, WebActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val color = 0xaf1c3f
        val notificationBuilder = NotificationCompat.Builder(this, "cheri")
                .setSmallIcon(R.drawable.sym_def_app_icon)
                .setColor(color) /* .setContentTitle("CHERI")*/
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FirebaseMsgService"
    }
}