package me.simon76800.yrdsbstudentplanner.util;

import android.util.Log;
import android.widget.ImageButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import me.simon76800.yrdsbstudentplanner.CalendarActivity;
import me.simon76800.yrdsbstudentplanner.MainActivity;

/**
 * Manages the data shown in the CalendarActivity.
 */
public final class CalendarHandler {
    public static final int TOTAL_YEARS = 5;

    public static int todayYear;
    public static int todayMonth;
    public static int todayDay;

    // All events in calendar
    public List<CalendarEvent> EVENTS_LIST = new ArrayList<>();

    // [year][month][row][column]; year 1 for current year, month 0 is January
    public final int[][][][] DATES = new int[TOTAL_YEARS][12][6][7];
    public int YEAR_ZERO;

    // [row][column]
    private final ImageButton[][] DATE_BUTTONS = new ImageButton[6][7];

    private CalendarActivity activity;

    public int currentYear;
    public int currentMonth;
    public int currentDay;

    public CalendarHandler() {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1; // Java Calendar months start with 0
        YEAR_ZERO = currentYear - 1;

        todayYear = currentYear;
        todayMonth = currentMonth;
        todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        int century = (currentYear - 1) / 100;
        int year = (currentYear - 2) % 100;
        int dayOfWeek = (int) ((29 - 2 * century + year + Math.floor(year / 4) + Math.floor(century / 4)) % 7);

        for (int i = 0; i < TOTAL_YEARS; i++) {
            for (int j = 0; j < 12; j++) {
                int curRow = 0;
                for (int k = 1; k <= numDays(j + 1, currentYear - 1 + i); k++) {
                    DATES[i][j][curRow][dayOfWeek] = k;
                    dayOfWeek++;
                    if (dayOfWeek >= 7) {
                        dayOfWeek = 0;
                        curRow++;
                    }
                }
            }
        }
    }

    public void nextMonth() {
        if (currentYear - YEAR_ZERO == TOTAL_YEARS && currentMonth == 12) return;
        currentMonth++;
        if (currentMonth > 12) {
            currentMonth = 1;
            currentYear++;
        }
        changeMonth();
    }

    public void prevMonth() {
        if (currentYear == YEAR_ZERO && currentMonth == 1) return;
        currentMonth--;
        if (currentMonth < 1) {
            currentMonth = 12;
            currentYear--;
        }
        changeMonth();
    }

    /**
     * Shared method calls when month is changed (prev/next)
     */
    private void changeMonth() {
        activity.initButtons();
        activity.monthText.setText(getMonthString().substring(0, 3).toUpperCase());
        activity.yearText.setText(String.valueOf(currentYear));
        activity.clearEvents();
    }

    public void setActivity(CalendarActivity activity) {
        this.activity = activity;
    }

    /**
     * Sets date on given parameters of DATES (current year, month)
     *
     * @param row row on screen calendar
     * @param col column on screen calendar
     */
    public void setDay(int row, int col) {
        int date = DATES[currentYear - YEAR_ZERO][currentMonth - 1][row][col];
        if (date == 0) return;
        currentDay = date;
        activity.updateEventScroll();
    }

    /**
     * @param year year to calculate
     * @param month month, numeric form (1-12)
     * @param day day (e.g. 22)
     * @return number of events in said day
     */
    public int getNumEvents(int year, int month, int day) {
        int counter = 0;
        int requestTime = year * 10000 + month * 100 + day;

        for (CalendarEvent e : EVENTS_LIST) {
            if (eventOnDay(e, requestTime)) counter++;
        }

        return counter;
    }

    /**
     * Determines if given event occurs on given date
     *
     * @param e instance of CalendarEvent
     * @param requestTime an integer value as follows:    year * 10000 + month * 100 + day
     * @return if the date of the event matches the given
     */
    public boolean eventOnDay(CalendarEvent e, int requestTime) {
        int startTime = e.startYear * 10000 + e.startMonth * 100 + e.startDay;
        int endTime = e.endYear * 10000 + e.endMonth * 100 + e.endDay;
        return startTime <= requestTime && requestTime <= endTime;
    }

    /**
     * Constructs events out of given strings
     *
     * @param str message from server to parse
     */
    public void parseString(String str) {
        // Save reminder strings to re-apply after reload
        List<String> reminders = new ArrayList<>();
        for (CalendarEvent event : CalendarActivity.calendarHandler.EVENTS_LIST) {
            if (event.hasReminder()) {
                String toAdd = event.condense() + ":" + event.reminderYear + ":" + event.reminderMonth + ":" + event.reminderDay + ":" + event.reminderHour + ":" + event.reminderMinute + ":" + event.notificationID;
                reminders.add(toAdd);
            }
        }

        EVENTS_LIST.clear();
        MainActivity.rawCalendarData = str;

        String[] array = str.substring(8).split("##");
        for (int i = 0; i < array.length; i++)
            EVENTS_LIST.add(new CalendarEvent(array[i]));

        for (String split : reminders) {
            String[] splitParts = split.split(":");
            for (CalendarEvent event : CalendarActivity.calendarHandler.EVENTS_LIST) {
                if (event.condense().equals(splitParts[0])) {
                    event.reminderYear = Short.parseShort(splitParts[1]);
                    event.reminderMonth = Byte.parseByte(splitParts[2]);
                    event.reminderDay = Byte.parseByte(splitParts[3]);
                    event.reminderHour = Byte.parseByte(splitParts[4]);
                    event.reminderMinute = Byte.parseByte(splitParts[5]);
                    if (splitParts.length > 6) {
                        event.notificationID = Integer.parseInt(splitParts[6]);
                        Log.d(MainActivity.LOG_TAG, "Loaded event with reminder... ID: " + event.notificationID);
                    }
                }
            }
        }

        Log.i(MainActivity.LOG_TAG, "Successfully loaded in all " + EVENTS_LIST.size() + " calendar events!");
    }

    /**
     * @param month month to convert
     * @return the current month, converted to String (e.g. 9 -> September)
     */
    public String getMonthString(int month) {
        switch (month) {
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";
        }
        return null;
    }

    public String getMonthString() {
        return getMonthString(currentMonth);
    }

    /**
     * @param month month in number format (e.g. 1-12)
     * @param year  year of month (for determining leap year)
     * @return number of days in said month
     */
    private int numDays(int month, int year) {
        if ((month < 8 && month % 2 == 1) || (month > 7 && month % 2 == 0))
            return 31;
        if (month == 2)
            return (year % 4 == 0 ? 29 : 28);
        return 30;
    }

    /**
     * CalendarEvent class, which stores a calendar event and its accompanying reminder information as a single object.
     */
    public class CalendarEvent {
        public String summary;

        public short startYear;
        public byte startMonth;
        public byte startDay;
        public byte startHour = -1;
        public byte startMinute = -1;

        public short endYear;
        public byte endMonth;
        public byte endDay;
        public byte endHour = -1;
        public byte endMinute = -1;

        public String location = "";
        public String address = "";

        public short reminderYear = -1;
        public byte reminderMonth;
        public byte reminderDay;
        public byte reminderHour;
        public byte reminderMinute;
        public int notificationID = -1;

        /**
         * Processes and constructs calendar event
         *
         * @param dataTag snippet of data sent from server
         */
        public CalendarEvent(String dataTag) {
            if (dataTag == null || dataTag.equals("")) {
                return;
            }

            String[] split = dataTag.substring(1).split("\\|");
            summary = split[0];

            String[] time1 = split[1].substring(2).split("-");
            startYear = Short.parseShort(time1[0]);
            startMonth = Byte.parseByte(time1[1]);
            startDay = Byte.parseByte(time1[2].substring(0, 2));
            if (split[1].startsWith("<&")) {
                startHour = Byte.parseByte(time1[2].substring(3, 5));
                startMinute = Byte.parseByte(time1[2].substring(6, 8));
            }

            String[] time2 = split[2].substring(2).split("-");
            endYear = Short.parseShort(time2[0]);
            endMonth = Byte.parseByte(time2[1]);
            endDay = Byte.parseByte(time2[2].substring(0, 2));
            if (split[2].startsWith("<&")) {
                endHour = Byte.parseByte(time2[2].substring(3, 5));
                endMinute = Byte.parseByte(time2[2].substring(6, 8));
            } else { // Whole day event should end 1 day earlier (because end is exclusive)
                endDay--;

                if (endDay == 0) {
                    endMonth--;
                    endDay = (byte) numDays(endMonth, endYear);
                }
                if (endMonth == 0) {
                    endYear--;
                    endMonth = 12;
                    endDay = (byte) numDays(endMonth, endYear);
                }
            }

            String allLoc = split[3];
            if (!allLoc.equals("null")) {
                if (!allLoc.contains(",")) {
                    location = allLoc;

                    String[] locSplit = allLoc.split(" ");
                    for (int i = 0; i < locSplit.length; i++) {
                        try {
                            int num = Integer.parseInt(locSplit[i]);

                            for (int j = i + 1; j < locSplit.length; j++) {
                                if (locSplit[j].equalsIgnoreCase("street") || locSplit[j].equalsIgnoreCase("drive") || locSplit[j].equalsIgnoreCase("road") || locSplit[j].toUpperCase().startsWith("AVE") || locSplit[j].equalsIgnoreCase("blvd") || locSplit[j].equalsIgnoreCase("boulevard")) {
                                    int locLength = 0;
                                    for (int k = 0; k < i; k++) {
                                        locLength += locSplit[k].length() + 1;
                                    }

                                    location = allLoc.substring(0, locLength);
                                    address = allLoc.substring(locLength);

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // Element is not a number, no action required
                        }

                        if (!address.equals("")) {
                            break;
                        }
                    }
                } else {
                    location = allLoc.substring(0, allLoc.indexOf(","));

                    boolean fullAddress; // if the entire location is an address
                    try {
                        int num = Integer.parseInt(allLoc.substring(0, allLoc.indexOf(" "))); // determines if the first word is a number
                        fullAddress = true;
                    } catch (Exception e) {
                        fullAddress = false;
                    }
                    address = fullAddress ? allLoc : allLoc.substring(allLoc.indexOf(",") + 2);
                }
            }
        }

        /**
         * Sets the reminder for this event to given time
         *
         * @param year   year of reminder
         * @param month  month of reminder
         * @param day    date of reminder
         * @param hour   hour of reminder
         * @param minute minute of reminder
         */
        public void setReminder(int year, int month, int day, int hour, int minute) {
            this.reminderYear = (short) year;
            this.reminderMonth = (byte) month;
            this.reminderDay = (byte) day;
            this.reminderHour = (byte) hour;
            this.reminderMinute = (byte) minute;

            // Calculates the notification time in milliseconds
            TimeZone tz = TimeZone.getTimeZone("America/Toronto");
            TimeZone.setDefault(tz);
            Calendar cal = Calendar.getInstance(tz, Locale.CANADA);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CANADA);

            try {
                Date dateBeforeDST = df.parse(reminderYear + "-" + reminderMonth + "-" + reminderDay + " " + (reminderHour < 10 ? "0" : "") + reminderHour + ":" + (reminderMinute < 10 ? "0" : "") + reminderMinute);
                cal.setTime(dateBeforeDST);
            } catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, "Reminder could not be set!");
                return;
            }
            long timeInMillis = cal.getTimeInMillis();

            if(timeInMillis < System.currentTimeMillis()) {
                removeReminder();
                return;
            }

            // Schedules the notification
            if (this.notificationID == -1) {
                this.notificationID = (int) System.currentTimeMillis();
            }
            if (this.notificationID == -1 || this.notificationID == -2) {
                this.notificationID = 0; 
            }

            Log.d(MainActivity.LOG_TAG, "Scheduling notification with ID " + this.notificationID + "...");
            NotificationPublisher publisher = new NotificationPublisher();
            publisher.scheduleNotification(MainActivity.getContext(), NotificationPublisher.notifChannelName, timeInMillis, notificationID, String.valueOf("Event coming up on " + getMonthString(startMonth) + " " + startDay), summary);
        }

        /**
         * Refreshes the reminder (re-initializes notification)
         */
        public void refreshReminder() {
            if (hasReminder())
                setReminder(reminderYear, reminderMonth, reminderDay, reminderHour, reminderMinute);
        }

        /**
         * Resets the reminder
         */
        public void removeReminder() {
            this.reminderYear = -1;
            this.notificationID = -1;
        }

        /**
         * Check if reminder was set
         *
         * @return if reminder exists
         */
        public boolean hasReminder() {
            return this.reminderYear != -1;
        }

        /**
         * Returns a single condensed String object of the calendar event
         *
         * @return a String object containing the condensed event data
         */
        public String condense() {
            return String.valueOf(summary.hashCode()) + startYear + startMonth + startDay + startHour + startMinute + endYear + endMonth + endDay + endHour + endMinute;
        }
    }
}
