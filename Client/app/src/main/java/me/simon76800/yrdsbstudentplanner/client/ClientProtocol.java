package me.simon76800.yrdsbstudentplanner.client;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.core.app.ActivityOptionsCompat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.simon76800.yrdsbstudentplanner.CalendarActivity;
import me.simon76800.yrdsbstudentplanner.LoginActivity;
import me.simon76800.yrdsbstudentplanner.MainActivity;
import me.simon76800.yrdsbstudentplanner.R;
import me.simon76800.yrdsbstudentplanner.SchoolActivity;
import me.simon76800.yrdsbstudentplanner.VerificationActivity;
import me.simon76800.yrdsbstudentplanner.util.CalendarHandler;

/**
 * Processes messages sent by the socket server and responds with appropriate action.
 */
public class ClientProtocol {
    private static final String VALID_STUDENT_NUM = "student_num_valid";

    private static final String DEVICE_ID_MISMATCH = "device_id_invalid";

    private static final String FAIL_W_RETRY = "verification_failed";
    private static final String FAIL_NO_RETRY = "verification_failed_no_retry";
    private static final String VERIFICATION_SUCCESS = "verification_success";

    public static final String SCHOOL_CHOICE = "school_choice";
    private static final String SCHOOL_CONFIRM = "school_confirmed";

    private static final String CALENDAR_REQUEST_STRING = "calendar_request";
    private static final String ANNOUNCEMENTS_REQUEST_STRING = "announcements_request";

    private static final int SYMMETRIC_LENGTH = 16;

    private static byte[] SYMMETRIC_KEY;

    public static boolean requiresCalendarOpen = false; // If calendar activity needs to be opened

    public LoginActivity loginActivityInstance;
    public VerificationActivity verificationActivityInstance;
    public SchoolActivity schoolActivityInstance;

    /**
     * Sets symmetric key to last 16 digits of input
     *
     * @param key symmetric key
     */
    public void setSymmetricKey(byte[] key) {
        Log.d(MainActivity.LOG_TAG, "Setting symmetric key!");

        if (key != null) {
            SYMMETRIC_KEY = Arrays.copyOfRange(key, key.length - SYMMETRIC_LENGTH, key.length);
            Log.d(MainActivity.LOG_TAG, "Successfully set symmetric key!");
        } else {
            Log.e(MainActivity.LOG_TAG, "Symmetric key null, must try again!");
        }
    }

    /**
     * Decrypts and reads message from application server
     *
     * @param input message from server
     */
    public void read(byte[] input) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(input);
        int ivLength = byteBuffer.getInt();
        if (ivLength < 12 || ivLength > 16)
            throw new IllegalArgumentException("Invalid IV Length");
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        final Cipher decCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SYMMETRIC_KEY, "AES"),
                new GCMParameterSpec(128, iv));
        byte[] plainText = decCipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));
        String inputStr = new String(plainText, StandardCharsets.UTF_8);
        Arrays.fill(iv, (byte) 0);

        if (MainActivity.client.clientVerified) {
            if (inputStr.startsWith("calendar")) { // If calendar data received
                if (CalendarActivity.calendarHandler == null)
                CalendarActivity.calendarHandler = new CalendarHandler(); // Initialize calendar handler if null

                CalendarActivity.calendarHandler.parseString(inputStr);
                Log.i(MainActivity.LOG_TAG, "Calendar data successfully received!");

                if(requiresCalendarOpen) {
                    Intent intent = new Intent(schoolActivityInstance, CalendarActivity.class);
                    schoolActivityInstance.startActivity(intent);
                    schoolActivityInstance.finish();
                }
            } else if (inputStr.startsWith("announcements")) { // If announcements data received
                MainActivity.rawAnnouncementsData = inputStr.substring(13);
                Log.i(MainActivity.LOG_TAG, "Announcements data successfully received!");
            } else if (inputStr.startsWith(SCHOOL_CONFIRM)) {
                requiresCalendarOpen = true;

                Thread writeThread = new Thread() {
                    @Override
                    public void run() {
                        writeToServer(CALENDAR_REQUEST_STRING + ":" + MainActivity.school.ID); // Request calendar data from server

                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                            // No action required
                        }

                        writeToServer(ANNOUNCEMENTS_REQUEST_STRING + ":" + MainActivity.school.ID);
                    }
                };
                writeThread.start();
            }
        } else {
            if (inputStr.equals(VALID_STUDENT_NUM)) {
                loginActivityInstance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loginActivityInstance.loginSpinner.setVisibility(View.GONE);
                    }
                });

                loginActivityInstance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(loginActivityInstance, VerificationActivity.class);
                        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(loginActivityInstance,
                                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                        loginActivityInstance.startActivity(intent, bundle);
                        loginActivityInstance.finish();
                    }
                });
            } else if (inputStr.equals(FAIL_W_RETRY)) { // Shake animation
                Animation shake = AnimationUtils.loadAnimation(verificationActivityInstance, R.anim.shake);

                verificationActivityInstance.codeText.startAnimation(shake);
                verificationActivityInstance.codeText.setText("");

                // 500ms vibration
                Vibrator v = (Vibrator) verificationActivityInstance.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }

                verificationActivityInstance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        verificationActivityInstance.codeText.setBackgroundTintList(ColorStateList.valueOf(verificationActivityInstance.getResources().getColor(R.color.mediumRed)));
                    }
                });

                Thread revertThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            // No action required
                        }

                        verificationActivityInstance.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verificationActivityInstance.codeText.setBackgroundTintList(ColorStateList.valueOf(verificationActivityInstance.getResources().getColor(R.color.colorGrey)));
                            }
                        });
                    }
                });
                revertThread.start();
            } else if (inputStr.equals(FAIL_NO_RETRY)) {
                LoginActivity.tooManyRetries = true;

                Intent intent = new Intent(verificationActivityInstance, LoginActivity.class);
                verificationActivityInstance.startActivity(intent);
                verificationActivityInstance.finish();
            } else if (inputStr.startsWith(VERIFICATION_SUCCESS)) {
                // Successful verification! Hide verify button and change to progress spinner
                try {
                    verificationActivityInstance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            verificationActivityInstance.verifyButton.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                            verificationActivityInstance.progressBar.getIndeterminateDrawable().setColorFilter(verificationActivityInstance.getResources().getColor(R.color.lightRed), android.graphics.PorterDuff.Mode.MULTIPLY);
                            verificationActivityInstance.progressBar.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    Log.d(MainActivity.LOG_TAG, "Previous user, skipping verification activity initialization");
                }

                MainActivity.client.clientVerified = true;

                Intent intent = new Intent(verificationActivityInstance, SchoolActivity.class);
                Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(verificationActivityInstance,
                        android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                verificationActivityInstance.startActivity(intent, bundle);
                verificationActivityInstance.finish();

            } else if (inputStr.equals(DEVICE_ID_MISMATCH)) {
                Intent intent = new Intent(MainActivity.getContext(), LoginActivity.class);
                MainActivity.getContext().startActivity(intent);

                // Resets all data upon device ID mismatch
                MainActivity.studentNumber = "";
                MainActivity.uniqueID = "";
            }
        }
    }

    /**
     * Writes a message to application server
     *
     * @param message message to write
     */
    public void writeToServer(String message) {
        boolean success = false;
        
        while (!success) {
            try {
                SecretKey secretKey = new SecretKeySpec(SYMMETRIC_KEY, "AES");
                byte[] iv = new byte[12];
                (new SecureRandom()).nextBytes(iv);

                final Cipher encCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
                encCipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
                byte[] cipherText = encCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
                byte[] encodedText = Base64.encode(cipherText, Base64.DEFAULT);

                ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + encodedText.length);
                byteBuffer.putInt(iv.length);
                byteBuffer.put(iv);
                byteBuffer.put(encodedText);
                byte[] cipherMessage = byteBuffer.array();

                MainActivity.client.writeToServer(cipherMessage);
                success = true;
            } catch (Exception e) {
                Log.d(MainActivity.LOG_TAG, "Failed to send message, trying again...");
                e.printStackTrace();

                try {
                    Thread.sleep(100);
                } catch (Exception e2) {
                    // No action required
                }
            }
        }
    }
}
