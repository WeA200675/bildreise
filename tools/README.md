# Modellvorbereitung

Die offizielle MediaPipe-Konvertierung erzeugt zunächst ein Modellverzeichnis.
Danach wird daraus ein geprüftes Bundle erstellt:

```bash
python3 tools/prepare_mediapipe_model.py \
  /pfad/zum/konvertierten-modell \
  build/image-generator.zip \
  --backend mobile-diffusion
```

Der ausgegebene SHA-256-Wert wird als `bildreise.modelBundleSha256` in die
Android-Build-Konfiguration eingetragen. Das ZIP wird auf einem HTTPS-Server
abgelegt und über `bildreise.modelBundleUrl` referenziert. Beide Werte können
auch als `BILDREISE_MODEL_BUNDLE_URL` und `BILDREISE_MODEL_BUNDLE_SHA256`
gesetzt werden. Der Build bricht ab, wenn nur einer der beiden Werte gesetzt
ist oder der Hash nicht exakt 64 Hex-Zeichen enthält.

Die offiziellen MediaPipe-Anweisungen verlangen ein kompatibles Stable-
Diffusion-Modell und eine Konvertierung in das Image-Generator-Format. Die
Modelllizenz muss vor Veröffentlichung geprüft werden.

Für den vorgesehenen mobilen Backend-Namen kann das Bundle inklusive der
exakten Build-Konfiguration mit einem Schritt erzeugt werden:

```bash
python3 tools/build_mobile_bundle.py \
  /pfad/zum/konvertierten-modell \
  build/bildreise-mobile-diffusion.zip \
  --url https://dein-server.example/models/bildreise-mobile-diffusion.zip
```

Das Skript akzeptiert nur HTTPS, lädt keine Modellgewichte selbst herunter und
gibt anschließend den geprüften SHA-256-Hash für Gradle aus.
