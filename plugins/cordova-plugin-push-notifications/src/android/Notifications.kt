package notifications

import android.util.Log
import com.google.android.gms.tasks.Task
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONException
import com.google.firebase.messaging.FirebaseMessaging

class Notifications: CordovaPlugin() {
  companion object {
    private const val TAG = "pushNotification"
  }
  var lastTapedNotification = ""

  override fun pluginInitialize() {
    Log.d(TAG, "initialize")
    val activity = cordova.activity
    val extras = activity.intent.extras
    if (extras != null) {
      val payload = extras.getString("pushNotification")
      if (payload != null) {
        lastTapedNotification = payload
      }
    }
    super.pluginInitialize()
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
            Log.d(TAG, "notification tapped")
            val res = lastTapedNotification
            lastTapedNotification = ""
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