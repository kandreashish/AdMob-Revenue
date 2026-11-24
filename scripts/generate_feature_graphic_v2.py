import os
import base64
import subprocess
from PIL import Image

def get_base64_img(path):
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode("utf-8")

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAYSTORE_DIR = os.path.join(ROOT_DIR, "playstore")

icon_b64 = get_base64_img(os.path.join(PLAYSTORE_DIR, "app_icon_512.png"))
p1_today = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_1_today.png"))
p2_apps = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_2_apps.png"))
p3_analytics = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_3_analytics.png"))
p4_detail = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_4_detail.png"))
p5_goal = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_5_goal.png"))

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>AdDash Feature Graphic - Red Edition</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;600;700;800;900&display=swap" rel="stylesheet">
<style>
  * {{
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    -webkit-font-smoothing: antialiased;
  }}

  body {{
    width: 1024px;
    height: 500px;
    background: #EDE8E5;
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }}

  .canvas {{
    width: 1004px;
    height: 480px;
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px;
  }}

  .card {{
    border-radius: 24px;
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    padding: 16px 12px 0 12px;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08), 0 2px 6px rgba(15, 23, 42, 0.04);
  }}

  /* Red Cards */
  .card-red {{
    background: linear-gradient(180deg, #D9252A 0%, #B81318 100%);
    color: #FFFFFF;
  }}

  .card-red::before {{
    content: '';
    position: absolute;
    width: 190px;
    height: 190px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0) 70%);
    top: -50px;
    right: -50px;
    pointer-events: none;
  }}

  .card-red::after {{
    content: '';
    position: absolute;
    width: 160px;
    height: 160px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(0, 0, 0, 0.12) 0%, rgba(0, 0, 0, 0) 70%);
    bottom: -30px;
    left: -40px;
    pointer-events: none;
  }}

  /* Light Cards */
  .card-light {{
    background: #FFFAFA;
    color: #0F172A;
    border: 1px solid rgba(226, 232, 240, 0.9);
  }}

  .card-light::before {{
    content: '';
    position: absolute;
    width: 180px;
    height: 180px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(220, 38, 38, 0.08) 0%, rgba(220, 38, 38, 0) 70%);
    top: 20px;
    right: -60px;
    pointer-events: none;
  }}

  .card-light::after {{
    content: '';
    position: absolute;
    width: 150px;
    height: 150px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(220, 38, 38, 0.06) 0%, rgba(220, 38, 38, 0) 70%);
    bottom: 30px;
    left: -40px;
    pointer-events: none;
  }}

  /* Typography */
  .headline {{
    font-size: 16.5px;
    font-weight: 900;
    line-height: 1.18;
    letter-spacing: -0.4px;
    margin-bottom: 5px;
    z-index: 2;
  }}

  .card-red .headline {{
    color: #FFFFFF;
  }}

  .card-light .headline {{
    color: #111827;
  }}

  .card-light .headline .hl-red {{
    color: #D32F2F;
    display: block;
  }}

  .subhead {{
    font-size: 9.5px;
    line-height: 1.25;
    font-weight: 500;
    margin-bottom: 10px;
    z-index: 2;
  }}

  .card-red .subhead {{
    color: rgba(255, 255, 255, 0.85);
  }}

  .card-light .subhead {{
    color: #64748B;
  }}

  /* Phone Mockup Container */
  .phone-container {{
    position: relative;
    width: 100%;
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: flex-start;
  }}

  .phone-mock {{
    width: 142px;
    height: auto;
    display: block;
    filter: drop-shadow(0 14px 28px rgba(0, 0, 0, 0.22));
    z-index: 5;
  }}

  /* Callout Pills & Badges */
  .callout-tag {{
    position: absolute;
    background: #FFFFFF;
    border-radius: 10px;
    padding: 5px 8px;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16), 0 1px 3px rgba(15, 23, 42, 0.08);
    z-index: 15;
    display: flex;
    flex-direction: column;
    pointer-events: none;
    border: 1px solid rgba(255, 255, 255, 0.95);
    white-space: nowrap;
  }}

  .tag-label {{
    font-size: 7.2px;
    font-weight: 700;
    color: #8E9BAE;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    line-height: 1;
    margin-bottom: 2px;
    display: flex;
    align-items: center;
    gap: 3px;
  }}

  .tag-label.red-alert {{
    color: #DC2626;
  }}

  .tag-value {{
    font-size: 10.2px;
    font-weight: 800;
    color: #0F172A;
    line-height: 1.1;
    letter-spacing: -0.2px;
  }}

  .tag-value.red-val {{
    color: #DC2626;
  }}

  /* Card 3 Pill Buttons */
  .pill-tag {{
    position: absolute;
    background: #FFFFFF;
    border-radius: 20px;
    padding: 3.5px 9px;
    box-shadow: 0 6px 14px rgba(0, 0, 0, 0.18);
    z-index: 15;
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 9.2px;
    font-weight: 800;
    color: #0F172A;
    pointer-events: none;
    border: 1px solid rgba(255, 255, 255, 0.95);
    white-space: nowrap;
  }}

  .pill-tag .icon {{
    font-size: 10.5px;
    line-height: 1;
  }}

  /* Card 1 Specifics */
  .card-1-top {{
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    z-index: 2;
  }}

  .card-1-icon {{
    width: 28px;
    height: 28px;
    border-radius: 7px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.85);
  }}

  .card-1-title {{
    font-size: 15px;
    font-weight: 800;
    color: #FFFFFF;
    letter-spacing: -0.3px;
  }}

  .card-1-hud {{
    width: 100%;
    background: rgba(15, 23, 42, 0.88);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.15);
    padding: 8px 10px;
    margin-bottom: 10px;
    box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.2), 0 8px 16px rgba(0, 0, 0, 0.25);
    z-index: 2;
  }}

  .hud-badge {{
    font-size: 8px;
    font-weight: 800;
    color: #10B981;
    letter-spacing: 0.6px;
    text-transform: uppercase;
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 2px;
  }}

  .hud-badge::before {{
    content: '';
    display: inline-block;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #10B981;
    box-shadow: 0 0 6px #10B981;
  }}

  .hud-earning {{
    font-size: 19px;
    font-weight: 900;
    color: #FFFFFF;
    letter-spacing: -0.4px;
    line-height: 1.1;
  }}

  .hud-sub {{
    font-size: 8px;
    font-weight: 600;
    color: #94A3B8;
    margin-top: 2px;
  }}

  .phone-tilted {{
    transform: rotate(4deg) translateX(10px) translateY(8px);
    width: 135px;
  }}

</style>
</head>
<body>

<div class="canvas">
  <!-- Card 1: Hero Desk HUD (Red) -->
  <div class="card card-red">
    <div class="card-1-top">
      <img src="{icon_b64}" class="card-1-icon" alt="AdDash">
      <div class="card-1-title">AdDash</div>
    </div>
    
    <div class="card-1-hud">
      <div class="hud-badge">● LIVE TODAY</div>
      <div class="hud-earning">$1,209.79</div>
      <div class="hud-sub">▲ $132.40 (12.3%) vs yesterday</div>
    </div>

    <div class="headline">Never miss a<br>single dollar</div>
    <div class="subhead">Live desk HUD with OLED burn-in protection</div>

    <div class="phone-container">
      <div class="callout-tag" style="top: 20px; left: -4px;">
        <div class="tag-label">SYNC SPEED</div>
        <div class="tag-value">60s Live HUD ⚡</div>
      </div>
      <img src="{p1_today}" class="phone-mock phone-tilted" alt="Today Screen">
    </div>
  </div>

  <!-- Card 2: Multi-App Portfolio (Light) -->
  <div class="card card-light">
    <div class="headline">Every app on<span class="hl-red">one single shelf.</span></div>
    <div class="subhead">Track impressions, eCPM and revenue across apps</div>

    <div class="phone-container">
      <!-- Callouts -->
      <div class="callout-tag" style="top: 20px; right: -6px;">
        <div class="tag-label">TOP EARNER</div>
        <div class="tag-value">Puzzle Quest $412</div>
      </div>

      <div class="callout-tag" style="top: 100px; left: -6px;">
        <div class="tag-label">PLATFORMS</div>
        <div class="tag-value">Android & iOS</div>
      </div>

      <div class="callout-tag" style="top: 180px; left: -4px;">
        <div class="tag-label">MATCH RATE</div>
        <div class="tag-value">96.4% Fill Rate</div>
      </div>

      <img src="{p2_apps}" class="phone-mock" alt="Apps Screen">
    </div>
  </div>

  <!-- Card 3: Metrics & Insights (Red) -->
  <div class="card card-red">
    <div class="headline">Every metric.<br>Every insight.</div>
    <div class="subhead">Revenue trends, eCPM curves & country stats</div>

    <div class="phone-container">
      <!-- Floating Pills -->
      <div class="pill-tag" style="top: 15px; left: -6px;">
        <span class="icon">📈</span> Revenue Trend
      </div>

      <div class="pill-tag" style="top: 55px; right: -6px;">
        <span class="icon">⚡</span> Live eCPM
      </div>

      <div class="pill-tag" style="top: 105px; left: -6px;">
        <span class="icon">🌍</span> Country Stats
      </div>

      <div class="pill-tag" style="top: 150px; right: -6px;">
        <span class="icon">⏱</span> Hourly Reports
      </div>

      <div class="pill-tag" style="top: 195px; left: -6px;">
        <span class="icon">🎯</span> Daily Targets
      </div>

      <div class="pill-tag" style="top: 235px; right: -6px;">
        <span class="icon">📱</span> Ad Formats
      </div>

      <img src="{p3_analytics}" class="phone-mock" alt="Analytics Screen">
    </div>
  </div>

  <!-- Card 4: Reports & Formats (Light) -->
  <div class="card card-light">
    <div class="headline">Hourly reports,<span class="hl-red">always at hand.</span></div>
    <div class="subhead">Break down earnings by ad format, country & hour</div>

    <div class="phone-container">
      <!-- Callouts -->
      <div class="callout-tag" style="top: 20px; left: -6px;">
        <div class="tag-label red-alert">▲ 7-DAY REVENUE</div>
        <div class="tag-value red-val">$2,731.06 (+9.8%)</div>
      </div>

      <div class="callout-tag" style="top: 100px; right: -6px;">
        <div class="tag-label">TOP AD FORMAT</div>
        <div class="tag-value">Rewarded 46%</div>
      </div>

      <div class="callout-tag" style="top: 180px; left: -6px;">
        <div class="tag-label">TOP TERRITORY</div>
        <div class="tag-value">United States 41%</div>
      </div>

      <img src="{p4_detail}" class="phone-mock" alt="Detail Screen">
    </div>
  </div>

  <!-- Card 5: Goals & Streaks (Red) -->
  <div class="card card-red">
    <div class="headline">Set daily goals?<br>Hit them in style.</div>
    <div class="subhead">Live progress ring, milestones & weekly streaks</div>

    <div class="phone-container">
      <!-- Callouts -->
      <div class="callout-tag" style="top: 20px; right: -6px;">
        <div class="tag-label">PROGRESS</div>
        <div class="tag-value">88% Reached 🎯</div>
      </div>

      <div class="callout-tag" style="top: 100px; left: -6px;">
        <div class="tag-label">MILESTONE</div>
        <div class="tag-value">7-Day Streak 🔥</div>
      </div>

      <div class="callout-tag" style="top: 180px; right: -6px;">
        <div class="tag-label">DAILY TARGET</div>
        <div class="tag-value">$1,375 Goal</div>
      </div>

      <img src="{p5_goal}" class="phone-mock" alt="Goal Screen">
    </div>
  </div>
</div>

</body>
</html>
"""

html_path = os.path.join(PLAYSTORE_DIR, "feature_graphic_v2.html")
with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Generated HTML template at: {html_path}")

output_png_2x = os.path.join(PLAYSTORE_DIR, "feature_graphic_v2_2x.png")
output_png = os.path.join(PLAYSTORE_DIR, "feature_graphic_v2.png")

chrome_cmd = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "--headless=new",
    "--disable-gpu",
    "--force-device-scale-factor=2",
    "--window-size=1024,500",
    f"--screenshot={output_png_2x}",
    html_path
]

print("Rendering with Google Chrome at 2x Retina...")
subprocess.run(chrome_cmd, check=True)

im = Image.open(output_png_2x)
print(f"Rendered 2x image size: {im.size}")
im_final = im.resize((1024, 500), Image.Resampling.LANCZOS)
im_final.save(output_png, "PNG", optimize=True)
print(f"Saved final Google Play Feature Graphic V2 (1024x500): {output_png}")
