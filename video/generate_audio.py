#!/usr/bin/env python3
"""
Generate natural neural voiceover (edge-tts), synthesized UI SFX,
sidechain-ducked ambient music, and frame-accurate Remotion timeline.json.
"""

import asyncio
import json
import math
import os
import re
import subprocess
import wave
from dataclasses import dataclass
from typing import List

import edge_tts
import numpy as np

VIDEO_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(VIDEO_DIR)
AUDIO_DIR = os.path.join(VIDEO_DIR, "public", "audio")
TIMELINE_JSON = os.path.join(VIDEO_DIR, "src", "timeline.json")
OUTPUT_SRT = os.path.join(ROOT_DIR, "playstore", "addash_explainer_16x9.srt")

FPS = 60
SAMPLE_RATE = 48000
VOICE = "en-US-AndrewMultilingualNeural"
RATE = "+2%"


@dataclass
class ChapterDef:
    id: str
    title: str
    badge: str
    narration: str
    min_duration: float


CHAPTERS: List[ChapterDef] = [
    ChapterDef(
        id="01_intro",
        title="Meet AdDash",
        badge="LIVE ADMOB REVENUE",
        narration=(
            "Checking your AdMob revenue shouldn't mean keeping a browser tab open and hitting refresh all day. "
            "Meet AdDash — the live, multi-platform AdMob dashboard built for app developers who want instant numbers at a glance."
        ),
        min_duration=15.0,
    ),
    ChapterDef(
        id="02_refresh",
        title="No More Website Refreshes",
        badge="AUTO-REFRESH ENGINE",
        narration=(
            "We all know the routine: open the AdMob website, wait for heavy scripts to load, log back in when your session expires, "
            "and mash refresh just to see if today's earnings moved. "
            "AdDash solves this completely. It connects directly to your AdMob account, caches your data locally for instant launch, "
            "and auto-refreshes in the background every thirty seconds, one minute, two minutes, or five minutes — zero manual reloads required."
        ),
        min_duration=29.0,
    ),
    ChapterDef(
        id="03_bubble",
        title="Always-On Floating Bubble",
        badge="ANDROID OVERLAY",
        narration=(
            "Need to stay in flow while watching your numbers? Turn on the AdDash Floating Bubble on Android. "
            "It stays visible on top of any app you're using, snaps cleanly to the edge of your screen, and updates live. "
            "The bubble even shifts color based on your daily goal progress — from red, to amber, to blue, and green the moment you hit one hundred percent. "
            "Customize its size and opacity, or drag it to the bottom target anytime to hide it."
        ),
        min_duration=32.0,
    ),
    ChapterDef(
        id="04_fullscreen",
        title="Fullscreen Dash for TVs & Screens",
        badge="TV & KIOSK HUD",
        narration=(
            "Want a dedicated display for your desk or studio wall? Switch into AdDash Fullscreen Mode. "
            "Designed for tablets, desktop monitors, smart TVs, and even a Raspberry Pi, this deep-navy landscape HUD stays awake "
            "with built-in OLED burn-in protection. "
            "Every time your live earnings tick upward, the counter bounces with a celebration burst of confetti. "
            "Plug in a monitor or mount a TV, and your revenue updates itself all day long."
        ),
        min_duration=34.0,
    ),
    ChapterDef(
        id="05_today_goals",
        title="Today Dashboard & Daily Goals",
        badge="REAL-TIME METRICS",
        narration=(
            "Inside the Today tab, you get an instant pulse on estimated earnings, live trend curves, eCPM, impressions, clicks, and match rate. "
            "Switch effortlessly between Today, Yesterday, Seven Days, This Month, or custom date ranges. "
            "Set a daily revenue target and track your progress ring alongside your multi-day streak."
        ),
        min_duration=26.0,
    ),
    ChapterDef(
        id="06_analytics_wallet",
        title="Apps, Analytics, Reports & Wallet",
        badge="COMPLETE SUITE",
        narration=(
            "Every iOS and Android app lives on one clean shelf. Dive into per-app performance, thirty and ninety-day analytics trends, "
            "top-earning countries, ad format breakdowns, and hourly reports. "
            "And when payout day approaches, the Wallet tab keeps your unpaid balance and full payout history crystal clear."
        ),
        min_duration=25.0,
    ),
    ChapterDef(
        id="07_outro",
        title="Stop Refreshing. Watch It Live.",
        badge="ANDROID • iOS • DESKTOP • PI",
        narration=(
            "Available across Android, iOS, macOS, Windows, Linux, and Raspberry Pi. "
            "Stop refreshing your browser. Download AdDash and watch your revenue live."
        ),
        min_duration=16.0,
    ),
]


def ffprobe_duration(path: str) -> float:
    out = subprocess.check_output(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", path],
        text=True,
    ).strip()
    return float(out)


def fmt_srt_time(seconds: float) -> str:
    ms = int(round(seconds * 1000))
    hrs = ms // 3600000
    ms %= 3600000
    mins = ms // 60000
    ms %= 60000
    secs = ms // 1000
    ms %= 1000
    return f"{hrs:02d}:{mins:02d}:{secs:02d},{ms:03d}"


def write_wav(path: str, stereo: np.ndarray) -> None:
    pcm = (np.clip(stereo, -0.95, 0.95) * 32767.0).astype(np.int16)
    with wave.open(path, "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm.tobytes())


def generate_ambient_pad(out_wav: str, duration: float) -> None:
    n_samples = int(duration * SAMPLE_RATE)
    t = np.linspace(0.0, duration, n_samples, endpoint=False, dtype=np.float32)
    chords = [
        [130.81, 196.00, 246.94, 293.66],  # Cmaj9
        [174.61, 220.00, 261.63, 329.63],  # Fmaj9
        [110.00, 164.81, 246.94, 261.63],  # Am9
        [98.00, 146.83, 196.00, 246.94],   # G6
    ]
    bar_len = 4.0
    sig = np.zeros(n_samples, dtype=np.float32)
    bar_samples = int(bar_len * SAMPLE_RATE)
    for i in range(int(math.ceil(duration / bar_len))):
        s = i * bar_samples
        e = min(n_samples, s + bar_samples)
        if s >= e:
            break
        seg_t = t[s:e] - (i * bar_len)
        env = np.sin(np.pi * np.clip(seg_t / bar_len, 0.0, 1.0)) ** 0.45
        bw = np.zeros(e - s, dtype=np.float32)
        for f in chords[i % len(chords)]:
            bw += 0.22 * np.sin(2.0 * np.pi * f * seg_t)
            bw += 0.08 * np.sin(2.0 * np.pi * (f * 1.003) * seg_t)
        sig[s:e] += bw * env
    delay = int(0.014 * SAMPLE_RATE)
    stereo = np.stack([sig, np.roll(sig, delay)], axis=1) * 0.48
    write_wav(out_wav, stereo)


def generate_sfx_files() -> None:
    # Soft transition whoosh (0.45s filtered noise swell)
    dur_w = 0.45
    n_w = int(dur_w * SAMPLE_RATE)
    rng = np.random.default_rng(7)
    noise = rng.normal(0.0, 1.0, n_w).astype(np.float32)
    env = np.sin(np.pi * np.linspace(0.0, 1.0, n_w, dtype=np.float32)) ** 2
    whoosh = np.stack([noise * env * 0.14, noise * env * 0.14], axis=1)
    raw_w = os.path.join(AUDIO_DIR, "whoosh_raw.wav")
    write_wav(raw_w, whoosh)
    subprocess.run(
        ["ffmpeg", "-y", "-v", "error", "-i", raw_w, "-af", "bandpass=f=480:width_type=h:w=320", os.path.join(AUDIO_DIR, "whoosh.wav")],
        check=True,
    )

    # Pleasant earnings chime (0.6s bell)
    dur_c = 0.65
    n_c = int(dur_c * SAMPLE_RATE)
    tc = np.linspace(0.0, dur_c, n_c, endpoint=False, dtype=np.float32)
    env_c = np.exp(-tc * 6.5)
    chime = (0.35 * np.sin(2 * np.pi * 880 * tc) + 0.25 * np.sin(2 * np.pi * 1318.5 * tc)) * env_c
    write_wav(os.path.join(AUDIO_DIR, "chime.wav"), np.stack([chime, chime], axis=1))


def parse_vtt_or_chunks(text: str, total_dur: float, start_offset: float) -> List[dict]:
    """Split chapter narration into clean 6-9 word phrase captions proportional to word count."""
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunk_words = words[i : i + 8]
        # Prefer breaking on punctuation if nearby
        for j in range(min(9, len(words) - i)):
            if words[i + j].endswith((".", ",", "—", "?")):
                chunk_words = words[i : i + j + 1]
                break
        chunks.append(" ".join(chunk_words))
        i += len(chunk_words)

    total_w = max(1, sum(len(c.split()) for c in chunks))
    res = []
    cur = start_offset
    for c in chunks:
        d = total_dur * (len(c.split()) / total_w)
        res.append(
            {
                "text": c,
                "startSec": round(cur, 3),
                "endSec": round(cur + d, 3),
                "startFrame": int(round(cur * FPS)),
                "endFrame": int(round((cur + d) * FPS)),
            }
        )
        cur += d
    return res


async def synth_all() -> None:
    os.makedirs(AUDIO_DIR, exist_ok=True)
    generate_sfx_files()

    padded_wavs: List[str] = []
    timeline_chapters = []
    srt_lines = []
    srt_idx = 1
    cursor = 0.0

    for ch in CHAPTERS:
        mp3_path = os.path.join(AUDIO_DIR, f"{ch.id}.mp3")
        raw_wav = os.path.join(AUDIO_DIR, f"{ch.id}_raw.wav")
        pad_wav = os.path.join(AUDIO_DIR, f"{ch.id}_pad.wav")

        communicate = edge_tts.Communicate(ch.narration, VOICE, rate=RATE)
        await communicate.save(mp3_path)

        subprocess.run(
            ["ffmpeg", "-y", "-v", "error", "-i", mp3_path, "-ar", str(SAMPLE_RATE), "-ac", "2", raw_wav],
            check=True,
        )
        speech_dur = ffprobe_duration(raw_wav)
        lead_in = 0.70
        tail = 1.15
        ch_dur = max(ch.min_duration, round(speech_dur + lead_in + tail, 2))

        subprocess.run(
            [
                "ffmpeg", "-y", "-v", "error",
                "-i", raw_wav,
                "-af", f"adelay={int(lead_in * 1000)}|{int(lead_in * 1000)},apad=whole_dur={ch_dur}",
                "-t", f"{ch_dur:.3f}",
                pad_wav,
            ],
            check=True,
        )
        padded_wavs.append(pad_wav)

        captions = parse_vtt_or_chunks(ch.narration, speech_dur, lead_in)
        for cap in captions:
            srt_lines.append(
                f"{srt_idx}\n{fmt_srt_time(cursor + cap['startSec'])} --> {fmt_srt_time(cursor + cap['endSec'])}\n{cap['text']}\n"
            )
            srt_idx += 1

        timeline_chapters.append(
            {
                "id": ch.id,
                "title": ch.title,
                "badge": ch.badge,
                "startSec": round(cursor, 3),
                "durationSec": ch_dur,
                "startFrame": int(round(cursor * FPS)),
                "durationFrames": int(round(ch_dur * FPS)),
                "captions": captions,
            }
        )
        cursor += ch_dur

    total_dur = cursor
    with open(OUTPUT_SRT, "w", encoding="utf-8") as f:
        f.write("\n".join(srt_lines))

    concat_txt = os.path.join(AUDIO_DIR, "concat.txt")
    with open(concat_txt, "w", encoding="utf-8") as f:
        for w in padded_wavs:
            f.write(f"file '{w}'\n")

    voice_all = os.path.join(AUDIO_DIR, "voice_all.wav")
    subprocess.run(
        ["ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", concat_txt, "-c", "copy", voice_all],
        check=True,
    )

    pad_raw = os.path.join(AUDIO_DIR, "ambient_pad.wav")
    generate_ambient_pad(pad_raw, total_dur)

    master_mp3 = os.path.join(AUDIO_DIR, "master.mp3")
    # Sidechain duck ambient pad under neural voiceover + loudnorm
    subprocess.run(
        [
            "ffmpeg", "-y", "-v", "error",
            "-i", voice_all,
            "-i", pad_raw,
            "-filter_complex",
            "[0:a]asplit=2[v1][v_sc];"
            "[1:a]lowpass=f=560,volume=0.14,afade=t=in:st=0:d=1.8,afade=t=out:st="
            f"{max(0.0, total_dur - 2.5):.2f}:d=2.5[bg_raw];"
            "[bg_raw][v_sc]sidechaincompress=threshold=0.02:ratio=6:attack=30:release=350[bg_ducked];"
            "[v1][bg_ducked]amix=inputs=2:duration=first:dropout_transition=2,loudnorm=I=-15:TP=-1.5:LRA=11[out]",
            "-map", "[out]",
            "-ar", str(SAMPLE_RATE), "-ac", "2", "-b:a", "192k",
            master_mp3,
        ],
        check=True,
    )

    timeline_obj = {
        "fps": FPS,
        "width": 1920,
        "height": 1080,
        "totalDurationSec": round(total_dur, 3),
        "totalFrames": sum(c["durationFrames"] for c in timeline_chapters),
        "chapters": timeline_chapters,
    }
    with open(TIMELINE_JSON, "w", encoding="utf-8") as f:
        json.dump(timeline_obj, f, indent=2)

    print(f"Generated neural voiceover + timeline: {total_dur:.2f}s ({timeline_obj['totalFrames']} frames)")


if __name__ == "__main__":
    asyncio.run(synth_all())
