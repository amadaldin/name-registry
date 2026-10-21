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
  // Remove a trailing stop command before extracting the neighborhood.
  text=text.replaceFirst("[\\s،,؛.!؟]+(?:انتهى|انتهي|خلاص|وقف التسجيل|توقف)[\\s،,؛.!؟]*$", "").trim();
  String canonical=text.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي');
  if(canonical.matches("(?i)^(خلاص|وقف|توقف|انتهى|انتهي|وقف التسجيل|ايقاف التسجيل)$"))return new Result("stop","","","");
  Matcher group=Pattern.compile("^(?:افتح|ابدأ|نبدأ|الحين|الآن|الاسماء التالية|الأسماء التالية)?\\s*(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+(.+)$").matcher(text);
  if(group.matches()&&!text.contains("من طرف")){area=group.group(1).trim();return new Result("set_area","",area,referrer);}
  Matcher from=Pattern.compile("^(?:الاسماء التالية|الأسماء التالية|كلهم|هؤلاء|الحين|الآن)?\\s*(?:من طرف|عن طريق|تبع|تابع)\\s+(.+)$").matcher(text);
  if(from.matches()){referrer=from.group(1).trim();return new Result("set_referrer","",area,referrer);}
  if(canonical.matches("^(امسح|احذف|حذف|صحح|عدل|نقل|انقل)(?:\\s+.*)?$"))return new Result("needs_confirmation",text,area,referrer);
  Matcher inlineArea=Pattern.compile("(?:من\\s+)?(?:حارة|الحارة|منطقة|المنطقة)\\s+(.+)$").matcher(text);
  String localArea=area;
  if(inlineArea.find()){localArea=inlineArea.group(1).trim();text=text.substring(0,inlineArea.start()).trim();}
  Matcher inlineRef=Pattern.compile("(?:من طرف|عن طريق|تبع|تابع)\\s+(.+)$").matcher(text);
  String localRef=referrer;
  if(inlineRef.find()){localRef=inlineRef.group(1).trim();text=text.substring(0,inlineRef.start()).trim();}
  text=text.replaceFirst("^(?:سجل لي|سجل|اضف|أضف|اسم)\\s+","").trim();
  if(text.isEmpty()||text.split(" ").length<2)return new Result("needs_confirmation",text,localArea,localRef);
  return new Result("add",text,localArea,localRef);
 }
}