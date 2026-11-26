package com.aravindkrish.friday;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

public class WakeService extends Service {
    private static final String TAG="FridayWake", CHANNEL="friday_listener";
    private static final String MODEL_URL="https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    private volatile boolean running=false;
    private AudioRecord recorder;
    private Thread worker;
    @Override public void onCreate(){super.onCreate();createChannel();}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        Notification n=notification();
        if(Build.VERSION.SDK_INT>=34)startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(7,n);
        if(!running){running=true;worker=new Thread(this::listen,"FridayVosk");worker.start();}
        return START_NOT_STICKY;
    }
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Friday listening",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent assist=new Intent("android.intent.action.ASSIST");assist.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent ai=PendingIntent.getActivity(this,1,assist,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setContentTitle("Friday is listening").setContentText("Wake phrase detection runs on this device").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentIntent(pi).setOngoing(true).addAction(new Notification.Action.Builder(null,"Open assistant",ai).build());return b.build();
    }
    private void listen(){Model model=null;Recognizer recognizer=null;try{
        File dir=new File(getFilesDir(),"vosk-model-small-en-us-0.15");if(!new File(dir,"am").exists())downloadModel(dir);
        model=new Model(dir.getAbsolutePath());recognizer=new Recognizer(model,16000.0f,"[\"friday\"]");
        int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);int size=Math.max(min,4096);
        recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Microphone could not be initialized");
        recorder.startRecording();byte[] data=new byte[size];while(running){int count=recorder.read(data,0,data.length);if(count>0&&recognizer.acceptWaveForm(data,count)){String phrase=new JSONObject(recognizer.getResult()).optString("text","");if(phrase.toLowerCase().contains("friday"))launchAssistant();}}
    }catch(Exception e){Log.e(TAG,"Wake listener stopped",e);stopSelf();}finally{running=false;try{if(recorder!=null){recorder.stop();recorder.release();}}catch(Exception ignored){}try{if(recognizer!=null)recognizer.close();}catch(Exception ignored){}try{if(model!=null)model.close();}catch(Exception ignored){}}}
    private void downloadModel(File destination)throws Exception{
        File zip=new File(getCacheDir(),"vosk-model.zip");HttpURLConnection c=(HttpURLConnection)new URL(MODEL_URL).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(60000);c.connect();try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(zip)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{c.disconnect();}
        File temp=new File(getFilesDir(),"model-unpack");temp.mkdirs();try(ZipInputStream zis=new ZipInputStream(new FileInputStream(zip))){ZipEntry e;byte[] b=new byte[8192];while((e=zis.getNextEntry())!=null){File f=new File(temp,e.getName());if(!f.getCanonicalPath().startsWith(temp.getCanonicalPath()+File.separator))throw new IOException("Invalid model archive");if(e.isDirectory())f.mkdirs();else{File p=f.getParentFile();if(p!=null)p.mkdirs();try(FileOutputStream out=new FileOutputStream(f)){int n;while((n=zis.read(b))!=-1)out.write(b,0,n);}}zis.closeEntry();}}
        File unpacked=new File(temp,"vosk-model-small-en-us-0.15");if(!unpacked.renameTo(destination))throw new IOException("Could not install speech model");zip.delete();
    }
    private void launchAssistant(){try{Intent i=new Intent("android.intent.action.ASSIST");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){Log.w(TAG,"Assistant launch blocked",e);}}
    @Override public void onDestroy(){running=false;if(recorder!=null)try{recorder.stop();}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
