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
    System.out.println("LocalCommandBrain tests passed");
  }
}
