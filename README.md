# Bildreise

Eine kreative Foto-App, die Bilder durch kontrollierten Zufall in erkennbare Unikate verwandelt.

## Prototyp starten

Die erste Version ist ein unabhängiger Browser-Prototyp ohne Build-Schritt. Öffne `index.html` direkt im Browser oder starte im Repository einen einfachen lokalen Server:

```bash
python3 -m http.server 8080
```

Danach `http://localhost:8080` öffnen.

## Aktueller Umfang

- Foto aus der Galerie laden
- Fantasie-, Sanft-, Kunst-, Surreal-, Anatomische-Skulptur-, Geometrie-, Natur-, Retro- und Glitch-Modus
- Erkennbarkeitsgrad und Transformationsstärke
- Flüssige Canvas-Animation mit kontrolliertem Zufall
- Varianten neu würfeln
- Ergebnis lokal als PNG herunterladen

Die Bildverarbeitung läuft vollständig im Browser. Das Bild wird nicht an einen Server übertragen.

Für eine generative API kann die Seite mit `?api=http://127.0.0.1:8787` geöffnet
werden. Ohne diesen Parameter bleibt die lokale Verarbeitung aktiv.

Der Fantasie-Modus durchläuft mehrere Phasen: Farbwelt, Wellenverformung,
Textur, Geometrie, Fragmentierung und eine abschließende Glitch-Variation.
Der Erkennbarkeitsregler steuert, wie weit sich das Ergebnis vom Motiv lösen darf.

## Projektstatus

Siehe [docs/meilensteine.md](docs/meilensteine.md).
