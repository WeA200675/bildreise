package app.bildreise

import android.content.Context
import java.io.File

object ModelConfig {
    fun asset(context: Context): ModelAsset? {
        val url = BuildConfig.MODEL_URL.trim()
        val sha = BuildConfig.MODEL_SHA256.trim()
        if (url.isBlank() || sha.length != 64) return null
        return ModelAsset(url, sha, "bildreise-model.tflite")
    }

    fun file(context: Context): File = File(context.filesDir, "bildreise-model.tflite")

    fun bundle(): ModelBundle? {
        val url = BuildConfig.MODEL_BUNDLE_URL.trim()
        val sha = BuildConfig.MODEL_BUNDLE_SHA256.trim()
        if (url.isBlank() || sha.length != 64) return null
        return ModelBundle(url, sha, backend = BuildConfig.MODEL_BACKEND)
    }
}
