package com.nameregister.app;
import java.util.*;
/** Splits an ASR transcript into explicit name utterances, never guessing at Arabic "و". */
public final class BatchNameParser {
  private BatchNameParser(){}
  public static List<String> split(String speech) {
    if(speech==null||speech.trim().isEmpty())return Collections.emptyList();
    String s=speech.replace('\r','\n');
    s=s.replaceAll("(?U)\\s+(?:وبعده|وبعدين|ثم|الاسم\\s+التالي|الاسم\\s+اللي\\s+بعده|الاسم\\s+(?:الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر)|والاسم\\s+التالي|والاسم\\s+(?:الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر))\\s*[:：،-]?\\s+","\n");
    String[] chunks=s.split("[،؛;\\n]+");
    List<String> result=new ArrayList<>();
    for(String chunk:chunks){
      String item=chunk.trim();
      if(!item.isEmpty())result.add(item);
    }
    return result;
  }
}
