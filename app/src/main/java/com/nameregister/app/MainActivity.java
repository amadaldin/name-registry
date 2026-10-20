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
import java.io.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import androidx.core.content.FileProvider;

public class MainActivity extends Activity {
  Db db;
  LinearLayout root, rows;
  EditText name, area, referrer, notes, search;
  static final int VOICE = 101;
  boolean voiceSession = false;
  String activeArea="",activeReferrer="";
  Button voiceButton;
  TextView dashboard;
  Button manualSave;
  String reportMode="all",reportFilter="";
  final Handler voiceHandler = new Handler(Looper.getMainLooper());
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    db = new Db(this);
    root = new LinearLayout(this); root.setOrientation(1); root.setPadding(20,20,20,20);
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
    root.setBackgroundColor(Color.rgb(246,249,248));
    TextView title=new TextView(this);title.setText("سجل الأسماء");title.setTextSize(28);title.setTextColor(Color.rgb(19,83,73));title.setTypeface(Typeface.DEFAULT_BOLD);root.addView(title);
    TextView subtitle=new TextView(this);subtitle.setText("إدارة الأسماء والحارات والمُحيلين");subtitle.setTextSize(14);subtitle.setPadding(0,0,0,20);root.addView(subtitle);
    dashboard=new TextView(this);dashboard.setTextSize(17);dashboard.setTextColor(Color.rgb(20,91,76));dashboard.setPadding(18,20,18,20);
    dashboard.setBackgroundColor(Color.rgb(225,241,235));root.addView(dashboard);
    voiceButton=button("🎙 ابدأ الإملاء الصوتي",()->toggleVoice());voiceButton.setTextSize(19);root.addView(voiceButton);
    TextView tip=new TextView(this);tip.setText("مثال: حارة خطو، من طرف وليد، ثم اذكر الأسماء");tip.setPadding(5,8,5,22);root.addView(tip);
    Button addToggle=button("＋ إضافة اسم يدوياً",()->{int visibility=name.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE;name.setVisibility(visibility);area.setVisibility(visibility);referrer.setVisibility(visibility);notes.setVisibility(visibility);manualSave.setVisibility(visibility);});
    root.addView(addToggle);
    name=field("اسم الشخص");area=field("الحارة (اختياري)");referrer=field("من طرف (اختياري)");notes=field("ملاحظات (اختياري)");
    manualSave=button("حفظ الاسم",()->save());root.addView(manualSave);
    name.setVisibility(View.GONE);area.setVisibility(View.GONE);referrer.setVisibility(View.GONE);notes.setVisibility(View.GONE);manualSave.setVisibility(View.GONE);
    search=field("بحث بالاسم أو الحارة أو من طرف");
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence t,int start,int count,int after){}
      public void onTextChanged(CharSequence t,int start,int before,int count){refresh();}
      public void afterTextChanged(Editable e){}
    });
    Button print=button("تصدير PDF حسب التصنيف",()->chooseExport());root.addView(print);
    TextView heading=new TextView(this);heading.setText("الأسماء المسجلة");heading.setTextSize(21);heading.setTypeface(Typeface.DEFAULT_BOLD);heading.setPadding(0,22,0,10);root.addView(heading);
    rows=new LinearLayout(this);rows.setOrientation(1);root.addView(rows);
    refresh();
  }
  EditText field(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); root.addView(e); return e; }
  Button button(String text,Runnable action) { Button b=new Button(this); b.setText(text); b.setOnClickListener(v->action.run()); return b; }
  void save(){
    String n=name.getText().toString().trim(), a=area.getText().toString().trim(), r=referrer.getText().toString().trim();
    if(n.isEmpty()){Toast.makeText(this,"الاسم مطلوب",Toast.LENGTH_SHORT).show();return;}
    if(db.exists(db.getReadableDatabase(),n,a,r)){Toast.makeText(this,"الاسم مسجل مسبقاً",Toast.LENGTH_LONG).show();return;}
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
    String[] parts=speech.split("\\s*(?:[،؛;\n]+|\\bثم\\b|\\bالاسم التالي\\b|\\bوبعده\\b)\\s*");
    SQLiteDatabase database=db.getWritableDatabase();
    database.beginTransaction();
    try {
      for(String raw:parts){
        String item=raw.trim();
        if(item.isEmpty())continue;
        if(item.matches("^(انتهى|انتهي|وقف|وقف التسجيل|إيقاف التسجيل|خلاص|تم|توقف)$")){
          voiceSession=false;voiceButton.setText("ابدأ الإملاء المتتابع");continue;
        }
        java.util.regex.Matcher group=java.util.regex.Pattern.compile("^(?:افتح|نبدأ|ابدأ|الحين|الآن|الاسماء التالية|الأسماء التالية|كل الأسماء التالية)?\\s*(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+(.+)$").matcher(item);
        if(group.matches()&&!item.matches(".*(?:من طرف|تبع|تابع|ملاحظة).*")){
          activeArea=group.group(1).trim();area.setText(activeArea);continue;
        }
        java.util.regex.Matcher referral=java.util.regex.Pattern.compile("^(?:الأسماء التالية|الاسماء التالية|كل الأسماء التالية|كلهم|هؤلاء|الحين|الآن)?\\s*(?:من طرف من|من طرف|تبع|تابع|عن طريق)\\s+(.+)$").matcher(item);
        if(referral.matches()){
          activeReferrer=referral.group(1).trim();referrer.setText(activeReferrer);continue;
        }
        String a="",r="",note="";
        java.util.regex.Matcher nm=java.util.regex.Pattern.compile("(?:ملاحظة|ملاحظات|تنبيه)\\s*[:：]?\\s*(.+)$").matcher(item);
        if(nm.find()){note=nm.group(1).trim();item=item.substring(0,nm.start()).trim();}
        java.util.regex.Matcher am=java.util.regex.Pattern.compile("(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+([^،؛]+?)(?=\\s+(?:من طرف من|من طرف|تبع|تابع|عن طريق|بواسطة)\\s+|$)").matcher(item);
        if(am.find()){a=am.group(1).trim();item=(item.substring(0,am.start())+" "+item.substring(am.end())).trim();}
        java.util.regex.Matcher rm=java.util.regex.Pattern.compile("(?:من طرف من|من طرف|تبع|تابع|عن طريق|بواسطة)\\s+(.+)$").matcher(item);
        if(rm.find()){r=rm.group(1).trim();item=item.substring(0,rm.start()).trim();}
        item=item.replaceFirst("^(?:سجل لي|سجل|أضف|اضف|اسم)\\s+","").trim();
        if(item.isEmpty())continue;
        if(a.isEmpty())a=activeArea.isEmpty()?area.getText().toString().trim():activeArea;
        if(r.isEmpty())r=activeReferrer.isEmpty()?referrer.getText().toString().trim():activeReferrer;
        if(!db.exists(database,item,a,r)){
          database.insert("people",null,db.values(item,a,r,note));count++;
        }
      }
      database.setTransactionSuccessful();
    }finally{database.endTransaction();}
    return count;
  }
  @Override protected void onDestroy(){voiceSession=false;voiceHandler.removeCallbacksAndMessages(null);db.close();super.onDestroy();}
  void refresh(){
    if(rows==null)return;
    if(dashboard!=null)dashboard.setText("الأسماء: "+db.count()+"   |   الحارات: "+db.distinct("area").length+"   |   المُحيلون: "+db.distinct("referrer").length);
    rows.removeAllViews();
    Cursor c=db.list(search.getText().toString());
    try {while(c.moveToNext()){
      final long id=c.getLong(0);
      final String n=c.getString(1),a=c.getString(2),r=c.getString(3),note=c.getString(4);
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(18,18,18,18);card.setBackgroundColor(Color.WHITE);
      TextView label=new TextView(this);label.setText(n+(a.isEmpty()?"":" — "+a)+(r.isEmpty()?"":"\nمن طرف: "+r)+(note.isEmpty()?"":"\nملاحظات: "+note));label.setTextSize(18);label.setTextColor(Color.rgb(28,57,52));card.addView(label);
      LinearLayout actions=new LinearLayout(this);
      actions.addView(button("تعديل / نقل",()->edit(id,n,a,r,note)));
      actions.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("حذف "+n+"؟").setPositiveButton("حذف",(d,w)->{db.delete(id);refresh();}).setNegativeButton("إلغاء",null).show()));
      card.addView(actions);rows.addView(card);TextView gap=new TextView(this);gap.setHeight(12);rows.addView(gap);
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
  void chooseExport(){
    String[] options={"السجل الكامل","حسب الحارة","حسب من طرف"};
    new AlertDialog.Builder(this).setTitle("نوع التقرير").setItems(options,(dialog,which)->{
      reportMode=which==1?"area":which==2?"referrer":"all";
      if(which==0){reportFilter="";share();return;}
      String[] values=db.distinct(which==1?"area":"referrer");
      if(values.length==0){Toast.makeText(this,"لا توجد بيانات لهذا التصنيف",Toast.LENGTH_SHORT).show();return;}
      String[] choices=new String[values.length+1];choices[0]="جميع "+(which==1?"الحارات":"المُحيلين");System.arraycopy(values,0,choices,1,values.length);
      new AlertDialog.Builder(this).setTitle("اختر التصنيف").setItems(choices,(d,index)->{reportFilter=index==0?"":values[index-1];share();}).show();
    }).show();
  }
  void share(){
    PdfDocument pdf=new PdfDocument();
    try{
      File folder=new File(getCacheDir(),"reports");if(!folder.exists())folder.mkdirs();
      File output=new File(folder,"name-register.pdf");
      TextPaint paint=new TextPaint(3);paint.setColor(Color.rgb(35,49,54));paint.setTextSize(13);
      java.util.TreeMap<String,java.util.TreeMap<String,java.util.ArrayList<String>>> groups=new java.util.TreeMap<>();
      Cursor c=db.list("");
      try{
        while(c.moveToNext()){
          String n=c.getString(1),a=c.getString(2),r=c.getString(3),note=c.getString(4);
          if(!reportFilter.isEmpty()&&!(reportMode.equals("area")?a:r).equals(reportFilter))continue;
          if(reportMode.equals("referrer")){String temp=a;a=r;r=temp;}
          if(reportMode.equals("all"))r="السجل الكامل";
          if(a.isEmpty())a="غير محددة";
          if(r.isEmpty())r="بدون مُحيل";
          groups.computeIfAbsent(a,k->new java.util.TreeMap<>())
            .computeIfAbsent(r,k->new java.util.ArrayList<>())
            .add(n+(note.isEmpty()?"":" — ملاحظات: "+note));
        }
      }finally{c.close();}
      PdfDocument.Page page=null;Canvas canvas=null;int y=80,pageNum=0,total=0;
      for(java.util.TreeMap<String,java.util.ArrayList<String>> refs:groups.values())
        for(java.util.ArrayList<String> names:refs.values())total+=names.size();
      for(java.util.Map.Entry<String,java.util.TreeMap<String,java.util.ArrayList<String>>> areaEntry:groups.entrySet()){
        int areaCount=0;
        for(java.util.ArrayList<String> names:areaEntry.getValue().values())areaCount+=names.size();
        if(page==null||y>685){
          if(page!=null)pdf.finishPage(page);
          page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNum).create());
          canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
          paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(21);
          drawRtl(canvas,reportMode.equals("referrer")?"سجل الأسماء حسب المُحيل":reportMode.equals("area")?"سجل الأسماء حسب الحارات":"السجل الكامل للأسماء",paint,40);
          paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12);
          drawRtl(canvas,"إجمالي الأسماء: "+total+"   |   الصفحة: "+pageNum,paint,78);
          y=130;
        }
        paint.setColor(Color.rgb(18,102,83));paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(17);
        y+=drawRtl(canvas,(reportMode.equals("referrer")?"من طرف: ":"الحارة: ")+areaEntry.getKey()+" — "+areaCount+" أسماء",paint,y)+18;
        paint.setColor(Color.rgb(35,49,54));paint.setTextSize(13);
        for(java.util.Map.Entry<String,java.util.ArrayList<String>> refEntry:areaEntry.getValue().entrySet()){
          if(y>690){
            pdf.finishPage(page);
            page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNum).create());
            canvas=page.getCanvas();canvas.drawColor(Color.WHITE);y=60;
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            y+=drawRtl(canvas,"الحارة: "+areaEntry.getKey()+" (تابع)",paint,y)+18;
          }
          paint.setTypeface(Typeface.DEFAULT_BOLD);
          y+=drawRtl(canvas,(reportMode.equals("referrer")?"الحارة: ":reportMode.equals("all")?"المجموعة: ":"من طرف: ")+refEntry.getKey()+" — "+refEntry.getValue().size()+" أسماء",paint,y)+12;
          paint.setTypeface(Typeface.DEFAULT);
          int index=0;
          for(String entry:refEntry.getValue()){
            if(y>755){
              pdf.finishPage(page);
              page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNum).create());
              canvas=page.getCanvas();canvas.drawColor(Color.WHITE);y=60;
              paint.setTypeface(Typeface.DEFAULT_BOLD);
              y+=drawRtl(canvas,"الحارة: "+areaEntry.getKey()+" | من طرف: "+refEntry.getKey()+" (تابع)",paint,y)+18;
              paint.setTypeface(Typeface.DEFAULT);
            }
            y+=drawRtl(canvas,(++index)+". "+entry,paint,y)+10;
          }
          y+=16;
        }
        y+=16;
      }
      if(page==null){
        page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
        canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
        drawRtl(canvas,"سجل الأسماء فارغ",paint,70);
      }
      pdf.finishPage(page);
      try(FileOutputStream stream=new FileOutputStream(output)){pdf.writeTo(stream);}
      android.net.Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",output);
      Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(send,"مشاركة تقرير PDF"));
    }catch(Exception e){Toast.makeText(this,"تعذر إنشاء PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    finally{pdf.close();}
  }
  int drawRtl(Canvas canvas,String value,TextPaint paint,int y){
    StaticLayout layout=StaticLayout.Builder.obtain(value,0,value.length(),paint,495)
      .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
      .setTextDirection(android.text.TextDirectionHeuristics.RTL).setIncludePad(false).build();
    canvas.save();canvas.translate(50,y);layout.draw(canvas);canvas.restore();
    return layout.getHeight();
  }
  static class Db extends SQLiteOpenHelper {
    int count(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM people",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    String[] distinct(String column){if(!column.equals("area")&&!column.equals("referrer"))return new String[0];ArrayList<String> a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT "+column+" FROM people WHERE TRIM("+column+") != '' ORDER BY "+column,null);try{while(c.moveToNext())a.add(c.getString(0));}finally{c.close();}return a.toArray(new String[0]);}
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
