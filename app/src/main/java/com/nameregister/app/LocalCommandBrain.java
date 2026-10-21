package com.nameregister.app;
import java.util.*;
import java.util.regex.*;
/** Offline Arabic command interpretation. Never executes or deletes data itself. */
public final class LocalCommandBrain {
 public static final class Result {
  public final String action, name, area, referrer;
  Result(String action,String name,String area,String referrer){this.action=action;this.name=name;this.area=area;this.referrer=referrer;}
 }
 private String area="",referrer="";
 public String getArea(){return area;} public String getReferrer(){return referrer;}
 public Result interpret(String spoken){
  String text=spoken==null?"":spoken.trim().replaceAll("\\s+"," ");
  // Speech recognizers sometimes append sentence punctuation to commands.
  text=text.replaceAll("^[،,؛.!؟]+|[،,؛.!؟]+$", "").trim();
  // A standalone stop command must remain actionable.
  String originalCanonical=text.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي');
  if(originalCanonical.matches("^(خلاص|وقف|توقف|انتهي|وقف التسجيل|ايقاف التسجيل)$"))return new Result("stop","","","");
  // Remove a trailing stop command before extracting the neighborhood.
  text=text.replaceFirst("[\\s،,؛.!؟]+(?:انتهى|انتهي|خلاص|وقف التسجيل|توقف)[\\s،,؛.!؟]*$", "").trim();
  String canonical=text.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي');
  if(canonical.matches("(?i)^(خلاص|وقف|توقف|انتهى|انتهي|وقف التسجيل|ايقاف التسجيل)$"))return new Result("stop","","","");
  Matcher group=Pattern.compile("^(?:افتح|ابدأ|نبدأ|الحين|الآن|الاسماء التالية|الأسماء التالية)?\\s*(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+(.+)$").matcher(text);
  if(group.matches()&&!text.contains("من طرف")){area=group.group(1).trim();return new Result("set_area","",area,referrer);}
  Matcher from=Pattern.compile("^(?:الاسماء التالية|الأسماء التالية|كلهم|هؤلاء|الحين|الآن)?\\s*(?:من طرف|عن طريق|تبع|تابع)\\s+(.+)$").matcher(text);
  if(from.matches()){referrer=from.group(1).trim();return new Result("set_referrer","",area,referrer);}
  if(canonical.matches("^(امسح|احذف|حذف|صحح|عدل|نقل|انقل)(?:\\s+.*)?$"))return new Result("needs_confirmation",text,area,referrer);
  // Extract area and referrer in either spoken order without inventing names.
  String localArea=area,localRef=referrer;
  Pattern qualifier=Pattern.compile("(?:من\\s+طرف|عن\\s+طريق|تبع|تابع|(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة))\\s+");
  Matcher q=qualifier.matcher(text);
  java.util.ArrayList<Integer> begins=new java.util.ArrayList<>();
  java.util.ArrayList<Integer> ends=new java.util.ArrayList<>();
  java.util.ArrayList<String> labels=new java.util.ArrayList<>();
  while(q.find()){begins.add(q.start());ends.add(q.end());labels.add(q.group());}
  if(!begins.isEmpty()){
    String rawName=text.substring(0,begins.get(0)).trim();
    for(int i=0;i<begins.size();i++){
      String value=text.substring(ends.get(i),i+1<begins.size()?begins.get(i+1):text.length()).trim();
      value=value.replaceAll("^[،,؛:]+|[،,؛:]+$","").trim();
      if(value.isEmpty())continue;
      if(labels.get(i).matches("^(?:من\\s+طرف|عن\\s+طريق|تبع|تابع)\\s+$"))localRef=value;
      else localArea=value;
    }
    text=rawName;
  }
  // Strip conversational filler only at the beginning; preserve the actual name.
  text=text.replaceFirst("^(?:(?:لو سمحت|الله يخليك|يا اخي|يا خوي|يا حبيبي|ممكن|ابغاك|اريدك|بغيتك|عندي|اسمع|شوف|طيب|زين|الحين|الان|اليوم|هذا|هذي|الشخص|واحد)\\s+)+","").trim();
  text=text.replaceFirst("^(?:تكتب لي|تسجل لي|تضيف لي|تكتب|تسجل|تضيف|اكتب عندك|حط عندك|حط لي|دون عندك|قيد عندك|سجل عندك)\\s+","").trim();
  text=text.replaceFirst("^(?:اسم(?:ه|ها| الشخص)?|الاسم(?: هو| الكامل)?|يدعى|يسمى|اسمه|هذا اسمه)\\s*[:：-]?\\s+","").trim();
  text=text.replaceFirst("^(?:اكتب لي|اكتب|سجل لي|سجل|اضف|أضف|اسم)\\s+","").trim();
  text=text.replaceFirst("^(?:اسم(?:ه|ها)?|الاسم|هو)\\s*[:：-]?\\s+","").trim();
  text=text.replaceFirst("[\\s،,؛.!؟]+(?:تمام|شكرا|وشكرا|الله يعطيك العافية|بس|هذا هو|اوكي)[\\s،,؛.!؟]*$","").trim();
  text=text.replaceAll("^[،,؛:.-]+|[،,؛:.-]+$","").trim();
  if(text.isEmpty()||text.split(" ").length<2)return new Result("needs_confirmation",text,localArea,localRef);
  return new Result("add",text,localArea,localRef);
 }
}