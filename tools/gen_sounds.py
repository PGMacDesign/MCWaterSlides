#!/usr/bin/env python3
"""Generate the mod's audio, reproducibly — synthesized from seeded noise (the repo is
original-content-only, so no downloaded samples). Pure-stdlib WAV synthesis, encoded to
.ogg via ffmpeg (brew's /opt/homebrew/bin/ffmpeg or PATH).

Emits, under assets/mcwaterslides/:
  sounds/ride_loop.ogg   seamless ~4s water-rush bed (the riding loop; pitched by speed)
  sounds/splash.ogg      splash-down impact (pool entry)
  sounds/swish.ogg       soft entry whoosh (ride start)
  sounds.json            events + subtitles
and merges subtitle strings into lang/en_us.json.
"""
import json
import math
import random
import shutil
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"
SR = 22050

rng = random.Random(20260712)


def one_pole_lp(samples, cutoff_hz):
    """One-pole lowpass — the water-shaping workhorse."""
    a = 1.0 - math.exp(-2.0 * math.pi * cutoff_hz / SR)
    y, out = 0.0, []
    for x in samples:
        y += a * (x - y)
        out.append(y)
    return out


def white(n):
    return [rng.uniform(-1.0, 1.0) for _ in range(n)]


def normalize(samples, peak=0.7):
    m = max(abs(s) for s in samples) or 1.0
    k = peak / m
    return [s * k for s in samples]


def water_bed(n):
    """Rushing water: a dull roar (heavily lowpassed noise) under a hissy band whose
    level breathes with two slow, incommensurate LFOs — reads as churn, not static."""
    roar = one_pole_lp(white(n), 320)
    hiss = one_pole_lp(white(n), 2600)
    out = []
    for i in range(n):
        t = i / SR
        breathe = 0.55 + 0.3 * math.sin(2 * math.pi * 0.7 * t) * math.sin(2 * math.pi * 1.9 * t + 1.3)
        out.append(roar[i] * 1.5 + hiss[i] * breathe * 0.8)
    return out


def gen_ride_loop():
    """Seamless loop: synth 5s, crossfade the last second onto the first, keep 4s."""
    n = SR * 5
    fade = SR
    raw = water_bed(n)
    body = raw[: SR * 4]
    tail = raw[SR * 4:]
    for i in range(fade):
        k = i / fade
        body[i] = body[i] * k + tail[i] * (1.0 - k)
    return normalize(body)


def gen_splash():
    """Splash-down: bright noise burst whose lowpass sweeps shut, with a few sine
    'plops' (falling pitch) in the first 200ms, all under an exponential decay."""
    n = int(SR * 0.9)
    noise = white(n)
    out, y = [], 0.0
    for i in range(n):
        t = i / n
        cutoff = 5200 * math.exp(-3.2 * t) + 240
        a = 1.0 - math.exp(-2.0 * math.pi * cutoff / SR)
        y += a * (noise[i] - y)
        env = math.exp(-4.5 * t) * min(1.0, i / (SR * 0.004))
        out.append(y * env * 2.2)
    for start, f0 in ((0.01, 240), (0.06, 190), (0.13, 150)):
        s0 = int(start * SR)
        dur = int(0.09 * SR)
        for i in range(dur):
            t = i / SR
            f = f0 * math.exp(-6.0 * t)
            if s0 + i < n:
                out[s0 + i] += 0.35 * math.sin(2 * math.pi * f * t * (1 + 8 * t)) * math.exp(-22 * t)
    return normalize(out, peak=0.8)


def gen_swish():
    """Entry whoosh: a mid-band noise swell under a raised-cosine envelope."""
    n = int(SR * 0.6)
    band = one_pole_lp(white(n), 1400)
    out = []
    for i in range(n):
        env = 0.5 * (1.0 - math.cos(2 * math.pi * i / n))
        out.append(band[i] * env * 2.0)
    return normalize(out, peak=0.55)


def write_ogg(samples, dest: Path):
    """Encode MONO Ogg Vorbis — Minecraft only attenuates positional audio if it's mono.
    oggenc (brew: vorbis-tools) handles mono natively; ffmpeg's builtin vorbis encoder
    is stereo-only, and Homebrew ffmpeg often lacks libvorbis."""
    oggenc = shutil.which("oggenc")
    if not oggenc:
        raise SystemExit("oggenc not found — `brew install vorbis-tools`")
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = Path(tmp.name)
    with wave.open(str(wav_path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples)
        w.writeframes(frames)
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([oggenc, "-Q", "-q", "4", "-o", str(dest), str(wav_path)], check=True)
    wav_path.unlink()


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def gen_data():
    sounds = RES / f"assets/{MOD}/sounds"
    write_ogg(gen_ride_loop(), sounds / "ride_loop.ogg")
    write_ogg(gen_splash(), sounds / "splash.ogg")
    write_ogg(gen_swish(), sounds / "swish.ogg")

    write_json(RES / f"assets/{MOD}/sounds.json", {
        "ride_loop": {"sounds": [f"{MOD}:ride_loop"], "subtitle": f"subtitles.{MOD}.ride_loop"},
        "splash": {"sounds": [f"{MOD}:splash"], "subtitle": f"subtitles.{MOD}.splash"},
        "swish": {"sounds": [f"{MOD}:swish"], "subtitle": f"subtitles.{MOD}.swish"},
    })

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"subtitles.{MOD}.ride_loop"] = "Water rushes"
    lang[f"subtitles.{MOD}.splash"] = "Rider splashes down"
    lang[f"subtitles.{MOD}.swish"] = "Slide whooshes"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print(f"Generated sounds under {RES}")
