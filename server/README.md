# Generative Dream Provider

Die Provider-Schicht verbindet Bildreise mit einem lokalen ComfyUI-Server.
Sie sendet Originalbilder nur an den ausdrücklich konfigurierten Provider.

## Open-Source-Basis

- FLUX.1 Kontext [dev] für Bildbearbeitung
- Diffusers mit Image-to-Image, IP-Adapter und ControlNet
- ComfyUI als lokaler Workflow-Runner

Die Modellgewichte werden nicht ins Repository eingecheckt. Dafür wird ein
ComfyUI-Workflow als `COMFY_WORKFLOW` geladen.

## Start

```bash
python3 server/main.py
```

Um einen lokalen ComfyUI-Provider zu verwenden:

```bash
export DREAM_PROVIDER=comfyui
export COMFY_URL=http://127.0.0.1:8188
export COMFY_WORKFLOW=server/workflows/flux-kontext-api.json
python3 server/main.py
```

Für direkte lokale Diffusers-Inferenz auf einer CUDA-GPU:

```bash
python3 -m pip install -r requirements-ai.txt
export DREAM_PROVIDER=diffusers
python3 server/main.py
```

Der Standard nutzt `Tongyi-MAI/Z-Image-Turbo` als Image-to-Image-Pipeline.
Das Modell wird beim ersten Start aus Hugging Face geladen und benötigt eine
geeignete GPU beziehungsweise ausreichend Arbeitsspeicher.

Ohne diese Variablen läuft der sichere Mock-Provider. Er erzeugt keine
KI-Bilder, prüft aber die gesamte Reise-, Seed- und Verlaufsschnittstelle.

Die Browser-App kann danach mit folgendem URL-Parameter verbunden werden:

```text
https://deine-seite.example/bildreise/?api=http://127.0.0.1:8787
```
