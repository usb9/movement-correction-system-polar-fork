package com.example.wearbluetoothtestapp;
/*
Nicolas Schmitt, 28/10/22
Wear OS Test App
Bluetooth connection is initiated by a mobile phone.
When a connection is established, each accelerometer reading is sent via bluetooth.
Note: permissions checks are currently suppressed except in onCreate() method.
This is because Android Studio asks for checks on permissions that are only available
on Android 12 or higher, the watch is running on Android 7.1.1

Code is based on an example project:
https://github.com/android/connectivity-samples/tree/master/BluetoothChat
 */

/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;

import androidx.core.app.ActivityCompat;

import com.example.wearbluetoothtestapp.databinding.ActivityMainBinding;


public class MainActivity extends Activity implements SensorEventListener {

    private ActivityMainBinding binding;

    private TextView BtStatus;
    private TextView tv_xVal;
    private TextView tv_yVal;
    private TextView tv_zVal;

    // Buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    private SensorManager sensorManager;
    private Sensor accSensor;

    private BluetoothService bluetoothService = null;
    BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private static final String TAG = "____Main___";
    private final int REQUEST_ENABLE_BT = 1;
    private final int ALL_PERMISSIONS = 100;
    public final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    private final static int REQUEST_BT_DISCOVERABLE = 4;
    private final static int REQUEST_BT_CONNECTION = 5;


    /*
     Initialize UI, SensorManager and BluetoothManager(not bluetoothService!), calls requestAppPermissions
     to obtain permissions from user
          */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BtStatus = binding.textBtStatus;
        tv_xVal = binding.textXVal;
        tv_yVal = binding.textYVal;
        tv_zVal = binding.textZVal;

        requestAppPermissions();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Log.d(TAG, "BLUETOOTH NOT SUPPORTED");
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (bluetoothAdapter == null) {
            return;
        }
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the streaming session
        } else if (bluetoothService == null) {
            setupBluetoothService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothService.getState() == bluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                bluetoothService.start();
            }
        }
    }

    private void setupBluetoothService() {
        Log.d(TAG, "setupBluetoothService");

        // Initialize the BluetoothService to perform bluetooth connections
        bluetoothService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
        bluetoothDiscoverable();
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void bluetoothDiscoverable() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABLE);

    }



    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Log.d(TAG, "Send: not connected");
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /*
    Enter here after startActivityForResult(...) returns
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                BtStatus.setText(getString(R.string.sEnabled));
            } else {
                BtStatus.setText(getString(R.string.sDisabled));
            }
        }
        if(requestCode == REQUEST_BT_DISCOVERABLE) {
            if(resultCode == RESULT_OK) {
                BtStatus.setText("Visible to other devices");
                setupBluetoothService();
            }
            else
                BtStatus.setText("Not visible");
        }
        super.onActivityResult(requestCode, resultCode, Data);
    }

    /*
    Enter here after requestPermissions(...) is called
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        for(int i = 0; i < grantResults.length; ++i) {
            if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                Log.d(TAG, "Permission " + permissions[i] + " granted");
            else
                Log.d(TAG, "Permission " + permissions[i] + " denied");
        }

    }


    /*
    Request necessary permissions from user
     */
    private void requestAppPermissions() {
        String[] permissions = {Manifest.permission.WAKE_LOCK, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS);
    }


    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothService != null)
            bluetoothService.stop();
    }

    /*
    Tries to send a message containing sensor data at each reading
     */
    public void onSensorChanged(SensorEvent event) {
        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];
        String sendString = "(" + x.toString() + "," + y.toString() + "," + z.toString() + ")";

        tv_xVal.setText(x.toString());
        tv_yVal.setText(y.toString());
        tv_zVal.setText(z.toString());

        sendMessage(sendString);
        Log.d(TAG, "Message sent if connected: " + sendString);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed");
    }

}

