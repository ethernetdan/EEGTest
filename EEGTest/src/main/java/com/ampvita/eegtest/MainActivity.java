package com.ampvita.eegtest;

import android.bluetooth.BluetoothAdapter;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;


import com.firebase.client.FirebaseError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGRawMulti;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;


public class MainActivity extends ActionBarActivity {
    PrintWriter out;
    TGDevice tgDevice;
    BluetoothAdapter btAdapter;
    TextView serverStatus;
    TGRawMulti tgRaw;
    GraphViewSeries eegSeries;
    double graph2LastXValue = 0d;
    GraphView graphView;
    int send = 0;
    static final Handler h = new Handler();
    SocketIO wsocket;


    ArrayList<BrainStateModel> brainStates; // Stores the brain state models used for classifying the current state of mind
    ArrayList<Integer> latestSignalSample; // Stores the most recently collected EEG data points
    public static int samplePointCount = 100; // The number of points to buffer in the most recently collected points




    // Default IP
    public static String SERVERIP = "10.0.2.2"; // Dan's Wi-Fi hotspot: "10.0.2.2";

    // Designate a port
    public static final int SERVERPORT = 6002;

    // Handler for network communications with Google Glass
    private Handler networkHandler = new Handler();

    private ServerSocket serverSocket; // Socket used to listen for connection

    // TODO: change this to your own Firebase URL
    private static final String FIREBASE_URL = "https://bubble-data.firebaseIO.com";

    // Create a reference to a Firebase location
    private Firebase firebaseReference;

    int meditiation = 0;
    int attention = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        tgRaw = new TGRawMulti();
        eegSeries = new GraphViewSeries(new GraphView.GraphViewData[] {});
        graphView = new LineGraphView(this, "EEG Data");
        graphView.setScrollable(true);
        graphView.setManualYAxis(true);
        String[] str = {""};
        graphView.setHorizontalLabels(str);

        graphView.setManualYAxisBounds(2000,-2200);
        graphView.addSeries(eegSeries);
        graphView.setViewPort(1, 100);

        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.addView(graphView);


        serverStatus = (TextView)findViewById(R.id.displayText);

        if (btAdapter != null) {
            eegHandler.sendEmptyMessage(TGDevice.MSG_THINKCAP_RAW);
            tgDevice = new TGDevice(btAdapter, eegHandler);
            tgDevice.connect(true);
        } else {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
        }
        serverStatus = (TextView)findViewById(R.id.displayText);
        serverStatus.setText("test");

        // Correct orientation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        /**
         * Set up data structures for monitoring the EEG data
         */

        // Initialize storage for latest signal sample
        latestSignalSample = new ArrayList<Integer>();

        // Initialize storage buffer for the current user's saved brain states.
        this.brainStates = new ArrayList<BrainStateModel>();

        /**
         * Start server
         */

//        Thread fst = new Thread(new ServerThread());
//        fst.start();

        /**
         * Set up Firebase for requests.
         */

        firebaseReference = new Firebase(FIREBASE_URL); // Connect to Firebase

        // Add test button
        Button testButton = (Button) findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //Toast.makeText(getApplicationContext(), "Testing!", Toast.LENGTH_SHORT).show();
                //firebaseReference.setValue("Bubble online!"); // Write data to Firebase
                postBrainwaveDataExample("woo");


            }
        });
        try {
        wsocket = new SocketIO("http://192.168.2.108:3000/");
        wsocket.connect(new IOCallback() {
            @Override
            public void onMessage(JSONObject json, IOAcknowledge ack) {
                try {
                    System.out.println("Server said:" + json.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(String data, IOAcknowledge ack) {
                System.out.println("Server said: " + data);
            }

            @Override
            public void onError(SocketIOException socketIOException) {
                System.out.println("an Error occured");
                socketIOException.printStackTrace();
            }

            @Override
            public void onDisconnect() {
                System.out.println("Connection terminated.");
            }

            @Override
            public void onConnect() {
                System.out.println("Connection established");
            }

            @Override
            public void on(String event, IOAcknowledge ack, Object... args) {
//                System.out.println("Server triggered event '" + event + "'");
                if(event.equals("request-mindwave-data")) {
                    if(latestSignalSample.size() > 0){
                        wsocket.emit("mindwave", latestSignalSample);
                        latestSignalSample.clear();
                    }
                } else if (event.equals("request-mindwave-attention")) {
                    wsocket.emit("mindwave-attention", attention);

                } else if (event.equals("request-mindwave-meditation")) {
                    wsocket.emit("mindwave-meditation", meditiation);
                }

            }
        });

        // This line is cached until the connection is establisched.
        wsocket.emit("smartphone", "hi");

    } catch(Exception e) {}}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            wsocket.emit("smartphone-volume-down");
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            wsocket.emit("smartphone-volume-up");
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Read data and react to changes
        firebaseReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snap) {
                //System.out.println(snap.getName() + " -> " + snap.getValue());
                Log.i("getData", snap.getName() + " -> " + snap.getValue());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        try {
//            // make sure you close the socket upon exiting
//            serverSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Retrieves and buffers the stored gestures for the current user (from the remote server)
     */
    public void getBrainDataModels() {
        // Retrieve data from server
        // Store data in BrainStateModel objects
        // Update BrainStateModel objects (if needed)
    }

    public void postBrainwaveDataExample(String brainwaveLabel) {
        try {
            // Create and return a new child reference for the object
            Firebase freshFirebaseReference = firebaseReference.push();
            //freshFirebaseReference.setValue("button pressed...");

            // Create object to store in Firebase
            Map<String, Object> toSet = new HashMap<String, Object>();
            toSet.put("label", brainwaveLabel); // Set the label of the brainwave activity data
            toSet.put("data", latestSignalSample); // Put the data array

            // Write example to Firebase
            freshFirebaseReference.setValue(toSet, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError error, Firebase ref) {
                    //outstandingSegments.remove(segmentName);
                    Toast.makeText(getApplicationContext(), "Testing!", Toast.LENGTH_SHORT).show();
                }
            });
            //firebaseReference.setValue(toSet);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void postBrainDataModels() {
        // POST brain data to server

        // Write data to Firebase
        firebaseReference.setValue("Do you have data? You'll love Firebase.");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private final Handler eegHandler = new Handler() {
        String TAG = "Cyborg";
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:
                    switch(msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            break;
                        case TGDevice.STATE_CONNECTED:
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                        case TGDevice.STATE_NOT_PAIRED:
                        default:
                            break;
                    }
                    break;
                case TGDevice.MSG_POOR_SIGNAL:
                    Log.v(TAG, "PoorSignal: " + msg.arg1);
                    break;
                case TGDevice.MSG_MEDITATION:
                    meditiation = msg.arg1;
                    break;
                case TGDevice.MSG_ATTENTION:
                    attention = msg.arg1;
                    Log.v(TAG, "Attention: " + msg.arg1);
                    break;
                case TGDevice.MSG_HEART_RATE:
                    serverStatus.setText("Heart rate: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_RAW_MULTI:
                    tgRaw = (TGRawMulti)msg.obj;
                    serverStatus.append("raw: " +
                            tgRaw.ch1 + ", " +
                            tgRaw.ch2 + ", " +
                            tgRaw.ch3 + ", " +
                            tgRaw.ch4 + ", " +
                            tgRaw.ch5 + ", " +
                            tgRaw.ch6 + ", " +
                            tgRaw.ch7 + ", " +
                            tgRaw.ch8 + ", " +
                            "\n");
                    break;

                case TGDevice.MSG_RAW_DATA:
                    int rawValue = msg.arg1;
                    graph2LastXValue += 1d;
                    eegSeries.appendData(new GraphView.GraphViewData(graph2LastXValue, rawValue),
                        true, 120);
                    if (out != null && (send % 20 == 0)) {
                        out.println("e" + rawValue);
                        Log.e("test", ""+rawValue);
                        send = 0;
                    }
                    send++;


                    // TODO: Store these values when in "capture" mode
                    latestSignalSample.add(rawValue); // Append new element to list
                    if (latestSignalSample.size() > MainActivity.samplePointCount) {
                        latestSignalSample.remove(0); // Remove first data point from list
                    }

                    break;
                case TGDevice.MSG_EEG_POWER:
                    /*TGEegPower ep = (TGEegPower)msg.obj;
                    serverStatus.setText("Delta: " + ep.delta + '\n' +
                            "HighAlpha: " + ep.highAlpha + '\n' +
                            "LowAlpha: " + ep.lowAlpha + '\n' +
                            "HighBeta: " + ep.highBeta + '\n' +
                            "LowBeta: " + ep.lowBeta + '\n' +
                            "MidGamma: " + ep.midGamma + '\n' +
                            "LowGamma: " + ep.lowGamma + '\n' +
                            "Theta: " + ep.theta + '\n');*/
                    break;
                default:
                    break;
            }
        }

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_main, container, false);
            return rootView;
        }
    }

    public class ServerThread implements Runnable {

        public void run() {
            try {
                if (SERVERIP != null) {
                    networkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            serverStatus.setText("Listening on IP: " + SERVERIP);
                        }
                    });
                    serverSocket = new ServerSocket(SERVERPORT);
                    while (true) {
                        // listen for incoming clients
                        Socket client = serverSocket.accept();
                        networkHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                serverStatus.setText("Connected.");
                            }
                        });

                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));
                            String line = null;
                            while ((line = in.readLine()) != null) {
                                final String data = line;
                                Log.d("ServerActivity", data);
                                networkHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        /// the fun goes here
                                        serverStatus.setText(data); // Set the text to that last received from Glass

                                        // TODO: Post data to Firebase
                                        // TODO: Update model (average)
                                        postBrainwaveDataExample(data);
                                    }
                                });
                            }
                            break;
                        } catch (Exception e) {
                            networkHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    serverStatus.setText("Oops. Connection interrupted. Please reconnect your phones.");
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                } else {
                    networkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            serverStatus.setText("Couldn't detect internet connection.");
                        }
                    });
                }
            } catch (Exception e) {
                final Exception ed = e;
                networkHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        serverStatus.setText("Error: " + ed.getStackTrace());
                    }
                });
                e.printStackTrace();
            }
        }
    }


}

