package me.simon76800.yrdsbstudentplanner.client;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.util.Arrays;

import me.simon76800.yrdsbstudentplanner.LoginActivity;
import me.simon76800.yrdsbstudentplanner.MainActivity;

/**
 * Asynchronously handles socket connection and communications.
 * Also responsible for providing the server with necessary information to authenticate the client.
 */
public class AppClient extends AsyncTask<Void, Void, Void> {
    private static final String DEST_ADDRESS = "yrdsbstudentplanner.ddns.net";
    private static final int DEST_PORT = 8031;

    private boolean connected = false;

    private boolean hasSymmetricKey = false;
    public boolean clientVerified = false;
    public boolean previousUser = false;

    public static boolean attemptConnection = true;

    public final ClientProtocol cp = new ClientProtocol();
    private InputStream in;
    private OutputStream out;

    private static final String KEY_RECEIVED = "key_Received";

    @Override
    protected Void doInBackground(Void... arg0) {
        while (attemptConnection) { // Retry connection upon failure
            if(isCancelled()) {
                return null;
            }

            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(DEST_ADDRESS, DEST_PORT), 5000);
                connected = true;
                Log.i(MainActivity.LOG_TAG, "Successfully connected to server!");

                out = socket.getOutputStream();
                in = socket.getInputStream();
                out.flush();

                requestKey(); // Sends public key, requests symmetric key

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[131072];
                int len;

                byte[] receive;

                int requestedLength = 0;
                int currentLength = 0;

                // Checks for input from server, processes with protocol and replies
                while ((len = in.read(buffer)) != -1) {
                    if (!hasSymmetricKey) { // If symmetric encryption key hasn't been initialized
                        os.write(buffer, 0, len);
                        receive = os.toByteArray();
                        os.reset();

                        try {
                            byte[] receiveArray = KeyHandler.decryptRSA(receive);
                            cp.setSymmetricKey(receiveArray);

                            if(receiveArray != null) {
                                hasSymmetricKey = true;

                                Thread.sleep(100);

                                if (previousUser)
                                    cp.writeToServer("check_existing:" + MainActivity.studentNumber + ":" + MainActivity.uniqueID);
                            } else {
                                Thread.sleep(100);

                                requestKey();

                                Thread.sleep(100);

                                requestKey();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        currentLength += len; // Accumulate current length

                        if (requestedLength == 0) {
                            os.write(buffer, 4, len - 4); // Read all data sans initial length
                            byte[] lengthBytes = Arrays.copyOf(buffer, 4);

                            ByteBuffer bb = ByteBuffer.wrap(lengthBytes);
                            requestedLength = bb.getInt();
                        } else {
                            os.write(buffer, 0, len);
                        }

                        if (currentLength >= requestedLength) {
                            // Reset length variables
                            currentLength = 0;
                            requestedLength = 0;

                            receive = os.toByteArray();
                            os.reset();

                            try {
                                cp.read(receive);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException | IllegalBlockingModeException | IllegalArgumentException e) { // If connection fails
                Log.e(MainActivity.LOG_TAG, "Could not connect to server, trying again...");
            } finally {
                if (connected) {
                    // If active connection was broken
                    LoginActivity.startNoConnectionThread();
                }

                connected = false;

                try {
                    socket.close(); // Attempts to close socket
                    Thread.sleep(10000); // Wait before next connection attempt
                } catch (IOException | InterruptedException ex) {
                    // No action required
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }

    /**
     * Writes message to application server
     * (does not include encryption)
     *
     * @param message message to write, in bytes
     */
    public void writeToServer(byte[] message) {
        try {
            out.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Requests the symmetric key
     */
    public void requestKey() {
        try {
            out.write(KeyHandler.getPublicKey().getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            // No action required
        }
    }
}
