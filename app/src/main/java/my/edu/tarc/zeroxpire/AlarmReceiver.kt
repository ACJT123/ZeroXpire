@file:JvmName("NotificationKt")

package my.edu.tarc.zeroxpire

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val totalExpiringIngredients = intent.getIntExtra("total", 0)
        val reminderWithinDays = intent.getIntExtra("days", 0)

        Log.d("totalNotif", totalExpiringIngredients.toString())

        // Create an Intent to open your MainActivity
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelID)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.final_logo))
            .setSmallIcon(R.drawable.final_logo)
            .setContentTitle("Hey! Don't forget!")
            .setContentText("$totalExpiringIngredients ingredients are going to expire within $reminderWithinDays days")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)
    }
}
