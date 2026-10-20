package com.nameregister.app;

/** Standalone JVM test: javac -d out SpeechEndDetector.java SpeechEndDetectorTest.java && java -cp out com.nameregister.app.SpeechEndDetectorTest */
public final class SpeechEndDetectorTest {
    private static short[] chunk(int amplitude) {
        short[] samples = new short[320];
        java.util.Arrays.fill(samples, (short) amplitude);
        return samples;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        SpeechEndDetector oneName = new SpeechEndDetector();
        for (int i = 0; i < 30; i++) check(!oneName.accept(chunk(2000), 320), "premature speech stop");
        for (int i = 0; i < 49; i++) check(!oneName.accept(chunk(0), 320), "premature silence stop");
        check(oneName.accept(chunk(0), 320), "should stop after 1 s silence");
        check(oneName.hasSpeech(), "voice was not detected");

        SpeechEndDetector silence = new SpeechEndDetector();
        for (int i = 0; i < 300; i++) check(!silence.accept(chunk(0), 320), "silent audio should not stop early");
        check(!silence.hasSpeech(), "silent audio was misclassified");

        SpeechEndDetector withPause = new SpeechEndDetector();
        for (int i = 0; i < 20; i++) withPause.accept(chunk(1500), 320);
        for (int i = 0; i < 35; i++) check(!withPause.accept(chunk(0), 320), "short pause ended recording");
        for (int i = 0; i < 20; i++) check(!withPause.accept(chunk(1500), 320), "second name ended recording");
        for (int i = 0; i < 49; i++) check(!withPause.accept(chunk(0), 320), "early stop after second name");
        check(withPause.accept(chunk(0), 320), "no stop after second name");

        SpeechEndDetector noise = new SpeechEndDetector();
        for (int i = 0; i < 300; i++) check(!noise.accept(chunk(200), 320), "background noise caused early stop");
        check(!noise.hasSpeech(), "background noise classified as speech");
        System.out.println("SpeechEndDetector: 4 scenarios passed");
    }
}
