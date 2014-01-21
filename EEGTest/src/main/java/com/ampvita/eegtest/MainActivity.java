package com.ampvita.eegtest;

import android.bluetooth.BluetoothAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;


public class MainActivity extends ActionBarActivity {
    TGDevice tgDevice;
    BluetoothAdapter btAdapter;
    TextView tv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        tv = (TextView)findViewById(R.id.text);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        if (btAdapter != null) {
            handler.sendEmptyMessage(TGDevice.MSG_THINKCAP_RAW);
            tgDevice = new TGDevice(btAdapter, handler);
            tgDevice.connect(true);
        } else {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
        }


    }

    private final Handler handler = new Handler() {
        String TAG = "EEGTest";
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
                case TGDevice.MSG_ATTENTION:
                    Log.v(TAG, "Attention: " + msg.arg1);
                    break;
                case TGDevice.MSG_HEART_RATE:
                    tv.setText("Heart rate: " + msg.arg1 + "\n");
                    break;

                case TGDevice.MSG_RAW_DATA:
                    int rawValue = msg.arg1;
                    //Log.v(TAG, "Data: " + rawValue);
                    break;
                case TGDevice.MSG_EEG_POWER:
                    TGEegPower ep = (TGEegPower)msg.obj;
                    Log.v(TAG, "Delta: " + ep.delta);
                default:
                    Log.e(TAG, "Unknown Message");
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
