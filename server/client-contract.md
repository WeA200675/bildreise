# Frontend-API-Vertrag

## Health

`GET /health`

Antwort:

```json
{"status":"ok","provider":"MockProvider"}
```

## Traumvariante anfordern

`POST /dream/generate`

```json
{
  "image_base64": "...",
  "profile": "fantasy",
  "recognition": 0.55,
  "intensity": 0.8,
  "seed": 12345,
  "phase": 2
}
```

Die Antwort enthält eine Variante-ID, Provider, Seed, Phase und den Status
`ready` oder `queued`. Die Browser-App darf bei Provider-Ausfall auf ihre
lokale GPU-/Canvas-Animation zurückfallen.
