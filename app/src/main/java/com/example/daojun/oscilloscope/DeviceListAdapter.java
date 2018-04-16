package com.example.daojun.oscilloscope;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice>{
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int mViewResourceId;

    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices){
        super(context,tvResourceId,devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    public View getView(int position, View converView, ViewGroup parent){

        if(converView == null){
            converView = mLayoutInflater.inflate(mViewResourceId,null);
        }

        BluetoothDevice device = mDevices.get(position);
        if(device != null){
            TextView deviceName = (TextView) converView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView) converView.findViewById(R.id.tvDeviceAddress);

            if(deviceName != null){
                if(device.getName() != null){
                    deviceName.setText(device.getName());
                }else{
                    deviceName.setText("Unknown");
                }
            }


            if(deviceAddress != null){
                deviceAddress.setText(device.getAddress());
            }
        }
        return converView;
    }
}
