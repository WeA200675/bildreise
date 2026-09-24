# Bildreise – Generative Traumreise

## Ziel

Bildreise soll ein Foto nicht nur filtern, sondern es immer wieder neu
interpretieren. Das ursprüngliche Motiv bleibt über Kontur, Komposition und
semantische Merkmale erkennbar, während Stil, Material, Farbe, Licht und Form
variieren.

## Empfohlene Pipeline

```text
Foto
  ↓
lokale Voranalyse: Motiv, Kontur, Tiefenkarte, sensible Bereiche
  ↓
generative Variante mit Seed und Transformationsprofil
  ↓
Strukturprüfung: Motiv noch erkennbar?
  ↓
fließender Übergang zwischen Varianten
  ↓
neuer Seed und nächste Traumphase
```

## Generatives Modell

Die Modellschicht bleibt austauschbar. Unterstützt werden sollen:

- Bild-zu-Bild-Diffusion oder Flow-Modelle
- Struktursteuerung über Kontur, Tiefe oder Kanten
- Stil- und Materialprofile
- niedrige bis hohe Erkennbarkeit
- reproduzierbare Seeds und endlose neue Seeds

Für die stärkste Qualität wird zunächst ein leistungsfähiger Modellserver
vorgesehen. Die App verarbeitet Auswahl, Vorschau, Animation und Speicherung
lokal. Ein späterer On-Device-Modus kann für unterstützte Geräte ergänzt werden.

Für Pixel-Geräte ist zusätzlich ein On-Device-Pfad vorgesehen. Dieser nutzt
ein kleineres quantisiertes Image-to-Image-Modell über LiteRT und fällt bei
fehlendem Modell auf die lokale GPU-/Canvas-Engine zurück.

Für die generative mobile Variante wird MediaPipe Image Generator als
aufgabenorientierte Schicht ergänzt. Dadurch können Seeds, iterative
Zwischenbilder und optionale Kanten-/Tiefenbedingungen direkt genutzt werden.

## Transformationsprofile

- Traum: weiche, organische, leuchtende Variationen
- Skulptur: Körper und Motive als Material, Stein, Glas oder Metall
- Natur: Wasser, Rauch, Blätter, Licht und Wolken
- Kosmos: Nebel, Sterne, Farben und räumliche Tiefe
- Surreal: kontrollierte, aber starke Formneuinterpretation
- Anatomische Skulptur: die gesamte Figur als neutrales Material, Licht- und Konturwesen
- Retro: Film, Papier, analoge Farben und alte Optik
- Frei: die Engine entscheidet selbst

## Wiederholung vermeiden

Eine absolute Wiederholungsfreiheit ist nicht garantierbar. Praktisch werden
Wiederholungen minimiert durch:

- zufällige Seeds
- Verlauf bereits verwendeter Seeds
- wechselnde Profile und Parameter
- neue Zwischenziele im Latent-Raum
- leichte, kontrollierte Abweichungen pro Phase
- Ähnlichkeitsprüfung gegen vorherige Varianten

## Datenschutz

Originale bleiben lokal. Vor einer Modellanfrage muss die App ausdrücklich
anzeigen, ob ein Bild den privaten Gerätespeicher verlässt. Der lokale Modus
verwendet ausschließlich On-Device-Modelle und lokale Effekte.

## Nächster technischer Schritt

Ein Provider-Interface mit einem Mock-Provider wird zuerst eingebaut. Danach
kann ein konkreter Diffusions-/Flow-Dienst angeschlossen werden, ohne die
Benutzeroberfläche oder den Variantenverlauf zu ändern.
