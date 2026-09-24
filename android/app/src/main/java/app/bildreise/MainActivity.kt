package app.bildreise

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.SeekBar
import java.io.FileOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import java.util.concurrent.Executors
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var profile: Spinner
    private lateinit var recognition: SeekBar
    private lateinit var intensity: SeekBar
    private var source: Bitmap? = null
    private var runner: ModelRunner = LocalFallbackRunner()
    private val executor = Executors.newSingleThreadExecutor()

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                source = android.graphics.BitmapFactory.decodeStream(stream)
                if (source == null) error("Bild konnte nicht gelesen werden")
                preview.setImageBitmap(source)
                status.text = "Bild bereit – lokal verarbeitet"
            } ?: error("Kein Bildzugriff möglich")
        } catch (error: Exception) { status.text = "Bild konnte nicht geladen werden" }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32) }
        val title = TextView(this).apply { text = "BILDREISE\nGenerative Traumreise"; textSize = 26f }
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_INSIDE }
        status = TextView(this).apply { text = "Wähle ein Foto aus"; textSize = 16f }
        profile = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Fantasie", "Anatomische Skulptur", "Natur", "Kosmos", "Surreal", "Retro", "Frei")) }
        recognition = SeekBar(this).apply { max = 70; progress = 35; contentDescription = "Erkennbarkeit" }
        intensity = SeekBar(this).apply { max = 90; progress = 60; contentDescription = "Verwandlungsstärke" }
        val choose = Button(this).apply { text = "Foto auswählen"; setOnClickListener { picker.launch("image/*") } }
        val start = Button(this).apply { text = "Traumreise starten"; setOnClickListener { startDream() } }
        val save = Button(this).apply { text = "Ergebnis privat speichern"; setOnClickListener { saveResult() } }
        root.addView(title); root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(status); root.addView(TextView(this).apply { text = "Traumprofil" }); root.addView(profile); root.addView(TextView(this).apply { text = "Erkennbarkeit" }); root.addView(recognition); root.addView(TextView(this).apply { text = "Verwandlungsstärke" }); root.addView(intensity); root.addView(choose); root.addView(start); root.addView(save)
        setContentView(root)
        prepareRunner()
    }

    private fun startDream() {
        if (source == null) { status.text = "Bitte zuerst ein Foto auswählen"; return }
        status.text = "On-Device-Engine arbeitet …"
        val input = source ?: return
        val controls = DreamControls(System.currentTimeMillis(), .1f + intensity.progress / 100f, .2f + recognition.progress / 100f, profile.selectedItem.toString().lowercase())
        executor.execute {
            try {
                val result = runner.transform(input, controls)
                runOnUiThread { preview.setImageBitmap(result); status.text = "Traumreise abgeschlossen – nur auf dem Gerät" }
            } catch (error: Throwable) {
                runOnUiThread { status.text = "KI-Modell nicht kompatibel – lokaler Fallback aktiv" }
            }
        }
    }

    private fun prepareRunner() {
        val bundle = ModelConfig.bundle()
        if (bundle != null) {
            status.text = "Generatives Modell-Bundle wird geprüft …"
            executor.execute {
                try {
                    val manager = ModelBundleManager(this)
                    val directory = if (manager.isReady(bundle)) manager.directory(bundle) else manager.downloadAndActivate(bundle) { percent ->
                        runOnUiThread { status.text = "Modell wird geladen: $percent %" }
                    }
                    val ready = MediaPipeDreamRunner.create(this, directory.absolutePath)
                    val old = runner
                    runner = ready
                    old.close()
                    runOnUiThread { status.text = "Generative On-Device-KI bereit" }
                } catch (error: Throwable) {
                    runOnUiThread { status.text = "Bundle nicht verfügbar – Offline-Modus aktiv" }
                }
            }
            return
        }
        val configured = ModelConfig.asset(this)
        val manager = ModelAssetManager(this)
        val model = ModelConfig.file(this)
        if (configured != null && !manager.isValid(configured)) {
            status.text = "On-Device-Modell wird sicher geladen …"
            executor.execute {
                try { manager.download(configured); initializeRunner(model) }
                catch (error: Throwable) { runOnUiThread { status.text = "Modell-Download fehlgeschlagen – Offline-Modus aktiv" } }
            }
            return
        }
        if (!model.exists()) { status.text = "Lokaler Modus bereit – kein Modell installiert"; return }
        initializeRunner(model)
    }

    private fun saveResult() {
        val bitmap = preview.drawable ?: run { status.text = "Noch kein Ergebnis vorhanden"; return }
        val output = File(filesDir, "bildreise-${System.currentTimeMillis()}.png")
        try {
            val drawable = bitmap as? android.graphics.drawable.BitmapDrawable
            FileOutputStream(output).use { stream -> requireNotNull(drawable).bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream) }
            status.text = "Privat gespeichert: ${output.name}"
        } catch (error: Throwable) { status.text = "Speichern fehlgeschlagen" }
    }

    private fun initializeRunner(model: File) {
        runOnUiThread { status.text = "On-Device-Modell wird geladen …" }
        LiteRtRunnerFactory.create(this, model.absolutePath, { ready ->
            runner = ready
            runOnUiThread { status.text = "On-Device-KI bereit" }
        }, { error -> runOnUiThread { status.text = "Modell nicht verfügbar – Fallback aktiv" } })
    }

    override fun onDestroy() { executor.shutdownNow(); runner.close(); super.onDestroy() }
}
