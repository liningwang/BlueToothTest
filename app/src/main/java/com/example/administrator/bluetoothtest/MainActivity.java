package com.example.administrator.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 1;
    Button scan;
    TextView find;
    ArrayAdapter<String> adapter;
    MyBluetoothService service;
    CheckBox serverCheck;
    CheckBox clientCheck;
    BluetoothDevice device;
    ListView lv;
    List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private UUID MY_UUID = UUID.fromString("b2861f0b-3964-4ef6-95eb-b7d6de0bc0ff");
//    private UUID MY_UUID = UUID.fromString("0000111F-0000-1000-8000-00805F9B34FB");
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String str = new String((byte[]) msg.obj);
            if(msg.what == MyBluetoothService.MessageConstants.MESSAGE_READ) {
                str = "read " + str;
                Log.d("wang", str);
            } else if(msg.what == MyBluetoothService.MessageConstants.MESSAGE_WRITE) {
                str = "write " + str;
            } else if(msg.what == MyBluetoothService.MessageConstants.MESSAGE_TOAST) {
                str = "toast " + str;
            }
            Toast.makeText(MainActivity.this,str,Toast.LENGTH_LONG)
                    .show();
        }
    };
    StringBuilder stringDevice = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lv = (ListView) findViewById(R.id.lv);
        serverCheck = (CheckBox) findViewById(R.id.server);
        scan = (Button) findViewById(R.id.scan);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = bluetoothDevices.get(position);
                ConnectThread thread = new ConnectThread(device);
                thread.start();
            }
        });
        serverCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(MainActivity.this,"server is " + isChecked,Toast.LENGTH_LONG)
                        .show();
                if(isChecked) {
                    AcceptThread thread = new AcceptThread();
                    thread.start();
                }
            }
        });
//        serverCheck.setEnabled(false);
//        clientCheck.setEnabled(false);
    }
    public void enableBluetooth(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this,"没有蓝牙设备",Toast.LENGTH_LONG)
                    .show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


    }

    public void discover(View view) {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
    public void receiveMsg(View view) {
        service.receiveMsg();
    }
    public void sendMsg(View view) {
        service.writeMsg("你好".getBytes());
    }
    public void scanBluetooth(View view) {
        view.setEnabled(false);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        //当搜索结束后调用onReceive
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                bluetoothDevices.add(device);
                adapter.add(deviceName);
                stringDevice.append(deviceName)
                        .append("\n")
                        .append(deviceHardwareAddress)
                        .append("\n")
                        .append("-------------\n");
                Log.d("wang","fdsdfd " + stringDevice.toString());
                if(device != null) {
//                    serverCheck.setEnabled(true);
//                    clientCheck.setEnabled(true);
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //find.setText(stringDevice.toString());
                stringDevice.delete(0,stringDevice.length());
                scan.setEnabled(true);
            }
        }
    };
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("wang", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("wang", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("wang", "Could not close the client socket", e);
            }
        }
    }
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("wang", MY_UUID);
            } catch (IOException e) {
                Log.e("wang", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    Log.d("wang","accept....");
                    socket = mmServerSocket.accept();
                    Log.d("wang","accepted");
                } catch (IOException e) {
                    Log.e("wang", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("wang", "Could not close the connect socket", e);
            }
        }
    }
    private void manageMyConnectedSocket(BluetoothSocket socket){
        service = new MyBluetoothService(socket,handler);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(this,"蓝牙已经打开",Toast.LENGTH_LONG)
                    .show();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    adapter.add(deviceName);
                    bluetoothDevices.add(device);
                }
            }

        }
    }
}
