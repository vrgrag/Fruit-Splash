#!/usr/bin/env python3
"""Pack Fruit Splash gray-flow secrets into Kotlin intArrayOf lines.

Codec is unique to this project (DJB2 + xorshift 7/9/8 + (i*13)).
Do not reuse SEED_PHRASE / STREAM_LEN from another game.
"""

SEED_PHRASE = "Hq8!bN4w_pT6z"
STREAM_LEN = 23


def build_stream() -> list[int]:
    h = 5381
    for ch in SEED_PHRASE:
        h = ((h << 5) + h + ord(ch)) & 0xFFFFFFFF

    state = h if h != 0 else 0xA5A5A5A5
    out: list[int] = []
    for _ in range(STREAM_LEN):
        state ^= (state << 7) & 0xFFFFFFFF
        state &= 0xFFFFFFFF
        state ^= state >> 9
        state &= 0xFFFFFFFF
        state ^= (state << 8) & 0xFFFFFFFF
        state &= 0xFFFFFFFF
        out.append((state >> 8) & 0xFF)
    return out


def pack(value: str) -> list[int]:
    stream = build_stream()
    return [
        (byte ^ stream[i % STREAM_LEN] ^ ((i * 13) & 0xFF)) & 0xFF
        for i, byte in enumerate(value.encode("latin-1"))
    ]


def kotlin_line(name: str, values: list[int]) -> str:
    chunks = []
    row = []
    for v in values:
        row.append(str(v))
        if len(row) == 12:
            chunks.append(", ".join(row))
            row = []
    if row:
        chunks.append(", ".join(row))
    inner = ",\n        ".join(chunks)
    return f"val {name} = intArrayOf(\n        {inner},\n    )"


PLAINTEXT = {
    "CONFIG_ENDPOINT_BYTES": "https://fruitsplassh.com/config.php",
    "ATTRIBUTION_KEY_BYTES": "QZ3UTJNACJsip6GZu2NYMX",
    "MESSAGING_PROJECT_BYTES": "441976468944",
    "CHROME_VERSION_BYTES": "149.0.7812.44",
    "WEBKIT_VERSION_BYTES": "537.36",
}


if __name__ == "__main__":
    for key, value in PLAINTEXT.items():
        print(kotlin_line(key, pack(value)))
        print()
