package me.simon76800.yrdsbstudentplanner.util;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import me.simon76800.yrdsbstudentplanner.CalendarActivity;
import me.simon76800.yrdsbstudentplanner.MainActivity;

/**
 * Handles local storage for the application.
 * Saved data includes reminders, agenda entries, settings, etc.
 */
public class StorageHandler {
    private static final String FILENAME = "savedata";
    private static final String DELIM = "$$$$";

    private static final int MAX_INPUT_LENGTH = 1000000;

    public static boolean loadingComplete = true; // Whether file reading has been completed

    /**
     * Attempts to load all saved data on device
     *
     * @param context an instance of any Activity (used to determine location of saved data)
     * @return if saved data file exists
     */
    public static boolean loadData(Context context) {
        loadingComplete = false;

        try {
            FileInputStream fileInputStream = context.openFileInput(FILENAME);

            byte[] inputArray = new byte[MAX_INPUT_LENGTH];
            int length = fileInputStream.read(inputArray);

            fileInputStream.close();

            inputArray = Arrays.copyOf(inputArray, length); // Resize array to fit data

            final String rawString = new String(inputArray, StandardCharsets.UTF_8);
            Log.i(MainActivity.LOG_TAG, "File reading complete! Length: " + length + " bytes (String length: " + rawString.length() + " bytes)");

            Thread loadThread = new Thread() {
                @Override
                public void run() {
                    final String[] parts = rawString.split("\\$\\$\\$\\$");

                    MainActivity.studentNumber = parts[0];
                    MainActivity.uniqueID = parts[1];
                    MainActivity.rawCalendarData = parts[2];
                    MainActivity.school = MainActivity.School.BSS;

                    String schoolID = parts[3];
                    for (MainActivity.School s : MainActivity.School.values())
                        if (s.ID.equalsIgnoreCase(schoolID)) MainActivity.school = s;

                    // Loads saved calendar data
                    if (CalendarActivity.calendarHandler == null)
                        CalendarActivity.calendarHandler = new CalendarHandler();
                    CalendarActivity.calendarHandler.parseString(MainActivity.rawCalendarData);

                    // Load and set reminder times
                    if (parts.length > 4) {
                        String[] reminderEvents = parts[4].split("X");

                        Log.d(MainActivity.LOG_TAG, "String lengths -> calendar:" + parts[2].length() + " reminder:" + parts[4].length());

                        for (String split : reminderEvents) {
                            String[] splitParts = split.split(":");
                            for (CalendarHandler.CalendarEvent event : CalendarActivity.calendarHandler.EVENTS_LIST) {
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
                    }

                    // Load announcements
                    if (parts.length > 5) {
                        MainActivity.rawAnnouncementsData = parts[5];
                    }

                    // Load agenda courses
                    if (parts.length > 6) {
                        AgendaActivity.COURSE_LIST.clear();

                        String[] courseParts = parts[6].split("%%%%");
                        for (String s : courseParts) {
                            if (s.isEmpty()) continue;

                            int colourID = Integer.parseInt(s.substring(0, s.indexOf(':')));
                            s = s.substring(s.indexOf(':') + 1);
                            int iconID = Integer.parseInt(s.substring(0, s.indexOf(':')));
                            s = s.substring(s.indexOf(':') + 1);

                            AgendaActivity.COURSE_LIST.add(new AgendaActivity.AgendaCourse(s, colourID, iconID));
                        }
                    }

                    // Load agenda items
                    int agendaItemCount = 0;
                    if (parts.length > 7) {
                        AgendaActivity.AGENDA_ITEMS.clear();

                        String[] agendaParts = parts[7].split("%%%%%");
                        for (String s : agendaParts) {
                            if (s.isEmpty()) continue;

                            String[] subSplit = s.split("%%%%");

                            AgendaActivity.AgendaCourse curCourse = null;
                            for (AgendaActivity.AgendaCourse course : AgendaActivity.COURSE_LIST)
                                if (subSplit.length > 4 ? course.courseName.equalsIgnoreCase(subSplit[3]) : course.courseName.equalsIgnoreCase(subSplit[2]))
                                    curCourse = course;

                            if (curCourse != null) { // Only add item if there is a valid course
                                String dateString = subSplit[1].contains("_") ? subSplit[1].substring(0, subSplit[1].indexOf('_')) : subSplit[1];
                                int dueHour = subSplit[1].contains("_") ? Integer.parseInt(subSplit[1].substring(subSplit[1].indexOf('_') + 1, subSplit[1].indexOf(':'))) : -1;
                                int dueMinute = subSplit[1].contains("_") ? Integer.parseInt(subSplit[1].substring(subSplit[1].indexOf(':') + 1)) : -1;

                                String completed = subSplit.length > 4 ? subSplit[4] : subSplit[3];

                                AgendaActivity.AgendaItem newItem = new AgendaActivity.AgendaItem(subSplit[0], dateString, subSplit.length > 4 ? subSplit[2] : "", curCourse);
                                AgendaActivity.AGENDA_ITEMS.add(newItem);
                                newItem.dueHour = dueHour;
                                newItem.dueMinute = dueMinute;
                                newItem.completed = completed.equalsIgnoreCase("1");

                                agendaItemCount++;
                            }
                        }
                    }

                    // Load settings
                    if (parts.length > 8) {
                        String[] settings = parts[8].split(":");

                        TimeUtils.use24HourClock = Boolean.parseBoolean(settings[0]);
                        SettingsActivity.enabledNotifications = Boolean.parseBoolean(settings[1]);
                        AgendaActivity.generalHomeworkCheck = Boolean.parseBoolean(settings[2]);
                        AgendaActivity.specificHomeworkCheck = Boolean.parseBoolean(settings[3]);
                        AgendaActivity.reminderHour = Integer.parseInt(settings[4]);
                        AgendaActivity.reminderMinute = Integer.parseInt(settings[5]);
                        AgendaActivity.reminderOffset = Integer.parseInt(settings[6]);
                    }

                    Log.i(MainActivity.LOG_TAG, "Successfully loaded in " + agendaItemCount + " agenda items.");

                    loadingComplete = true;
                }
            };
            loadThread.start();
            return true;
        } catch (IOException e) { // if save data does not exist
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Attempts to write all data to file
     *
     * @param context an instance of any Activity (used to determine location of saved data)
     */
    public static void writeData(Context context) {
        if (MainActivity.uniqueID.equals("")) {
            Log.i(MainActivity.LOG_TAG, "User not logged in, data saving skipped");
            return;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            Log.i(MainActivity.LOG_TAG, "Saving data...");

            // Create reminder data string
            StringBuilder reminderString = new StringBuilder("X");

            for (CalendarHandler.CalendarEvent event : CalendarActivity.calendarHandler.EVENTS_LIST) {
                if (event.hasReminder()) {
                    String toAdd = event.condense() + ":" + event.reminderYear + ":" + event.reminderMonth + ":" + event.reminderDay + ":" + event.reminderHour + ":" + event.reminderMinute + ":" + event.notificationID + "X";
                    reminderString.append(toAdd);
                }
            }

            // Create agenda course list
            StringBuilder courseString = new StringBuilder("%%%%");
            for (AgendaActivity.AgendaCourse course : AgendaActivity.COURSE_LIST) {
                String toAdd = course.colourID + ":" + course.iconID + ":" + course.courseName + "%%%%";
                courseString.append(toAdd);
            }

            // Create agenda item list
            StringBuilder agendaString = new StringBuilder("%%%%%");
            for (AgendaActivity.AgendaItem item : AgendaActivity.AGENDA_ITEMS) {
                String toAdd = item.itemName + "%%%%" + item.dueDate + (item.dueHour != -1 ? "_" + item.dueHour + ":" + item.dueMinute : "") + "%%%%" + (item.notes.isEmpty() ? "" : item.notes + "%%%%") + item.course.courseName + "%%%%" + (item.completed ? "1" : "0") + "%%%%%";
                agendaString.append(toAdd);
            }

            // Create settings list
            String settingsString = TimeUtils.use24HourClock + ":" + SettingsActivity.enabledNotifications + ":" + AgendaActivity.generalHomeworkCheck + ":" + AgendaActivity.specificHomeworkCheck + ":" + AgendaActivity.reminderHour + ":" + AgendaActivity.reminderMinute + ":" + AgendaActivity.reminderOffset;
            
            // Put it all together!
            String dataToWrite = MainActivity.studentNumber + DELIM + MainActivity.uniqueID + DELIM + MainActivity.rawCalendarData + DELIM + reminderString.toString() + DELIM + MainActivity.rawAnnouncementsData + DELIM + courseString.toString() + DELIM + agendaString.toString() + DELIM + settingsString;

            fileOutputStream.write(dataToWrite.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();

            Log.i(MainActivity.LOG_TAG, "Data successfully saved! Length: " + dataToWrite.getBytes(StandardCharsets.UTF_8).length + " bytes (character length: " + dataToWrite.length() + ")");
        } catch (IOException e) {
            Log.e(MainActivity.LOG_TAG, "Could not save data to file!");
            e.printStackTrace();
        }
    }
}

