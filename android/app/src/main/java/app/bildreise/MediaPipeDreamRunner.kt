package app.bildreise

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imagegenerator.ConditionOptions
import com.google.mediapipe.tasks.vision.imagegenerator.ConditionType
import com.google.mediapipe.tasks.vision.imagegenerator.EdgeConditionOptions
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGeneratorOptions
import java.io.File

/**
 * MediaPipe Image Generator adapter. The foundation-model path is deliberately
 * kept isolated so a converted model bundle can be replaced without changing
 * the UI or storage code.
 */
class MediaPipeDreamRunner private constructor(
    private val generator: ImageGenerator,
    private val edgeConditioning: Boolean
) : ModelRunner {
    override fun transform(input: Bitmap, controls: DreamControls): Bitmap {
        val prompt = promptFor(controls.profile)
        val iterations = (12 + controls.intensity * 18f).toInt().coerceIn(12, 30)
        val seed = (controls.seed xor input.width.toLong() xor input.height.toLong()).toInt()
        val sourceImage = BitmapImageBuilder(input).build()
        val result = if (edgeConditioning) {
            val condition = generator.createConditionImage(sourceImage, ConditionType.EDGE)
            generator.setInputs(prompt, condition, ConditionType.EDGE, iterations, seed)
            generator.execute(false)
        } else {
            generator.generate(prompt, iterations, seed)
        }
        return BitmapExtractor.extract(requireNotNull(result).generatedImage())
    }

    override fun close() = generator.close()

    companion object {
        fun create(context: Context, modelDirectory: String): MediaPipeDreamRunner {
            val modelDir = File(modelDirectory)
            val edgePlugin = File(modelDir, "canny_edge_plugin.tflite")
            val base = ImageGeneratorOptions.builder()
                .setImageGeneratorModelDirectory(modelDirectory)
            if (!edgePlugin.isFile) {
                return MediaPipeDreamRunner(ImageGenerator.createFromOptions(context, base.build()), false)
            }
            val edgeOptions = EdgeConditionOptions.builder()
                .setThreshold1(100.0f)
                .setThreshold2(200.0f)
                .setApertureSize(3)
                .setL2Gradient(false)
                .setPluginModelBaseOptions(BaseOptions.builder().setModelAssetPath(edgePlugin.absolutePath).build())
                .build()
            val conditions = ConditionOptions.builder().setEdgeConditionOptions(edgeOptions).build()
            return MediaPipeDreamRunner(ImageGenerator.createFromOptions(context, base.build(), conditions), true)
        }

        private fun promptFor(profile: String): String = when (profile) {
            "natur" -> "dreamlike natural reinterpretation, preserve the main subject and composition, luminous atmosphere, rich organic detail"
            "kosmos" -> "dreamlike cosmic reinterpretation, preserve the main subject and composition, nebula light, surreal depth, rich detail"
            "retro" -> "dreamlike retro-futurist reinterpretation, preserve the main subject and composition, analog color, cinematic texture"
            "surreal" -> "dreamlike surreal reinterpretation, preserve the recognizable main subject, imaginative shapes, flowing forms, rich detail"
            "anatomische skulptur" -> "dreamlike artistic whole-body sculpture reinterpretation, preserve the recognizable subject, non-explicit museum material study, flowing stone and glass"
            else -> "dreamlike fantasy reinterpretation, preserve the recognizable main subject and composition, imaginative color, flowing forms, rich detail"
        }
    }
}
