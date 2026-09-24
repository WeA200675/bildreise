# Bildreise On-Device für Pixel

Dieser Ordner definiert den Android-Pfad für lokale KI-Verarbeitung auf dem
Pixel 10 Pro. Das Foto bleibt auf dem Gerät. Die Browser-Version bleibt als
Desktop-/Preview-Oberfläche erhalten.

## Zielarchitektur

```text
Galerie
  ↓
BitmapDecoder
  ↓
Motiv-/Kontur-Voranalyse
  ↓
quantisiertes Image-to-Image-Modell über LiteRT
  ↓
GPU/NPU-Delegat
  ↓
Zwischenbild-Animator
  ↓
private App-Datei oder Galerie-Export
```

## Modellanforderungen

Das mobile Modell muss:

- Image-to-Image oder Referenzbild-Conditioning unterstützen
- quantisiert sein, bevorzugt INT8 oder gemischte Präzision
- eine Eingabegröße von 512 oder 768 Pixel unterstützen
- einen Seed und eine Stärke akzeptieren
- ohne Netzwerkzugriff ausführbar sein

Das Modell wird als optionales Asset außerhalb des Git-Repositories verwaltet.
Beim ersten Start kann es nach ausdrücklicher Zustimmung geladen und lokal
verschlüsselt gespeichert werden.

MediaPipe-Foundation-Modelle werden als Bundle behandelt. Der Android-Code
prüft die SHA-256-Prüfsumme, begrenzt die Downloadgröße, schützt vor
gefährlichen ZIP-Pfaden und aktiviert ein Bundle erst nach erfolgreicher
Extraktion.

Für echte mobile Bildgenerierung verwendet der Android-Pfad zusätzlich die
MediaPipe Image Generator Task. Diese unterstützt Diffusionsmodelle, Seeds,
iterative Zwischenbilder und optionale Referenzbedingungen wie Kanten oder
Tiefe. Das Foundation-Modell muss vor der Nutzung in das von MediaPipe
verlangte Modellformat konvertiert werden.

## Hardwareauswahl

Die App wählt zur Laufzeit den besten verfügbaren Backend aus:

1. GPU/NPU-Delegat über LiteRT
2. CPU-Fallback für kleine Modelle
3. Canvas-/GPU-Effekte, falls kein Modell verfügbar ist

Die App zeigt vor einer langen Reise eine ungefähre Laufzeit und den
Energiebedarf an und stoppt sicher bei thermischer Drosselung.

## Datenschutz

- kein Upload im On-Device-Modus
- Original bleibt im privaten App-Speicher
- EXIF-Daten werden beim Export entfernt
- Modell- und Ergebnisdateien werden beim Löschen der Reise entfernt
- separater, sichtbarer Schalter für jeden Cloud-Modus

## Modellbereitstellung

Der Android-Code lädt das Bundle beim ersten Start über HTTPS, prüft Größe,
ZIP-Pfade, Manifest und SHA-256 und aktiviert es atomar im privaten
App-Speicher. Danach arbeitet die App offline. Bei einer neuen Prüfsumme wird
das Bundle automatisch erneut geladen; ein altes oder beschädigtes Bundle wird
nicht weiterverwendet.

## Modell konfigurieren

Für einen signierten Modellserver werden beim Build zwei Felder gesetzt:

```kotlin
buildConfigField("String", "MODEL_URL", "\"https://example/model.tflite\"")
buildConfigField("String", "MODEL_SHA256", "\"<64-stellige-sha256-prüfsumme>\"")
```

Für MediaPipe-Bundles werden stattdessen `MODEL_BUNDLE_URL` und
`MODEL_BUNDLE_SHA256` gesetzt. Das Bundle wird nach erfolgreicher Prüfung als
Verzeichnis aktiviert und von `MediaPipeDreamRunner` an den Image Generator
übergeben. Der Runner verwendet deterministische Seeds und profilabhängige
Prompts. Referenzbild-Conditioning über Edge-, Depth- oder Face-Plugins bleibt
ein separates, optionales Bundle und wird erst aktiviert, wenn die passenden
Plugin-Dateien ebenfalls versioniert und geprüft vorliegen.

Ohne beide Werte bleibt die App vollständig offline und verwendet den lokalen
Fallback. Ein Download wird nur bei gültiger URL und exakt 64-stelliger
SHA-256-Prüfsumme versucht.

## Automatische Prüfung

Jeder Push auf `main` startet den Workflow `Verify Bildreise`. Er prüft die
Python-Provider und baut zusätzlich ein Android-Debug-APK mit JDK 17 und
Android SDK. Ein Modell-Download ist im CI absichtlich deaktiviert.
