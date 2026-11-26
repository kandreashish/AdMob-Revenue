#!/usr/bin/env python3
"""
Generate a 3-minute (180s), 1920x1080 (16:9), 30fps explainer video for AdDash.

Outputs:
  - playstore/addash_explainer_16x9.mp4
  - playstore/addash_explainer_16x9.srt
"""

import base64
import json
import math
import os
import random
import shutil
import subprocess
import sys
import wave
from dataclasses import dataclass
from typing import Callable, List, Tuple

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAYSTORE_DIR = os.path.join(ROOT_DIR, "playstore")
WORK_DIR = os.path.join(ROOT_DIR, "build", "explainer_video")
ASSETS_DIR = os.path.join(WORK_DIR, "assets")
AUDIO_DIR = os.path.join(WORK_DIR, "audio")

OUTPUT_MP4 = os.path.join(PLAYSTORE_DIR, "addash_explainer_16x9.mp4")
OUTPUT_SRT = os.path.join(PLAYSTORE_DIR, "addash_explainer_16x9.srt")

CHROME_BIN = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
WIDTH, HEIGHT = 1920, 1080
FPS = 30
SAMPLE_RATE = 48000
VOICE_NAME = "Samantha"
VOICE_RATE = 168


@dataclass
class Chapter:
    key: str
    title: str
    badge: str
    narration: str
    min_duration: float


CHAPTERS: List[Chapter] = [
    Chapter(
        key="01_intro",
        title="Meet AdDash",
        badge="LIVE ADMOB REVENUE",
        narration=(
            "Checking your AdMob revenue shouldn't mean keeping a browser tab open and hitting refresh all day. "
            "Meet AdDash — the live, multi-platform AdMob dashboard built for app developers who want instant numbers at a glance."
        ),
        min_duration=15.0,
    ),
    Chapter(
        key="02_refresh",
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
    Chapter(
        key="03_bubble",
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
    Chapter(
        key="04_fullscreen",
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
    Chapter(
        key="05_today_goals",
        title="Today Dashboard & Daily Goals",
        badge="REAL-TIME METRICS",
        narration=(
            "Inside the Today tab, you get an instant pulse on estimated earnings, live trend curves, eCPM, impressions, clicks, and match rate. "
            "Switch effortlessly between Today, Yesterday, Seven Days, This Month, or custom date ranges. "
            "Set a daily revenue target and track your progress ring alongside your multi-day streak."
        ),
        min_duration=26.0,
    ),
    Chapter(
        key="06_analytics_wallet",
        title="Apps, Analytics, Reports & Wallet",
        badge="COMPLETE SUITE",
        narration=(
            "Every iOS and Android app lives on one clean shelf. Dive into per-app performance, thirty and ninety-day analytics trends, "
            "top-earning countries, ad format breakdowns, and hourly reports. "
            "And when payout day approaches, the Wallet tab keeps your unpaid balance and full payout history crystal clear."
        ),
        min_duration=25.0,
    ),
    Chapter(
        key="07_outro",
        title="Stop Refreshing. Watch It Live.",
        badge="ANDROID • iOS • DESKTOP • PI",
        narration=(
            "Available across Android, iOS, macOS, Windows, Linux, and Raspberry Pi. "
            "Stop refreshing your browser. Download AdDash and watch your revenue live."
        ),
        min_duration=16.0,
    ),
]


def ensure_dirs() -> None:
    os.makedirs(ASSETS_DIR, exist_ok=True)
    os.makedirs(AUDIO_DIR, exist_ok=True)


def b64_img(path: str) -> str:
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode("utf-8")


def render_html_to_png(html_str: str, out_png: str, w: int, h: int, scale: int = 1) -> None:
    html_path = out_png.replace(".png", ".html")
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_str)
    cmd = [
        CHROME_BIN,
        "--headless=new",
        "--disable-gpu",
        "--hide-scrollbars",
        "--default-background-color=00000000",
        f"--force-device-scale-factor={scale}",
        f"--window-size={w},{h}",
        f"--screenshot={out_png}",
        html_path,
    ]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if scale != 1:
        im = Image.open(out_png)
        if im.size != (w, h):
            im = im.resize((w, h), Image.Resampling.LANCZOS)
            im.save(out_png, "PNG")


def ffprobe_duration(path: str) -> float:
    out = subprocess.check_output(
        [
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            path,
        ],
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


def split_sentences(text: str) -> List[str]:
    parts = []
    cur = []
    for token in text.split(" "):
        cur.append(token)
        if token.endswith((".", "!", "?")):
            parts.append(" ".join(cur))
            cur = []
    if cur:
        parts.append(" ".join(cur))
    return parts


def build_audio_and_subtitles() -> Tuple[str, List[Tuple[Chapter, float, float]]]:
    """Generate per-chapter TTS, pad to chapter durations, mix warm ambient pad, return master WAV and timeline."""
    timeline: List[Tuple[Chapter, float, float]] = []
    padded_wavs: List[str] = []
    srt_entries: List[str] = []
    srt_idx = 1
    cursor = 0.0

    for idx, ch in enumerate(CHAPTERS):
        aiff_path = os.path.join(AUDIO_DIR, f"{ch.key}.aiff")
        raw_wav = os.path.join(AUDIO_DIR, f"{ch.key}_raw.wav")
        pad_wav = os.path.join(AUDIO_DIR, f"{ch.key}_pad.wav")

        subprocess.run(
            ["say", "-v", VOICE_NAME, "-r", str(VOICE_RATE), "-o", aiff_path, ch.narration],
            check=True,
        )
        subprocess.run(
            [
                "ffmpeg", "-y", "-v", "error",
                "-i", aiff_path,
                "-ar", str(SAMPLE_RATE), "-ac", "2",
                raw_wav,
            ],
            check=True,
        )

        speech_dur = ffprobe_duration(raw_wav)
        lead_in = 0.75
        tail = 1.15
        ch_dur = max(ch.min_duration, round(speech_dur + lead_in + tail, 2))

        # Place speech at +lead_in inside a ch_dur stereo buffer
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

        # Build SRT cues proportional to sentence word count
        sentences = split_sentences(ch.narration)
        total_words = sum(len(s.split()) for s in sentences)
        s_cursor = cursor + lead_in
        for s in sentences:
            s_dur = speech_dur * (len(s.split()) / max(1, total_words))
            srt_entries.append(
                f"{srt_idx}\n{fmt_srt_time(s_cursor)} --> {fmt_srt_time(s_cursor + s_dur)}\n{s}\n"
            )
            srt_idx += 1
            s_cursor += s_dur

        timeline.append((ch, cursor, ch_dur))
        cursor += ch_dur

    total_dur = cursor

    # Write SRT file
    with open(OUTPUT_SRT, "w", encoding="utf-8") as f:
        f.write("\n".join(srt_entries))

    # Concat all chapter voiceover WAVs
    concat_txt = os.path.join(AUDIO_DIR, "concat.txt")
    with open(concat_txt, "w", encoding="utf-8") as f:
        for w in padded_wavs:
            f.write(f"file '{w}'\n")

    voice_all = os.path.join(AUDIO_DIR, "voice_all.wav")
    subprocess.run(
        [
            "ffmpeg", "-y", "-v", "error",
            "-f", "concat", "-safe", "0", "-i", concat_txt,
            "-c", "copy", voice_all,
        ],
        check=True,
    )

    # Synthesize warm ambient chord pad (Cmaj9 -> Fmaj9 -> Am9 -> G6)
    pad_raw = os.path.join(AUDIO_DIR, "ambient_pad.wav")
    generate_ambient_pad(pad_raw, total_dur)

    # Mix voiceover + ambient pad (-23 dB) + loudnorm
    master_wav = os.path.join(AUDIO_DIR, "master.wav")
    subprocess.run(
        [
            "ffmpeg", "-y", "-v", "error",
            "-i", voice_all,
            "-i", pad_raw,
            "-filter_complex",
            "[0:a]acompressor=threshold=-18dB:ratio=2.5:attack=10:release=120[v];"
            "[1:a]lowpass=f=550,volume=0.075,afade=t=in:st=0:d=2.0,afade=t=out:st="
            f"{max(0.0, total_dur - 3.0):.2f}:d=3.0[bg];"
            "[v][bg]amix=inputs=2:duration=first:dropout_transition=2,loudnorm=I=-16:TP=-1.5:LRA=11[out]",
            "-map", "[out]",
            "-ar", str(SAMPLE_RATE), "-ac", "2",
            master_wav,
        ],
        check=True,
    )

    return master_wav, timeline


def generate_ambient_pad(out_wav: str, duration: float) -> None:
    """Synthesize a soft, warm ambient chord pad using numpy."""
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
        start = i * bar_samples
        end = min(n_samples, start + bar_samples)
        if start >= end:
            break
        seg_t = t[start:end] - (i * bar_len)
        freqs = chords[i % len(chords)]
        # Smooth crossfade envelope per chord bar
        env = np.sin(np.pi * np.clip(seg_t / bar_len, 0.0, 1.0)) ** 0.45
        bar_wave = np.zeros(end - start, dtype=np.float32)
        for f in freqs:
            bar_wave += 0.22 * np.sin(2.0 * np.pi * f * seg_t)
            bar_wave += 0.08 * np.sin(2.0 * np.pi * (f * 1.003) * seg_t)
        sig[start:end] += bar_wave * env

    # Subtle stereo width via slight delay on R channel
    delay = int(0.014 * SAMPLE_RATE)
    left = sig
    right = np.roll(sig, delay)
    stereo = np.stack([left, right], axis=1)
    stereo = np.clip(stereo * 0.5, -0.95, 0.95)
    pcm = (stereo * 32767.0).astype(np.int16)

    with wave.open(out_wav, "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm.tobytes())


def render_static_chrome_assets() -> dict:
    """Render crisp HTML/CSS UI cards at 1920x1080 or component sizes using headless Chrome."""
    assets = {}

    # 1. Browser "Old Way" AdMob console card (760x580)
    browser_html = """<!DOCTYPE html><html><head><meta charset="utf-8">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
      *{margin:0;padding:0;box-sizing:border-box;font-family:'Inter',sans-serif;}
      body{width:760px;height:580px;background:transparent;display:flex;align-items:center;justify-content:center;}
      .win{width:740px;height:560px;background:#1E2430;border-radius:18px;border:1px solid rgba(255,255,255,0.12);overflow:hidden;box-shadow:0 24px 60px rgba(0,0,0,0.45);}
      .bar{height:48px;background:#282F3D;display:flex;align-items:center;padding:0 16px;gap:10px;border-bottom:1px solid rgba(255,255,255,0.08);}
      .dots{display:flex;gap:7px;}
      .dot{width:12px;height:12px;border-radius:50%;}
      .url{flex:1;height:30px;background:#171C26;border-radius:8px;display:flex;align-items:center;padding:0 12px;color:#94A3B8;font-size:13px;justify-content:space-between;}
      .tag{background:rgba(239,68,68,0.18);color:#FCA5A5;padding:3px 10px;border-radius:999px;font-size:11px;font-weight:700;}
      .body{padding:28px;color:#E2E8F0;display:flex;flex-direction:column;gap:18px;height:512px;justify-content:center;align-items:center;text-align:center;}
      .skeleton{width:100%;height:76px;background:rgba(255,255,255,0.05);border-radius:12px;border:1px dashed rgba(255,255,255,0.1);}
      .warn{background:rgba(239,68,68,0.14);border:1px solid rgba(239,68,68,0.35);color:#FECACA;padding:14px 20px;border-radius:12px;font-size:15px;font-weight:600;width:100%;}
      .keys{display:flex;gap:10px;align-items:center;margin-top:6px;}
      .kbd{background:#0F172A;border:1px solid #475569;border-bottom-width:3px;padding:6px 14px;border-radius:8px;font-weight:700;font-size:14px;color:#F8FAFC;}
    </style></head><body>
    <div class="win">
      <div class="bar">
        <div class="dots"><div class="dot" style="background:#EF4444"></div><div class="dot" style="background:#F59E0B"></div><div class="dot" style="background:#10B981"></div></div>
        <div class="url"><span>https://apps.admob.com/v2/home</span><span class="tag">STALE DATA</span></div>
      </div>
      <div class="body">
        <div style="font-size:20px;font-weight:700;color:#F8FAFC;">Manual Browser Refresh Cycle</div>
        <div class="warn">Session timed out or data stale — Reload page to fetch latest earnings</div>
        <div class="skeleton"></div>
        <div class="skeleton" style="height:120px;"></div>
        <div class="keys"><span class="kbd">⌘ Cmd</span><span style="color:#94A3B8;font-weight:700;">+</span><span class="kbd">R</span><span style="color:#94A3B8;font-size:14px;margin-left:8px;">Repeated 20× a day</span></div>
      </div>
    </div></body></html>"""
    p_browser = os.path.join(ASSETS_DIR, "browser_old.png")
    render_html_to_png(browser_html, p_browser, 760, 580, scale=2)
    assets["browser_old"] = p_browser

    # 2. Platform badges card for Outro & Fullscreen (1200x140)
    platforms_html = """<!DOCTYPE html><html><head><meta charset="utf-8">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@600;700;800&display=swap" rel="stylesheet">
    <style>
      *{margin:0;padding:0;box-sizing:border-box;font-family:'Plus Jakarta Sans',sans-serif;}
      body{width:1200px;height:130px;background:transparent;display:flex;align-items:center;justify-content:center;gap:22px;}
      .pill{background:rgba(255,255,255,0.08);border:1px solid rgba(255,255,255,0.18);border-radius:18px;padding:16px 28px;color:#FFF;display:flex;flex-direction:column;align-items:center;gap:4px;min-width:220px;}
      .t{font-size:22px;font-weight:800;letter-spacing:-0.3px;}
      .s{font-size:13px;font-weight:600;color:#94A3B8;letter-spacing:0.6px;text-transform:uppercase;}
    </style></head><body>
      <div class="pill"><div class="t">Android</div><div class="s">App + Floating Bubble</div></div>
      <div class="pill"><div class="t">iOS</div><div class="s">iPhone &amp; iPad</div></div>
      <div class="pill"><div class="t">Desktop</div><div class="s">macOS · Win · Linux</div></div>
      <div class="pill"><div class="t">Raspberry Pi</div><div class="s">ARM64 Kiosk .deb</div></div>
    </body></html>"""
    p_platforms = os.path.join(ASSETS_DIR, "platforms.png")
    render_html_to_png(platforms_html, p_platforms, 1200, 130, scale=2)
    assets["platforms"] = p_platforms

    return assets


def load_rgba(path: str) -> Image.Image:
    return Image.open(path).convert("RGBA")


def fit_height(im: Image.Image, target_h: int) -> Image.Image:
    w, h = im.size
    new_w = int(round(w * (target_h / h)))
    return im.resize((new_w, target_h), Image.Resampling.LANCZOS)


def round_corners(im: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", im.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), im.size], radius=radius, fill=255)
    out = im.copy()
    out.putalpha(mask)
    return out


def ease_out_cubic(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return 1.0 - (1.0 - x) ** 3


def ease_in_out(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return 3 * x * x - 2 * x * x * x


def get_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]
    for c in candidates:
        if os.path.exists(c):
            try:
                return ImageFont.truetype(c, size=size)
            except Exception:
                pass
    return ImageFont.load_default()


def make_gradient_bg(c1: Tuple[int, int, int], c2: Tuple[int, int, int], accent: Tuple[int, int, int] = None) -> Image.Image:
    arr = np.zeros((HEIGHT, WIDTH, 3), dtype=np.uint8)
    y = np.linspace(0.0, 1.0, HEIGHT, dtype=np.float32)[:, None]
    for ch_i in range(3):
        arr[:, :, ch_i] = np.clip((1.0 - y) * c1[ch_i] + y * c2[ch_i], 0, 255).astype(np.uint8)
    bg = Image.fromarray(arr, mode="RGB").convert("RGBA")
    if accent is not None:
        glow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        gd.ellipse([WIDTH // 2 - 520, -240, WIDTH // 2 + 520, 620], fill=(*accent, 55))
        glow = glow.filter(ImageFilter.GaussianBlur(110))
        bg = Image.alpha_composite(bg, glow)
    return bg


def draw_header_bar(canvas: Image.Image, ch: Chapter, t_local: float) -> None:
    draw = ImageDraw.Draw(canvas)
    f_badge = get_font(20, bold=True)
    f_title = get_font(46, bold=True)

    alpha = int(255 * ease_out_cubic(min(1.0, t_local / 0.45)))
    # Top-left badge pill
    badge_text = ch.badge
    bbox = draw.textbbox((0, 0), badge_text, font=f_badge)
    bw = bbox[2] - bbox[0] + 34
    draw.rounded_rectangle([84, 52, 84 + bw, 90], radius=19, fill=(217, 37, 42, min(230, alpha)))
    draw.text((101, 59), badge_text, font=f_badge, fill=(255, 255, 255, alpha))
    draw.text((84, 102), ch.title, font=f_title, fill=(248, 250, 252, alpha))


def draw_progress_bar(canvas: Image.Image, global_t: float, total_dur: float) -> None:
    draw = ImageDraw.Draw(canvas)
    prog = max(0.0, min(1.0, global_t / total_dur))
    draw.rectangle([0, HEIGHT - 6, WIDTH, HEIGHT], fill=(255, 255, 255, 30))
    draw.rectangle([0, HEIGHT - 6, int(WIDTH * prog), HEIGHT], fill=(217, 37, 42, 230))


def paste_centered(dst: Image.Image, src: Image.Image, cx: int, cy: int) -> None:
    x = int(cx - src.width // 2)
    y = int(cy - src.height // 2)
    dst.alpha_composite(src, (x, y))


def main() -> None:
    ensure_dirs()
    print("[1/4] Generating voiceover, ambient pad, and SRT subtitles...")
    master_wav, timeline = build_audio_and_subtitles()
    total_dur = sum(d for _, _, d in timeline)
    print(f"      Total timeline duration: {total_dur:.2f}s ({total_dur/60:.2f} min)")

    print("[2/4] Rendering high-DPI Chrome scene assets...")
    chrome_assets = render_static_chrome_assets()

    # Load real app images
    app_icon = load_rgba(os.path.join(PLAYSTORE_DIR, "app_icon_512.png")).resize((190, 190), Image.Resampling.LANCZOS)
    p_today = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_1_today.png")), 770)
    p_apps = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_2_apps.png")), 730)
    p_analytics = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_3_analytics.png")), 730)
    p_detail = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_4_detail.png")), 730)
    p_reports = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_4_reports.png")), 730)
    p_goal = fit_height(load_rgba(os.path.join(PLAYSTORE_DIR, "phone_5_goal.png")), 770)

    # Crop phone screen from 07_wallet.png cleanly without blue background halo
    wallet_raw = load_rgba(os.path.join(PLAYSTORE_DIR, "screenshots", "07_wallet.png"))
    wallet_crop = round_corners(fit_height(wallet_raw.crop((136, 344, 944, 1886)), 730), 46)

    browser_old = fit_height(load_rgba(chrome_assets["browser_old"]), 560)
    platforms_img = fit_height(load_rgba(chrome_assets["platforms"]), 124)

    # Pre-build backgrounds
    bg_dark_red = make_gradient_bg((15, 23, 42), (26, 12, 18), accent=(217, 37, 42))
    bg_slate = make_gradient_bg((13, 19, 33), (22, 30, 48), accent=(59, 130, 246))
    bg_navy = make_gradient_bg((11, 15, 23), (22, 35, 58), accent=(129, 201, 149))

    f_hero = get_font(94, bold=True)
    f_sub = get_font(34, bold=False)
    f_card_t = get_font(30, bold=True)
    f_card_s = get_font(24, bold=False)
    f_big_num = get_font(124, bold=True)
    f_med = get_font(28, bold=True)
    f_small = get_font(22, bold=True)

    # Pre-generate deterministic confetti particles for Chapter 4
    rng = random.Random(42)
    confetti = [
        (
            rng.randint(360, 1560),
            rng.randint(280, 620),
            rng.uniform(-220, 220),
            rng.uniform(-320, -80),
            rng.choice([(129, 201, 149), (251, 188, 4), (138, 180, 248), (242, 139, 130), (255, 255, 255)]),
            rng.randint(8, 16),
        )
        for _ in range(70)
    ]

    print("[3/4] Compositing 1920x1080 @ 30fps video & encoding MP4...")
    ffmpeg_cmd = [
        "ffmpeg", "-y", "-v", "error",
        "-f", "rawvideo", "-pix_fmt", "bgr24",
        "-s", f"{WIDTH}x{HEIGHT}", "-r", str(FPS),
        "-i", "-",
        "-i", master_wav,
        "-c:v", "libx264", "-preset", "medium", "-crf", "18",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "192k",
        "-movflags", "+faststart",
        "-shortest",
        OUTPUT_MP4,
    ]
    proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE)

    for ch, start_t, ch_dur in timeline:
        n_frames = int(round(ch_dur * FPS))
        for f_idx in range(n_frames):
            t_local = f_idx / FPS
            global_t = start_t + t_local

            if ch.key == "01_intro":
                frame = bg_dark_red.copy()
                d = ImageDraw.Draw(frame)
                sc = 0.75 + 0.25 * ease_out_cubic(min(1.0, t_local / 0.6))
                icon_s = app_icon.resize((int(190 * sc), int(190 * sc)), Image.Resampling.LANCZOS)
                paste_centered(frame, icon_s, WIDTH // 2, 290)
                a = int(255 * ease_out_cubic(min(1.0, max(0.0, t_local - 0.2) / 0.5)))
                d.text((WIDTH // 2, 455), "AdDash", font=f_hero, fill=(255, 255, 255, a), anchor="mm")
                d.text(
                    (WIDTH // 2, 540),
                    "Live AdMob Revenue — Without Refreshing a Browser Tab",
                    font=f_sub,
                    fill=(203, 213, 225, a),
                    anchor="mm",
                )
                # Live hero pill
                pulse = 0.6 + 0.4 * math.sin(t_local * 4.0)
                d.rounded_rectangle([WIDTH // 2 - 230, 625, WIDTH // 2 + 230, 715], radius=45, fill=(22, 35, 58, 235), outline=(129, 201, 149, 180), width=2)
                d.ellipse([WIDTH // 2 - 185, 661, WIDTH // 2 - 167, 679], fill=(52, 211, 153, int(255 * pulse)))
                d.text((WIDTH // 2 + 12, 670), "LIVE  $1,209.79   ▲ +12.3%", font=f_med, fill=(248, 250, 252, a), anchor="mm")

                # 3 feature chips
                chips = ["Auto-Refresh (30s–5m)", "Always-On Bubble", "Fullscreen TV & Pi HUD"]
                for ci, label in enumerate(chips):
                    cx = 440 + ci * 520
                    d.rounded_rectangle([cx - 210, 810, cx + 210, 878], radius=16, fill=(38, 28, 36), outline=(120, 90, 98), width=2)
                    d.text((cx, 844), label, font=f_small, fill=(241, 245, 249), anchor="mm")

            elif ch.key == "02_refresh":
                frame = bg_slate.copy()
                draw_header_bar(frame, ch, t_local)
                d = ImageDraw.Draw(frame)

                # Left: Old browser console
                paste_centered(frame, browser_old, 500, 565)
                # Spinning reload ring on browser
                ang = (t_local * 260) % 360
                d.arc([465, 515, 535, 585], start=ang, end=ang + 250, fill=(239, 68, 68, 230), width=6)
                d.text((500, 910), "Manual F5 / Re-Login Loop", font=f_card_t, fill=(252, 165, 165), anchor="mm")

                # Right: AdDash phone + Auto-refresh selector
                slide = ease_out_cubic(min(1.0, t_local / 0.7))
                px = int(1460 + (1.0 - slide) * 160)
                paste_centered(frame, p_today, px, 585)

                # Floating Auto-Refresh card
                d.rounded_rectangle([945, 310, 1265, 585], radius=22, fill=(15, 23, 42, 235), outline=(52, 211, 153, 180), width=2)
                d.text((1105, 352), "AUTO-REFRESH", font=f_small, fill=(110, 231, 183), anchor="mm")
                intervals = ["30s", "1 min", "2 min", "5 min"]
                active_idx = int(t_local // 2.2) % len(intervals)
                for ii, iv in enumerate(intervals):
                    y0 = 392 + ii * 44
                    is_on = ii == active_idx
                    fill_c = (16, 185, 129) if is_on else (30, 41, 59)
                    d.rounded_rectangle([975, y0, 1235, y0 + 36], radius=10, fill=fill_c)
                    d.text((1105, y0 + 18), f"Every {iv}", font=f_small, fill=(255, 255, 255), anchor="mm")
                d.text((1460, 995), "Instant Cache + Background Sync", font=f_card_t, fill=(110, 231, 183), anchor="mm")

            elif ch.key == "03_bubble":
                frame = bg_dark_red.copy()
                draw_header_bar(frame, ch, t_local)
                d = ImageDraw.Draw(frame)

                # Left: Simulated Android screen with another app open
                phone_x0, phone_y0, phone_x1, phone_y1 = 230, 195, 670, 985
                d.rounded_rectangle([phone_x0, phone_y0, phone_x1, phone_y1], radius=46, fill=(18, 24, 38), outline=(148, 163, 184, 130), width=4)
                d.rounded_rectangle([phone_x0 + 14, phone_y0 + 14, phone_x1 - 14, phone_y1 - 14], radius=36, fill=(30, 41, 59))
                # Fake background app content (code/messages)
                d.text((phone_x0 + 42, phone_y0 + 60), "Any App You're Using", font=f_small, fill=(148, 163, 184))
                for li in range(8):
                    ly = phone_y0 + 120 + li * 80
                    d.rounded_rectangle([phone_x0 + 36, ly, phone_x1 - 36, ly + 56], radius=14, fill=(51, 65, 85, 180))

                # Animate bubble position (left edge -> right edge snap) & goal color cycle
                phase = (t_local / ch_dur)
                if phase < 0.25:
                    bx = phone_x0 + 68
                    by = int(340 + 140 * ease_in_out(phase / 0.25))
                    b_col = (217, 48, 37)  # Red <33%
                    amt = "$312.40"
                    g_lbl = "23% Goal"
                elif phase < 0.50:
                    p2 = ease_in_out((phase - 0.25) / 0.25)
                    bx = int((phone_x0 + 68) + p2 * ((phone_x1 - 68) - (phone_x0 + 68)))
                    by = 480
                    b_col = (227, 116, 0)  # Amber 33-65%
                    amt = "$684.10"
                    g_lbl = "50% Goal"
                elif phase < 0.75:
                    bx = phone_x1 - 68
                    by = int(480 - 110 * ease_in_out((phase - 0.50) / 0.25))
                    b_col = (51, 103, 214)  # Blue 66-99%
                    amt = "$1,209"
                    g_lbl = "88% Goal"
                else:
                    bx = phone_x1 - 68
                    by = 370
                    b_col = (30, 142, 62)   # Green >=100%
                    amt = "$1,412"
                    g_lbl = "103% Goal"

                # Size animation (36dp..96dp scaled)
                rad = int(50 + 12 * math.sin(t_local * 1.4))
                d.ellipse([bx - rad - 6, by - rad - 6, bx + rad + 6, by + rad + 6], fill=(0, 0, 0, 90))
                d.ellipse([bx - rad, by - rad, bx + rad, by + rad], fill=(*b_col, 245), outline=(255, 255, 255, 200), width=3)
                d.text((bx, by - 8), amt, font=get_font(22, bold=True), fill=(255, 255, 255), anchor="mm")
                d.text((bx, by + 16), g_lbl, font=get_font(13, bold=True), fill=(241, 245, 249), anchor="mm")

                # Bottom X dismiss target (drawn with crisp lines to avoid missing font glyphs)
                d.ellipse([450 - 30, 895, 450 + 30, 955], fill=(15, 23, 42), outline=(255, 255, 255), width=2)
                d.line([438, 913, 462, 937], fill=(255, 255, 255), width=4)
                d.line([462, 913, 438, 937], fill=(255, 255, 255), width=4)

                # Right side feature cards
                cards = [
                    ("Always On Top", "Floats over any app, snaps to screen edges, and remembers position."),
                    ("Goal-Aware Colors", "Red (<33%) → Amber (33–65%) → Blue (66–99%) → Green (100% Goal Hit)."),
                    ("Custom Size & Opacity", "Tune diameter from 36dp to 96dp and idle opacity from 20% to 100%."),
                    ("Auto-Refresh Sync", "Updates automatically while your screen is on. Tap to open AdDash."),
                ]
                for ci, (ct, cs) in enumerate(cards):
                    cy0 = 225 + ci * 185
                    d.rounded_rectangle([820, cy0, 1780, cy0 + 150], radius=22, fill=(32, 24, 34), outline=(92, 68, 78), width=2)
                    d.text((860, cy0 + 34), ct, font=f_card_t, fill=(255, 255, 255))
                    d.text((860, cy0 + 82), cs, font=f_card_s, fill=(203, 213, 225))

            elif ch.key == "04_fullscreen":
                frame = bg_navy.copy()
                draw_header_bar(frame, ch, t_local)
                d = ImageDraw.Draw(frame)

                # TV / Monitor Bezel
                tx0, ty0, tx1, ty1 = 220, 195, 1700, 890
                d.rounded_rectangle([tx0, ty0, tx1, ty1], radius=28, fill=(10, 14, 22), outline=(71, 85, 105), width=6)
                # Monitor stand
                d.rectangle([910, 890, 1010, 940], fill=(51, 65, 85))
                d.rounded_rectangle([760, 935, 1160, 955], radius=8, fill=(71, 85, 105))

                # Live earnings tick + bounce + confetti
                tick_t = t_local % 8.0
                if tick_t < 0.35:
                    bounce = 1.0 + 0.05 * math.sin((tick_t / 0.35) * math.pi)
                else:
                    bounce = 1.0
                steps_done = int(t_local // 8.0)
                revenue = 1209.79 + steps_done * 4.56

                d.ellipse([865, 272, 881, 288], fill=(129, 201, 149))
                d.text((975, 280), "LIVE · TODAY", font=f_small, fill=(129, 201, 149), anchor="mm")

                f_num = get_font(int(124 * bounce), bold=True)
                d.text((960, 445), f"${revenue:,.2f}", font=f_num, fill=(224, 224, 224), anchor="mm")
                d.text((960, 560), f"▲ ${132.40 + steps_done * 4.56:.2f} (12.3%) vs yesterday", font=f_med, fill=(129, 201, 149), anchor="mm")

                # 3 bottom HUD metrics
                hud_stats = [("eCPM", "$14.82"), ("Impressions", "81.6K"), ("Daily Goal", "88%")]
                for hi, (hl, hv) in enumerate(hud_stats):
                    hx = 610 + hi * 350
                    d.text((hx, 675), hl, font=f_small, fill=(154, 160, 166), anchor="mm")
                    d.text((hx, 720), hv, font=get_font(40, bold=True), fill=(232, 234, 237), anchor="mm")

                # Confetti burst for first 2.0s after each 8s tick
                if tick_t < 2.2 and t_local > 1.0:
                    for cx0, cy0, vx, vy, col, sz in confetti:
                        px = int(cx0 + vx * tick_t)
                        py = int(cy0 + vy * tick_t + 0.5 * 380 * (tick_t ** 2))
                        if tx0 + 20 < px < tx1 - 20 and ty0 + 20 < py < ty1 - 20:
                            alpha_c = int(255 * max(0.0, 1.0 - tick_t / 2.2))
                            d.rectangle([px, py, px + sz, py + sz // 2], fill=(*col, alpha_c))

                # Bottom badges
                d.text((960, 1005), "OLED Burn-In Safe  •  Always-On Landscape  •  Desktop Multi-Monitor  •  Raspberry Pi Kiosk", font=f_small, fill=(203, 213, 225), anchor="mm")

            elif ch.key == "05_today_goals":
                frame = bg_slate.copy()
                draw_header_bar(frame, ch, t_local)
                d = ImageDraw.Draw(frame)

                slide = ease_out_cubic(min(1.0, t_local / 0.7))
                paste_centered(frame, p_today, int(470 - (1.0 - slide) * 120), 590)
                paste_centered(frame, p_goal, int(1450 + (1.0 - slide) * 120), 590)

                mid_cards = [
                    ("Today & Horizons", "Today, Yesterday, 7d, Month & Custom"),
                    ("Core Ad Metrics", "eCPM, Impressions, Clicks & Match Rate"),
                    ("Daily Goal Ring", "Live progress % with 7-day streak"),
                ]
                for mi, (mt, ms) in enumerate(mid_cards):
                    my = 340 + mi * 170
                    d.rounded_rectangle([720, my, 1200, my + 130], radius=20, fill=(24, 34, 54), outline=(72, 92, 128), width=2)
                    d.text((960, my + 42), mt, font=f_card_t, fill=(255, 255, 255), anchor="mm")
                    d.text((960, my + 88), ms, font=get_font(20), fill=(203, 213, 225), anchor="mm")

            elif ch.key == "06_analytics_wallet":
                frame = bg_dark_red.copy()
                draw_header_bar(frame, ch, t_local)
                d = ImageDraw.Draw(frame)

                # Smoothly highlight 4 screens across the chapter
                phones = [
                    (p_apps, "All Apps Shelf"),
                    (p_analytics, "7/30/90d Analytics"),
                    (p_reports, "Hourly & Format Reports"),
                    (wallet_crop, "Wallet & Payouts"),
                ]
                active = min(3, int((t_local / ch_dur) * 4))
                for pi, (pim, plbl) in enumerate(phones):
                    cx = 300 + pi * 440
                    cy = 560 if pi != active else 535
                    paste_centered(frame, pim, cx, cy)
                    col = (255, 255, 255) if pi == active else (148, 163, 184)
                    d.text((cx, 975), plbl, font=f_small, fill=col, anchor="mm")

            elif ch.key == "07_outro":
                frame = bg_dark_red.copy()
                d = ImageDraw.Draw(frame)
                paste_centered(frame, app_icon, WIDTH // 2, 265)
                d.text((WIDTH // 2, 430), "Stop Refreshing. Watch It Live.", font=get_font(76, bold=True), fill=(255, 255, 255), anchor="mm")
                d.text((WIDTH // 2, 515), "AdDash — Live AdMob Revenue, Floating Bubble & Fullscreen TV HUD", font=f_sub, fill=(203, 213, 225), anchor="mm")
                paste_centered(frame, platforms_img, WIDTH // 2, 695)
                d.text((WIDTH // 2, 855), "github.com/kandreashish/AdMob-Revenue", font=f_med, fill=(252, 165, 165), anchor="mm")

            draw_progress_bar(frame, global_t, total_dur)
            bgr = cv2.cvtColor(np.array(frame.convert("RGB")), cv2.COLOR_RGB2BGR)
            proc.stdin.write(bgr.tobytes())

    proc.stdin.close()
    ret = proc.wait()
    if ret != 0:
        raise RuntimeError(f"ffmpeg exited with code {ret}")

    print(f"[4/4] Done! Video saved to: {OUTPUT_MP4}")
    print(f"      Subtitles saved to: {OUTPUT_SRT}")


if __name__ == "__main__":
    main()
