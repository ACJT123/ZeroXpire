package my.edu.tarc.zeroxpire

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationServiceStarterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            NotificationEventReceiver.setupAlarm(context)
        }
    }
}