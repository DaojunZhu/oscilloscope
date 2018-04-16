package com.example.daojun.oscilloscope;

import android.Manifest;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "DAOJUN";

    private LineChart mChart;
    private XAxis xAxis;
    private YAxis leftYAxis;
    private YAxis rightYAxis;

    private ImageView bluetoothImage;

    private ListView lvNewDevices;
    private LayoutInflater inflater;
    private View lvNewDevicesLayout;
    private AlertDialog mDeviceListDialog;


    /*BlueTooth*/
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    BluetoothConnectionService mBluetoothConnetion;
    TextView txtBltConnection;
    private ProgressDialog mProgressDialog;

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothDevice mBTDevice;      //the remote device to be connect

    //Debug blutooth connection
//    Button btnSend;
//    EditText editMsg;
//    StringBuilder messages = new StringBuilder();


    //create a broadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //when discovery finds a device
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive:  STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON ");
                        break;
                }
            }
        }
    };

    /*
    * Broadcast Receiver for listing devices that are not yet paired
    * */
    private BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND");

            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ":" + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context,R.layout.device_adpater_view,mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    /*
    * Broadcast Receiver that detects bond state changes (Pairing status changes)
    * */
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case 1: bonded already
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                }
                //case 2: creating a bond
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING. ");
                }
                //case 3: breaking a bond
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };



    /*
    * Broadcast Receiver for discovery finished
    * */
    BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mBroadcastReceiver4: discovery finished.");

        }
    };

    /*
     * Broadcast Receiver for discovery started
     * */
    BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){

            }
        }

    };

    BroadcastReceiver bltMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
//            messages.append(text+"\n");
//            editMsg.setText(messages);
        }
    };


    BroadcastReceiver bltConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "bltConnectionStateReceiver: connection state changed");
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
                Log.d(TAG, "bltConnectionStateReceiver: connected");
                Toast.makeText(MainActivity.this,"Bluetooth Connected",Toast.LENGTH_SHORT).show();
                bluetoothImage.setImageResource(R.drawable.bluetooth_connected);
                txtBltConnection.setText(mBTDevice.getAddress());
            }else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                Log.d(TAG, "bltConnectionStateReceiver: disconnected");
                if(mProgressDialog != null){
                    mProgressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this,"Bluetooth Disonnected",Toast.LENGTH_SHORT).show();
                bluetoothImage.setImageResource(R.drawable.bluetooth_disabled);
                txtBltConnection.setText("Waiting to connect");
            }
        }
    };



    //Devices list Found item click callback
    private AdapterView.OnItemClickListener mDeviceItemClickedHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(TAG, "onItemClick: click the device.");
            mBluetoothAdapter.cancelDiscovery();
            mDeviceListDialog.dismiss();
            String deviceName = mBTDevices.get(i).getName();
            String deviceAddress = mBTDevices.get(i).getAddress();
            Log.d(TAG, "Device name: " + deviceName);
            Log.d(TAG, "Device address: " + deviceAddress);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                Log.d(TAG, "Trying to pair with " + deviceName);
                mBTDevices.get(i).createBond();

                mBTDevice = mBTDevices.get(i);
                mBluetoothConnetion = new BluetoothConnectionService(MainActivity.this);
                startConnection();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate execute");

        inflater = getLayoutInflater();
        lvNewDevicesLayout = inflater.inflate(R.layout.device_list_view,null);
        lvNewDevices = lvNewDevicesLayout.findViewById(R.id.lvNewDevices);
        lvNewDevices.setOnItemClickListener(mDeviceItemClickedHandler);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Broadcasts when bond state changes(ie: pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver3,filter);

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1,BTIntent);

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver2,discoverDevicesIntent);

        IntentFilter discoveryFinishedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver4,discoveryFinishedIntent);

        IntentFilter discoveryStartedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(mBroadcastReceiver5,discoveryStartedIntent);

        LocalBroadcastManager.getInstance(this).registerReceiver(bltMessageReceiver,new IntentFilter("incomingMessage"));

        IntentFilter bltConnectionStateIntent = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter bltDisconnectionStateIntent = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bltConnectionStateReceiver,bltConnectionStateIntent);
        registerReceiver(bltConnectionStateReceiver,bltDisconnectionStateIntent);


        mChart = (LineChart)findViewById(R.id.chart);
        xAxis = mChart.getXAxis();
        leftYAxis = mChart.getAxisLeft();
        rightYAxis = mChart.getAxisRight();

        bluetoothImage = (ImageView)findViewById(R.id.bluetooth_image);
        txtBltConnection = (TextView)findViewById(R.id.txtBltConnection);

        bluetoothImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: open bluetooth");
                if(mBluetoothAdapter.isEnabled()){
                    if(mBluetoothConnetion!=null){
                        if(mBluetoothConnetion.isConnected()){
                            mProgressDialog = ProgressDialog.show(MainActivity.this,"Disonnecting Bluetooth","Please Wait....",true);
                            mBluetoothConnetion.resetConnection();
                            return;
                        }
                    }
                    openBT();
                    discoverDevices();

                    if(lvNewDevicesLayout.getParent() != null){
                        ((ViewGroup)lvNewDevicesLayout.getParent()).removeView(lvNewDevicesLayout);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    mDeviceListDialog = builder.setTitle("Devices Found:")
                            .setView(lvNewDevicesLayout)
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Log.d(TAG, "onClick: click the dialog cancel button.");
                                }
                            })
                            .create();
                    mDeviceListDialog.show();
                }
            }
        });

        chartInit();
        addDataSets();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy:  called");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        unregisterReceiver(mBroadcastReceiver5);
        unregisterReceiver(bltConnectionStateReceiver);
    }

    public void openBT(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "openBT: Does not have BT capabilities.");
            return;
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "openBT: Enabling BT. ");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
        } else{
            Log.d(TAG, "openBT: BT has already opened.");
        }
    }

    public void discoverDevices(){
        Log.d(TAG, "discoverDevices: looking for unpaired devices.");

        if(!mBTDevices.isEmpty()){
            mBTDevices.clear();
        }

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "discoverDevices: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();

        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();

        }
    }

    private void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
           int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    //create method for starting connection
    //remember the conncction will fail and app will crash if you haven't paired first
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnetion.startClient(device,uuid);
    }

    private void chartInit(){
        mChart.setNoDataText("No data for the moment");

        mChart.setTouchEnabled(true);
        mChart.setHighlightPerDragEnabled(true);
        //enable dragging and scaling
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        //set pinch zoom to avoid scaling x and y separately
        mChart.setPinchZoom(true);
        mChart.setScaleYEnabled(true);
        //  background color
        mChart.setBackgroundColor(Color.BLACK);
        mChart.getDescription().setEnabled(false);

        // now work on data
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        // add data to line chart
        mChart.setData(data);

        // get legend object
        Legend l = mChart.getLegend();
        //customize legend
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(500f);

        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setAxisMaximum(16f);
        leftYAxis.setAxisMinimum(-16f);
        leftYAxis.setDrawGridLines(true);

        rightYAxis.setAxisMaximum(16f);
        rightYAxis.setAxisMinimum(-16f);
        rightYAxis.setTextColor(Color.WHITE);
        rightYAxis.setEnabled(true);
        mChart.invalidate();
    }


    void addDataSets(){

        float peakValue = 7.96f;
        float frequency = 1f;

        List<Entry> sinEntries = new ArrayList<>();
        for(float i=0;i<10000f;i +=1f){
            sinEntries.add(new Entry( i,(float)Math.sin(frequency*1000*(2*3.14f)*i/44100)*peakValue/2));
        }
        List<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet sinSet = new LineDataSet(sinEntries,"CH1");
        sinSet.setColor(Color.GREEN);
        sinSet.setDrawCircles(false);
        dataSets.add(sinSet);
        mChart.setData(new LineData(dataSets));
        mChart.invalidate();
    }
}
