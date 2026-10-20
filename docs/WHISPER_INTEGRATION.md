# Local Whisper integration — engineering plan

Target: Android ARM64, Arabic multilingual Whisper tiny/base, offline.

Upstream official reference: https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android.java

## Required before claiming integration is working
1. Pin an upstream whisper.cpp commit and vendor/build its C/C++ runtime using Android NDK/CMake.
2. Adapt JNI bindings from the official Android Java example into com.nameregister.app; do not call JNI until native .so is bundled.
3. Record 16 kHz mono PCM from Android AudioRecord; run inference off the UI thread.
4. Package a multilingual (not *.en) ggml model or implement one-time user-approved download with integrity verification, local storage and progress display.
5. Use model output as input to LocalCommandBrain, with review before saving.
6. Keep Android SpeechRecognizer fallback if model unavailable; never pretend it is Whisper.
7. Verify debug APK builds in GitHub Actions and on-device performance/accuracy with Omani names.
8. Preserve SQLite data across upgrades. Never ship an API key.

## Current status
The existing HybridSpeechEngine uses Android SpeechRecognizer, not Whisper. LocalCommandBrain is a specialized offline rule-based parser, not a neural model. Whisper native binaries and model have NOT yet been integrated. No claim of fully offline speech recognition is warranted.
