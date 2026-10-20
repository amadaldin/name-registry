package com.nameregister.app;

/** End-of-utterance detector for 16 kHz mono PCM.
 * Reduces waiting after a short name while keeping a hard limit in the recorder.
 */
public final class SpeechEndDetector {
    private static final int SAMPLE_RATE = 16000;
    private static final int MIN_VOICED_SAMPLES = SAMPLE_RATE / 4;
    private static final int END_SILENCE_SAMPLES = SAMPLE_RATE;
    private static final int MIN_TOTAL_SAMPLES = SAMPLE_RATE * 12 / 10;
    private static final double VOICE_RMS_THRESHOLD = 0.012;

    private long totalSamples;
    private long voicedSamples;
    private long trailingSilenceSamples;

    /** True when at least 250 ms of voice is followed by 1 s of silence. */
    public boolean accept(short[] pcm, int count) {
        if (pcm == null || count <= 0 || count > pcm.length) return false;
        double energy = 0;
        for (int i = 0; i < count; i++) {
            double normalized = pcm[i] / 32768.0;
            energy += normalized * normalized;
        }
        double rms = Math.sqrt(energy / count);
        totalSamples += count;
        if (rms >= VOICE_RMS_THRESHOLD) {
            voicedSamples += count;
            trailingSilenceSamples = 0;
        } else if (voicedSamples > 0) {
            trailingSilenceSamples += count;
        }
        return totalSamples >= MIN_TOTAL_SAMPLES
                && voicedSamples >= MIN_VOICED_SAMPLES
                && trailingSilenceSamples >= END_SILENCE_SAMPLES;
    }

    public boolean hasSpeech() {
        return voicedSamples >= MIN_VOICED_SAMPLES;
    }
}
