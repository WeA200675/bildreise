"""Provider abstraction for Bildreise's generative dream journey."""
from __future__ import annotations

import base64
import json
import os
import random
import time
import urllib.request
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Protocol


PROFILES = {
    "fantasy": "dreamlike organic light, flowing material, recognizable subject",
    "sculpture": "anatomy-neutral full-figure sculpture, stone glass smoke and light",
    "nature": "water, leaves, mist, wind and natural light",
    "cosmos": "nebula, stars, deep color and spatial light",
    "surreal": "strong surreal reinterpretation while preserving the main silhouette",
    "retro": "analog film, paper texture, faded colors and cinematic grain",
    "free": "invent a surprising artistic reinterpretation while preserving the subject",
}


@dataclass
class DreamRequest:
    image_base64: str
    profile: str = "fantasy"
    recognition: float = 0.55
    intensity: float = 0.7
    seed: int | None = None
    phase: int = 0

    def normalized(self) -> "DreamRequest":
        profile = self.profile if self.profile in PROFILES else "fantasy"
        return DreamRequest(
            image_base64=self.image_base64,
            profile=profile,
            recognition=max(0.2, min(0.9, float(self.recognition))),
            intensity=max(0.1, min(1.0, float(self.intensity))),
            seed=int(self.seed if self.seed is not None else random.randrange(1, 2**31)),
            phase=max(0, int(self.phase)),
        )


class DreamProvider(Protocol):
    def generate(self, request: DreamRequest) -> dict[str, Any]: ...


class MockProvider:
    def generate(self, request: DreamRequest) -> dict[str, Any]:
        request = request.normalized()
        return {
            "id": str(uuid.uuid4()),
            "provider": "mock",
            "seed": request.seed,
            "phase": request.phase,
            "status": "ready",
            "image": request.image_base64,
            "prompt": build_prompt(request),
        }


class ComfyUIProvider:
    def __init__(self, url: str, workflow_path: str):
        self.url = url.rstrip("/")
        self.workflow_path = Path(workflow_path)

    def generate(self, request: DreamRequest) -> dict[str, Any]:
        request = request.normalized()
        workflow = json.loads(self.workflow_path.read_text(encoding="utf-8"))
        prompt = build_prompt(request)
        workflow = inject_prompt_and_seed(workflow, prompt, request.seed)
        payload = json.dumps({"prompt": workflow, "client_id": "bildreise"}).encode()
        req = urllib.request.Request(
            f"{self.url}/prompt", data=payload,
            headers={"Content-Type": "application/json"}, method="POST"
        )
        with urllib.request.urlopen(req, timeout=30) as response:
            result = json.loads(response.read().decode())
        return {
            "id": result.get("prompt_id", str(uuid.uuid4())),
            "provider": "comfyui",
            "seed": request.seed,
            "phase": request.phase,
            "status": "queued",
            "prompt": prompt,
            "comfy": result,
        }


def build_prompt(request: DreamRequest) -> str:
    strength = "subtle" if request.intensity < .35 else "strong" if request.intensity > .7 else "moderate"
    preserve = "preserve the main subject silhouette and composition" if request.recognition > .5 else "allow bold changes while retaining recognizable identity"
    return f"{PROFILES[request.profile]}, {strength} transformation, {preserve}, phase {request.phase}, seed {request.seed}"


def inject_prompt_and_seed(workflow: dict[str, Any], prompt: str, seed: int) -> dict[str, Any]:
    """Best-effort injection for common ComfyUI API workflows.

    Custom workflows can keep their own nodes; the API remains valid even when
    no matching node IDs exist.
    """
    result = json.loads(json.dumps(workflow))
    for node in result.values():
        inputs = node.get("inputs", {}) if isinstance(node, dict) else {}
        for key in ("text", "prompt", "positive"):
            if key in inputs and isinstance(inputs[key], str):
                inputs[key] = prompt
        if "seed" in inputs:
            inputs["seed"] = seed
        if "noise_seed" in inputs:
            inputs["noise_seed"] = seed
    return result


def create_provider() -> DreamProvider:
    if os.getenv("DREAM_PROVIDER", "mock").lower() == "diffusers":
        from diffusers_provider import DiffusersProvider
        return DiffusersProvider()
    if os.getenv("DREAM_PROVIDER", "mock").lower() == "comfyui":
        return ComfyUIProvider(
            os.getenv("COMFY_URL", "http://127.0.0.1:8188"),
            os.getenv("COMFY_WORKFLOW", "server/workflows/flux-kontext-api.json"),
        )
    return MockProvider()
