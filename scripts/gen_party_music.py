#!/usr/bin/env python3
"""
Generate assets/re_plushie_party/sounds/party_music.ogg

An ORIGINAL synthesized party loop (no copyrighted material). The length is
an exact multiple of the plushie dance cycle (74 ticks = 3.7 s), so the
server-side loop (which restarts the sound every lengthTicks) stays on beat:

    beat = 0.4625 s  (one dance beat = 74 ticks / 8 beats)
    loop = 32 beats  = 4 dance cycles = 14.8 s

The user can replace this file with any track they own; the mod parses the
ogg length at runtime, so any duration keeps working.
"""
import numpy as np
import soundfile as sf
import subprocess, os, struct

SR = 44100
BEAT = 0.4625                    # dance beat (74 ticks / 8)
BAR = BEAT * 4                   # 1.85 s
BARS = 8                         # Am F C G x2
DUR = BAR * BARS                 # 14.8 s = 4 dance cycles
N = int(round(DUR * SR))
mix = np.zeros(N, dtype=np.float64)

def place(buf, start_s, sig):
    i0 = int(round(start_s * SR))
    if i0 >= N:
        return
    n = min(len(sig), N - i0)
    buf[i0:i0 + n] += sig[:n]

def env_exp(n, decay):
    return np.exp(-np.arange(n) / SR * decay)

def kick(amp=1.0):
    n = int(0.26 * SR); t = np.arange(n) / SR
    f = 140 * np.exp(-t * 30) + 46
    ph = 2 * np.pi * np.cumsum(f) / SR
    return amp * np.sin(ph) * np.exp(-t * 12)

def clap(amp=0.4):
    n = int(0.22 * SR)
    noise = np.diff(np.random.default_rng(7).standard_normal(n), prepend=0)
    e = env_exp(n, 30) + 0.35 * env_exp(n, 90)
    return amp * noise * e

def hat(amp=0.18, open_=False):
    n = int((0.16 if open_ else 0.045) * SR)
    rng = np.random.default_rng(11 if open_ else 3)
    noise = np.diff(rng.standard_normal(n), prepend=0)
    return amp * noise * env_exp(n, 24 if open_ else 95)

def splash(amp=0.22):
    n = int(0.5 * SR)
    noise = np.diff(np.random.default_rng(21).standard_normal(n), prepend=0)
    return amp * noise * env_exp(n, 9)

def saw(freq, n, detune=0.0, duty_saw=True):
    t = np.arange(n) / SR
    f = freq * (1 + detune)
    ph = (f * t) % 1.0
    return (2 * ph - 1) if duty_saw else np.where(ph < 0.5, 2 * ph - 1, 1 - 2 * ph)

def bass_note(freq, dur, amp=0.5, duck=0.3):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = saw(freq, n, +0.004) + saw(freq, n, -0.004) + 0.4 * saw(freq / 2, n)
    e = np.minimum(1, (t / 0.06) ** 0.5) * (1 - duck) + duck * np.exp(-t * 16)
    e *= np.minimum(1, (dur - t) / 0.03).clip(0, 1)
    return amp * w * e / 2.4

def pluck(freq, dur, amp=0.2):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = saw(freq, n, duty_saw=False) + 0.3 * saw(freq * 2, n)
    return amp * w * np.exp(-t * 10)

NOTE = {nm: 440.0 * 2 ** ((i - 9) / 12) for i, nm in enumerate(
    ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}
def nf(name):
    return NOTE[name[:-1]] * 2 ** (int(name[-1]) - 4)

PROG = ["A", "F", "C", "G"] * 2
ROOT2 = {"A": "A1", "F": "F1", "C": "C2", "G": "G1"}
ARPS = {"A": ["A3", "C4", "E4", "A4"], "F": ["F3", "A3", "C4", "F4"],
        "C": ["C4", "E4", "G4", "C5"], "G": ["G3", "B3", "D4", "G4"]}
# Original catchy motif, 8 eighth-notes per bar (None = rest)
MOTIF = {
    "A": ["E4", None, "A4", "G4", "E4", None, "C4", "D4"],
    "F": ["C4", None, "F4", "E4", "C4", None, "A3", "C4"],
    "C": ["G4", None, "C5", "G4", "E4", "G4", None, "E4"],
    "G": ["D4", None, "G4", "B4", "D5", "B4", "G4", None],
}

for bar in range(BARS):
    t0 = bar * BAR
    chord = PROG[bar]
    # drums: four on the floor
    for beat in range(4):
        place(mix, t0 + beat * BEAT, kick(0.85))
    for beat in (1, 3):
        place(mix, t0 + beat * BEAT, clap(0.38))
    for e8 in range(8):
        place(mix, t0 + e8 * BEAT / 2, hat(0.14 if e8 % 2 else 0.09, open_=(e8 == 7)))
    if bar == 0:
        place(mix, t0, splash())                     # masks the loop seam
    # bass: bouncy offbeat octave feel
    for e8, off in enumerate((0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5)):
        f = nf(ROOT2[chord]) * (2 if e8 % 2 else 1)
        place(mix, t0 + off * BEAT, bass_note(f, 0.38, 0.46, duck=0.35))
    # pluck arpeggio
    for e8 in range(8):
        place(mix, t0 + e8 * BEAT / 2, pluck(nf(ARPS[chord][e8 % 4]), 0.28, 0.15))
    # motif lead
    for i, name in enumerate(MOTIF[chord]):
        if name:
            place(mix, t0 + i * BEAT / 2, pluck(nf(name), BEAT / 2 * 1.6, 0.24))

mix = np.tanh(mix * 1.1)
mix *= 0.82 / max(1e-9, np.max(np.abs(mix)))

out = "/home/z/my-project/src/main/resources/assets/re_plushie_party/sounds/party_music.ogg"
os.makedirs(os.path.dirname(out), exist_ok=True)
sf.write("/tmp/party_music.wav", mix.astype(np.float32), SR, subtype="FLOAT")
r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", "/tmp/party_music.wav",
                    "-ac", "1", "-c:a", "libvorbis", "-qscale:a", "4", out],
                   capture_output=True, text=True)
if r.returncode != 0:
    raise RuntimeError(f"ffmpeg failed: {r.stderr}")

# verify duration
data = open(out, "rb").read()
j = data.rfind(b"OggS")
granule = struct.unpack("<q", data[j + 6:j + 14])[0]
i = data.find(b"\x01vorbis")
rate = struct.unpack("<I", data[i + 12:i + 16])[0]
print(f"party_music.ogg: {granule / rate:.3f}s ({granule / rate / BEAT:.2f} beats), {os.path.getsize(out)} B")
