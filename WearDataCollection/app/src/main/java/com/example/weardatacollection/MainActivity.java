package com.example.weardatacollection;

/*
Nicolas Schmitt, 28/10/22
Wear OS Test App
Bluetooth connection is initiated by a mobile phone.
When a connection is established, each accelerometer reading is sent via bluetooth.
Note: permissions checks are currently suppressed except in onCreate() method.
This is because Android Studio asks for checks on permissions that are only available
on Android 12 or higher, the watch is running on Android 7.1.1
*/
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;

import android.content.pm.PackageManager;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;

import androidx.core.app.ActivityCompat;

import com.example.weardatacollection.databinding.ActivityMainBinding;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Date;



public class MainActivity extends Activity implements SensorEventListener{

    private long timestamp1 = System.currentTimeMillis();


    Button mButtonAdd;
    private ActivityMainBinding binding;


    private Boolean isRecording;

    private TextView tv_xVal;
    private TextView tv_yVal;
    private TextView tv_zVal;
    private TextView Buffet;


    private SensorManager sensorManager;
    private Sensor accSensor;

    BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private static final String TAG = "____Main___";
    private final int REQUEST_ENABLE_BT = 1;
    private final int ALL_PERMISSIONS = 100;
;


    private File dataFile;
    public FileWriter fileWriter;
    /*
     Initialize UI, SensorManager, calls requestAppPermissions
     to obtain permissions from user
          */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //BtStatus = binding.textBtStatus;
        tv_xVal = binding.textXVal;
        tv_yVal = binding.textYVal;
        tv_zVal = binding.textZVal;



        Buffet =  binding.textReadBuffer;

        requestAppPermissions();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();


        mButtonAdd = findViewById(R.id.btn_stop);



        isRecording = false;
        mButtonAdd.setText("Start");

        SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        Date date = new Date();
        String dateString = formatter.format(date).toString();
        Log.d(TAG, dateString);
        String fileName = dateString + ".csv";
        File samples = getDir("samples", Context.MODE_PRIVATE);

        try {
            dataFile = new File (samples, fileName);
            fileWriter = new FileWriter(dataFile);
            fileWriter.write("test\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(isRecording == true) {
                    Log.d(TAG, isRecording.toString());

                    Log.e(TAG, "Stop Clicked");
                    mButtonAdd.setText("Start");
                    isRecording = false;
                }
                else{
                    mButtonAdd.setText("Stop");
                    isRecording = true;
                    Log.e("TAG","Start clicked");
                }


            }
        });



    }


    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    /*
    Enter here after startActivityForResult(...) returns
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
//        if (requestCode == REQUEST_ENABLE_BT) {
//            if (resultCode == RESULT_OK) {
//                BtStatus.setText(getString(R.string.sEnabled));
//            } else {
//                BtStatus.setText(getString(R.string.sDisabled));
//            }
//        }
//        if(requestCode == REQUEST_BT_DISCOVERABLE) {
//            if(resultCode == RESULT_OK) {
//                BtStatus.setText("Visible to other devices");
//                setupBluetoothService();
//            }
//            else
//                BtStatus.setText("Not visible");
//        }
//        super.onActivityResult(requestCode, resultCode, Data);
//    }


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
        //stop = false;
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Tries to send a message containing sensor data at each reading
     */
    public void onSensorChanged(SensorEvent event) {
        if (isRecording) {
            //point = new Point();
            Float x = event.values[0];
            Float y = event.values[1];
            Float z = event.values[2];

            long time = System.currentTimeMillis();

            long time1 = time - timestamp1;

            String saveString = "" + time1 + " " + x.toString() + " " + y.toString() + " " + z.toString() +"\n";
            try {
                fileWriter.write(saveString);
            } catch (IOException e) {
                e.printStackTrace();
            }


            tv_xVal.setText(x.toString());
            tv_yVal.setText(y.toString());
            tv_zVal.setText(z.toString());


            Buffet.setText(Double.toString(time1 / 1000));

        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed");
    }

}

