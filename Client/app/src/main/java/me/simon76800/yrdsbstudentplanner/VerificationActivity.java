package me.simon76800.yrdsbstudentplanner;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Manages the verification phase of the application authentication process.
 */
public class VerificationActivity extends AppCompatActivity {
    public EditText codeText;
    public ProgressBar progressBar;
    public Button verifyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlack));

        setContentView(R.layout.activity_verification);

        codeText = findViewById(R.id.verification_code_text);
        progressBar = findViewById(R.id.verification_progress_bar);
        verifyButton = findViewById(R.id.verify_button);
        
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorB_start), android.graphics.PorterDuff.Mode.MULTIPLY);

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String verificationCode = "verification_code:" + codeText.getText();

                Thread writeThread = new Thread() {
                    public void run() {
                        try {
                            MainActivity.client.cp.writeToServer(verificationCode);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                writeThread.start();

                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(verifyButton.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        if (MainActivity.DP_WIDTH < 400) {
            codeText.setWidth(MainActivity.convertToPx((int) (MainActivity.DP_WIDTH * 0.7)));
        }

        // Animate the verify button 3 seconds after verification screen appears
        ObjectAnimator slide = ObjectAnimator.ofFloat(verifyButton, "translationY", MainActivity.convertToPx(-20));
        slide.setDuration(1000);
        slide.setStartDelay(2000);
        slide.start();

        AlphaAnimation appear = new AlphaAnimation(0.0f, 1.0f);
        appear.setDuration(1000);
        appear.setStartOffset(2000);
        appear.setFillAfter(true);
        verifyButton.startAnimation(appear);

        verifyButton.setVisibility(View.VISIBLE);

        MainActivity.client.cp.verificationActivityInstance = this;
    }

    @Override
    public void onPause() {
        super.onPause();

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
}
