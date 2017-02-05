package com.shrimp.android.ble.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 项目名称：BluetoothDemo
 * 包名称： com.shrimp.android.ble.bluetooth
 * 类描述： 蓝牙状态监听变化
 * author: ywq
 * 创建时间：2017/2/4
 */
public class BluetoothReceiver
        extends
		BroadcastReceiver
{
	
	private static final String TAG = BluetoothReceiver.class.getSimpleName();
	private IBluetoothDiscoverListener discoverListener;
	
	public BluetoothReceiver() {
	}
	
	public BluetoothReceiver(IBluetoothDiscoverListener listener) {
		this.discoverListener = listener;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction(); // android.bluetooth.adapter.action.STATE_CHANGED
		if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
		{
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
			switch (state) {
				case BluetoothAdapter.STATE_TURNING_ON:
//					Log.d(TAG, "蓝牙正在打开");
					break;
				case BluetoothAdapter.STATE_ON:
//					Log.d(TAG, "蓝牙已打开");
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
//					Log.d(TAG, "蓝牙正在关闭");
					break;
				case BluetoothAdapter.STATE_OFF:
//					Log.d(TAG, "蓝牙已关闭");
					//BalanceCarUtil.showToast(context, "蓝牙已断开");
					break;
			}
		}
		else if (action.equals(BluetoothDevice.ACTION_FOUND)) //搜索到蓝牙设备
		{
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			//	  Log.d(TAG, "scan bluetooth device " + device.getAddress());
			if (discoverListener != null)
			{
				discoverListener.onDeviceFound(device);
			}
		}
		else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
		{
			if (discoverListener != null)
			{
				discoverListener.onDiscoveryFinish();
			}
		}
	}
	
	/**
	 * 蓝牙状态变化
	 * 
	 * @return
	 */
	public static IntentFilter makeBluetoothIntentFilter() {
		final IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		return filter;
	}
	
	/**
	 * 注册蓝牙搜索广播
	 */
	public static IntentFilter makeBluetoothSearchFilter() {
		final IntentFilter filter = new IntentFilter();
		// Register for broadcasts when a device is discovered
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		// Register for broadcasts when discovery has finished
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		return filter;
	}
	
	public interface IBluetoothDiscoverListener
	{
		void onDeviceFound(BluetoothDevice device);
		
		void onDiscoveryFinish();
	}
	
}
