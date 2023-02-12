package notifications

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONException

class Notifications: CordovaPlugin() {
  companion object {
    private const val TAG = "pushNotification"
  }
  var lastTapedNotification = ""
  var targetDomain = "";

  override fun pluginInitialize() {
    Log.d(TAG, "initialize")
    val activity = cordova.activity
    val intent = activity.intent
    val extras = intent.extras
    val data = intent.data

    val metaData = cordova.context.getPackageManager().getApplicationInfo(cordova.context.getPackageName(), PackageManager.GET_META_DATA).metaData
    targetDomain = metaData.getString("org.makesoil.app.targetdomain", "<no target domain>")
    Log.d(TAG, "targetDomain=" + targetDomain)

    if (extras != null) {
      val payload = extras.getString("pushNotification")
      if (payload != null) {
        lastTapedNotification = payload
      }
    }
    else if(data != null) {
      lastTapedNotification = data.toString();
    }

    Log.d(TAG, "notificationUrl=" + lastTapedNotification)

    super.pluginInitialize()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (intent != null && (intent.action == "android.intent.action.MAIN" || intent.action == "android.intent.action.VIEW") && intent.data != null) {
        lastTapedNotification = intent.data.toString()
        Log.d(TAG, "smartLinkUrl=" + lastTapedNotification)
    }
  }

  @Throws(JSONException::class)
  override fun execute(action: String, data: JSONArray, callbackContext: CallbackContext): Boolean {
    val context = callbackContext
    var result = true
    try {
      when (action) {
        "registration" -> {
          cordova.threadPool.execute {
            Log.d(TAG, "get token")
            getFirebaseToken(context)
          }
        }
        "tapped" -> {
          cordova.threadPool.execute {
            val res = lastTapedNotification.replace(targetDomain ,"")
            lastTapedNotification = ""
            Log.d(TAG, "notification tapped => " + res)
            context.success(res)
          }
        }
        else -> {
          handleError("Invalid action", context)
          result = false
        }
      }
    } catch (e: Exception) {
      handleError(e.toString(), context)
      result = false
    }

    return result
  }

  private fun getFirebaseToken(context: CallbackContext) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String> ->
      if (!task.isSuccessful) {
        return@addOnCompleteListener
      }
      var token = task.result
      Log.d("FIREBASE TOKEN", token)
      context.success(token)
    }
  }

  private fun handleError(errorMsg: String, context: CallbackContext) {
    try {
      Log.e(TAG, errorMsg)
      context.error(errorMsg)
    } catch (e: Exception) {
      Log.e(TAG, e.toString())
    }
  }
}