package il.co.radioapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import il.co.radioapp.service.RadioPlayerService

/**
 * Handles notification action buttons (Play/Pause/Next/Prev/Stop).
 * Using a BroadcastReceiver is more reliable than PendingIntent.getService()
 * for foreground MediaSessionService on Android 12+.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, RadioPlayerService::class.java).apply {
            this.action = action
        }
        context.startService(serviceIntent)
    }
}



