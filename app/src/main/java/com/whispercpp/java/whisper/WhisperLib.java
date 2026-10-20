package com.whispercpp.java.whisper;
public final class WhisperLib {
 static { System.loadLibrary("whisper"); }
 public static native long initContext(String modelPath);
 public static native void freeContext(long ptr);
 public static native void fullTranscribe(long ptr,int threads,float[] samples);
 public static native int getTextSegmentCount(long ptr);
 public static native String getTextSegment(long ptr,int index);
 private WhisperLib(){}
}
