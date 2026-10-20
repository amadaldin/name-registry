package com.nameregister.app;
import android.content.*;import android.os.*;import android.speech.*;import java.util.*;
public final class HybridSpeechEngine implements RecognitionListener {
 public interface Listener {void onText(String text);void onStatus(String status);void onStopped();}
 private final Context context;private final Listener listener;private final Handler handler=new Handler(Looper.getMainLooper());
 private SpeechRecognizer recognizer;private boolean running=false,listening=false;private int failures=0;
 public HybridSpeechEngine(Context c,Listener l){context=c;listener=l;}
 public void start(){
  if(running)return;running=true;failures=0;
  try{
   if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(context)){recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context);listener.onStatus("التعرف المحلي متاح");}
   else if(SpeechRecognizer.isRecognitionAvailable(context)){recognizer=SpeechRecognizer.createSpeechRecognizer(context);listener.onStatus("استخدام خدمة التعرف في الهاتف");}
   else{running=false;listener.onStatus("لا توجد خدمة تعرف صوتي");listener.onStopped();return;}
   recognizer.setRecognitionListener(this);listen();
  }catch(Exception e){stop();listener.onStatus("تعذر تشغيل الصوت");}
 }
 private void listen(){if(!running||listening||recognizer==null)return;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ar-OM");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);try{listening=true;recognizer.startListening(i);}catch(Exception e){listening=false;retry();}}
 private void retry(){if(!running)return;if(++failures>5){stop();listener.onStatus("توقف الاستماع بعد أخطاء متكررة");return;}handler.postDelayed(this::listen,Math.min(1800,failures*350));}
 public void stop(){running=false;listening=false;handler.removeCallbacksAndMessages(null);if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}listener.onStopped();}
 public boolean isRunning(){return running;}
 public void onReadyForSpeech(Bundle b){listener.onStatus("أستمع إليك");}public void onBeginningOfSpeech(){}public void onRmsChanged(float f){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){listener.onStatus("أعالج الكلام");}
 public void onError(int error){listening=false;if(!running)return;if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){stop();listener.onStatus("إذن الميكروفون غير متاح");return;}retry();}
 public void onResults(Bundle b){listening=false;failures=0;ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())listener.onText(a.get(0));if(running)handler.postDelayed(this::listen,300);}
 public void onPartialResults(Bundle b){}public void onEvent(int type,Bundle b){}
}