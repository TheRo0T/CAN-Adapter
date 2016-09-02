package com.example.can;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MyService extends Service {

    int cmdBufIdx = 0;
    byte[] cmdBuf = new byte[30];

    public static final String NEW_CAN_MSG = "com.example.action.NEW_CAN_MSG";

    public static final String PARAM_NAME = "name";
    public final static String PARAM_VALUE = "value";

    public static final int CODE_IGNITION = 1;
    public static final int CODE_EXT_TEMPERATURE = 2;
    public static final int CODE_RADIO_FREQ = 3;
    public static final int CODE_RADIO_MEM = 4;
    public static final int CODE_RADIO_BAND = 5;
    public static final int CODE_TIME = 6;
    public static final int CODE_DATE = 7;

    private final String DEVICE_ADDRESS="20:15:12:21:21:74";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    boolean stopThread;
    byte buffer[];

    final String LOG_TAG = "MyLog";
    boolean deviceConnected=false;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "myService: onCreate");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "myService: onStartCommand");

        if(BTinit())
        {
            if(BTconnect())
            {
                deviceConnected=true;
                beginListenForData();
                Toast.makeText(MyService.this, "Connection Opened!", Toast.LENGTH_SHORT).show();
            }

        }


        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Service: onDestroy");
//        stopThread = true;
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        deviceConnected=false;
        Toast.makeText(MyService.this, "Connection Closed!", Toast.LENGTH_SHORT).show();

    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);

                            for (byte rxChar : rawBytes) {
                                if (rxChar == '\r') {
                                    execCmd(cmdBuf);
                                    cmdBufIdx = 0;
                                } else {
                                    cmdBuf[cmdBufIdx++] = rxChar;
                                }
                            }
/*
                    //--------------------------------------------------------------
                            try {
                                outputStream.write(rawBytes);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    //---------------------------------------------------------------
*/
                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    public static byte ascii2byte(byte val) {
        byte temp = val;
        if (temp > 0x60) temp -= 0x27;                // convert chars a-f
        else if (temp > 0x40) temp -= 0x07;                // convert chars A-F
        temp -= 0x30;                                        // convert chars 0-9
        return (byte) (temp & 0x0F);

    }

    private void execCmd(byte[] cmdBuf) {

        int bufferPos = 0;
        int id;
        byte dlc;
        byte data[] = new byte[8];


        switch (cmdBuf[bufferPos]) {
            case 't':
                // store id
                id = ascii2byte(cmdBuf[++bufferPos]);
                id <<= 4;
                id += ascii2byte(cmdBuf[++bufferPos]);
                id <<= 4;
                id += ascii2byte(cmdBuf[++bufferPos]);

                // store data length
                dlc = ascii2byte(cmdBuf[++bufferPos]);

                // store data
                for (int dataCnt = 0; dataCnt < dlc; dataCnt++) {
                    data[dataCnt] = ascii2byte(cmdBuf[++bufferPos]);
                    data[dataCnt] <<= 4;
                    data[dataCnt] += ascii2byte(cmdBuf[++bufferPos]);
                }

                parseData(id, dlc, data);

                break;
/*
//----------------------------------------------------------------------
            case 'v':
                try {
                    String string = "v0101";
                    outputStream.write(string.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

//------------------------------------------------------------------------
*/
        }

    }

    private void parseData(int id, byte dlc, byte data[]) {

        switch (id) {

            case 0x36:
                boolean ignition = (data[7] == (byte) 0xAC);
                sendMessage(CODE_IGNITION, Boolean.toString(ignition));
                break;

            case 0xF6:
                int extTemperature = (int) (((data[6] & 0xFF) / 2.0) - 39.5);
                sendMessage(CODE_EXT_TEMPERATURE, Integer.toString(extTemperature));
                Log.i(LOG_TAG,"send intent temperature");
                break;

            case 0x225:
                float radioFreq = (float) (((((data[1] & 0x0F) << 8) + data[2])*5.0/100.0)+50.0);
                int radioMem = (data[0] & 0x07);
                int radioBand = (data[1] & 0xE0) >> 5;
                sendMessage(CODE_RADIO_FREQ, Float.toString(radioFreq));
                sendMessage(CODE_RADIO_MEM, Integer.toString(radioMem));
                sendMessage(CODE_RADIO_BAND, Integer.toString(radioBand));
                break;

            case 0x276:
                Calendar calendar = Calendar.getInstance();
                calendar.set((data[0]&0x7F)+2000, (data[1]&0x0F)-1, data[2]&0x1F, data[3]&0x1F, data[4]&0x3F);

                Locale locale = new Locale("ru", "RU");
                SimpleDateFormat dateFormat1 = new SimpleDateFormat("HH:mm");
                SimpleDateFormat dateFormat2 = new SimpleDateFormat("EE dd/MM/yy", locale);

                String time = dateFormat1.format(calendar.getTime());
                String date = dateFormat2.format(calendar.getTime());
                sendMessage(CODE_TIME, time);
                sendMessage(CODE_DATE, date);
                break;

        }
    }

    private void sendMessage (int code, String value ) {
        Intent intent = new Intent(NEW_CAN_MSG);
        intent.putExtra(PARAM_NAME, code);
        intent.putExtra(PARAM_VALUE, value);
        sendBroadcast(intent);
    }

}
