package me.simon76800.yrdsbstudentplanner.util;

/**
 * Helper functions for displaying time.
 */
public class TimeUtils {
    public static boolean use24HourClock = true; // to display time as 22:00 or 10:00 PM

    /**
     *
     * @param hour hour value (0 - 24)
     * @param minute minute value (0 - 59)
     * @return appropriate string display for given time
     */
    public static String displayString(int hour, int minute) {
        if(use24HourClock) {
            return hour + ":" + (minute < 10 ? "0" : "") + minute;
        }

        return ((hour - 1) % 12 + 1) + ":" + (minute < 10 ? "0" : "") + minute + " " + (hour < 12 ? "AM" : "PM");
    }
}
