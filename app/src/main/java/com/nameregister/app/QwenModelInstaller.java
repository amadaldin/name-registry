package com.nameregister.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/** Installs the official Qwen3-ASR INT8 model into private app storage. */
public final class QwenModelInstaller {
  public interface Listener { void status(String message); void complete(); void error(String message); }
  private static final String URL_MODEL =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25.tar.bz2";
  private final Context context;
  private final Handler main = new Handler(Looper.getMainLooper());
  private volatile boolean running;
  public QwenModelInstaller(Context context) { this.context=context.getApplicationContext(); }
  public boolean isRunning() { return running; }
  public void install(Listener listener) {
    if (running) return;
    running=true;
    new Thread(() -> {
      File temp=new File(context.getFilesDir(),"qwen-install-tmp");
      try {
        if (temp.exists()) deleteTree(temp);
        if (!temp.mkdirs()) throw new IOException("تعذر تجهيز مساحة التنزيل");
        post(listener,"تنزيل نموذج Qwen3-ASR (ملف كبير، استخدم Wi-Fi)...");
        HttpURLConnection conn=(HttpURLConnection)new URL(URL_MODEL).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        try {
          if (conn.getResponseCode()!=200) throw new IOException("HTTP "+conn.getResponseCode());
          try (InputStream network=new BufferedInputStream(conn.getInputStream());
               BZip2CompressorInputStream bz=new BZip2CompressorInputStream(network);
               TarArchiveInputStream tar=new TarArchiveInputStream(bz)) {
            TarArchiveEntry entry;
            long bytes=0;
            while ((entry=tar.getNextTarEntry())!=null) {
              if (!entry.isFile() && !entry.isDirectory()) continue;
              String path=entry.getName();
              File output=new File(temp,path);
              String root=temp.getCanonicalPath()+File.separator;
              if (!output.getCanonicalPath().startsWith(root)) throw new IOException("مسار غير آمن في الأرشيف");
              if (entry.isDirectory()) { if(!output.exists()&&!output.mkdirs())throw new IOException("تعذر إنشاء مجلد");continue; }
              File parent=output.getParentFile();
              if(!parent.exists()&&!parent.mkdirs())throw new IOException("تعذر إنشاء مجلد");
              try (FileOutputStream file=new FileOutputStream(output)) {
                byte[] buffer=new byte[65536];int n;
                while((n=tar.read(buffer))!=-1){file.write(buffer,0,n);bytes+=n;}
              }
              if (bytes>0) post(listener,"استخراج النموذج: "+(bytes/(1024*1024))+" ميجابايت");
            }
          }
        } finally { conn.disconnect(); }
        File extracted=new File(temp,"sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25");
        if (!extracted.isDirectory()) throw new IOException("لم يتم العثور على مجلد النموذج");
        for(String filename:new String[]{"conv_frontend.onnx","encoder.int8.onnx","decoder.int8.onnx","tokenizer"}) {
          if(!new File(extracted,filename).exists())throw new IOException("ملف ناقص: "+filename);
        }
        File destination=new File(context.getFilesDir(),extracted.getName());
        if(destination.exists())deleteTree(destination);
        if(!extracted.renameTo(destination))throw new IOException("تعذر نقل النموذج بعد التنزيل");
        deleteTree(temp);
        main.post(listener::complete);
      } catch(Exception e) {
        deleteTree(temp);
        main.post(()->listener.error("فشل تنزيل النموذج: "+e.getMessage()));
      } finally { running=false; }
    },"qwen-model-installer").start();
  }
  private void post(Listener l,String message){main.post(()->l.status(message));}
  private static void deleteTree(File file) {
    if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)deleteTree(child);}
    file.delete();
  }
}
