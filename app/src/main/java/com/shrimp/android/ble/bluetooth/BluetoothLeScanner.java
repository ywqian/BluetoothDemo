package com.shrimp.android.ble.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;


/**
 * 项目名称：BluetoothDemo
 * 包名称： com.shrimp.android.ble.bluetooth
 * 类描述： 蓝牙扫描类：搜索蓝牙，连接前取消搜索，加快连接
 * author: ywq
 * 创建时间：2017/2/4
 */
public class BluetoothLeScanner
{

	private static BluetoothAdapter mBluetoothAdapter;
	
	// 默认扫描设备30s，之后停止扫描，可修改扫描时间
//	private long					  SCAN_PERIOD = 60 * 1000;
	private static boolean			  mScanning;
	
	/**
	 * 开启蓝牙设备
	 */
	public static boolean enable(Context context) {
		final BluetoothManager bluetoothManager = (BluetoothManager) context.getApplicationContext()
				.getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager == null)
		{
			return false;
		}
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// 获得系统默认的蓝牙适配器
		if (mBluetoothAdapter != null)
		{
			// 检查手机的蓝牙是否打开，如果没有，通过enable()方法打开
			if (!mBluetoothAdapter.isEnabled())
			{
				//请求用户开启
				/*
				 * Intent intent=new
				 * Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				 * activity.startActivityForResult(intent,
				 * activity.RESULT_FIRST_USER);
				 */
				//直接开启，不经过提示
				mBluetoothAdapter.enable();
			}
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * 搜索设备
	 */
	public static void scanDevice() {
		if (mBluetoothAdapter == null) {
			return;
		}
		if (!mBluetoothAdapter.isEnabled())
		{
			mBluetoothAdapter.enable();
		}
		mScanning = true;
		mBluetoothAdapter.startDiscovery();
	}
	
	/**
	 * 取消搜索设备
	 */
	public static void cancelScan() {
		if (mBluetoothAdapter == null) {
			return;
		}
		if (mScanning || mBluetoothAdapter.isDiscovering())
		{
			mBluetoothAdapter.cancelDiscovery();
			mScanning = false;
		}
	}

	/**
	 * 是否正在扫描
	 * @return
     */
	public static boolean isScanning() {
		if (mBluetoothAdapter == null) {
			return false;
		}
		return (mScanning || mBluetoothAdapter.isDiscovering());
	}

	public static BluetoothAdapter getBluetoothAdapter() {
		return mBluetoothAdapter;
	}
}
