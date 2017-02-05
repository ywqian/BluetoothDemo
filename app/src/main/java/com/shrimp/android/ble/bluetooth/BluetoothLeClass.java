/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shrimp.android.ble.bluetooth;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
/**
 * 项目名称：BluetoothDemo
 * 包名称： com.shrimp.android.ble.bluetooth
 * 类描述： 蓝牙连接、搜索服务、读写数据、断开连接
 * author: ywq
 * 创建时间：2017/2/4
 */
public class BluetoothLeClass {
    private final static String TAG = BluetoothLeClass.class.getSimpleName();

    /**
     * 连接
     */
    public final static String ACTION_GATT_CONNECTED = "com.shrimp.android.balancecar.ACTION_GATT_CONNECTED";
    /**
     * 断开连接
     */
    public final static String ACTION_GATT_DISCONNECTED = "com.shrimp.android.balancecar.ACTION_GATT_DISCONNECTED";
    // 发现服务
    public final static String ACTION_GATT_SERVICESDISCOVERED = "com.shrimp.android.balancecar.ACTION_GATT_SERVICESDISCOVERED";

    public final static String ACTION_DATA_AVAILABLE = "com.shrimp.android.balancecar.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA_RAW = "com.shrimp.android.balancecar.EXTRA_DATA_RAW";
    public final static String EXTRA_UUID_CHAR = "com.shrimp.android.balancecar.EXTRA_UUID_CHAR";
    public final static String EXTRA_SERVICE_RAW = "com.shrimp.android.balancecar.EXTRA_SERVICE_RAW";

    private static BluetoothLeClass mInstance;
    private static BluetoothAdapter mBluetoothAdapter;
    private static String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private Context context;

    private BluetoothLeClass(){
    }

    public static BluetoothLeClass getInstance() {
        if (mInstance == null) {
            synchronized (BluetoothLeClass.class) {
                if (mInstance == null) {
                    mInstance = new BluetoothLeClass();
                }
            }
        }
        return mInstance;
    }

    /**
     * 回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange--" + "status=" + status + ", " + "newState=" + newState);
            final String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED)
			{
                Log.d(TAG, "Connected to GATT server.");
                intentAction = ACTION_GATT_CONNECTED;

                Log.d(TAG, "Attempting to start service discovery:");
//                // important, Attempts to discover services after successful connection.
				mBluetoothGatt.discoverServices();

                broadcastUpdate(intentAction);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.d(TAG, "Disconnected from GATT server.");
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
            if (status == 133) { // 蓝牙连接自动断开的原因->需要清除所有的连接，重连机制
                Log.e(TAG, "蓝牙连接自动断开,status=" + status);
                close();
                sleep();
                connect(mBluetoothDeviceAddress);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            Log.d(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 解析服务
                displayGattServices(getSupportedGattServices());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
//            Log.d(TAG, "onCharacteristicRead:" + status);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.d(TAG, "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
//            Log.d(TAG, "onCharacteristicWrite");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(Context context, BluetoothAdapter adapter) {

        this.context = context.getApplicationContext();

        if (adapter == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(
                    Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
//                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
//            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
        } else {
            mBluetoothAdapter = adapter;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public synchronized boolean connect(final String address) {
        if (mBluetoothAdapter == null || TextUtils.isEmpty(address)) {
//            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
//            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        /**-------- 在连接下一个蓝牙之前彻底断开之前的连接 -----------**/
        if (mBluetoothGatt != null) {
            close();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
//            Log.e(TAG, "e=" + e);
        }
        /**--------休息500毫秒再连---------**/

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
//        Log.d(TAG, "Trying to create a new connection.----mBluetoothGatt=" + mBluetoothGatt);
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
//        Log.e(TAG, "BluetoothGatt is " + mBluetoothGatt);
        /*try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
            mBluetoothGatt.close();
        }*/
        mBluetoothGatt.close();
        mBluetoothGatt = null;
//        Log.e(TAG, "after close BluetoothGatt is " + mBluetoothGatt);

//        mCharacFFE4 = null;
//        mCharacFFE9 = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
       /* List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for(BluetoothGattDescriptor dp : descriptors){
            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(dp);
        }*/
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "writeCharacteristic failed BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void sleep() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
//            Log.e(TAG, "----e--" + e);
        }
    }

    /**
     * 连接、断开发送广播
     * @param action
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    /**
     * 服务发送广播
     * @param action
     * @param connectedAddress
     */
    private void broadcastUpdateService(final String action, final String connectedAddress) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_SERVICE_RAW, connectedAddress);
        context.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID_CHAR, characteristic.getUuid().toString());

        // Always try to add the RAW value
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA_RAW, data);
        }
        context.sendBroadcast(intent);
    }

//    public static BluetoothGattCharacteristic mCharacFFE4, mCharacFFE9;

    /**
     * 解析服务：循环打印uuid，针对项目的char uuid可找到后就停止解析
     * @param gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            broadcastUpdateService(ACTION_GATT_SERVICESDISCOVERED, null);
            return;
        }

        for (BluetoothGattService gattService : gattServices) {
            //-----Service的字段信息-----//
            Log.e(TAG,"-->service uuid:"+gattService.getUuid());

//            String serviceUUID = gattService.getUuid().toString().toUpperCase();
//            if (serviceUUID.contains("FFE0"))
//            {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());
//                    String charUUID = gattCharacteristic.getUuid().toString().toUpperCase();
//                    if (charUUID.contains("FFE4")) {
//                        mCharacFFE4 = gattCharacteristic;
//                        setCharacteristicNotification(gattCharacteristic, true);
//                        break;
//                    }
                }
//            }
//            if (serviceUUID.contains("FFE5"))
//            {
//                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                    Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());
//                    String charUUID = gattCharacteristic.getUuid().toString().toUpperCase();
//                    if (charUUID.contains("FFE9")) {
//                        mCharacFFE9 = gattCharacteristic;
//                        break;
//                    }
//                }
//            }
//            if (mCharacFFE4 != null && mCharacFFE9 != null) {
//                break;
//            }
        }
//        if (mCharacFFE4 != null && mCharacFFE9 != null) {
//            broadcastUpdateService(ACTION_GATT_SERVICESDISCOVERED, mBluetoothDeviceAddress);
//        } else {
//            broadcastUpdateService(ACTION_GATT_SERVICESDISCOVERED, null);
//        }
        broadcastUpdateService(ACTION_GATT_SERVICESDISCOVERED, mBluetoothDeviceAddress);
    }
}
