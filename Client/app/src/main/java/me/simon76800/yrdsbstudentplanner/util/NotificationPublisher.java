package me.simon76800.yrdsbstudentplanner.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import me.simon76800.yrdsbstudentplanner.CalendarActivity;
import me.simon76800.yrdsbstudentplanner.MainActivity;
import me.simon76800.yrdsbstudentplanner.R;

/**
 * Manages this app's Android push notifications.
 * Mostly needed for calendar reminders!
 */
public class NotificationPublisher extends BroadcastReceiver {
    public static final String EVENT_PENDING = "EVENT_PENDING";
    public static final String AGENDA_PENDING = "AGENDA_PENDING";

    private static final String NOTIFICATION = "Notification";
    private static final String NOTIFICATION_ID = "Notification_ID";

    private static final String INTENT_ACTION = "me.simon76800.action.ALARM";

    public static String notifChannelName;
    public static NotificationChannel notifChannel;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!SettingsActivity.enabledNotifications) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);

        if (notificationId == AgendaActivity.NOTIFICATION_ID) {
            Log.i(MainActivity.LOG_TAG, "Notification received!");
            notificationManager.notify(notificationId, notification);
        } else {
            for (CalendarHandler.CalendarEvent e : CalendarActivity.calendarHandler.EVENTS_LIST) {
                if (e.notificationID == notificationId) { // If notification is still scheduled
                    Log.i(MainActivity.LOG_TAG, "Notification received!");
                    notificationManager.notify(notificationId, notification);
                }
            }
        }
    }

    /**
     * Schedules a delayed notification
     *
     * @param context instance of activity
     * @param channel channel through which notification is fired (applies to SKK 26 and up)
     * @param time the currentTimeMillis value of the notification
     * @param notificationId the id tag of notification
     * @param title title of event
     * @param content the content of the notification (body text)
     */
    public void scheduleNotification(Context context, String channel, long time, int notificationId, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.yrdsb_logo_colour)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).setAutoCancel(true);

        Intent intent = new Intent(context, IntentActivity.class); // Activity to open upon click
        intent.putExtra(EVENT_PENDING, notificationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        PendingIntent activity = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(activity);

        Notification notification = builder.build();

        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        notificationIntent.putExtra((notificationId == 2 ? AGENDA_PENDING : EVENT_PENDING), notificationId);
        notificationIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        notificationIntent.setAction(INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        else alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);


        Log.i(MainActivity.LOG_TAG, "Notification set! (ID: " + notificationId + ")");
    }
}
