package me.simon76800.yrdsbstudentplanner;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import me.simon76800.yrdsbstudentplanner.client.ClientProtocol;

/**
 * School selection activity for users to select the school they currently attend.
 */
public class SchoolActivity extends AppCompatActivity {
    private Spinner schoolSpinner;
    private ProgressBar progressBar;
    private Button selectButton;
    private LinearLayout layout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SchoolActivity instance = this;

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlack));

        setContentView(R.layout.activity_school);

        schoolSpinner = findViewById(R.id.school_choice_spinner);
        progressBar = findViewById(R.id.verification_progress_bar);
        selectButton = findViewById(R.id.select_button);
        layout = findViewById(R.id.school_select_layout);

        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorB_start), android.graphics.PorterDuff.Mode.MULTIPLY);

        final String[] schoolOptions = new String[MainActivity.School.values().length + 1];
        schoolOptions[0] = getString(R.string.choose_school);

        int counter = 1;
        for(MainActivity.School s : MainActivity.School.values()) {
            schoolOptions[counter] = s.name;
            counter++;
        }

        ArrayAdapter<String> schoolSpinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, schoolOptions) {
            @Override
            public boolean isEnabled(int position) {
                // First item is hint, thus disabled
                return (position != 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0) tv.setTextColor(getResources().getColor(R.color.colorMediumGrey)); // Set hint colour to grey
                else tv.setTextColor(getResources().getColor(R.color.colorWhite));
                return view;
            }
        };
        schoolSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schoolSpinner.setAdapter(schoolSpinnerAdapter);

        ObjectAnimator slide = ObjectAnimator.ofFloat(selectButton, "translationY", MainActivity.convertToPx(-20));
        slide.setDuration(1000);
        slide.setStartDelay(2000);
        slide.start();

        AlphaAnimation appear = new AlphaAnimation(0.0f, 1.0f);
        appear.setDuration(1000);
        appear.setStartOffset(2000);
        appear.setFillAfter(true);
        selectButton.startAnimation(appear);

        selectButton.setVisibility(View.VISIBLE);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String schoolName = schoolSpinner.getSelectedItem().toString();
                MainActivity.School school = null;

                for(MainActivity.School s : MainActivity.School.values()) {
                    if(s.name.equalsIgnoreCase(schoolName)) school = s;
                }

                if(school == null) {
                    return;
                }

                MainActivity.school = school;

                final String messageToServer = ClientProtocol.SCHOOL_CHOICE + ":" + schoolName;

                Thread writeThread = new Thread() {
                    public void run() {
                        try {
                            MainActivity.client.cp.writeToServer(messageToServer);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                writeThread.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        instance.layout.setVisibility(View.GONE);
                        instance.progressBar.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        MainActivity.client.cp.schoolActivityInstance = this;
    }
}
