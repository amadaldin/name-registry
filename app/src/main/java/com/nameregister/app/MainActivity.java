package com.nameregister.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.*;

public class MainActivity extends Activity {
  Db db;
  LinearLayout root, rows;
  EditText name, area, referrer, notes, search;
  static final int VOICE = 101;
  boolean voiceSession = false;
  Button voiceButton;
  final Handler voiceHandler = new Handler(Looper.getMainLooper());
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    db = new Db(this);
    root = new LinearLayout(this); root.setOrientation(1); root.setPadding(20,20,20,20);
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
    TextView title = new TextView(this); title.setText("سجل الأسماء حسب الحارات"); title.setTextSize(24); root.addView(title);
    name = field("اسم الشخص"); area = field("الحارة"); referrer = field("من طرف (اختياري)"); notes = field("ملاحظات (اختياري)");
    LinearLayout buttons = new LinearLayout(this);
    Button save = button("حفظ الاسم", () -> save());
    voiceButton = button("ابدأ الإملاء المتتابع", () -> toggleVoice());
    buttons.addView(save); buttons.addView(voiceButton); root.addView(buttons);
    search = field("بحث بالاسم أو الحارة أو المُحيل");
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int c,int a){}
      public void onTextChanged(CharSequence s,int st,int before,int count){ refresh(); }
      public void afterTextChanged(Editable e){}
    });
    Button print = button("مشاركة / طباعة السجل", () -> share()); root.addView(print);
    rows = new LinearLayout(this); rows.setOrientation(1); root.addView(rows);
    refresh();
  }
  EditText field(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); root.addView(e); return e; }
  Button button(String text,Runnable action) { Button b=new Button(this); b.setText(text); b.setOnClickListener(v->action.run()); return b; }
  void save(){
    String n=name.getText().toString().trim(), a=area.getText().toString().trim(), r=referrer.getText().toString().trim();
    if(n.isEmpty()){Toast.makeText(this,"الاسم مطلوب",Toast.LENGTH_SHORT).show();return;}
    db.add(n,a,r,notes.getText().toString().trim());name.setText("");notes.setText("");refresh();
  }
  void toggleVoice(){
    if(voiceSession){voiceSession=false;voiceHandler.removeCallbacksAndMessages(null);voiceButton.setText("ابدأ الإملاء المتتابع");return;}
    voiceSession=true;voiceButton.setText("إيقاف الإملاء");speak();
  }
  void speak(){
    if(!voiceSession)return;
    try { Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ar-OM");
      i.putExtra(RecognizerIntent.EXTRA_PROMPT,"قل اسماً أو أكثر، ثم انتظر الحفظ");
      startActivityForResult(i,VOICE);
    }catch(Exception e){voiceSession=false;voiceButton.setText("ابدأ الإملاء المتتابع");Toast.makeText(this,"التعرف الصوتي غير متاح",Toast.LENGTH_LONG).show();}
  }
  @Override protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data);
    if(req!=VOICE)return;
    if(result!=RESULT_OK||data==null){voiceSession=false;voiceButton.setText("ابدأ الإملاء المتتابع");return;}
    ArrayList<String> a=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    if(a!=null&&!a.isEmpty()){
      int count=saveSpeech(a.get(0));
      Toast.makeText(this,"تم حفظ "+count+" اسم",Toast.LENGTH_SHORT).show();
      refresh();
    }
    if(voiceSession)voiceHandler.postDelayed(()->speak(),550);
  }
  int saveSpeech(String speech){
    int count=0;
    String[] parts=speech.split("\\s*(?:[،؛;\\n]+|\\bثم\\b|\\bالاسم التالي\\b|\\bوبعده\\b)\\s*");
    SQLiteDatabase database=db.getWritableDatabase();
    database.beginTransaction();
    try {
      for(String raw:parts){
        String item=raw.trim();
        if(item.matches("^(انتهى|انتهي|وقف|وقف التسجيل|خلاص|تم|توقف)$")){voiceSession=false;voiceButton.setText("ابدأ الإملاء المتتابع");continue;}
        if(item.isEmpty())continue;
        String a="",r="",note="";
        java.util.regex.Matcher nm=java.util.regex.Pattern.compile("(?:ملاحظة|ملاحظات|تنبيه)\\s*[:：]?\\s*(.+)$").matcher(item);
        if(nm.find()){note=nm.group(1).trim();item=item.substring(0,nm.start()).trim();}
        java.util.regex.Matcher areaMatch=java.util.regex.Pattern.compile("(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+([^،؛]+?)(?=\\s+(?:من طرف من|من طرف|تبع|تابع|عن طريق|بواسطة)\\s+|$)").matcher(item);
        if(areaMatch.find()){a=areaMatch.group(1).trim();item=(item.substring(0,areaMatch.start())+" "+item.substring(areaMatch.end())).trim();}
        java.util.regex.Matcher refMatch=java.util.regex.Pattern.compile("(?:من طرف من|من طرف|تبع|تابع|عن طريق|بواسطة)\\s+(.+)$").matcher(item);
        if(refMatch.find()){r=refMatch.group(1).trim();item=item.substring(0,refMatch.start()).trim();}
        item=item.replaceFirst("^(?:سجل|سجل لي|أضف|اضف|اسم)\\s+","").trim();
        if(item.isEmpty())continue;
        if(a.isEmpty())a=area.getText().toString().trim();
        if(r.isEmpty())r=referrer.getText().toString().trim();
        if(!db.exists(database,item,a,r)){database.insert("people",null,db.values(item,a,r,note));count++;}
      }
      database.setTransactionSuccessful();
    }finally{database.endTransaction();}
    return count;
  }
  @Override protected void onDestroy(){voiceSession=false;voiceHandler.removeCallbacksAndMessages(null);db.close();super.onDestroy();}
  void refresh(){
    if(rows==null)return;
    rows.removeAllViews();
    Cursor c=db.list(search.getText().toString());
    try {while(c.moveToNext()){
      final long id=c.getLong(0);
      final String n=c.getString(1),a=c.getString(2),r=c.getString(3),note=c.getString(4);
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(8,12,8,12);
      TextView label=new TextView(this);label.setText(n+(a.isEmpty()?"":" — "+a)+(r.isEmpty()?"":"\nمن طرف: "+r)+(note.isEmpty()?"":"\nملاحظات: "+note));label.setTextSize(18);card.addView(label);
      LinearLayout actions=new LinearLayout(this);
      actions.addView(button("تعديل / نقل",()->edit(id,n,a,r,note)));
      actions.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("حذف "+n+"؟").setPositiveButton("حذف",(d,w)->{db.delete(id);refresh();}).setNegativeButton("إلغاء",null).show()));
      card.addView(actions);rows.addView(card);
    }} finally{c.close();}
  }
  void edit(long id,String n,String a,String r,String note){
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(24,8,24,8);
    EditText nn=new EditText(this);nn.setText(n);nn.setHint("الاسم");panel.addView(nn);
    EditText aa=new EditText(this);aa.setText(a);aa.setHint("الحارة");panel.addView(aa);
    EditText rr=new EditText(this);rr.setText(r);rr.setHint("من طرف");panel.addView(rr);
    EditText nt=new EditText(this);nt.setText(note);nt.setHint("ملاحظات");panel.addView(nt);
    new AlertDialog.Builder(this).setTitle("تعديل السجل").setView(panel)
      .setPositiveButton("حفظ",(d,w)->{if(!nn.getText().toString().trim().isEmpty()){db.update(id,nn.getText().toString().trim(),aa.getText().toString().trim(),rr.getText().toString().trim(),nt.getText().toString().trim());refresh();}})
      .setNegativeButton("إلغاء",null).show();
  }
  void share(){
    StringBuilder sb=new StringBuilder("سجل الأسماء\n");
    Cursor c=db.list("");try{while(c.moveToNext())sb.append(c.getString(1)).append(" | ").append(c.getString(2)).append(" | ").append(c.getString(3)).append("\n");}finally{c.close();}
    Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,sb.toString());startActivity(Intent.createChooser(i,"مشاركة السجل أو طباعته"));
  }
  static class Db extends SQLiteOpenHelper {
    Db(Context c){super(c,"names.db",null,2);}
    @Override public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE people (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,area TEXT NOT NULL,referrer TEXT NOT NULL DEFAULT '',notes TEXT NOT NULL DEFAULT '')");}
    @Override public void onUpgrade(SQLiteDatabase d,int old,int next){if(old<2)d.execSQL("ALTER TABLE people ADD COLUMN notes TEXT NOT NULL DEFAULT ''");}
    boolean exists(SQLiteDatabase d,String n,String a,String r){Cursor c=d.rawQuery("SELECT 1 FROM people WHERE name=? AND area=? AND referrer=? LIMIT 1",new String[]{n,a,r});try{return c.moveToFirst();}finally{c.close();}}
    void add(String n,String a,String r,String note){ContentValues v=values(n,a,r,note);getWritableDatabase().insert("people",null,v);}
    void update(long id,String n,String a,String r,String note){getWritableDatabase().update("people",values(n,a,r,note),"_id=?",new String[]{""+id});}
    void delete(long id){getWritableDatabase().delete("people","_id=?",new String[]{""+id});}
    ContentValues values(String n,String a,String r,String note){ContentValues v=new ContentValues();v.put("name",n);v.put("area",a);v.put("referrer",r);v.put("notes",note);return v;}
    Cursor list(String q){String x="%"+q+"%";return getReadableDatabase().rawQuery("SELECT _id,name,area,referrer,notes FROM people WHERE name LIKE ? OR area LIKE ? OR referrer LIKE ? OR notes LIKE ? ORDER BY area,name",new String[]{x,x,x,x});}
  }
}
