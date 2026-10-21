package com.nameregister.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import com.k2fsa.sherpa.onnx.*;
import java.io.File;
import java.util.Arrays;

/** Local Qwen3-ASR microphone adapter. Model assets are installed separately. */
public final class QwenOfflineEngine implements AsrEngine {
  private static final int SAMPLE_RATE = 16000;
  private static final int MAX_SECONDS = 120;
  private static final String MODEL = "sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25";
  private final Context context;
  private final Listener listener;
  private final Handler main = new Handler(Looper.getMainLooper());
  private volatile boolean closed = false;
  private volatile boolean busy = false;
  private volatile boolean recordingRequested = false;
  public void stopRecording() { recordingRequested = false; }
  public boolean isBusy() { return busy; }

  public QwenOfflineEngine(Context context, Listener listener) {
    this.context = context.getApplicationContext();
    this.listener = listener;
  }
  public String id() { return "qwen3-asr-0.6b-int8"; }
  public File modelDirectory() { return new File(context.getFilesDir(), MODEL); }
  public boolean isReady() {
    File dir = modelDirectory();
    return new File(dir,"conv_frontend.onnx").isFile()
      && new File(dir,"encoder.int8.onnx").isFile()
      && new File(dir,"decoder.int8.onnx").isFile()
      && new File(dir,"tokenizer").exists();
  }
  private void status(String s) { main.post(() -> { if (!closed) listener.status(s); }); }
  private void error(String s) { main.post(() -> { if (!closed) listener.error(s); }); }
  public void recordAndTranscribe() {
    if (busy) { status("التسجيل السابق ما زال يعمل"); return; }
    if (!isReady()) { error("نموذج Qwen3-ASR غير مثبت بعد؛ لا يمكن تشغيله قبل تنزيل ملفاته"); return; }
    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      error("يلزم السماح بالميكروفون"); return;
    }
    busy = true;
    recordingRequested = true;
    new Thread(() -> {
      AudioRecord recorder = null;
      OfflineRecognizer recognizer = null;
      OfflineStream stream = null;
      try {
        int minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) throw new IllegalStateException("تعذر تهيئة الميكروفون");
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            Math.max(minimum, SAMPLE_RATE * 2));
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("الميكروفون غير متاح");
        float[] samples = new float[SAMPLE_RATE * MAX_SECONDS];
        short[] chunk = new short[2048];
        int offset = 0;
        status("جارٍ التسجيل... ارفع إصبعك للإيقاف");
        recorder.startRecording();
        while (offset < samples.length && !closed && recordingRequested) {
          int n = recorder.read(chunk, 0, Math.min(chunk.length, samples.length - offset));
          if (n < 0) throw new IllegalStateException("فشل قراءة الصوت: " + n);
          if (n == 0) continue;
          for (int i = 0; i < n; i++) samples[offset++] = chunk[i] / 32768f;
        }
        recorder.stop();
        if (closed || offset < SAMPLE_RATE / 3) { if (!closed) error("التسجيل قصير جداً؛ حاول مجدداً"); return; }
        if (offset != samples.length) samples = Arrays.copyOf(samples, offset);
        status("جاري تحويل الصوت إلى نص محلياً...");
        File dir = modelDirectory();
        OfflineQwen3AsrModelConfig qwen = OfflineQwen3AsrModelConfig.builder()
          .setConvFrontend(new File(dir,"conv_frontend.onnx").getAbsolutePath())
          .setEncoder(new File(dir,"encoder.int8.onnx").getAbsolutePath())
          .setDecoder(new File(dir,"decoder.int8.onnx").getAbsolutePath())
          .setTokenizer(new File(dir,"tokenizer").getAbsolutePath())
          .build();
        OfflineModelConfig model = OfflineModelConfig.builder()
          .setQwen3Asr(qwen).setNumThreads(4).setProvider("cpu").setDebug(false).build();
        recognizer = new OfflineRecognizer(OfflineRecognizerConfig.builder()
          .setOfflineModelConfig(model).build());
        stream = recognizer.createStream();
        stream.acceptWaveform(samples, SAMPLE_RATE);
        recognizer.decode(stream);
        String transcript = recognizer.getResult(stream).getText();
        main.post(() -> { if (!closed) listener.result(transcript == null ? "" : transcript.trim()); });
      } catch (Exception | LinkageError ex) {
        error("تعذر تشغيل Qwen3-ASR: " + ex.getMessage());
      } finally {
        if (stream != null) stream.release();
        if (recognizer != null) recognizer.release();
        if (recorder != null) recorder.release();
        recordingRequested = false;
        busy = false;
      }
    }, "qwen3-asr-worker").start();
  }
  public void close() { closed = true; recordingRequested = false; }
}
