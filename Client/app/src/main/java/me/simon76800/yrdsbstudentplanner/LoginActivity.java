package me.simon76800.yrdsbstudentplanner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.UUID;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import me.simon76800.yrdsbstudentplanner.layout.VerificationDialog;

/**
 * Interface that allows the user to input their YRDSB student planner and receive a verification code before entering the app.
 * No outside users allowed!
 */
public class LoginActivity extends FragmentActivity {
    public LoginActivity instance;

    private static Thread detectConnectionThread;
    public static boolean tooManyRetries = false; // If true, show retry fail dialog

    public EditText studentNumField;
    public Button loginButton;
    public ProgressBar loginSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        instance = this;

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlack));

        // Gets the dimensions of login interface background, sets dimensions of contained components
        final ImageView loginInterfaceImage = findViewById(R.id.login_interface);
        studentNumField = findViewById(R.id.student_num_field);
        loginButton = findViewById(R.id.login_button);
        final int textFieldPadding = MainActivity.convertToPx(2);

        loginSpinner = findViewById(R.id.login_progress_bar);

        ViewTreeObserver vto = loginInterfaceImage.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                loginInterfaceImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = loginInterfaceImage.getMeasuredWidth();
                int height = loginInterfaceImage.getMeasuredHeight();

                RelativeLayout.LayoutParams studentNumParams = new RelativeLayout.LayoutParams(width - 30, RelativeLayout.LayoutParams.WRAP_CONTENT);
                studentNumParams.setMargins(0, height - MainActivity.convertToPx(15) - studentNumField.getMeasuredHeight() - loginButton.getMeasuredHeight(), 0, 0);
                studentNumParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                studentNumField.setLayoutParams(studentNumParams);
                studentNumField.setHint("Student Number");
                studentNumField.setPadding(textFieldPadding, textFieldPadding, textFieldPadding, textFieldPadding);
                studentNumField.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams loginButtonParams = new RelativeLayout.LayoutParams(width - 30, RelativeLayout.LayoutParams.WRAP_CONTENT);
                loginButtonParams.setMargins(0, height - MainActivity.convertToPx(6) - loginButton.getMeasuredHeight(), 0, 0);
                loginButtonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                loginButton.setLayoutParams(loginButtonParams);
                loginButton.setText(R.string.login);
                loginButton.setVisibility(View.VISIBLE);

                loginSpinner.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorB_start), android.graphics.PorterDuff.Mode.MULTIPLY);
                loginSpinner.setVisibility(View.GONE);
            }
        });

        // Displays error message when Internet or servers are down
        detectConnectionThread = new Thread() {
            ImageView connectedImage = findViewById(R.id.connection_status_box);
            TextView connectedText = findViewById(R.id.connection_status_text);

            boolean connectionFail = false; // If connection bar showed up

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // No action required
                }

                while (!connectionFail) {
                    // Loop as long as Internet connection isn't available
                    if (noNetworkConnection()) {
                        connectionFail = true;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedImage.setBackgroundResource(R.drawable.red_rectangle);
                                connectedText.setText(R.string.not_connected);
                            }
                        });

                        while (noNetworkConnection()) {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException e) {
                                // No action required
                            }
                        }
                    }

                    // Loop as long as server connection can't be established
                    if (!MainActivity.client.isConnected()) {
                        connectionFail = true;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedImage.setBackgroundResource(R.drawable.red_rectangle);
                                connectedText.setText(R.string.server_down);
                            }
                        });

                        while (!MainActivity.client.isConnected()) {
                            Log.d(MainActivity.LOG_TAG, "Connection achieved, trying to connect to servers...");

                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                // Exception does not need to be handled, proceed as usual
                            }
                        }
                    }

                    if (connectionFail) {
                        connectedImage.setBackgroundResource(R.drawable.green_rectangle);
                        connectedText.setText(R.string.connected);
                        try {
                            Thread.sleep(2400);
                        } catch (InterruptedException e) {
                            // Exception does not need to be handled, proceed as usual
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedImage.setBackgroundResource(0);
                                connectedText.setText("");
                            }
                        });
                    }
                }
            }
        };
        detectConnectionThread.start();

        // Adds action listener to login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String studentNumber = studentNumField.getText().toString();
                if (studentNumber.length() != 9) { // Invalid student number length
                    VerificationDialog dialog = new VerificationDialog();
                    VerificationDialog.message = "Invalid student number!";
                    dialog.show(getSupportFragmentManager(), "invalid");
                    return;
                } else if (!MainActivity.client.isConnected()) {
                    VerificationDialog dialog = new VerificationDialog();
                    VerificationDialog.message = "Can't connect to network :(";
                    dialog.show(getSupportFragmentManager(), "invalid");
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        instance.studentNumField.setVisibility(View.GONE);
                        instance.loginButton.setVisibility(View.GONE);
                        instance.loginSpinner.setVisibility(View.VISIBLE);
                    }
                });

                MainActivity.studentNumber = studentNumber;
                MainActivity.uniqueID = UUID.randomUUID().toString();

                final String valToWrite = "verify:" + studentNumber + ":" + MainActivity.uniqueID;
                Thread writeThread = new Thread() {
                    public void run() {
                        MainActivity.client.cp.writeToServer(valToWrite);
                    }
                };
                writeThread.start();

                InputMethodManager inputManager = (InputMethodManager) instance.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(loginButton.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        MainActivity.client.cp.loginActivityInstance = this;

        if(tooManyRetries) {
            VerificationDialog dialog = new VerificationDialog();
            VerificationDialog.message = "You have retried too many times!";
            dialog.show(getSupportFragmentManager(), "invalid");

            tooManyRetries = false;
        }
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

    @Override
    public void onBackPressed() {
        MainActivity.noInitThread();
        super.onBackPressed();
    }

    /**
     * @return if an Internet connection is detected
     */
    private boolean noNetworkConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return !(activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public static void startNoConnectionThread() {
        boolean success = false;

        while (!success) {
            try {
                detectConnectionThread.start();
                success = true;
            } catch (Exception e) {
                // No action required

                try {
                    Thread.sleep(100);
                } catch (Exception e2) {
                    // No action required
                }
            }
        }
    }
}
