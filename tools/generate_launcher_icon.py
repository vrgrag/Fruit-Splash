"""Build adaptive launcher icons from assets/icon2.jpg.

Artwork is scaled to the largest square that fits any circular mask
(side = diameter / sqrt(2) ~ 70.7%) so the full illustration stays visible.
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "icon2.jpg"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
PREVIEW = ROOT / "build/icon_preview"
BG = (0x26, 0x03, 0x09, 255)
CANVAS = 512

# Largest inscribed square in a circle — entire artwork stays visible.
SCALE = 1.0 / math.sqrt(2)


def composite(scale: float) -> Image.Image:
    art = Image.open(SOURCE).convert("RGBA")
    side = max(1, round(CANVAS * scale))
    art = art.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), BG)
    offset = (CANVAS - side) // 2
    canvas.paste(art, (offset, offset), art)
    return canvas


def circle_preview(image: Image.Image) -> Image.Image:
    size = image.width
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    out = Image.new("RGBA", (size, size), BG)
    out.paste(image, (0, 0), mask)
    return out


def write_mipmaps(image: Image.Image) -> None:
    for folder, size in {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }.items():
        target = ROOT / "app/src/main/res" / folder
        target.mkdir(parents=True, exist_ok=True)
        scaled = image.resize((size, size), Image.Resampling.LANCZOS)
        scaled.save(target / "ic_launcher.png")
        scaled.save(target / "ic_launcher_round.png")


def main() -> None:
    if not SOURCE.is_file():
        raise SystemExit(f"Missing source icon: {SOURCE}")

    DRAWABLE.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)

    full = Image.open(SOURCE).convert("RGBA").resize(
        (CANVAS, CANVAS), Image.Resampling.LANCZOS
    )
    full.save(DRAWABLE / "icon_art.png")

    launcher = composite(SCALE)
    launcher.save(DRAWABLE / "icon_source.png")

    preview = circle_preview(launcher)
    preview.save(PREVIEW / "launcher_circle.png")

    write_mipmaps(launcher)
    inset_pct = (1.0 - SCALE) / 2.0 * 100.0
    print(f"Adaptive icon scale={SCALE:.4f} (~{inset_pct:.1f}% inset)")
    print(f"Preview: {PREVIEW / 'launcher_circle.png'}")


if __name__ == "__main__":
    main()
