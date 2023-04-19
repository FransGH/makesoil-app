package notifications

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONException

class Notifications : CordovaPlugin() {
    companion object {
        private const val TAG = "pushNotification"
    }

    var lastTapedNotification = ""
    var targetDomain = ""

    override fun pluginInitialize() {
        val metaData = cordova.context.getPackageManager().getApplicationInfo(
            cordova.context.getPackageName(),
            PackageManager.GET_META_DATA
        ).metaData
        targetDomain = metaData.getString("org.makesoil.app.targetdomain", "<no target domain>")
        Log.d(TAG, "Initialize: targetDomain=" + targetDomain + " intent=" + cordova.activity.intent.toString())

        getSmartLink(cordova.activity.intent)
        super.pluginInitialize()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getSmartLink(intent)
    }

    fun getSmartLink(intent: Intent?) {
        intent?.let {
            val extras = it.extras
            val data = it.data
            Log.d(TAG, "Checking intent: action=${it.action} data=$data extras=${extras?.getString("pushNotification")}")
            if (it.action == "android.intent.action.MAIN" || it.action == "android.intent.action.VIEW") {
                lastTapedNotification = extras?.getString("pushNotification") ?: data?.toString() ?: ""
                Log.d(TAG, "smartLinkUrl=$lastTapedNotification")
            }
            else {
                Log.d(TAG, "No data or extra, smartLinkUrl not set")
            }
        }
    }

    @Throws(JSONException::class)
    override fun execute(
        action: String,
        data: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
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
                        val res = lastTapedNotification.replace(targetDomain, "")
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
            val token = task.result
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