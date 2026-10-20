package com.nameregister.app;
import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import com.whispercpp.java.whisper.WhisperLib;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
/** Records one utterance and transcribes on device using Whisper.cpp, never uploads audio. */
public final class WhisperOfflineEngine {
 public interface Listener { void status(String s); void result(String s); void error(String s); }
 private static final String MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin";
 private final Context context;
 private final Listener listener;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private volatile boolean cancelled;
 private volatile AudioRecord recorder;
 public WhisperOfflineEngine(Context c,Listener l){context=c.getApplicationContext();listener=l;}
 private void status(String s){main.post(()->listener.status(s));}
 private void error(String s){main.post(()->listener.error(s));}
 public boolean hasModel(){File f=modelFile();return f.exists()&&f.length()>70000000;}
 private File modelFile(){return new File(context.getFilesDir(),"ggml-tiny.bin");}
 public void downloadModel(){
  worker.execute(()->{
   if(hasModel()){status("النموذج جاهز");return;}
   File partial=new File(context.getFilesDir(),"ggml-tiny.bin.part");
   try{
    status("جار تنزيل نموذج Whisper مرة واحدة (حوالي 75 ميجابايت)");
    HttpURLConnection c=(HttpURLConnection)new URL(MODEL_URL).openConnection();
    c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(true);
    if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
    long size=c.getContentLengthLong(),received=0,last=0;
    try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(partial)){
     byte[] b=new byte[32768];int n;
     while((n=in.read(b))!=-1){out.write(b,0,n);received+=n;
      if(received-last>5000000){last=received;status("تنزيل النموذج: "+(size>0?(received*100/size)+"%":(received/1048576)+" ميجابايت"));}
     }
    }finally{c.disconnect();}
    if(partial.length()<70000000)throw new IOException("ملف النموذج غير مكتمل");
    if(!partial.renameTo(modelFile()))throw new IOException("تعذر حفظ النموذج");
    status("تم تنزيل نموذج Whisper، يمكنك الآن تسجيل الأسماء دون إنترنت");
   }catch(Exception e){partial.delete();error("تعذر تنزيل النموذج: "+e.getMessage());}
  });
 }
 public void recordAndTranscribe(){
  if(!hasModel()){error("يجب تنزيل نموذج Whisper أولاً");return;}
  cancelled=false;
  worker.execute(()->{
   long ptr=0;
   try{
    final int sampleRate=16000;
    int min=AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
    if(min<=0)throw new IOException("الميكروفون غير متاح");
    int buffer=Math.max(min,4096);
    AudioRecord ar=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer);
    recorder=ar;
    if(ar.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("تعذر تشغيل الميكروفون");
    ByteArrayOutputStream pcm=new ByteArrayOutputStream();
    ar.startRecording();status("تحدث الآن — التسجيل لمدة 8 ثوانٍ");
    byte[] bytes=new byte[buffer];long end=System.currentTimeMillis()+8000;
    while(!cancelled&&System.currentTimeMillis()<end){
     short[] samples=new short[buffer/2];int n=ar.read(samples,0,samples.length);
     if(n<0)throw new IOException("خطأ في قراءة الصوت "+n);
     for(int i=0;i<n;i++){pcm.write(samples[i]&255);pcm.write((samples[i]>>8)&255);}
    }
    ar.stop();ar.release();recorder=null;
    if(cancelled)return;
    byte[] raw=pcm.toByteArray();
    float[] audio=new float[raw.length/2];
    for(int i=0;i<audio.length;i++){int lo=raw[i*2]&255,hi=raw[i*2+1];audio[i]=(short)((hi<<8)|lo)/32768f;}
    status("Whisper يحلل الصوت محلياً...");
    ptr=WhisperLib.initContext(modelFile().getAbsolutePath());
    if(ptr==0)throw new IOException("فشل تحميل النموذج");
    WhisperLib.fullTranscribe(ptr,Math.max(2,Math.min(4,Runtime.getRuntime().availableProcessors())),audio);
    StringBuilder text=new StringBuilder();
    for(int i=0;i<WhisperLib.getTextSegmentCount(ptr);i++)text.append(WhisperLib.getTextSegment(ptr,i)).append(' ');
    String result=text.toString().trim();
    main.post(()->listener.result(result));
   }catch(Throwable e){error("Whisper: "+e.getClass().getSimpleName()+": "+e.getMessage());}
   finally{if(ptr!=0)WhisperLib.freeContext(ptr);AudioRecord r=recorder;if(r!=null){try{r.stop();}catch(Exception ignored){}r.release();recorder=null;}}
  });
 }
 public void stop(){cancelled=true;AudioRecord r=recorder;if(r!=null){try{r.stop();}catch(Exception ignored){}}}
}
