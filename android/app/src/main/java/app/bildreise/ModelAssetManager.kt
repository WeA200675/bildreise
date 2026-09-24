package app.bildreise

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ModelAsset(val url: String, val sha256: String, val filename: String = "bildreise-model.tflite")

class ModelAssetManager(private val context: Context) {
    fun localFile(asset: ModelAsset): File = File(context.filesDir, asset.filename)

    fun isValid(asset: ModelAsset): Boolean {
        val file = localFile(asset)
        return file.isFile && sha256(file).equals(asset.sha256, ignoreCase = true)
    }

    @Throws(Exception::class)
    fun download(asset: ModelAsset, progress: (Int) -> Unit = {}): File {
        val target = localFile(asset)
        val partial = File(context.cacheDir, "${asset.filename}.partial")
        val connection = (URL(asset.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 60_000; requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) error("Modellserver antwortete mit ${connection.responseCode}")
            val total = connection.contentLengthLong
            connection.inputStream.use { input -> partial.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024); var done = 0L; var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    if (read == 0) continue
                    output.write(buffer, 0, read); done += read
                    if (total > 0) progress((done * 100 / total).toInt().coerceIn(0, 100))
                }
            }}
            require(sha256(partial).equals(asset.sha256, ignoreCase = true)) { "Modell-Prüfsumme stimmt nicht" }
            check(partial.renameTo(target)) { "Modell konnte nicht atomar gespeichert werden" }
            return target
        } finally { connection.disconnect(); if (partial.exists() && !target.exists()) partial.delete() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(64 * 1024); var read: Int; while (input.read(buffer).also { read = it } >= 0) if (read > 0) digest.update(buffer, 0, read) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
