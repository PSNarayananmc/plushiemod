#!/usr/bin/env python3
"""
Plushie Party asset generation.

IMPORTANT NOTE ON MUSIC:
The "Gangnam Style" disc ships with an ORIGINAL synthesized dance track
(original composition in a similar energetic K-pop/EDM party style).
The actual PSY recording is copyrighted and is NOT included. The user can
swap in their own legally-obtained copy by replacing:
    assets/plushie_party/sounds/disc/gangnam_style.ogg
"""
import numpy as np
import soundfile as sf
import struct, zlib, os, shutil

SRC = "/home/z/my-project/addon_extracted/plushie_party_rp"
P = "/home/z/my-project/plushie_party/src/main/resources/assets/plushie_party"
os.makedirs(f"{P}/textures/entity", exist_ok=True)
os.makedirs(f"{P}/textures/item", exist_ok=True)
os.makedirs(f"{P}/sounds/disc", exist_ok=True)
os.makedirs(f"{P}/sounds/toy", exist_ok=True)

# ---------- 1. Copy textures & sounds from the addon ----------
shutil.copy(f"{SRC}/textures/re_plushie_party/creeper_plushie.png", f"{P}/textures/entity/creeper_plushie.png")
shutil.copy(f"{SRC}/pack_icon.png", f"{P}/icon.png")
for i in (1, 2, 3):
    shutil.copy(f"{SRC}/sounds/re_plushie_party/toy_use{i}.ogg", f"{P}/sounds/toy/toy_use{i}.ogg")
print("textures + toy sounds copied")

# ---------- 2. Silent party_music.ogg (replaceable via resource pack, like Bedrock) ----------
sf.write(f"{P}/sounds/party_music.ogg", np.zeros(44100, dtype=np.float32), 44100,
         format="OGG", subtype="VORBIS")
print("silent party_music.ogg written")

# ---------- 3. Original synthesized dance track for the disc ----------
if os.path.exists(f"{P}/sounds/disc/gangnam_style.ogg"):
    print("song already generated - skipping synthesis")
else:
    pass
SR = 44100
BPM = 132.0
BEAT = 60.0 / BPM          # 0.4545s
BAR = BEAT * 4.0           # 1.818s
BARS = 48
DUR = BAR * BARS
N = int(DUR * SR)
t_all = np.arange(N) / SR
mix = np.zeros(N, dtype=np.float64)

def place(buf, start_s, sig):
    i0 = int(start_s * SR)
    if i0 >= N: return
    n = min(len(sig), N - i0)
    buf[i0:i0 + n] += sig[:n]

def env_exp(n, decay):
    return np.exp(-np.arange(n) / SR * decay)

def kick(amp=1.0):
    n = int(0.28 * SR); t = np.arange(n) / SR
    f = 150 * np.exp(-t * 28) + 44
    ph = 2 * np.pi * np.cumsum(f) / SR
    return amp * np.sin(ph) * np.exp(-t * 11)

def clap(amp=0.45):
    n = int(0.25 * SR)
    noise = np.random.default_rng(7).standard_normal(n)
    e = env_exp(n, 32) + 0.35 * env_exp(n, 90)
    # simple band emphasis via differentiation (crude but effective)
    noise = np.diff(noise, prepend=0)
    return amp * noise * e

def hat(amp=0.2, open_=False):
    n = int((0.18 if open_ else 0.05) * SR)
    rng = np.random.default_rng(11 if open_ else 3)
    noise = np.diff(rng.standard_normal(n), prepend=0)
    return amp * noise * env_exp(n, 26 if open_ else 90)

def saw(freq, n, detune=0.0, duty_saw=True):
    t = np.arange(n) / SR
    f = freq * (1 + detune)
    ph = (f * t) % 1.0
    if duty_saw:
        w = 2 * ph - 1
    else:
        w = np.where(ph < 0.5, 2 * ph - 1, 1 - 2 * ph)  # triangle-ish
    return w

def bass_note(freq, dur, amp=0.5, duck=0.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = saw(freq, n, +0.004) + saw(freq, n, -0.004) + 0.4 * saw(freq / 2, n)
    # duck (sidechain): volume dips right at note start then recovers
    e = np.minimum(1, (t / 0.09) ** 0.5) * (1 - duck) + duck * np.exp(-t * 14)
    e *= np.minimum(1, (dur - t) / 0.03).clip(0, 1)
    return amp * w * e / 2.4

def lead_note(freq, dur, amp=0.32):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = (saw(freq, n, +0.006) + saw(freq, n, -0.006) + 0.5 * saw(freq, n, 0, duty_saw=False))
    e = np.minimum(1, t / 0.012) * np.exp(-t * 3.2)
    e *= np.minimum(1, (dur - t) / 0.05).clip(0, 1)
    return amp * w * e

def pluck(freq, dur, amp=0.22):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = saw(freq, n, duty_saw=False) + 0.3 * saw(freq * 2, n)
    e = np.exp(-t * 9)
    return amp * w * e

def pad_chord(freqs, dur, amp=0.1):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = np.zeros(n)
    for f in freqs:
        w += saw(f, n, +0.003, duty_saw=False) + saw(f, n, -0.003, duty_saw=False)
    e = np.minimum(1, t / 0.4) * np.minimum(1, (dur - t) / 0.8).clip(0, 1)
    return amp * w / len(freqs) * e

NOTE = {n: 440.0 * 2 ** ((i - 9) / 12) for i, n in enumerate(
    ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}
def nf(name):  # e.g. nf("A2")
    return NOTE[name[:-1]] * 2 ** (int(name[-1]) - 4)

# Original chord/riff design: Am - F - C - G, catchy syncopated pentatonic hook
PROG = ["A", "F", "C", "G"]                      # one chord per bar
ROOT2 = {"A": "A1", "F": "F1", "C": "C2", "G": "G1"}
CHORDS = {"A": ["A3", "C4", "E4"], "F": ["F3", "A3", "C4"], "C": ["C4", "E4", "G4"], "G": ["G3", "B3", "D4"]}
# Original hook melody (16 sixteenths per bar), A-minor pentatonic. None = rest.
HOOK = [
    ["A4", None, "E4", None, "G4", None, "A4", None, "C5", None, "A4", None, "G4", "E4", "G4", None],
    ["F4", None, "C4", None, "E4", None, "F4", None, "A4", None, "F4", None, "E4", "C4", "E4", None],
    ["E4", None, "G4", None, "C5", None, "G4", None, "E5", None, "C5", None, "G4", "E4", "G4", None],
    ["D4", None, "G4", None, "B4", None, "G4", None, "D5", None, "B4", None, "A4", "G4", "D4", None],
]
BASS_RHYTHM = [0, 0.75, 1.5, 2.25, 3, 3.5]       # syncopated bass hits within a bar

def bar_index(b):
    return b * BAR

SONG_DONE = os.path.exists(f"{P}/sounds/disc/gangnam_style.ogg")

for bar in range(BARS if not SONG_DONE else 0):
    t0 = bar_index(bar)
    chord = PROG[bar % 4]
    section = bar // 8                            # 0..5
    full = section in (2, 4, 6)
    # Drums
    for beat in range(4):
        place(mix, t0 + beat * BEAT, kick(0.9 if full else 0.75))
    if section >= 1:
        for beat in (1, 3):
            place(mix, t0 + beat * BEAT, clap(0.4 if full else 0.3))
        for s16 in range(8):
            place(mix, t0 + s16 * BEAT / 2, hat(0.16 if s16 % 2 else 0.1, open_=(full and s16 in (3, 7))))
    # Bass
    if section >= 1:
        for off in BASS_RHYTHM:
            place(mix, t0 + off * BEAT, bass_note(nf(ROOT2[chord]), 0.4, 0.5 if full else 0.42, duck=0.35))
    # Hook lead
    if full:
        for i, name in enumerate(HOOK[bar % 4]):
            if name:
                place(mix, t0 + i * BEAT / 4, lead_note(nf(name), BEAT / 4 * 1.8, 0.3))
    # Pluck arpeggio (build + verse)
    if section in (1, 3):
        tones = CHORDS[chord]
        for s16 in range(8):
            place(mix, t0 + s16 * BEAT / 2, pluck(nf(tones[s16 % 3]) * 2, 0.3, 0.16))
    # Pad in break + final
    if section in (5, 6):
        place(mix, t0, pad_chord([nf(x) for x in CHORDS[chord]], BAR, 0.12))

# Master: soft clip + normalize
if SONG_DONE:
    print(f"keeping existing song: {os.path.getsize(f'{P}/sounds/disc/gangnam_style.ogg')//1024} KB")
else:
    mix = np.tanh(mix * 1.15)
    mix *= 0.85 / max(1e-9, np.max(np.abs(mix)))
    # NOTE: libsndfile's vorbis writer segfaults on long buffers in this env,
    # so write a WAV first and transcode with ffmpeg (mono, ogg vorbis).
    sf.write("/tmp/gangnam_style.wav", mix.astype(np.float32), SR, subtype="FLOAT")
    print(f"wav written: {DUR:.1f}s")
    import subprocess
    r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", "/tmp/gangnam_style.wav",
                        "-ac", "1", "-c:a", "libvorbis", "-qscale:a", "5",
                        f"{P}/sounds/disc/gangnam_style.ogg"], capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"ffmpeg failed: {r.stderr}")
    print(f"gangnam_style.ogg written: {DUR:.1f}s, {os.path.getsize(f'{P}/sounds/disc/gangnam_style.ogg')//1024} KB")

# ---------- 4. Music disc item texture (16x16 pixel art, original design) ----------
def png_chunk(tag, data):
    c = struct.pack(">I", len(data)) + tag + data
    return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

def write_png(path, w, h, pixels):
    raw = b"".join(b"\x00" + b"".join(bytes(v) for v in pixels[y]) for y in range(h))
    png = (b"\x89PNG\r\n\x1a\n"
           + png_chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
           + png_chunk(b"IDAT", zlib.compress(raw))
           + png_chunk(b"IEND", b""))
    open(path, "wb").write(png)

# disc: dark body, teal+lime band pattern, hole in center
pal = {
    ".": (0, 0, 0, 0),
    "k": (18, 18, 22, 255),   # near-black vinyl
    "d": (34, 36, 44, 255),   # dark ring
    "g": (70, 74, 88, 255),   # groove highlight
    "t": (64, 220, 200, 255), # teal band
    "l": (170, 230, 90, 255), # lime band
    "w": (225, 230, 235, 255),# center ring
    "h": (10, 10, 12, 255),   # hole
}
rows = [
    "......kkkkkk....",
    "....kkddddddkk..",
    "...kddggggggdk..",
    "..kdggkkkkkkgdk.",
    ".kdggkttllttkgdk",
    ".kdgkttllllttkdk",
    "kdgkkllwwwwllkdgk"[:16],
    "kdgklwwhhhwwlkdgk"[:16],
    "kdgklwwhhhwwlkdgk"[:16],
    "kdgkkllwwwwllkdgk"[:16],
    ".kdgkttllllttkdk",
    ".kdggkttllttkgdk",
    "..kdggkkkkkkgdk.",
    "...kddggggggdk..",
    "....kkddddddkk..",
    "......kkkkkk....",
]
px = [[pal[c] for c in row] for row in rows]
write_png(f"{P}/textures/item/gangnam_style_disc.png", 16, 16, px)
print("disc texture written")

# ---------- 5. verify all files ----------
for root, _, files in os.walk(P):
    for f in files:
        fp = os.path.join(root, f)
        print(f"{os.path.relpath(fp, P):50s} {os.path.getsize(fp):>8d} B")
