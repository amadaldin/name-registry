package com.nameregister.app

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineQwen3AsrModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import java.io.File

/** Kotlin adapter for sherpa-onnx Android AAR's Kotlin data-class API. */
object QwenBridge {
    @JvmStatic
    fun transcribe(dir: File, samples: FloatArray, sampleRate: Int): String {
        val qwen = OfflineQwen3AsrModelConfig(
            convFrontend = File(dir, "conv_frontend.onnx").absolutePath,
            encoder = File(dir, "encoder.int8.onnx").absolutePath,
            decoder = File(dir, "decoder.int8.onnx").absolutePath,
            tokenizer = File(dir, "tokenizer").absolutePath
        )
        val model = OfflineModelConfig(qwen3Asr = qwen, numThreads = 4, provider = "cpu")
        val recognizer = OfflineRecognizer(config = OfflineRecognizerConfig(modelConfig = model))
        try {
            val stream = recognizer.createStream()
            try {
                stream.acceptWaveform(samples, sampleRate)
                recognizer.decode(stream)
                return recognizer.getResult(stream).text.trim()
            } finally {
                stream.release()
            }
        } finally {
            recognizer.release()
        }
    }
}
