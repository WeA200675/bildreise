"""Optional local GPU provider using Hugging Face Diffusers.

Install the AI requirements separately. The model is downloaded by Diffusers
to the configured Hugging Face cache and is never committed to this repo.
"""
from __future__ import annotations

import base64
import io
import os
from typing import Any

from dream_provider import DreamRequest, build_prompt


class DiffusersProvider:
    def __init__(self, model_id: str | None = None):
        self.model_id = model_id or os.getenv("DIFFUSERS_MODEL", "Tongyi-MAI/Z-Image-Turbo")
        self._pipe = None

    def _pipeline(self):
        if self._pipe is None:
            import torch
            from diffusers import ZImageImg2ImgPipeline

            dtype = torch.bfloat16 if torch.cuda.is_available() else torch.float32
            self._pipe = ZImageImg2ImgPipeline.from_pretrained(self.model_id, dtype=dtype)
            self._pipe.to("cuda" if torch.cuda.is_available() else "cpu")
        return self._pipe

    def generate(self, request: DreamRequest) -> dict[str, Any]:
        from PIL import Image
        import torch

        request = request.normalized()
        raw = base64.b64decode(request.image_base64)
        source = Image.open(io.BytesIO(raw)).convert("RGB")
        source.thumbnail((1024, 1024))
        pipe = self._pipeline()
        result = pipe(
            prompt=build_prompt(request),
            image=source,
            strength=max(.2, min(.9, request.intensity)),
            num_inference_steps=int(os.getenv("DIFFUSERS_STEPS", "8")),
            guidance_scale=0.0,
            generator=torch.Generator(device=pipe.device).manual_seed(request.seed),
        ).images[0]
        output = io.BytesIO()
        result.save(output, format="PNG", optimize=True)
        return {
            "id": f"diffusers-{request.seed}-{request.phase}",
            "provider": "diffusers",
            "seed": request.seed,
            "phase": request.phase,
            "status": "ready",
            "image": base64.b64encode(output.getvalue()).decode(),
            "prompt": build_prompt(request),
        }
