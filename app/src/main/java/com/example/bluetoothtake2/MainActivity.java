package com.example.bluetoothtake2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static no.nordicsemi.android.support.v18.scanner.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_MODE_AGGRESSIVE;

public class MainActivity extends AppCompatActivity {

    // UUID
    private final static UUID MEASUREMENT_SERVICE_UUID = UUID.fromString("46a970e0-0d5f-11e2-8b5e-0002a5d5c51b");
    private static final UUID MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("0aad7ea0-0d60-11e2-8e3c-0002a5d5c51b");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Location permission objects
    private boolean permissionAccepted = false;
    private String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

    // Objects for scanning
    private List<ScanFilter> filters;
    private BluetoothLeScannerCompat scanner;
    private ScanSettings settings;
    private boolean isScanning;
    private int REQUEST_PERMISSIONS = 1;
    private static final long SCAN_PERIOD = 10000;
    private Handler handler = new Handler();

    // Connection
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int connectionState = STATE_DISCONNECTED;

    // Bluetooth objects
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt bluetoothGatt;

    // Strings that will be displayed
    private String deviceName, spo2String, pulseRateString;;
    private final String NOT_SCANNING = "Not connected";
    private final String SCANNING = "Scanning...";
    private final String CONNECTED = "Connected!";

    // Lists
    private final ArrayList<Integer> spo2List = new ArrayList<Integer>();
    private final ArrayList<Integer> pulseRateList = new ArrayList<Integer>();
    int spo2Avg;
    int pulseRateAvg;

    // UI objects
    private TextView fieldDeviceName, fieldConnectionStatus,
            fieldSpo2, fieldPR;
    private Button disconnectButton, startScanButton, showDataButton;

    // User identificaton number
    final String USER_ID = "001";
    private String fileName;

    // ScanCallback object
    private ScanCallback scanCallback = new ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if (btDevice.getName() != null) {
                Log.i("TAG", "Got device name: " + btDevice.getName());
                if (btDevice.getName().startsWith("Nonin")) {
                    Log.i("TAG", "Got Nonin Device");
                    // stop scanning after Nonin has been found
                    scanLeDevice(false);
                    // get the device name
                    deviceName = btDevice.getName();
                    // connect to the device
                    connect(btDevice.getAddress());
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionAccepted) finish();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);

        // connect to respective UI id attributes
        fieldDeviceName = (TextView) findViewById(R.id.deviceNameField);
        fieldConnectionStatus = (TextView) findViewById(R.id.isScanning);
        fieldSpo2 = (TextView) findViewById(R.id.spo2Field);
        fieldPR = (TextView) findViewById(R.id.pulseRateField);

        // Connect to disconnect button and set onclick listener
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton
                .setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("Tag", "Disconnect Button pressed");
                        disconnect();
                    }
                });

        // Connect to start scanning button and set onclick listener
        startScanButton = (Button) findViewById(R.id.startScanButton);
        startScanButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Tag", "Start scan button pressed");
                scanLeDevice(true);
            }
        });

        // Will call the method to show the data in the file
        showDataButton = (Button) findViewById(R.id.show_data);
        showDataButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Tag", "Show data button pressed");
                showData();
            }
        });

        // Initialize bluetooth adapter and manager
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Set scan settings
        settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(MATCH_MODE_AGGRESSIVE)
                .build();
        // Filter for Nonin device
        filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(MEASUREMENT_SERVICE_UUID)).build();
        filters.add(filter);

        // Start scanning
        scanLeDevice(true);
    }

    /**
     * @desc This method either starts scanning for new devices
     * for ten seconds or stops scanning
     *
     * @param enable
     *            - boolean which determines if scan will be started
     *            or stopped
     *
     */
    private void scanLeDevice(final boolean enable) {
        // Initialize scanner
        scanner = BluetoothLeScannerCompat.getScanner();
        if (enable) {
            // Remove start scan again button
            startScanButton.setVisibility(View.GONE);
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // If after 10 seconds the device isn't connected, then stop scanning and
                    // ask user to scan again
                    if (connectionState == STATE_DISCONNECTED) {
                        isScanning = false;
                        scanner.stopScan(scanCallback);
                        updateUIConnectionStatus(NOT_SCANNING);
                        showAlert();
                    }
                }
            }, SCAN_PERIOD);
            Log.i("TAG", "Scan start w/ filters");
            updateUIConnectionStatus(SCANNING);
            isScanning = true;
            scanner.startScan(filters, settings, scanCallback);
        } else {
            isScanning = false;
            scanner.stopScan(scanCallback);
        }
        Log.i("TAG", "Is scanning? " + Boolean.toString(isScanning));
    }


    /**
     * @desc This method displays an alert dialog which prompts the user to scan again
     */
    private void showAlert() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Device not found")
                .setMessage("Make sure the device is all the way on your finger and is within 15" +
                        " feet!\nPlease scan again :)")
                .setCancelable(false)
                .setPositiveButton("Scan Again",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                scanLeDevice(true);
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * @desc This method tries to connect to the selected the device
     *
     * @param address
     *            - Bluetooth LE device address
     */
    public boolean connect(final String address) {

        if (bluetoothAdapter == null || address == null) {
            Log.i("TAG",
                    "BluetoothAdapter not initialized or unspecified address");
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.i("TAG", "Device not found. Unable to connect");
            return false;
        }

        // Connect to the device
        connectGatt(device);
        Log.i("Tag", "Connecting to " + device.getName());
        connectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * @desc connects to the GATT server hosted by this device
     *
     * @param device
     *            - the Bluetooth device to be connected
     */
    private void connectGatt(final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothGatt = device.connectGatt(getApplication(), false,
                        gattCallback);
            }
        });
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        /**
         * @desc Callback invoked when the list of remote services, charcters
         * and descriptors for the remote device have been updated, i.e. new
         * service have been discovered
         *
         * @param gatt - Gat client invoked discoverServices()
         *
         * @param status - GATT_SUCCESS if the remote device has been explored
         * successfully
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("TAG", "Inside service dicovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("TAG", "read device for service and characteristics");
                readDevice(gatt);
            } else {
                Log.i("TAG", "onServicesDiscovered not sucess: " + status);
            }
        }

        /**
         * @desc Callback triggered as a result of a remote characteristic
         *       notification Also, the received data are parsed here into
         *       measurements
         *
         * @param gatt
         *            - GATT client the characteristic is associated with
         *
         * @param characteristic
         *            Characteristic that has been updated as a result of remote
         *            notification event
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("TAG", "Measurements recieved!");

            // Indicates the current device status
            final int status = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            // Indicates that the display is synchronized to the SpO2 and pulse
            // rate values contained in this packet
            final int kSyncIndication = (status & 0x1);
            // Average amplitude indicates low or marginal signal quality
            final int kWeakSignal = (status & 0x2) >> 1;
            // Used to indicate that the data successfully passed the SmartPoint
            // Algorithm
            final int kSmartPoint = (status & 0x4) >> 2;
            // An absence of consecutive good pulse signal
            final int kSearching = (status & 0x8) >> 3;
            // CorrectCheck technology indicates that the finger is placed
            // correctly in the oximeter
            final int kCorrectCheck = (status & 0x10) >> 4;
            // Low or critical battery is indicated on the device
            final int kLowBattery = (status & 0x20) >> 5;
            // indicates whether Bluetooth connection is encrypted
            final int kEncryption = (status & 0x40) >> 6;

            // Voltage level of the batteries in use in .1 volt increments
            // [decivolts]
            final int decivoltage = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            // Value that indicates the relative strength of the pulsatile
            // signal. Units 0.01% (hundreds of a precent)
            final int paiValue = (characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 3) * 256 + characteristic
                    .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4));
            // Value that indicates that number of seconds since the device went
            // into run mod (between 0-65535)
            final int secondCnt = (characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 5) * 256 + characteristic
                    .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 6));
            // SpO2 percentage 0-100 (127 indicates missing)
            final int spo2 = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 7);
            // Pulse Rate in beats per minute, 0-325. (511 indicates missing)
            final int pulseRate = (characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, 8) * 256 + characteristic
                    .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9));

            // Toast if battery of Nonin is low
            // It seems that not low battery happens when kLowBattery = 0
            if (kLowBattery != 0){
                Toast.makeText(MainActivity.this, "Nonin device has low battery", Toast.LENGTH_LONG).show();
            }
            // A value of 127 indicates no data for SpO2
            if (spo2 == 127) {
                spo2String = "--";
            } else {
                spo2String = String.format(" %d", spo2);
                spo2List.add(spo2);
            }
            // A value of 511 indicates no data for pulse
            if (pulseRate == 511) {
                pulseRateString = "--";
            } else {
                pulseRateString = String.format(" %d", pulseRate);
                pulseRateList.add(pulseRate);
            }

            // Display the measurement
            updateUI();
        }

        /**
         * @desc Callback indicating when GATT client has connected/disconnected
         *       to/from a remote GATT server
         *
         * @param gatt
         *            - GATT client
         *
         * @param status
         *            - status of the connect or disconnect operation
         *
         * @param newState
         *            - returns the new connection state
         */
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.i("Tag", "ConnectionStateChanged");
            // connected to a GATT server
            if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Tag", "Attempting to start service discovery:"
                        + bluetoothGatt.discoverServices());
                updateUIConnectionStatus(CONNECTED);
                connectionState = STATE_CONNECTED;
                // disconnected from a GATT server
            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("Tag", "Disconnected from GATT server.");

                if (connectionState != STATE_DISCONNECTED) {
                    disconnect();
                }

                // GATT failure
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("Tag", "GATT Failure");
                if (connectionState != STATE_DISCONNECTED) {
                    disconnect();
                }
            }
        }

        /**
         * @desc reads the specified characteristic of the service and enables
         *       notification
         *
         * @param gatt
         *            - GATT client
         */
        public void readDevice(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;

            characteristic = bluetoothGatt
                    .getService(MEASUREMENT_SERVICE_UUID).getCharacteristic(
                            MEASUREMENT_CHARACTERISTIC_UUID);

            bluetoothGatt.readCharacteristic(characteristic);
            /*
             * Once the notification are enabled for a characteristics,
             * onCharacteristicsChnaged() callback is triggered if the
             * characteristic changes on the remote device.
             */
            bluetoothGatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor desc = characteristic
                    .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(desc);

            Log.i("Tag", "Success while reading");
        }
    };

    /**
     * @desc update the UI with the measurements
     */
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // display device name
                fieldDeviceName.setText(deviceName);
                // display Spo2 value
                fieldSpo2.setText(spo2String);
                // display Pulse Rate value
                fieldPR.setText(pulseRateString);
            }
        });
    }

    /**
     * @desc update the UI with the measurements and shows/hides Start scanning
     *            and disconnect buttons
     *
     *
     * @param status
     *            - represents the status of the current connection, "Connected"
     *            or "Scanning"
     */
    private void updateUIConnectionStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // update the status field
                fieldConnectionStatus.setText(status);
                // connected
                if (status == CONNECTED) {
                    // show disconnect button
                    disconnectButton.setVisibility(View.VISIBLE);
                } else {
                    // hide the disconnect button
                    disconnectButton.setVisibility(View.GONE);
                    // empty the UI field
                    fieldDeviceName.setText("");
                    fieldSpo2.setText("");
                    fieldPR.setText("");
                    // If not scanning, show scan again button
                    if (status == NOT_SCANNING) startScanButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * @desc Disconnects an existing connection or cancel a pending connection.
     *       The disconnection result is reported asynchronously through the
     *       {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *       callback.
     */
    public void disconnect() {
        connectionState = STATE_DISCONNECTED;
        // disconnect
        bluetoothGatt.disconnect();
        close();
        // reScan
        updateUIConnectionStatus(NOT_SCANNING);
    }

    /**
     * @desc After using a given BLE device, the app must call this method to
     *       ensure resources are released properly.
     */
    public void close() {
        sendData();
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * @desc This method writes the SpO2 and pulse rate averages to a file
     *       named with the date and time of the readings
     *       participant id number, date (year, month, day), time
     */
    public void sendData() {
        getAvgs();
        Log.i("tag", "SpO2 Avg: " + spo2Avg);
        Log.i("tag", "Pulse Rate Avg: " + pulseRateAvg);

        // get date and time
        Calendar rightNow = Calendar.getInstance();
        int month = rightNow.get(Calendar.MONTH);
        int day = rightNow.get(Calendar.DAY_OF_MONTH);
        int year = rightNow.get(Calendar.YEAR);
        int hours = rightNow.get(Calendar.HOUR);
        int minutes = rightNow.get(Calendar.MINUTE);
        String dateAndTime = year + "." + month + "."
                + day + "--" + hours + ":";
        if (minutes < 10) dateAndTime += "0" + minutes;
        else dateAndTime += Integer.toString(minutes);
        String data = dateAndTime + "\nSpO2 Avg: " + spo2Avg
                + "\nPulse Rate Avg: " + pulseRateAvg;
        fileName = USER_ID + "--" + dateAndTime + ".txt";


        // Write to internal storage
        File file = new File(getFilesDir(), fileName);
        FileOutputStream fos = null;

        try {
            fos = this.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            Toast.makeText(MainActivity.this, "Saved data to " + getFilesDir() +
                    "/" + fileName, Toast.LENGTH_LONG).show();
            fos.close();
            showDataButton.setVisibility(View.VISIBLE);
        } catch (FileNotFoundException e) {
            Toast.makeText(MainActivity.this, "The File " + fileName + " was not found",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String emailAddress = "emcmullen@g.hmc.edu";
        String subject = "Nonin data" + dateAndTime;

        // send email with data in attachment
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,new String[] { emailAddress });
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, data);
        /*
        If the file is stored in external storage instead of internal, it can be attached to the
        email from the app. It is sightly more complicated to get that to work though
         */

        this.startActivity(Intent.createChooser(emailIntent,"Sending email..."));



    }

    /**
     * @desc This method calculates up to a 30 second average of the
     *       SpO2 and Pulse Rate values that have been sent by the Nonin
     *       pulse oximeter
     */
    private void getAvgs() {
        int spo2Total = 0;
        int pulseRateTotal = 0;
        if (spo2List.size() < 30) {
            for (int i : spo2List) {
                spo2Total += i;
            }
            spo2Avg = spo2Total/spo2List.size();
            for (int j : pulseRateList) {
                pulseRateTotal += j;
            }
            pulseRateAvg = pulseRateTotal/pulseRateList.size();
        } else {
            for (int k = 0; k < 30; k++) {
                spo2Total += spo2List.get(k);
            }
            spo2Avg = spo2Total/30;
            for (int l = 0; l < 30; l++) {
                spo2Total += pulseRateList.get(l);
            }
            pulseRateAvg = pulseRateTotal/30;
        }
    }

    /**
     * @desc This method reads the data from the internal file and shows a toast with the data
     */
    private void showData() {
        FileInputStream fin = null;
        try {
            fin = openFileInput(fileName);
            int c;
            String temp="";
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            Toast.makeText(MainActivity.this, "Content of " + fileName +
                    ":\n" + temp, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}