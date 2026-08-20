#!/usr/bin/env python3
"""Pack Grove plaintext into Kotlin intArrayOf lines.

Mirror SEED_PHRASE / STREAM_LEN with grove/rind/GroveXor.kt.
"""

SEED_PHRASE = "kP9#wL2mQx7!"
STREAM_LEN = 23


def djb2(text: str) -> int:
    h = 5381
    for ch in text:
        h = ((h << 5) + h + ord(ch)) & 0xFFFFFFFF
    return h


def build_stream() -> list[int]:
    state = djb2(SEED_PHRASE)
    if state == 0:
        state = 0xA5A5A5A5
    out: list[int] = []
    for _ in range(STREAM_LEN):
        state = (18000 * (state & 0xFFFF) + (state >> 16)) & 0xFFFFFFFF
        out.append((state >> 8) & 0xFF)
    return out


def pack(value: str) -> list[int]:
    stream = build_stream()
    return [
        (byte ^ stream[i % STREAM_LEN] ^ ((i * 13) & 0xFF)) & 0xFF
        for i, byte in enumerate(value.encode("latin-1"))
    ]


def kotlin_line(name: str, values: list[int]) -> str:
    return f"val {name} = intArrayOf({', '.join(str(v) for v in values)})"


PLAINTEXT = {
    "CONFIG_ENDPOINT_BYTES": "https://fruitsplassh.com/config.php",
    "ATTRIBUTION_KEY_BYTES": "QZ3UTJNACJsip6GZu2NYMX",
    "MESSAGING_PROJECT_BYTES": "441976468944",
    "CHROME_VERSION_BYTES": "149.0.7742.118",
    "WEBKIT_VERSION_BYTES": "537.36",
}


if __name__ == "__main__":
    for key, value in PLAINTEXT.items():
        print(kotlin_line(key, pack(value)))
