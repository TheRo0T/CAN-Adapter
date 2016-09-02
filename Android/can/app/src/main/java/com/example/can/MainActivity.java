package com.example.can;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

    BroadcastReceiver br;
    TextView tv1,tv2,tv3,tv4,tv5,tv6;
    ImageView ivBand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv1 = (TextView) findViewById(R.id.textView1);
        tv2 = (TextView) findViewById(R.id.textView2);
        tv3 = (TextView) findViewById(R.id.textView3);
        tv4 = (TextView) findViewById(R.id.textView4);
        tv5 = (TextView) findViewById(R.id.textView5);
        tv6 = (TextView) findViewById(R.id.textView6);
        ivBand = (ImageView) findViewById(R.id.imageViewBand);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                int paramName = intent.getIntExtra(MyService.PARAM_NAME, 0);
                String paramValue = intent.getStringExtra(MyService.PARAM_VALUE);

      //          Log.d(MyService.LOG_TAG, "onReceive: param_name = " + paramName + ", value = " + paramValue);

                switch (paramName) {
                    case MyService.CODE_EXT_TEMPERATURE:
                        tv1.setText(paramValue);
                        break;

                    case MyService.CODE_RADIO_FREQ:
                        tv3.setText(paramValue);
                        break;

                    case MyService.CODE_RADIO_MEM:
                        tv5.setText("Mem" + paramValue);
                        break;

                    case MyService.CODE_TIME:
                        tv2.setText(paramValue);
                        break;

                    case MyService.CODE_DATE:
                        tv6.setText(paramValue);
                        break;

                    case MyService.CODE_RADIO_BAND:
                        switch (paramValue) {
                            case "1":
                                ivBand.setImageResource(R.drawable.fm1);
                                break;
                            case "2":
                                ivBand.setImageResource(R.drawable.fm2);
                                break;
                        }
                        break;



                }


            }
        };

        IntentFilter filter = new IntentFilter(MyService.NEW_CAN_MSG);
        registerReceiver(br, filter);

        startService(new Intent(this, MyService.class));

    }
}
