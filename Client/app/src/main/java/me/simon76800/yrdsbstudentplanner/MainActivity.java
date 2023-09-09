package me.simon76800.yrdsbstudentplanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import me.simon76800.yrdsbstudentplanner.client.AppClient;
import me.simon76800.yrdsbstudentplanner.client.KeyHandler;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

/**
 * Starting point of the application after the splash screen.
 * Reads local storage to determine if the user is a returning user.
 */
public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = "YRDSB Student Planner";

    private static MainActivity context;

    public static AppClient client;

    public static String studentNumber = "";
    public static String uniqueID = "";
    public static String rawCalendarData = "";
    public static String rawAnnouncementsData = "";
    public static School school;

    public static float DP_WIDTH;
    public static float DP_HEIGHT;
    public static float PX_WIDTH;
    public static float PX_HEIGHT;
    private static float pixelDensity;

    public static boolean requiresInit = false;

    private static Thread idleThread;
    private static boolean idleThreadRunning;

    // List of intents used to ensure notifications work properly on all phones
    public static final Intent[] POWER_MANAGER_INTENTS = {
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        stopIdleThread();

        if (!requiresInit) {
            // If back button is pressed
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);

            requiresInit = true;
        } else {

            try {
                // Generate keys
                KeyHandler.generateKeys();
            } catch (Exception e) {
                e.printStackTrace();
            }

            requiresInit = false;

            Log.i(MainActivity.LOG_TAG, "Launching YRDSB Student Planner app");
            setContentView(R.layout.activity_main);

            boolean newClient = false;
            if(client == null) {
                newClient = true;
                client = new AppClient(); // Must create new client because async task can only execute once
            }

            try {
                startService(new Intent(this, BackgroundService.class));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to start background service!");
            }

            Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            // Measures screen dimensions
            pixelDensity = getResources().getDisplayMetrics().density;
            MainActivity.DP_WIDTH = displayMetrics.widthPixels / pixelDensity;
            MainActivity.DP_HEIGHT = displayMetrics.heightPixels / pixelDensity;
            MainActivity.PX_WIDTH = displayMetrics.widthPixels;
            MainActivity.PX_HEIGHT = displayMetrics.heightPixels;

            // Creates notification channel
            createNotificationChannel();

            // Verifies and loads save data
            boolean hasData = StorageHandler.loadData(context);

            if (!hasData) {
                // If no save data exists (first time use of app)
                uniqueID = UUID.randomUUID().toString();
                boolean requirePrompt = false;

                for (Intent intent : POWER_MANAGER_INTENTS) {
                    if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        EnableNotifDialog dialog = new EnableNotifDialog();

                        dialog.show(getSupportFragmentManager(), "enable_notif");
                        requirePrompt = true;

                        break;
                    }
                }

                if (!requirePrompt) {
                    Log.i(LOG_TAG, "Enable notification request not needed");

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(FLAG_ACTIVITY_NEW_DOCUMENT | FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            } else {
                Log.i(LOG_TAG, "Welcome back! Initialization almost complete...");

                client.previousUser = true;

                initNotifs();

                Intent intent = new Intent(this, CalendarActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_DOCUMENT | FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0); // cancel animation
            }

            // Begins asynchronous client
            try {
                if(newClient) client.execute();
            } catch (Exception e) {
                Log.d(LOG_TAG, "Client already running");
            }
        }
    }

    /**
     * Converts a given dpi measurement into pixels
     *
     * @param dp dpi measurement
     * @return same value in pixels
     */
    public static int convertToPx(int dp) {
        return (int) (dp * pixelDensity);
    }

    /**
     * Turns off requiresInit for 500 ms
     */
    public static void noInitThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                MainActivity.requiresInit = false;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                MainActivity.requiresInit = true;
            }
        };
        thread.start();
    }

    /**
     * Begin running idle thread that keeps app alive in background
     */
    public static void startIdleThread() {
        idleThreadRunning = true;

        idleThread = new Thread() {
            @Override
            public void run() {
                while (idleThreadRunning) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        // No action required
                    }
                }
            }
        };
        idleThread.start();

        Log.d(MainActivity.LOG_TAG, "Idle mode started");
    }

    /**
     * Stops idle thread when app is re-opened by user
     */
    public static void stopIdleThread() {
        try {
            idleThreadRunning = false;
            idleThread.interrupt();
        } catch (Exception e) {
            // No action required
        }

        Log.d(MainActivity.LOG_TAG, "Idle mode stopped");
    }

    public static MainActivity getContext() {
        return context;
    }

    /**
     * Enum constants for all available secondary schools within the app
     */
    public enum School {
        BSS("BSS", "Bayview Secondary School");

        public String ID;
        public String name;

        School(String ID, String name) {
            this.ID = ID;
            this.name = name;
        }
    }
}
