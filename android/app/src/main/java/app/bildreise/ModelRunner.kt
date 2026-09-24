package app.bildreise

import android.graphics.Bitmap

data class DreamControls(
    val seed: Long,
    val strength: Float,
    val recognition: Float,
    val profile: String,
)

interface ModelRunner {
    fun transform(source: Bitmap, controls: DreamControls): Bitmap
    fun close() {}
}

/** Safe fallback used when the optional model asset is not installed. */
class LocalFallbackRunner : ModelRunner {
    override fun transform(source: Bitmap, controls: DreamControls): Bitmap {
        return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
    }
}
