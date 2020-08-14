package com.example.bluetoothtake2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.ValueChangedCallback;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public class MyBleManager //extends BleManager
    {
//    final static UUID SERVICE_UUID       = UUID.fromString("46a970e0-0d5f-11e2-8b5e-0002a5d5c51b");
//    final static UUID OXIMETRY_CHAR      = UUID.fromString("0aad7ea0-0d60-11e2-8e3c-0002a5d5c51b");
//    final static UUID PI_CHAR            = UUID.fromString("34e27863-76ff-4f8e-96f1-9e3993aa6199");
//    final static UUID CONTROL_POINT_CHAR = UUID.fromString("1447af80-0d60-11e2-88b6-0002a5d5c51b");
//
//    // Client characteristics
//    private BluetoothGattCharacteristic oxCharacteristic, piCharacteristic,
//            controlPointCharacteristic;
//    private ValueChangedCallback oxCallback;
//    private ValueChangedCallback piCallback;
//    //private List<int> SpO2List = new ArrayList<>();
//
//    MyBleManager(@NonNull final Context context) {
//        super(context);
//    }
//
//    /**
//     * This method must return the GATT callback used by the manager.
//     * This method must not create a new gatt callback each time it is being invoked, but rather
//     * return a single object.
//     * The object must exist when this method is called, that is in the BleManager's constructor.
//     * Therefore, it cannot return a local field in the extending manager, as this is created after
//     * the constructor finishes.
//     *
//     * @return The gatt callback object.
//     */
//    @NonNull
//    @Override
//    protected BleManagerGattCallback getGattCallback() {
//        return new MyManagerGattCallback();
//    }
//
//    @Override
//    public void log(final int priority, @NonNull final String message) {
//        if (priority == Log.ERROR) {
//            Log.println(priority, "MyBleManager", message);
//        }
//    }
//
//    /**
//     * BluetoothGatt callbacks object.
//     */
//    private class MyManagerGattCallback extends BleManagerGattCallback {
//
//        // This method will be called when the device is connected and services are discovered.
//        // You need to obtain references to the characteristics and descriptors that you will use.
//        // Return true if all required services are found, false otherwise.
//        @Override
//        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
//            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
//            if (service != null) {
//                oxCharacteristic = service.getCharacteristic(OXIMETRY_CHAR);
//                piCharacteristic = service.getCharacteristic(PI_CHAR);
//                controlPointCharacteristic = service.getCharacteristic(CONTROL_POINT_CHAR);
//            }
//            // Validate properties
//            boolean notify = false;
//            if (oxCharacteristic != null) {
//                final int properties = oxCharacteristic.getProperties();
//                notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//            }
//            boolean writeRequest = false;
//            if (piCharacteristic != null) {
//                final int properties = controlPointCharacteristic.getProperties();
//                writeRequest = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
//                piCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//            }
//            // Return true if all required services have been found
//            return oxCharacteristic != null && piCharacteristic != null
//                    && notify && writeRequest;
//        }
//
//        // If you have any optional services, allocate them here. Return true only if
//        // they are found.
//        @Override
//        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
//            return super.isOptionalServiceSupported(gatt);
//        }
//
//
//        // Initialize your device here. Often you need to enable notifications and set required
//        // MTU or write some initial data. Do it here.
//        @Override
//        protected void initialize() {
//            // You may enqueue multiple operations. A queue ensures that all operations are
//            // performed one after another, but it is not required.
//            beginAtomicRequestQueue()
//                    .add(requestMtu(247) // Remember, GATT needs 3 bytes extra. This will allow packet size of 244 bytes.
//                            .with((device, mtu) -> log(Log.INFO, "MTU set to " + mtu))
//                            .fail((device, status) -> log(Log.WARN, "Requested MTU not supported: " + status)))
//                    .add(setPreferredPhy(PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED)
//                            .fail((device, status) -> log(Log.WARN, "Requested PHY not supported: " + status)))
//                    .add(enableNotifications(oxCharacteristic))
//                    .add(enableNotifications(piCharacteristic))
//                    .done(device -> log(Log.INFO, "Target initialized"))
//                    .enqueue();
//            // You may easily enqueue more operations here like such:
//            byte[] notifCode = "01 00".getBytes();
//            writeCharacteristic(oxCharacteristic, notifCode)
//                    .done(device -> log(Log.INFO, "Greetings sent"))
//                    .enqueue();
//            writeCharacteristic(piCharacteristic, notifCode)
//                    .done(device -> log(Log.INFO, "Greetings sent"))
//                    .enqueue();
//            // Set a callback for your notifications. You may also use waitForNotification(...).
//            // Both callbacks will be called when notification is received.
//            // do this not in initialize i don't think
//            /*setNotificationCallback(oxCharacteristic, oxCallback);
//            setNotificationCallback(piCharacteristic, piCallback);*/
//            // If you need to send very long data using Write Without Response, use split()
//            // or define your own splitter in split(DataSplitter splitter, WriteProgressCallback cb).
//            /*writeCharacteristic(piCharacteristic, "Very, very long data that will no fit into MTU")
//                    .split()
//                    .enqueue();*/
//        }
//
//        @Override
//        protected void onDeviceDisconnected() {
//            // Device disconnected. Release your references here.
//            oxCharacteristic = null;
//            piCharacteristic = null;
//        }
//    }
//
//    // Define your API.
//
//    private abstract class PulseOxCallback implements ProfileDataCallback {
//        @Override
//        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
//            // Some validation?
//            if (data.size() != 1) {
//                onInvalidDataReceived(device, data);
//                return;
//            }
//            receivedPulseOxData();
//        }
//
//        abstract void receivedPulseOxData();
//    }
//
//    /** Initialize time machine. */
//    public void enableFluxCapacitor(final int year) {
//        /*waitForNotification(oxCharacteristic)
//                .trigger(
//                        SpO2List.add(oxCharacteristic.getIntValue())
//                                .done(device -> log(Log.INFO, "Got int value"))
//                )
//                .with(new FluxHandler() {
//                    public void onFluxCapacitorEngaged() {
//                        log(Log.WARN, "Flux Capacitor enabled! Going back to the future in 3 seconds!");
//
//                        sleep(3000).enqueue();
//                        write(piCharacteristic, "Hold on!".getBytes())
//                                .done(device -> log(Log.WARN, "It's " + year + "!"))
//                                .fail((device, status) -> "Not enough flux? (status: " + status + ")")
//                                .enqueue();
//                    }
//                })
//                .enqueue();*/
//    }
//
//    /**
//     * Aborts time travel. Call during 3 sec after enabling Flux Capacitor and only if you don't
//     * like 2020.
//     */
//    public void abort() {
//        cancelQueue();
//    }

}
