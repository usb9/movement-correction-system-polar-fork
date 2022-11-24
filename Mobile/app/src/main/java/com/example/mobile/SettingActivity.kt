package com.example.mobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.io.FileOutputStream
import java.util.*


class SettingActivity : AppCompatActivity() {
    companion object {  // ~ static JAVA
        private const val TAG = "SettingActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private var deviceId = ""
    private val fileName: String = "MyDevicesId.txt"

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(applicationContext, PolarBleApi.ALL_FEATURES)
    }

    private var scanDisposable: Disposable? = null
    private var bluetoothEnabled = false

    private lateinit var scanButton: Button

    // ATTENTION! Replace with the device ID from your device at index 1st of array below.
    private val listDeviceId = mutableListOf<String>("select one device")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())

        scanButton = findViewById(R.id.scan_button)

        api.setPolarFilter(false)
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            override fun streamingFeaturesReady(
                identifier: String, features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

        val spinner: Spinner = findViewById(R.id.spinner)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, listDeviceId
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listDeviceId[position] != "select one device") {
                    deviceId = listDeviceId[position]

                    try {
                        var fout: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                        fout.write("$deviceId".toByteArray())
                        fout.close()
                    } catch (ex: Exception) {
                        //Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                        Log.e("hihihaha", "Error: ${ex.message}")
                    }
                }
            }
        }

        scanButton.setOnClickListener {
            val isDisposed = scanDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(scanButton, R.string.scanning_devices)
                scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarDeviceInfo: PolarDeviceInfo ->
                            listDeviceId.add("${polarDeviceInfo.deviceId}")
                            showToast("Found and added ${polarDeviceInfo.deviceId}")
                            // Log.d(TAG, "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable)
                        },
                        { error: Throwable ->
                            toggleButtonUp(scanButton, "Scan devices")
                            showToast("$error")
                            // Log.e(TAG, "Device scan failed. Reason $error")
                        },
                        {
                            toggleButtonUp(scanButton, "Scan devices")
                            scanDisposable?.dispose()
                            showToast("complete")
                            // Log.d(TAG, "complete")
                        }
                    )
            } else {
                toggleButtonUp(scanButton, "Scan devices")
                scanDisposable?.dispose()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()

    }

    private fun disableAllButtons() {
    }

    private fun enableAllButtons() {
    }
}