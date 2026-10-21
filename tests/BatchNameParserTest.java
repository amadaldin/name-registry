import com.nameregister.app.BatchNameParser;
import java.util.List;
public class BatchNameParserTest {
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
  public static void main(String[] args){
    List<String> names=BatchNameParser.split("سالم بن راشد الزيدي الاسم التالي خالد بن محمد الشكيلي ثم أحمد بن سيف اليحيائي");
    check(names.size()==3,"three names");
    check(names.get(0).equals("سالم بن راشد الزيدي"),"first");
    check(names.get(1).equals("خالد بن محمد الشكيلي"),"second");
    check(names.get(2).equals("أحمد بن سيف اليحيائي"),"third");
    check(BatchNameParser.split("عبدالله بن سالم ومحمد بن سعيد").size()==1,"no split on waw");
    check(BatchNameParser.split("سالم بن حمد\nراشد بن علي").size()==2,"line separated");
    check(BatchNameParser.split("").isEmpty(),"empty");
    System.out.println("BatchNameParser tests passed");
  }
}
