# ComfyUI-Workflow

`flux-kontext-api.json` ist absichtlich ein leerer Platzhalter. Ein konkreter
Workflow hängt von GPU, VRAM, Modellgewicht und Lizenz ab und darf nicht
ungeprüft ins Repository übernommen werden.

Der Workflow muss als API-Format aus ComfyUI exportiert werden. Er sollte
mindestens enthalten:

1. Bild-Upload beziehungsweise Load Image
2. Bild-zu-Bild- oder Kontext-Edit-Modell
3. optional ControlNet/IP-Adapter für Strukturtreue
4. Seed-Eingang
5. Prompt-Eingang
6. Save Image-Ausgabe

Die Provider-Schicht injiziert Prompt und Seed in gängige Node-Eingänge.
Individuelle Node-IDs oder Sonderknoten können im Workflow beibehalten werden.
