package com.shrimp.android.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.shrimp.android.ble.bluetooth.BluetoothLeClass;
import com.shrimp.android.ble.bluetooth.BluetoothLeScanner;
import com.shrimp.android.ble.bluetooth.BluetoothReceiver;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BluetoothReceiver.IBluetoothDiscoverListener {

    private ListView listView;
    private ArrayList<BluetoothDevice> data = new ArrayList<>();
    private ArrayList<String> list = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private BluetoothLeClass bluetoothLeClass;
    private BluetoothReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();


                BluetoothLeScanner.scanDevice();
                Toast.makeText(MainActivity.this, "正在搜索...", Toast.LENGTH_SHORT).show();
            }
        });

        register();
        initView();

        BluetoothLeScanner.enable(MainActivity.this);
        bluetoothLeClass = BluetoothLeClass.getInstance();
        bluetoothLeClass.initialize(this, BluetoothLeScanner.getBluetoothAdapter());
    }

    private void register() {
        receiver = new BluetoothReceiver(this);
        registerReceiver(receiver, BluetoothReceiver.makeBluetoothSearchFilter());
    }

    private void initView() {
        listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, R.layout.item_bluetooth, R.id.item_detail, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothLeClass.connect(data.get(position).getAddress());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        if (!data.contains(device)) {
            data.add(device);
            list.add("name：" + device.getName() + "\naddress：" + device.getAddress());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDiscoveryFinish() {
        Snackbar.make(listView, "搜索完成", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

        BluetoothLeScanner.cancelScan();
    }
}
