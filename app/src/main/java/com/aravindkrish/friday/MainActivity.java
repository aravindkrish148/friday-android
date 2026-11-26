package com.aravindkrish.friday;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private static final int REQUEST_MIC = 10;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,56,32,32); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("Friday"); title.setTextSize(34); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(80)));
        status = new TextView(this); status.setText("Local wake-word detection\nSay ‘Friday’ to open your assistant."); status.setTextSize(16); status.setGravity(Gravity.CENTER);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(110)));
        Button start = new Button(this); start.setText("Start listening"); start.setOnClickListener(v -> startListening()); root.addView(start,new LinearLayout.LayoutParams(-1,dp(60)));
        Button stop = new Button(this); stop.setText("Stop listening"); stop.setOnClickListener(v -> { stopService(new Intent(this,WakeService.class)); status.setText("Listening stopped."); }); root.addView(stop,new LinearLayout.LayoutParams(-1,dp(60)));
        setContentView(root);
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private void startListening(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_MIC);return;}
        Intent i=new Intent(this,WakeService.class); if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        status.setText("Friday is listening locally. Keep the notification visible.");
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==REQUEST_MIC && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)startListening();}
}
