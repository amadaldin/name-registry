import com.nameregister.app.LocalCommandBrain;
public class LocalCommandBrainTest {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    LocalCommandBrain brain = new LocalCommandBrain();
    check(brain.interpret("انتهى").action.equals("stop"), "Standalone stop");
    check(brain.interpret("اكتب لي وليد راشد محمد سيف الزيدي من حارة خطو انتهى").name.equals("وليد راشد محمد سيف الزيدي"), "Name extraction");
    check(brain.interpret("اكتب لي وليد راشد محمد سيف الزيدي من حارة خطو انتهى").area.equals("خطو"), "Area extraction");
    check(brain.interpret("حارة خطو").action.equals("set_area"), "Area command");
    check(brain.interpret("من طرف وليد").action.equals("set_referrer"), "Referrer command");
    check(brain.interpret("اكتب لي راشد بن محمد بن سيف الزيدي").action.equals("add"), "Add command");
    LocalCommandBrain.Result both=brain.interpret("اكتب لي سالم بن حمد من حارة خطو من طرف راشد");
    check(both.name.equals("سالم بن حمد"),"Combined name");
    check(both.area.equals("خطو"),"Combined area");
    check(both.referrer.equals("راشد"),"Combined referrer");
    LocalCommandBrain.Result reversed=brain.interpret("سالم بن حمد من طرف راشد من حارة خطو");
    check(reversed.name.equals("سالم بن حمد"),"Reverse order name");
    check(reversed.area.equals("خطو"),"Reverse order area");
    check(reversed.referrer.equals("راشد"),"Reverse order referrer");
    check(brain.interpret("احذف سالم بن حمد").action.equals("needs_confirmation"),"No automatic deletion");
    check(brain.interpret("انتهى.").action.equals("stop"),"Stop with punctuation");
    check(brain.interpret("، وقف!").action.equals("stop"),"Stop with leading punctuation");
    LocalCommandBrain.Result natural=brain.interpret("لو سمحت يا خوي سجل عندك اسم سالم بن راشد بن محمد من حارة الغبي تمام");
    check(natural.action.equals("add"),"Natural speech add");
    check(natural.name.equals("سالم بن راشد بن محمد"),"Extract name inside conversation");
    check(natural.area.equals("الغبي"),"Extract area inside conversation");
    check(brain.interpret("عندي اسم خالد بن سيف").name.equals("خالد بن سيف"),"Conversational name");
    check(brain.interpret("ممكن تكتب لي اسم أحمد بن سعيد وشكرا").name.equals("أحمد بن سعيد"),"Polite speech name");
    System.out.println("LocalCommandBrain tests passed");
  }
}
