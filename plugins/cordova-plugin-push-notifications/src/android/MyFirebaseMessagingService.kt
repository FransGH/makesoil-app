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
import org.staging.makesoil.app.MainActivity
import org.staging.makesoil.app.R
import java.util.concurrent.atomic.AtomicInteger


class MyFirebaseMessagingService : FirebaseMessagingService() {
  companion object {
    private const val TAG = "pushService"
  }

  val mainfestIconKey = "com.google.firebase.messaging.default_notification_icon"
  val mainfestChannelKey = "com.google.firebase.messaging.default_notification_channel_id"
  val mainfestColorKey = "com.google.firebase.messaging.default_notification_color"

  private var defaultNotificationIcon = 0
  private var defaultNotificationColor = 0
  private var defaultNotificationChannelID = "general"
  private var notificationManager: NotificationManager? = null

  var mainActivity: Class<*>? = null

  override fun onCreate() {
    notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)

    Log.d(TAG, "create");

    try {
      // Get MainActivity without import
      var launchIntent: Intent? =
        packageManager.getLaunchIntentForPackage(applicationContext.packageName)
      var className = launchIntent?.component?.className as String
      mainActivity = Class.forName(className)

      // Other
      val ai = packageManager.getApplicationInfo(
        applicationContext.packageName,
        PackageManager.GET_META_DATA
      )

      defaultNotificationChannelID = ai.metaData.getString(mainfestChannelKey, "general")
      val channel: NotificationChannel
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        channel = NotificationChannel(
          defaultNotificationChannelID,
          "Miscellaneous",
          NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager!!.createNotificationChannel(channel)
        Log.d(TAG, "creating channel: " + channel.id);
      }
      defaultNotificationIcon = ai.metaData.getInt(mainfestIconKey, ai.icon)
      defaultNotificationColor = ai.metaData.getInt(mainfestColorKey, 0)
    } catch (e: PackageManager.NameNotFoundException) {
      Log.e(TAG, "Failed to load data from AndroidManifest.xml", e)
    }
    super.onCreate()
  }

  override fun onMessageReceived(p0: RemoteMessage) {
    super.onMessageReceived(p0)

    if (p0 !== null && p0.data !== null) {
      var data = p0.data;
      for ((key, value) in data) {
        Log.d(TAG, "notification: " + key + " => " + value);
      }
      sendNotification(data)
    } else {
      Log.d(TAG, "missing notification: " + p0.toString())
    }
  }

  private fun sendNotification(data: Map<String, String>) {
    val title = data["title"]
    val body = data["body"]
    var channel_id = data["android_channel_id"]
    var action = data["click_action"]

    var resultIntent = Intent(this, mainActivity)

    if (action != null) {
      resultIntent.putExtra("pushNotification", action)
    }

    if (channel_id == null) {
      channel_id = defaultNotificationChannelID
    }

    /*
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
     */

    val notifyIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val resultPendingIntent = PendingIntent.getActivity(
      this, 0, notifyIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Create notification
    var soundUri =
      Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/raw/" + channel_id)

    val notificationBuilder = NotificationCompat.Builder(this, channel_id)
      .setSmallIcon(R.drawable.notification_icons)
      .setColor(resources.getColor(R.color.cdv_splashscreen_background))
      .setSound(soundUri)
      .setContentTitle(title)
      .setContentText(body)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(resultPendingIntent)

    with(NotificationManagerCompat.from(this)) {
      val notificationId = (AtomicInteger(0)).incrementAndGet()
      notify(notificationId, notificationBuilder.build())
    }
  }
}
