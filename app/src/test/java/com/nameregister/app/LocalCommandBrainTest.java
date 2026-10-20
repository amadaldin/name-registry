package com.nameregister.app;
public final class LocalCommandBrainTest {
    static void eq(String a, String b) { if (!a.equals(b)) throw new AssertionError(a + " != " + b); }
    public static void main(String[] args) {
        LocalCommandBrain brain = new LocalCommandBrain();
        eq("set_area", brain.interpret("حارة خطو").action);
        eq("set_referrer", brain.interpret("من طرف وليد").action);
        LocalCommandBrain.Result item = brain.interpret("أضف أحمد بن علي من حارة العارض من طرف حمد");
        eq("add", item.action);
        eq("أحمد بن علي", item.name);
        eq("العارض", item.area);
        eq("حمد", item.referrer);
        System.out.println("Arabic parser checks passed");
    }
}
