package me.simon76800.yrdsbstudentplanner;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import me.simon76800.yrdsbstudentplanner.util.CalendarHandler;
import me.simon76800.yrdsbstudentplanner.util.StorageHandler;
import me.simon76800.yrdsbstudentplanner.util.TimeUtils;

/**
 * Used to display school events loaded from each school's Google Calendar.
 * Currently only offers a monthly view.
 */
public class CalendarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int EVENTS_LIST_COMPONENTS = 10;

    public static CalendarHandler calendarHandler;
    public static CalendarHandler.CalendarEvent selectedEvent;

    public TextView monthText;
    public TextView yearText;

    private boolean checkButtons = false;
    private boolean listenersSet = false;
    private long bufferChecks = 5;

    private float slideX1, slideY1;

    public static int eventNotifID = -1; // Event notification ID, used for opening a calendar event

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        while (!StorageHandler.loadingComplete) {
            try {
                Thread.sleep(500);
                Log.i(MainActivity.LOG_TAG, "Waiting on file loading...");
            } catch (Exception e) {
                // No action required
            }
        }
        calendarHandler.setActivity(this);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorB_end));

        setContentView(R.layout.activity_calendar);

        Toolbar toolbar = findViewById(R.id.calendar_toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(1).setChecked(true);

        monthText = findViewById(R.id.month_text);
        monthText.setText(calendarHandler.getMonthString().substring(0, 3).toUpperCase());

        yearText = findViewById(R.id.year_text);
        yearText.setText(String.valueOf(calendarHandler.currentYear));

        for (int i = 0; i < EVENTS_LIST_COMPONENTS; i++) {
            // Initialize margins for all events boxes' text
            TextView eventsText = findViewById(getResources().getIdentifier("events_text_" + i, "id", getPackageName()));
            TextView eventsTimeText = findViewById(getResources().getIdentifier("events_time_" + i, "id", getPackageName()));
            TextView eventsLocText = findViewById(getResources().getIdentifier("events_location_" + i, "id", getPackageName()));

            RelativeLayout.LayoutParams eventsTextLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams eventsTimeLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams eventsLocLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            eventsTextLayoutParams.setMargins((int) (MainActivity.PX_WIDTH * 0.1), (int) (MainActivity.PX_WIDTH / 4 * 0.06), 0, 0);
            eventsTimeLayoutParams.setMargins((int) (MainActivity.PX_WIDTH * 0.1) + MainActivity.convertToPx(34), (int) (MainActivity.PX_WIDTH / 4 * 0.48), 0, 0);
            eventsLocLayoutParams.setMargins((int) (MainActivity.PX_WIDTH * 0.1) + MainActivity.convertToPx(34), (int) (MainActivity.PX_WIDTH / 4 * 0.74), 0, 0);

            eventsText.setLayoutParams(eventsTextLayoutParams);
            eventsTimeText.setLayoutParams(eventsTimeLayoutParams);
            eventsLocText.setLayoutParams(eventsLocLayoutParams);
        }

        findViewById(R.id.calendar_arrow_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarHandler.prevMonth();
            }
        });
        findViewById(R.id.calendar_arrow_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarHandler.nextMonth();
            }
        });

        clearEvents();

        Thread thread = new Thread() {
            @Override
            public void run() { // Initialize calendar date buttons
                try {
                    while (!checkButtons || bufferChecks > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkButtons = initButtons();
                                if (checkButtons) bufferChecks--;
                            }
                        });

                        Thread.sleep(1000);
                    }
                } catch (Exception e) {

                }
            }
        };
        thread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        // If notification was tapped, open calendar event (or agenda activity)
        if (eventNotifID == AgendaActivity.NOTIFICATION_ID) {
            Intent intent = new Intent(this, AgendaActivity.class);
            startActivity(intent);
            finish();

            eventNotifID = -1; // Reset notification ID
        } else if (eventNotifID != -1) {
            Log.d(MainActivity.LOG_TAG, "Looking for event with following notification ID: " + eventNotifID + " (total " + calendarHandler.EVENTS_LIST.size() + " events)");
            for (CalendarHandler.CalendarEvent e : calendarHandler.EVENTS_LIST) { // Look for event with matching notification ID
                if (eventNotifID == e.notificationID) { // Event found!
                    Log.d(MainActivity.LOG_TAG, "Event with matching notification ID found!");
                    showEvent(e, true);
                }
            }

            eventNotifID = -1; // Reset notification ID
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        StorageHandler.writeData(MainActivity.getContext());

        (new Thread() {
            @Override
            public void run() {
                try {
                    MainActivity.requiresInit = false;

                    Thread.sleep(1000);
                    MainActivity.requiresInit = true;
                } catch (Exception e) {
                    // No action required
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            MainActivity.noInitThread();

            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_announcements) {
            Intent intent = new Intent(this, AnnouncementsActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_calendar) {
            // No action required... already in this activity
        } else if (id == R.id.nav_agenda) {
            Intent intent = new Intent(this, AgendaActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                slideX1 = event.getRawX();
                slideY1 = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                float slideX2 = event.getRawX();
                float slideY2 = event.getRawY();
                float deltaX = slideX2 - slideX1;
                if (Math.abs(deltaX) > Math.abs(slideY2 - slideY1) && slideY1 < MainActivity.PX_HEIGHT / 2 && slideY2 < MainActivity.PX_HEIGHT / 2) {
                    if (deltaX > MainActivity.PX_WIDTH / 5) {
                        calendarHandler.prevMonth();
                    } else if (-1 * deltaX > MainActivity.PX_WIDTH / 5) {
                        calendarHandler.nextMonth();
                    }
                }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Clears events list underneath calendar
     */
    public void clearEvents() {
        for (int i = 0; i < EVENTS_LIST_COMPONENTS; i++) {
            RelativeLayout layout = findViewById(getResources().getIdentifier("events_view_" + i, "id", getPackageName()));
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            params.height = 0;
            layout.setLayoutParams(params);
        }
    }

    /**
     * Initializes buttons
     *
     * @return true if successful
     */
    public boolean initButtons() {
        int year = calendarHandler.currentYear - calendarHandler.YEAR_ZERO;
        int month = calendarHandler.currentMonth - 1;
        int total = 0;

        LinearLayout[] dateRows = new LinearLayout[6];
        dateRows[0] = findViewById(R.id.calendar_row_0);
        dateRows[1] = findViewById(R.id.calendar_row_1);
        dateRows[2] = findViewById(R.id.calendar_row_2);
        dateRows[3] = findViewById(R.id.calendar_row_3);
        dateRows[4] = findViewById(R.id.calendar_row_4);
        dateRows[5] = findViewById(R.id.calendar_row_5);
        int rowHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MainActivity.DP_WIDTH / 9, getResources().getDisplayMetrics());

        for (int i = 0; i < 6; i++) {
            ViewGroup.LayoutParams params = dateRows[i].getLayoutParams();
            if (i >= 4 && calendarHandler.DATES[year][month][i][0] == 0) params.height = 0;
            else params.height = rowHeight;
            dateRows[i].setLayoutParams(params);
        }

        // Calculates previous and next year/month for number to display outside of current month
        int prevRow = 5, prevYear = year, prevMonth = month - 1;
        if (month == 0) {
            prevYear = year - 1;
            prevMonth = 11;
        }

        for (int i = 5; i > 0; i--) {
            if (prevYear != -1 && calendarHandler.DATES[prevYear][prevMonth][i][0] != 0) {
                prevRow = i;
                break;
            }
        }

        int nextYear = year, nextMonth = month + 1;
        if (month == 11) {
            nextMonth = 0;
            nextYear = year + 1;
        }

        // Calculations for dates of extremum
        int startLastDay = -1, afterDay = 1;
        for (int i = 0; i < 6; i++) {
            if (calendarHandler.DATES[year][month][i][0] != 0) startLastDay = 32 - i;
        }

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                Button button = findViewById(getResources().getIdentifier("button_" + i + "_" + j, "id", getPackageName()));

                if (calendarHandler.DATES[year][month][i][j] == 0) { // Not part of this month
                    if (i == 0) {
                        if (prevYear != -1)
                            button.setText(String.valueOf(calendarHandler.DATES[prevYear][prevMonth][prevRow][j]));
                        else {
                            button.setText(String.valueOf(startLastDay));
                            startLastDay++;
                        }
                    } else {
                        if (nextYear < CalendarHandler.TOTAL_YEARS)
                            button.setText(String.valueOf(calendarHandler.DATES[nextYear][nextMonth][0][j]));
                        else {
                            button.setText(String.valueOf(afterDay));
                            afterDay++;
                        }
                    }
                    setTone(button, 0, false);
                    button.setTextColor(getResources().getColor(R.color.colorDarkGrey));
                } else {
                    int number_tone = calendarHandler.getNumEvents(calendarHandler.currentYear, calendarHandler.currentMonth, calendarHandler.DATES[year][month][i][j]);

                    button.setText(String.valueOf(calendarHandler.DATES[year][month][i][j]));
                    total += number_tone;

                    int tone = 0;
                    button.setTypeface(Typeface.DEFAULT);
                    if (calendarHandler.currentYear == CalendarHandler.todayYear && calendarHandler.currentMonth == CalendarHandler.todayMonth && calendarHandler.DATES[year][month][i][j] == CalendarHandler.todayDay)
                    {
                        button.setTypeface(Typeface.DEFAULT_BOLD);
                        tone = 1;
                    }

                    setTone(button, tone, true);
                    if(number_tone == 0) button.setTextColor(getResources().getColor(R.color.colorMediumGrey));
                    else button.setTextColor(Color.rgb(218 + ((255 - 218) * (number_tone - 1) / 8), 68 + ((255 - 68) * (number_tone - 1) / 8), 83 + ((255 - 83) * (number_tone - 1) / 8)));
                }

                if (!listenersSet) {
                    final int row = i;
                    final int col = j;

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            calendarHandler.setDay(row, col);
                        }
                    });
                }
            }
        }
        listenersSet = true;

        if (total == 0) return false;
        return true;
    }

    /**
     * Sets background image of given button to appropriate tone
     *
     * @param button       instance of button to manipulate
     * @param tone         tone (0-1)
     * @param currentMonth month displayed on screen
     */
    private void setTone(Button button, int tone, boolean currentMonth) {
        button.setBackgroundResource(getResources().getIdentifier("tile_tone_" + tone, "drawable", getPackageName()));
        if (currentMonth)
            button.setTextColor(tone > 3 ? getResources().getColor(R.color.colorWhite) : getResources().getColor(R.color.colorBlack));
        else button.setTextColor(getResources().getColor(R.color.colorGrey));
    }

    /**
     * Starts event activity with given event
     *
     * @param event          CalendarEvent instance to use
     * @param removeReminder whether to remove the reminder upon actiivty opening
     */
    private void showEvent(CalendarHandler.CalendarEvent event, boolean removeReminder) {
        selectedEvent = event;

        Intent intent = new Intent(this, CalendarEventActivity.class);
        intent.putExtra(CalendarEventActivity.REMOVE_REMINDER, removeReminder);
        startActivity(intent);
    }

    /**
     * Updates events scroll list under calendar
     */
    public void updateEventScroll() {
        int counter = 0;
        int requestTime = calendarHandler.currentYear * 10000 + calendarHandler.currentMonth * 100 + calendarHandler.currentDay;

        for (CalendarHandler.CalendarEvent event : calendarHandler.EVENTS_LIST) { // Loop through all events in the calendar
            if (calendarHandler.eventOnDay(event, requestTime)) { // If event is on the same day as selected
                RelativeLayout layout = findViewById(getResources().getIdentifier("events_view_" + counter, "id", getPackageName()));
                ViewGroup.LayoutParams params = layout.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                layout.setLayoutParams(params);

                final TextView text = findViewById(getResources().getIdentifier("events_text_" + counter, "id", getPackageName()));
                text.setText(event.summary);
                text.setSingleLine(true);
                text.setEllipsize(TextUtils.TruncateAt.END);

                ImageButton image = findViewById(getResources().getIdentifier("events_image_" + counter, "id", getPackageName()));
                image.setBackgroundResource(getResources().getIdentifier("event_box", "drawable", getPackageName()));

                final CalendarHandler.CalendarEvent eventFinal = event;
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showEvent(eventFinal, false);
                    }
                });

                TextView timeText = findViewById(getResources().getIdentifier("events_time_" + counter, "id", getPackageName()));
                boolean sameStart = event.startDay == calendarHandler.currentDay && event.startMonth == calendarHandler.currentMonth && event.startYear == calendarHandler.currentYear;
                boolean sameEnd = event.endDay == calendarHandler.currentDay && event.endMonth == calendarHandler.currentMonth && event.endYear == calendarHandler.currentYear;
                if (event.startHour == -1 || (!sameStart && !sameEnd)) timeText.setText("All day");
                else {
                    String displayText;
                    if (sameStart && sameEnd)
                        displayText = TimeUtils.displayString(event.startHour, event.startMinute) + " – " + TimeUtils.displayString(event.endHour, event.endMinute);
                    else if (sameStart)
                        displayText = TimeUtils.displayString(event.startHour, event.startMinute);
                    else
                        displayText = TimeUtils.displayString(event.endHour, event.endMinute);
                    timeText.setText(displayText);
                }

                TextView dateText = findViewById(getResources().getIdentifier("events_date_" + counter, "id", getPackageName()));
                String dateString;
                if (sameStart && sameEnd)
                    dateString = event.startDay + " " + calendarHandler.getMonthString(event.startMonth).substring(0, 3);
                else if (event.startMonth == event.endMonth)
                    dateString = event.startDay + " – " + event.endDay + " " + calendarHandler.getMonthString(event.startMonth).substring(0, 3);
                else
                    dateString = event.startDay + " " + calendarHandler.getMonthString(event.startMonth).substring(0, 3) + " – " + event.endDay + " " + calendarHandler.getMonthString(event.endMonth).substring(0, 3);
                dateText.setText(dateString);

                TextView locationText = findViewById(getResources().getIdentifier("events_location_" + counter, "id", getPackageName()));
                ImageView locationIcon = findViewById(getResources().getIdentifier("icon_location_" + counter, "id", getPackageName()));
                locationText.setText(event.location);
                locationText.setSingleLine(true);
                locationText.setEllipsize(TextUtils.TruncateAt.END);
                if (event.location.equals("")) locationIcon.setVisibility(View.INVISIBLE);
                else locationIcon.setVisibility(View.VISIBLE);

                counter++;
            }
        }

        for (int i = counter; i < EVENTS_LIST_COMPONENTS; i++) {
            // Make the remaining event blocks invisible
            RelativeLayout layout = findViewById(getResources().getIdentifier("events_view_" + i, "id", getPackageName()));
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            params.height = 0;
            layout.setLayoutParams(params);
        }

        ScrollView scrollView = findViewById(R.id.events_scroll);
        scrollView.scrollTo(0, 0);
    }
}
