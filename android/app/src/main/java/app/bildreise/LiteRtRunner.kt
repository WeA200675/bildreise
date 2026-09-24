package app.bildreise

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.gpu.TfLiteGpu
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Executors

/** Creates a LiteRT runner off the UI thread and falls back from GPU to CPU. */
object LiteRtRunnerFactory {
    fun create(context: Context, modelFile: String, onReady: (ModelRunner) -> Unit, onError: (Throwable) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                Tasks.await(com.google.android.gms.tflite.java.TfLite.initialize(context))
                val buffer = mapFile(File(modelFile))
                val gpu = runCatching { Tasks.await(TfLiteGpu.isGpuDelegateAvailable(context)) }.getOrDefault(false)
                val options = InterpreterApi.Options().setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
                if (gpu) options.addDelegateFactory(GpuDelegateFactory())
                val interpreter = InterpreterApi.create(buffer, options)
                onReady(LiteRtBitmapRunner(interpreter))
            } catch (gpuOrInitError: Throwable) {
                onError(gpuOrInitError)
            }
        }
    }

    private fun mapFile(file: File): MappedByteBuffer {
        require(file.isFile && file.length() > 0) { "Modell-Datei fehlt oder ist leer" }
        FileInputStream(file).use { input -> return input.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, file.length()) }
    }
}

private class LiteRtBitmapRunner(private val interpreter: InterpreterApi) : ModelRunner {
    override fun transform(source: Bitmap, controls: DreamControls): Bitmap {
        val input = interpreter.getInputTensor(0).shape()
        require(input.size == 4 && input[3] == 3) { "Modell benötigt ein [1,H,W,3]-Eingabetensor" }
        val width = input[2]; val height = input[1]
        val resized = Bitmap.createScaledBitmap(source, width, height, true)
        val inputBuffer = ByteBuffer.allocateDirect(width * height * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height); resized.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.forEach { pixel -> inputBuffer.putFloat(((pixel shr 16) and 255) / 255f); inputBuffer.putFloat(((pixel shr 8) and 255) / 255f); inputBuffer.putFloat((pixel and 255) / 255f) }
        inputBuffer.rewind()
        val output = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
        interpreter.run(inputBuffer, output)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val out = IntArray(width * height)
            for (i in out.indices) { val rgb = output[0][i / width][i % width]; out[i] = (255 shl 24) or ((rgb[0].coerceIn(0f,1f)*255).toInt() shl 16) or ((rgb[1].coerceIn(0f,1f)*255).toInt() shl 8) or (rgb[2].coerceIn(0f,1f)*255).toInt() }
            bitmap.setPixels(out, 0, width, 0, 0, width, height)
        }
    }
    override fun close() { interpreter.close() }
}
