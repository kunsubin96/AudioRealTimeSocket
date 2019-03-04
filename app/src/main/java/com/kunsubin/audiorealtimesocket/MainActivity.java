package com.kunsubin.audiorealtimesocket;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private boolean isRecord=true;
    private CallAudioSocket mCallAudioSocket;
    private Button mButtonRecordStop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(permission.RECORD_AUDIO)
               == PackageManager.PERMISSION_GRANTED){
                Log.d("MainActivity", "Permission is granted");
            }else {
                Log.d("MainActivity", "Permission is revoked");
                ActivityCompat
                          .requestPermissions(this, new String[]{permission.RECORD_AUDIO}, 1);
            }
        }
    
       
        
        
        mButtonRecordStop=findViewById(R.id.btn_record_stop);
        mButtonRecordStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecord){
                    Log.d(TAG,"click record");
                    InetAddress inetAddress=null;
                    try {
                        inetAddress=InetAddress.getByName(Configs.IP_SERVER);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    mCallAudioSocket=new CallAudioSocket(inetAddress,Configs.PORT_AUDIO);
                    mCallAudioSocket.start();
                    mButtonRecordStop.setText("Stop");
                    isRecord=false;
                    
                }else {
                    Log.d(TAG,"click stop");
                    if(mCallAudioSocket!=null){
                        mCallAudioSocket.close();
                    }
                    mButtonRecordStop.setText("Record");
                    isRecord=true;
                }
            }
        });
        
        
    }
}
