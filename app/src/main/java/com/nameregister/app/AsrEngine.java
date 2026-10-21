package com.nameregister.app;

/**
 * Common speech-engine contract for the planned offline Qwen3-ASR integration.
 * A model is not considered ready until its runtime, model assets and on-device
 * transcription have all been verified. Never silently save a transcript.
 */
public interface AsrEngine {
    interface Listener {
        void status(String message);
        void result(String transcript);
        void error(String message);
    }
    String id();
    boolean isReady();
    void recordAndTranscribe();
    void close();
}
