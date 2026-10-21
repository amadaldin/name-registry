# Qwen3-ASR Android evaluation (before integration)

## Official test build
The sherpa-onnx project publishes a prebuilt Android arm64-v8a microphone test APK:
https://github.com/k2-fsa/sherpa-onnx/blob/master/flutter/sherpa_onnx/example/example.md

Find: `flutter-vad-non-streaming-asr-from-microphone-qwen3-asr-multi-arm64-v8a.apk`.

Model: `sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25`.
Official model and API documentation:
https://k2-fsa.github.io/sherpa/onnx/qwen3-asr/index.html

## Evaluation script (speak naturally, once each)
1. وليد راشد محمد سيف الزيدي من حارة خطو انتهى
2. راشد بن محمد بن سيف الزيدي
3. أحمد بن راشد بن محمد الزيدي
4. حارة خطو
5. من طرف وليد
6. علي بن حميد بن عبدالله الشكيلي
7. معاوية بن محمد بن سيف الزيدي
8. خالد محمد مبارك اليحيائي
9. سجل وليد راشد محمد سيف الزيدي من حارة خطو
10. وليد راشد محمد سيف الزيدي

## Measure
For each utterance record: exact spoken phrase, verbatim recognized text, time from end of speech to transcript, and whether model hallucinated extra words. Keep the recording and result private. Never auto-save a guessed name.

## Decision gate
Compare against the same recordings on existing Whisper Tiny. Only integrate after measured improvement in exact person-name and neighborhood transcription at acceptable latency. This prototype is a separate app; it does not replace or migrate the name-registry database.

## Integration requirements
- Native sherpa-onnx Android/Kotlin/Java runtime, offline model download with checksum and resume.
- Separate model storage; no model inside APK.
- Persist the chosen engine; allow fallback to Whisper.
- Confirmation UI and explicit user-approved correction dictionary for local names and neighborhoods.
- Build verification on GitHub Actions and physical-device test before calling it ready.
