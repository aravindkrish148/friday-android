package com.aravindkrish.friday;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_MIC = 10;
    private static final String GEMINI_PACKAGE="com.google.android.apps.bard";
    private TextView status, wakeIndicator;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean blinkOn=true;
    private final Runnable blink=new Runnable(){
        @Override public void run(){
            if(wakeIndicator==null)return;
            wakeIndicator.setTextColor(blinkOn?Color.rgb(0,150,45):Color.rgb(0,90,25));
            wakeIndicator.setAlpha(blinkOn?1.0f:0.35f);
            blinkOn=!blinkOn;
            handler.postDelayed(this,350);
        }
    };
    private final BroadcastReceiver wakeReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            String heard=intent.getStringExtra(WakeService.EXTRA_RECOGNISED_TEXT);
            wakeIndicator.setText("✓ FRIDAY DETECTED"+(heard==null?"":" — heard: "+heard));
            wakeIndicator.setTextColor(Color.rgb(0,150,45));
            wakeIndicator.setAlpha(1.0f);
            handler.removeCallbacks(blink);
            blinkOn=true;
            handler.post(blink);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,40,32,32); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("Friday — Voice Debug"); title.setTextSize(27); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(75)));
        status = new TextView(this); status.setText("Step 1: Start listening, then say Friday."); status.setTextSize(16); status.setGravity(Gravity.CENTER);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(75)));
        wakeIndicator = new TextView(this); wakeIndicator.setText("● WAITING FOR “FRIDAY”"); wakeIndicator.setTextSize(19); wakeIndicator.setGravity(Gravity.CENTER); wakeIndicator.setTextColor(Color.DKGRAY);
        root.addView(wakeIndicator,new LinearLayout.LayoutParams(-1,dp(85)));
        Button start = new Button(this); start.setText("Start listening"); start.setOnClickListener(v -> startListening()); root.addView(start,new LinearLayout.LayoutParams(-1,dp(58)));
        Button openGemini = new Button(this); openGemini.setText("Step 2: Open Gemini (manual test)"); openGemini.setOnClickListener(v -> openGemini()); root.addView(openGemini,new LinearLayout.LayoutParams(-1,dp(62)));
        Button stop = new Button(this); stop.setText("Stop listening"); stop.setOnClickListener(v -> { stopService(new Intent(this,WakeService.class)); status.setText("Listening stopped."); wakeIndicator.setText("● LISTENING STOPPED"); handler.removeCallbacks(blink); }); root.addView(stop,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView hint=new TextView(this);hint.setText("Debug flow: green flashing text = wake word recognised. Then tap Open Gemini to test the launch separately.");hint.setTextSize(14);hint.setGravity(Gravity.CENTER);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(95)));
        setContentView(root);
    }
    @Override protected void onStart(){
        super.onStart();
        IntentFilter filter=new IntentFilter(WakeService.ACTION_WAKE_DETECTED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(wakeReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(wakeReceiver,filter);
    }
    @Override protected void onStop(){
        try{unregisterReceiver(wakeReceiver);}catch(Exception ignored){}
        handler.removeCallbacks(blink);
        super.onStop();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private void startListening(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_MIC);return;}
        Intent i=new Intent(this,WakeService.class); if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        status.setText("Listener started. Say Friday while keeping this screen open.");
        wakeIndicator.setText("● WAITING FOR “FRIDAY”");
        wakeIndicator.setTextColor(Color.DKGRAY);
    }
    private void openGemini(){
        try{
            Intent i=getPackageManager().getLaunchIntentForPackage(GEMINI_PACKAGE);
            if(i==null){status.setText("Gemini app not found. Install/update Gemini, then retry.");return;}
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            status.setText("Gemini launch requested. Check whether Gemini opens; tap its mic manually if needed.");
        }catch(Exception e){status.setText("Could not open Gemini: "+e.getMessage());}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==REQUEST_MIC && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)startListening();}
}