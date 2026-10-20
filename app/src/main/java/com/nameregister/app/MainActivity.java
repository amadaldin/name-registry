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
  EditText name, area, referrer, search;
  static final int VOICE = 101;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    db = new Db(this);
    root = new LinearLayout(this); root.setOrientation(1); root.setPadding(20,20,20,20);
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
    TextView title = new TextView(this); title.setText("سجل الأسماء حسب الحارات"); title.setTextSize(24); root.addView(title);
    name = field("اسم الشخص"); area = field("الحارة"); referrer = field("اسم المُحيل أو المُسجّل");
    LinearLayout buttons = new LinearLayout(this);
    Button save = button("حفظ الاسم", () -> save());
    Button voice = button("إملاء الاسم بالصوت", () -> speak());
    buttons.addView(save); buttons.addView(voice); root.addView(buttons);
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
    if(n.isEmpty()||a.isEmpty()){Toast.makeText(this,"الاسم والحارة مطلوبان",Toast.LENGTH_SHORT).show();return;}
    db.add(n,a,r);name.setText("");referrer.setText("");refresh();
  }
  void speak(){
    try { Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ar-OM");
      i.putExtra(RecognizerIntent.EXTRA_PROMPT,"قل الاسم"); startActivityForResult(i,VOICE);
    }catch(Exception e){Toast.makeText(this,"التعرف الصوتي غير متاح على هذا الهاتف",Toast.LENGTH_LONG).show();}
  }
  @Override protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data);
    if(req==VOICE&&result==RESULT_OK&&data!=null){
      ArrayList<String> a=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
      if(a!=null&&!a.isEmpty())name.setText(a.get(0));
    }
  }
  void refresh(){
    if(rows==null)return;
    rows.removeAllViews();
    Cursor c=db.list(search.getText().toString());
    try {while(c.moveToNext()){
      final long id=c.getLong(0);
      final String n=c.getString(1),a=c.getString(2),r=c.getString(3);
      LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(8,12,8,12);
      TextView label=new TextView(this);label.setText(n+" — "+a+(r.isEmpty()?"":"\nالمُحيل: "+r));label.setTextSize(18);card.addView(label);
      LinearLayout actions=new LinearLayout(this);
      actions.addView(button("تعديل / نقل",()->edit(id,n,a,r)));
      actions.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("حذف "+n+"؟").setPositiveButton("حذف",(d,w)->{db.delete(id);refresh();}).setNegativeButton("إلغاء",null).show()));
      card.addView(actions);rows.addView(card);
    }} finally{c.close();}
  }
  void edit(long id,String n,String a,String r){
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(24,8,24,8);
    EditText nn=new EditText(this);nn.setText(n);nn.setHint("الاسم");panel.addView(nn);
    EditText aa=new EditText(this);aa.setText(a);aa.setHint("الحارة");panel.addView(aa);
    EditText rr=new EditText(this);rr.setText(r);rr.setHint("المُحيل");panel.addView(rr);
    new AlertDialog.Builder(this).setTitle("تعديل السجل").setView(panel)
      .setPositiveButton("حفظ",(d,w)->{if(!nn.getText().toString().trim().isEmpty()&&!aa.getText().toString().trim().isEmpty()){db.update(id,nn.getText().toString().trim(),aa.getText().toString().trim(),rr.getText().toString().trim());refresh();}})
      .setNegativeButton("إلغاء",null).show();
  }
  void share(){
    StringBuilder sb=new StringBuilder("سجل الأسماء\n");
    Cursor c=db.list("");try{while(c.moveToNext())sb.append(c.getString(1)).append(" | ").append(c.getString(2)).append(" | ").append(c.getString(3)).append("\n");}finally{c.close();}
    Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,sb.toString());startActivity(Intent.createChooser(i,"مشاركة السجل أو طباعته"));
  }
  static class Db extends SQLiteOpenHelper {
    Db(Context c){super(c,"names.db",null,1);}
    @Override public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE people (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,area TEXT NOT NULL,referrer TEXT NOT NULL DEFAULT '')");}
    @Override public void onUpgrade(SQLiteDatabase d,int old,int next){}
    void add(String n,String a,String r){ContentValues v=values(n,a,r);getWritableDatabase().insert("people",null,v);}
    void update(long id,String n,String a,String r){getWritableDatabase().update("people",values(n,a,r),"_id=?",new String[]{""+id});}
    void delete(long id){getWritableDatabase().delete("people","_id=?",new String[]{""+id});}
    ContentValues values(String n,String a,String r){ContentValues v=new ContentValues();v.put("name",n);v.put("area",a);v.put("referrer",r);return v;}
    Cursor list(String q){String x="%"+q+"%";return getReadableDatabase().rawQuery("SELECT _id,name,area,referrer FROM people WHERE name LIKE ? OR area LIKE ? OR referrer LIKE ? ORDER BY area,name",new String[]{x,x,x});}
  }
}
