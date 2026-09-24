package app.bildreise

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class ModelBundle(
    val url: String,
    val sha256: String,
    val name: String = "image-generator",
    val backend: String = "mobile-diffusion",
)

class ModelBundleManager(private val context: Context) {
    private val maxBytes = 2L * 1024L * 1024L * 1024L
    fun directory(bundle: ModelBundle): File = File(context.filesDir, bundle.name)

    fun isReady(bundle: ModelBundle): Boolean =
        File(directory(bundle), ".ready").let { it.isFile && it.readText().trim().equals(bundle.sha256, ignoreCase = true) }

    @Throws(Exception::class)
    fun downloadAndActivate(bundle: ModelBundle, progress: (Int) -> Unit = {}): File {
        require(bundle.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Ungültige Modell-Prüfsumme" }
        val zip = File(context.cacheDir, "${bundle.name}.zip.partial")
        val staging = File(context.cacheDir, "${bundle.name}.staging")
        if (staging.exists()) staging.deleteRecursively()
        staging.mkdirs()
        val connection = (URL(bundle.url).openConnection() as HttpURLConnection).apply { connectTimeout=15_000; readTimeout=120_000 }
        try {
            if (connection.responseCode !in 200..299) error("Modellserver antwortete mit ${connection.responseCode}")
            var total = 0L
            connection.inputStream.use { input -> zip.outputStream().use { output ->
                val buffer=ByteArray(64*1024); var read:Int
                while(input.read(buffer).also{read=it}>=0){ if(read==0)continue; total+=read; require(total<=maxBytes){"Modell-Bundle ist zu groß"}; output.write(buffer,0,read); val expected=connection.contentLengthLong; if(expected>0)progress((total*100/expected).toInt().coerceIn(0,100)) }
            }}
            require(sha256(zip).equals(bundle.sha256,true)){"Modell-Prüfsumme stimmt nicht"}
            extractSafely(zip, staging)
            require(staging.walkTopDown().any{it.isFile}){"Modell-Bundle ist leer"}
            require(File(staging, "manifest.json").isFile) { "Modell-Bundle enthält kein Manifest" }
            val target=directory(bundle); if(target.exists())target.deleteRecursively(); require(staging.renameTo(target)){"Modell-Bundle konnte nicht aktiviert werden"}
            File(target,".ready").writeText(bundle.sha256)
            return target
        } finally { connection.disconnect(); zip.delete(); if(staging.exists())staging.deleteRecursively() }
    }

    private fun extractSafely(zip:File,target:File){ZipInputStream(zip.inputStream().buffered()).use{input->var entry=input.nextEntry;var count=0;while(entry!=null){require(++count<=10000){"Zu viele Modell-Dateien"};val out=File(target,entry.name);require(out.canonicalPath.startsWith(target.canonicalPath+File.separator)){"Ungültiger ZIP-Pfad"};if(entry.isDirectory)out.mkdirs()else{out.parentFile?.mkdirs();out.outputStream().use{input.copyTo(it,64*1024)}};entry=input.nextEntry}}}
    private fun sha256(file:File):String{val digest=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val buffer=ByteArray(64*1024);var read:Int;while(input.read(buffer).also{read=it}>=0)if(read>0)digest.update(buffer,0,read)};return digest.digest().joinToString(""){ "%02x".format(it) }}
}
