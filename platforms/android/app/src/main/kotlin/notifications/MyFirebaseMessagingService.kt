package notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "pushService"
    }

    private val defaultNotificationChannelID = "general"
    private var notificationManager: NotificationManager? = null
    private var currentToken = ""

    private var mainActivity: Class<*>? = null
    private var notificationIcons = 0
    private var notificationColor = 0

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "newToken: " + token)
        currentToken = token;
    }

    override fun onCreate() {
        notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        Log.d(TAG, "creating push service")
        try {
            val launchIntent: Intent? =
                packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            val className = launchIntent?.component?.className as String
            mainActivity = Class.forName(className)
            notificationIcons =
                resources.getIdentifier("notification_icons", "drawable", packageName)
            notificationColor =
                resources.getIdentifier("cdv_splashscreen_background", "color", packageName)

            val channel: NotificationChannel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = NotificationChannel(
                    defaultNotificationChannelID,
                    "Miscellaneous",
                    NotificationManager.IMPORTANCE_HIGH
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    channel.setShowBadge(true)
                }
                notificationManager!!.createNotificationChannel(channel)
                Log.d(TAG, "creating channel: " + channel.id)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to load data from AndroidManifest.xml", e)
        }
        super.onCreate()
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        val data = p0.data
        for ((key, value) in data) {
            Log.d(TAG, "notification: $key => $value")
        }
        sendNotification(data)
    }

    private fun sendNotification(data: Map<String, String>) {
        val title = data["title"] ?: "MakeSoil"
        val body = data["body"] ?: ""
        val channelId = data["android_channel_id"] ?: defaultNotificationChannelID
        val action = data["click_action"]
        val badgeCount = data["notification_count"] ?: "0"

        val resultIntent = Intent(this, mainActivity)

        if (action != null) {
            resultIntent.putExtra("pushNotification", action)
        }

        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            if (Build.VERSION.SDK_INT >= 31) {
                getPendingIntent(
                    101,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                getPendingIntent(101, PendingIntent.FLAG_CANCEL_CURRENT)
            }
        }

        // Create notification
        val soundUri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/raw/" + channelId)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(notificationIcons)
            .setColor(getColorWrapper(notificationColor))
            .setSound(soundUri)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setNumber(badgeCount.toInt())
            .setContentIntent(resultPendingIntent)

        with(NotificationManagerCompat.from(this)) {
            val notificationId = (AtomicInteger(0)).incrementAndGet()
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun getColorWrapper(colorId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resources.getColor(colorId, null)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(colorId)
        }
    }
}
