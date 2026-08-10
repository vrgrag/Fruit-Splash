"""Prepare Fruit Splash art for Android without modifying source assets."""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "assets"
SPRITES = ROOT / "app/src/main/assets/sprites"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
SOURCE = ROOT / "app/src/main/assets/source"
RAW = ROOT / "app/src/main/res/raw"

CATEGORIES = {
    "fruits": ("Apple_", "Grapes_", "Rare_", "Magic_Seeds"),
    "player": ("Fruit_Gardener",),
    "pests": ("Garden_Pests",),
    "world": ("Garden_", "Fruit_Garden_Trees", "Magic_Fruit_Fountain", "Restoration_Portal", "Upgraded_"),
    "effects": ("Fruit_Energy", "Fruit_Juice", "Fruit_Shell", "Fruit_Splash"),
    "rewards": ("Fruit_Reward",),
}

PIECE_NAMES = {
    "Apple_Orange_Strawberry_Lemon_Set": ["apple", "orange", "strawberry", "lemon"],
    "Grapes_Watermelon_Cherries_Mango_Set": ["grapes", "watermelon", "cherries", "mango"],
    "Rare_Magic_Fruits_Set": ["starfruit", "dragonfruit", "moonberry", "golden_apple"],
    "Garden_Pests_Set_01": ["beetle", "caterpillar", "wasp", "snail"],
    "Garden_Obstacles_Set": ["rock", "stump", "thorn_bush", "mud"],
    "Garden_Decorations_Set": ["bench", "lantern", "fence", "sign"],
    "Garden_Plants_Set": ["flower", "sprout", "bush", "herbs"],
    "Fruit_Garden_Trees_Set": ["apple_tree", "orange_tree", "berry_tree", "lemon_tree"],
    "Magic_Seeds_Set": ["sun_seed", "rain_seed", "moon_seed", "rainbow_seed"],
}

def safe(stem: str) -> str:
    stem = stem.replace("_asset", "")
    return re.sub(r"[^a-z0-9_]", "_", stem.lower())

def category(stem: str) -> str:
    return next((cat for cat, prefixes in CATEGORIES.items() if stem.startswith(prefixes)), "world")

def trim(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    box = image.getchannel("A").point(lambda p: 255 if p > 8 else 0).getbbox()
    return image.crop(box) if box else image

def main() -> None:
    for d in (SPRITES, DRAWABLE, SOURCE, RAW):
        d.mkdir(parents=True, exist_ok=True)
    metadata = []
    for source in sorted(SRC.iterdir()):
        if not source.is_file():
            continue
        shutil.copy2(source, SOURCE / source.name)
        stem = source.stem.replace("_asset", "")
        android_name = safe(stem)
        if stem == "Icon":
            shutil.copy2(source, DRAWABLE / "icon_source.png")
            icon = Image.open(source).convert("RGBA")
            for folder, size in {
                "mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
                "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192,
            }.items():
                target = ROOT / "app/src/main/res" / folder
                target.mkdir(parents=True, exist_ok=True)
                scaled = icon.resize((size, size), Image.Resampling.LANCZOS)
                scaled.save(target / "ic_launcher.png")
                scaled.save(target / "ic_launcher_round.png")
            continue
        if stem in {"Vertical_Loading_Screen", "Horizontal_Loading_Screen"} or "Background" in stem or stem == "Game_Name":
            shutil.copy2(source, DRAWABLE / f"{android_name}{source.suffix.lower()}")
            continue
        image = Image.open(source).convert("RGBA")
        names = PIECE_NAMES.get(stem)
        pieces = []
        if names:
            width = image.width // 4
            for i, name in enumerate(names):
                pieces.append((name, trim(image.crop((i * width, 0, image.width if i == 3 else (i + 1) * width, image.height)))))
        else:
            pieces.append((android_name, trim(image)))
        folder = SPRITES / category(stem)
        folder.mkdir(parents=True, exist_ok=True)
        for name, piece in pieces:
            output = folder / f"{name}.png"
            piece.save(output, optimize=True)
            metadata.append({"name": name, "category": folder.name, "file": str(output.relative_to(SPRITES)).replace("\\", "/"), "width": piece.width, "height": piece.height})
    for sound in sorted((ROOT / "sounds").glob("*.mp3")):
        shutil.copy2(sound, RAW / f"{safe(sound.stem)}.mp3")
    (SPRITES / "sprites.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(f"Prepared {len(metadata)} sprites, {len(list(RAW.glob('*.mp3')))} sounds")

if __name__ == "__main__":
    main()
