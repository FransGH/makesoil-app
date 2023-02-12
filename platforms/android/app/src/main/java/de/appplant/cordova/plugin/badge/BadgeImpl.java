/*
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 */

package de.appplant.cordova.plugin.badge;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import me.leolin.shortcutbadger.ShortcutBadger;

import static me.leolin.shortcutbadger.ShortcutBadger.isBadgeCounterSupported;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Implementation of the badge interface methods.
 */
@SuppressWarnings("WeakerAccess")
public final class BadgeImpl {

    private static final String TAG = "BADGE";

    // The name for the shared preferences key
    private static final String BADGE_KEY = "badge";

    // The name for the shared preferences key
    private static final String CONFIG_KEY = "badge.config";

    // The application context
    private final Context ctx;

    // if the device does support native badges
    private final boolean isSupported;

    private NotificationCompat.Builder mBuilder = null;

    /**
     * Initializes the impl with the context of the app.
     *
     * @param context The app context.
     */
    public BadgeImpl (Context context) {
        if (isBadgeCounterSupported(context)) {
            ctx         = context;
            isSupported = true;
        } else {
            ctx         = context.getApplicationContext();
            isSupported = isBadgeCounterSupported(ctx);
        }
        updateNotification(getBadge());
        ShortcutBadger.applyCount(ctx, getBadge());
    }

    /**
     * Clear the badge number.
     */
    public void clearBadge() {
        saveBadge(0);
        ShortcutBadger.removeCount(ctx);
    }

    /**
     * Get the badge number.
     *
     * @return The badge number
     */
    public int getBadge() {
        return getPrefs().getInt(BADGE_KEY, 0);
    }

    /**
     * Check if the device/launcher does support badges.
     */
    public boolean isSupported() {
        return isSupported;
    }

    /**
     * Set the badge number.
     *
     * @param badge The number to set as the badge number.
     */
    public void setBadge (int badge) {
        saveBadge(badge);
        ShortcutBadger.applyCount(ctx, badge);
    }

    public void updateNotification(int badge) {
        if(badge == 0) {
            NotificationManagerCompat.from(ctx).cancelAll();
            mBuilder = null;
        }
        else {
            if (mBuilder == null) {
                mBuilder = new NotificationCompat.Builder(ctx, "general");
            }

            try {
                Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
                String className = launchIntent.getComponent().getClassName();
                Class mainActivityClass = Class.forName(className).getClass();
                Intent intent = new Intent (ctx, mainActivityClass);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
//                stackBuilder.addParentStack(mainActivityClass);
                stackBuilder.addNextIntent(intent);
                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                mBuilder.setNumber(badge)
                        .setSmallIcon(ctx.getResources().getIdentifier("notification_icons", "drawable", ctx.getPackageName()))
                        .setColor(ctx.getResources().getIdentifier("cdv_splashscreen_background", "color", ctx.getPackageName()))
                        .setContentTitle("MakeSoil")
                        .setContentText(String.format("You have %d unread %s", badge, badge > 1 ? " messages" : "message"))
//                    .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(pendingIntent)
                        .setSilent(true)
                        .setPriority(Notification.PRIORITY_MIN);
                NotificationManagerCompat.from(ctx).notify(0xFBFB, mBuilder.build());
            }
            catch (Exception e) {
                Log.d(TAG, e.toString());
            }

        }
    }

    /**
     * Get the persisted config map.
     */
    public JSONObject loadConfig() {
        String json = getPrefs().getString(CONFIG_KEY, "{}");

        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /**
     * Persist the config map so that `autoClear` has same value after restart.
     *
     * @param config The config map to persist.
     */
    public void saveConfig (JSONObject config) {
        SharedPreferences.Editor editor = getPrefs().edit();

        editor.putString(CONFIG_KEY, config.toString());
        editor.apply();
    }

    /**
     * Persist the badge of the app icon so that `getBadge` is able to return
     * the badge number back to the client.
     *
     * @param badge The badge number to persist.
     */
    private void saveBadge (int badge) {
        Log.d(TAG, "set count=" + badge);
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putInt(BADGE_KEY, badge);
        editor.apply();
        updateNotification(badge);
    }

    /**
     * The Local storage for the application.
     */
    private SharedPreferences getPrefs() {
        return ctx.getSharedPreferences(BADGE_KEY, Context.MODE_PRIVATE);
    }

}
