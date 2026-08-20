"""One-shot Fruit Splash gray rebrand: nectar.* → grove.* themed names."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/fruitsplash/fruitsplashgame"
RES = ROOT / "app/src/main/res"

# Longest identifiers first so prefixes do not eat longer names.
IDENTIFIERS: list[tuple[str, str]] = [
    ("NectarLaunchActivity", "HarvestGate"),
    ("NectarOfflineActivity", "QuietCanopy"),
    ("NectarInviteActivity", "TrellisAsk"),
    ("NectarWebActivity", "CanopyShell"),
    ("NectarPushService", "GroveNoteService"),
    ("NectarSplashFace", "GroveSplashFace"),
    ("NectarInviteFace", "GroveAskFace"),
    ("NectarOfflineFace", "GroveQuietFace"),
    ("NectarGardenTheme", "GroveTheme"),
    ("NectarLoadingCaption", "GroveLoadingCaption"),
    ("rememberNectarDots", "rememberGroveDots"),
    ("NectarArtMetrics", "GroveArtMetrics"),
    ("NectarButtonText", "GroveButtonText"),
    ("NectarRawReply", "GroveRawReply"),
    ("NectarPalette", "GrovePalette"),
    ("NectarCard", "GroveCard"),
    ("NectarBand", "GroveBand"),
    ("NectarPill", "GrovePill"),
    ("NectarFill", "GroveFill"),
    ("NectarApp", "GroveApp"),
    ("NectarBox", "PulpVault"),
    ("NectarPush", "GroveNote"),
    ("NectarWarm", "WarmPip"),
    ("NectarXor", "GroveXor"),
    ("NectarBytes", "GroveBytes"),
    ("NectarBrand", "GroveMark"),
    ("NectarLegal", "GroveLegal"),
    ("NectarPost", "VinePost"),
    ("NectarAf", "VineAf"),
    ("NectarReach", "VineReach"),
    ("NectarChrome", "VineChrome"),
    ("NectarUrl", "LatchUrl"),
    ("NectarMode", "GroveLane"),
    ("NectarReply", "GroveReply"),
    ("NectarPulse", "GrovePulse"),
    ("NectarPaint", "GrovePaint"),
    ("NectarPin", "GrovePin"),
    ("NectarWeb", "CanopyShell"),
    ("NectarLaunch", "HarvestGate"),
]

PACKAGES: list[tuple[str, str]] = [
    ("com.fruitsplash.fruitsplashgame.nectar.box", "com.fruitsplash.fruitsplashgame.grove.crate"),
    ("com.fruitsplash.fruitsplashgame.nectar.net", "com.fruitsplash.fruitsplashgame.grove.vine"),
    ("com.fruitsplash.fruitsplashgame.nectar.mix", "com.fruitsplash.fruitsplashgame.grove.rind"),
    ("com.fruitsplash.fruitsplashgame.nectar.face", "com.fruitsplash.fruitsplashgame.grove.trellis"),
    ("com.fruitsplash.fruitsplashgame.nectar.gate", "com.fruitsplash.fruitsplashgame.grove.latch"),
    ("com.fruitsplash.fruitsplashgame.nectar.id", "com.fruitsplash.fruitsplashgame.grove.mark"),
    ("com.fruitsplash.fruitsplashgame.nectar.kind", "com.fruitsplash.fruitsplashgame.grove.lane"),
    ("com.fruitsplash.fruitsplashgame.nectar", "com.fruitsplash.fruitsplashgame.grove"),
    (".nectar.box.", ".grove.crate."),
    (".nectar.", ".grove."),
]

RESOURCES: list[tuple[str, str]] = [
    ("ic_nectar_flame", "ic_grove_ember"),
    ("nectar_flame_tint", "grove_flame_tint"),
    ("nectar_meadow", "grove_meadow"),
    ("nectar_splash_port", "grove_splash_port"),
    ("nectar_splash_land", "grove_splash_land"),
    ("nectar_invite_port", "grove_invite_port"),
    ("nectar_invite_land", "grove_invite_land"),
    ("nectar_quiet_port", "grove_quiet_port"),
    ("nectar_quiet_land", "grove_quiet_land"),
    ("nectarPill", "grovePill"),
    ("nectarBar", "groveBar"),
]

TEXT_SUFFIXES = {".kt", ".xml", ".kts", ".md", ".pro", ".py", ".toml", ".properties", ".txt"}

FILE_MOVES: list[tuple[Path, Path]] = [
    (JAVA / "nectar/NectarApp.kt", JAVA / "grove/GroveApp.kt"),
    (JAVA / "nectar/NectarLaunchActivity.kt", JAVA / "grove/HarvestGate.kt"),
    (JAVA / "nectar/NectarWebActivity.kt", JAVA / "grove/CanopyShell.kt"),
    (JAVA / "nectar/NectarInviteActivity.kt", JAVA / "grove/TrellisAsk.kt"),
    (JAVA / "nectar/NectarOfflineActivity.kt", JAVA / "grove/QuietCanopy.kt"),
    (JAVA / "nectar/box/NectarBox.kt", JAVA / "grove/crate/PulpVault.kt"),
    (JAVA / "nectar/box/NectarPush.kt", JAVA / "grove/crate/GroveNote.kt"),
    (JAVA / "nectar/box/NectarWarm.kt", JAVA / "grove/crate/WarmPip.kt"),
    (JAVA / "nectar/net/NectarPost.kt", JAVA / "grove/vine/VinePost.kt"),
    (JAVA / "nectar/net/NectarAf.kt", JAVA / "grove/vine/VineAf.kt"),
    (JAVA / "nectar/net/NectarReach.kt", JAVA / "grove/vine/VineReach.kt"),
    (JAVA / "nectar/net/NectarChrome.kt", JAVA / "grove/vine/VineChrome.kt"),
    (JAVA / "nectar/mix/NectarXor.kt", JAVA / "grove/rind/GroveXor.kt"),
    (JAVA / "nectar/mix/NectarBytes.kt", JAVA / "grove/rind/GroveBytes.kt"),
    (JAVA / "nectar/id/NectarBrand.kt", JAVA / "grove/mark/GroveMark.kt"),
    (JAVA / "nectar/id/NectarLegal.kt", JAVA / "grove/mark/GroveLegal.kt"),
    (JAVA / "nectar/gate/NectarUrl.kt", JAVA / "grove/latch/LatchUrl.kt"),
    (JAVA / "nectar/kind/NectarMode.kt", JAVA / "grove/lane/GroveLane.kt"),
    (JAVA / "nectar/kind/NectarReply.kt", JAVA / "grove/lane/GroveReply.kt"),
    (JAVA / "nectar/face/NectarFill.kt", JAVA / "grove/trellis/GroveFill.kt"),
    (JAVA / "nectar/face/NectarPaint.kt", JAVA / "grove/trellis/GrovePaint.kt"),
    (JAVA / "nectar/face/NectarPin.kt", JAVA / "grove/trellis/GrovePin.kt"),
    (JAVA / "nectar/face/NectarPulse.kt", JAVA / "grove/trellis/GrovePulse.kt"),
    (JAVA / "nectar/face/NectarInviteFace.kt", JAVA / "grove/trellis/GroveAskFace.kt"),
    (JAVA / "nectar/face/NectarSplashFace.kt", JAVA / "grove/trellis/GroveSplashFace.kt"),
]

RES_MOVES: list[tuple[Path, Path]] = [
    (RES / "drawable/ic_nectar_flame.xml", RES / "drawable/ic_grove_ember.xml"),
    (RES / "drawable-nodpi/nectar_splash_port.webp", RES / "drawable-nodpi/grove_splash_port.webp"),
    (RES / "drawable-nodpi/nectar_splash_land.webp", RES / "drawable-nodpi/grove_splash_land.webp"),
    (RES / "drawable-nodpi/nectar_invite_port.webp", RES / "drawable-nodpi/grove_invite_port.webp"),
    (RES / "drawable-nodpi/nectar_invite_land.webp", RES / "drawable-nodpi/grove_invite_land.webp"),
    (RES / "drawable-nodpi/nectar_quiet_port.webp", RES / "drawable-nodpi/grove_quiet_port.webp"),
    (RES / "drawable-nodpi/nectar_quiet_land.webp", RES / "drawable-nodpi/grove_quiet_land.webp"),
]

DOC_MOVES: list[tuple[Path, Path]] = [
    (ROOT / ".cursor/rules/nectar_pitfalls.md", ROOT / ".cursor/rules/grove_pitfalls.md"),
    (ROOT / ".cursor/rules/nectar_launch_flow.md", ROOT / ".cursor/rules/grove_launch_flow.md"),
    (ROOT / ".cursor/rules/nectar_webview_safe_area.md", ROOT / ".cursor/rules/grove_webview_safe_area.md"),
    (ROOT / ".cursor/rules/nectar_user_agent.md", ROOT / ".cursor/rules/grove_user_agent.md"),
    (ROOT / ".cursor/rules/nectar_gray_guide.md", ROOT / ".cursor/rules/grove_gray_guide.md"),
]


def rewrite(text: str) -> str:
    for old, new in IDENTIFIERS + PACKAGES + RESOURCES:
        text = text.replace(old, new)
    return text


def rewrite_tree() -> int:
    touched = 0
    skip_dirs = {".git", "build", ".gradle", "node_modules"}
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in skip_dirs for part in path.parts):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        if path.name == "rebrand_grove.py":
            continue
        original = path.read_text(encoding="utf-8")
        updated = rewrite(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")
            touched += 1
            print(f"edit  {path.relative_to(ROOT)}")
    return touched


def move_pairs(pairs: list[tuple[Path, Path]]) -> None:
    for src, dst in pairs:
        if not src.exists():
            print(f"skip  {src.relative_to(ROOT)} (missing)")
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if dst.exists():
            dst.unlink()
        shutil.move(str(src), str(dst))
        print(f"move  {src.relative_to(ROOT)} -> {dst.relative_to(ROOT)}")


def prune_empty(folder: Path) -> None:
    if not folder.exists():
        return
    for child in sorted(folder.rglob("*"), reverse=True):
        if child.is_dir() and not any(child.iterdir()):
            child.rmdir()
    if folder.exists() and folder.is_dir() and not any(folder.iterdir()):
        folder.rmdir()


def main() -> None:
    print("rewriting identifiers…")
    n = rewrite_tree()
    print(f"updated {n} files")
    print("moving kotlin…")
    move_pairs(FILE_MOVES)
    print("moving resources…")
    move_pairs(RES_MOVES)
    print("moving docs…")
    move_pairs(DOC_MOVES)
    prune_empty(JAVA / "nectar")
    print("done")


if __name__ == "__main__":
    main()
