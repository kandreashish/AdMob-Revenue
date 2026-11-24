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
p4_reports = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_4_reports.png"))
p5_goal = get_base64_img(os.path.join(PLAYSTORE_DIR, "phone_5_goal.png"))

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>AdDash Feature Graphic</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@600;700;800&family=Caveat:wght@700&display=swap" rel="stylesheet">
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
    background: #EAEFF5;
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }}

  .canvas {{
    width: 996px;
    height: 476px;
    background: #FFFFFF;
    border-radius: 26px;
    box-shadow: 0 12px 36px rgba(15, 23, 42, 0.07), 0 2px 6px rgba(15, 23, 42, 0.03);
    padding: 13px 16px 13px 16px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    border: 1px solid rgba(226, 232, 240, 0.95);
  }}

  /* Top Bar */
  .header {{
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 44px;
    padding: 0 4px;
  }}

  .header-left {{
    display: flex;
    align-items: center;
    gap: 12px;
  }}

  .app-icon {{
    width: 42px;
    height: 42px;
    border-radius: 11px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.85);
  }}

  .brand-title {{
    font-size: 23px;
    font-weight: 800;
    color: #0F172A;
    letter-spacing: -0.5px;
    line-height: 1;
  }}

  .header-center {{
    display: flex;
    flex-direction: column;
    justify-content: center;
    margin-left: 12px;
  }}

  .tagline-main {{
    font-size: 13.5px;
    font-weight: 700;
    color: #1E293B;
    letter-spacing: -0.2px;
  }}

  .tagline-main .highlight {{
    color: #2563EB;
    font-weight: 800;
  }}

  .tagline-sub {{
    font-size: 11px;
    font-weight: 600;
    color: #64748B;
    margin-top: 1px;
  }}

  .header-right {{
    display: flex;
    align-items: center;
    gap: 8px;
  }}

  .badge-btn {{
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: -0.2px;
    text-decoration: none;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  }}

  .badge-play {{
    background: #1A73E8;
    color: #FFFFFF;
    border: 1px solid rgba(255, 255, 255, 0.25);
  }}

  .badge-web {{
    background: #FFFFFF;
    color: #1E293B;
    border: 1px solid #CBD5E1;
  }}

  /* Cards Row */
  .cards-row {{
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 11px;
    height: 396px;
  }}

  .card {{
    border-radius: 20px;
    padding: 10px 6px 0px 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.02);
  }}

  .card-title {{
    font-size: 13.5px;
    font-weight: 800;
    text-align: center;
    line-height: 1.25;
    letter-spacing: -0.3px;
    color: #0F172A;
    margin-bottom: 8px;
    height: 36px;
    display: block;
    width: 100%;
  }}

  .card-title span {{
    color: #2563EB;
  }}

  /* Phone Mockup */
  .phone-wrap {{
    position: relative;
    width: 154px;
    height: 326px;
    display: flex;
    justify-content: center;
  }}

  .phone-img {{
    width: 100%;
    height: auto;
    display: block;
    filter: drop-shadow(0 12px 24px rgba(15, 23, 42, 0.16));
  }}

  /* Washi Tape Decor */
  .tape {{
    position: absolute;
    width: 30px;
    height: 10px;
    background: rgba(160, 207, 255, 0.7);
    backdrop-filter: blur(2px);
    z-index: 10;
    border-radius: 1px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }}

  .tape-left {{
    top: -3px;
    left: 12px;
    transform: rotate(-24deg);
  }}

  .tape-right {{
    top: -3px;
    right: 12px;
    transform: rotate(24deg);
  }}

  /* Handwritten annotations */
  .handwritten {{
    font-family: 'Caveat', cursive;
    font-size: 14.5px;
    font-weight: 700;
    color: #1D4ED8;
    text-shadow: 0 1px 2px rgba(255, 255, 255, 0.9);
    position: absolute;
    z-index: 25;
    pointer-events: none;
    line-height: 1.05;
    display: flex;
    align-items: center;
    gap: 3px;
  }}

  .note-1 {{
    top: 142px;
    left: 2px;
    transform: rotate(-12deg);
  }}

  .note-3 {{
    top: 105px;
    right: 8px;
    transform: rotate(6deg);
  }}

  .note-5 {{
    top: 185px;
    left: 2px;
    transform: rotate(-10deg);
  }}

  /* Floating 3D Badges */
  .float-badge {{
    position: absolute;
    width: 25px;
    height: 25px;
    border-radius: 50%;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2), inset 0 1.5px 2px rgba(255, 255, 255, 0.85);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 20;
    border: 2px solid #FFFFFF;
  }}

  .float-badge svg {{
    width: 13px;
    height: 13px;
    fill: #FFFFFF;
  }}

  .badge-pos-1 {{
    top: 16px;
    right: 4px;
    background: linear-gradient(135deg, #FBBF24, #D97706);
    transform: rotate(10deg);
  }}

  .badge-pos-3 {{
    top: 16px;
    right: 4px;
    background: linear-gradient(135deg, #10B981, #047857);
    transform: rotate(8deg);
  }}

  .badge-pos-5 {{
    top: 16px;
    right: 4px;
    background: linear-gradient(135deg, #6366F1, #4338CA);
    transform: rotate(8deg);
  }}

</style>
</head>
<body>

<div class="canvas">
  <!-- Top Header -->
  <div class="header">
    <div class="header-left">
      <img src="{icon_b64}" class="app-icon" alt="AdDash Icon">
      <div class="brand-title">AdDash</div>
      <div class="header-center">
        <div class="tagline-main"><span class="highlight">Live AdMob Revenue</span> — Real-time earnings, eCPM, impressions & apps</div>
        <div class="tagline-sub">Free & ad-free · Desk HUD with OLED burn-in protection, hourly reports & targets</div>
      </div>
    </div>
    <div class="header-right">
      <div class="badge-btn badge-play">
        <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor">
          <path d="M3.6 1.8c-.4.4-.6 1-.6 1.8v16.8c0 .8.2 1.4.6 1.8l9.4-9.4L3.6 1.8z" fill="#EA4335"/>
          <path d="M16.5 9.5l-3.5 3.5 3.5 3.5 4-2.3c1.1-.6 1.1-1.7 0-2.3l-4-2.4z" fill="#FBBC04"/>
          <path d="M13 13l-9.4 9.4c.5.5 1.3.6 2.2.1l10.7-6.2L13 13z" fill="#34A853"/>
          <path d="M13 13l3.5-3.5-10.7-6.2c-.9-.5-1.7-.4-2.2.1L13 13z" fill="#4285F4"/>
        </svg>
        Google Play
      </div>
      <div class="badge-btn badge-web">
        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="2" y1="12" x2="22" y2="12"></line>
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
        </svg>
        addash.app
      </div>
    </div>
  </div>

  <!-- Cards Row -->
  <div class="cards-row">
    <!-- Card 1: Today Live -->
    <div class="card">
      <div class="card-title">Live earnings in<br><span>real time.</span></div>
      
      <div class="phone-wrap">
        <div class="handwritten note-1">
          <span>updates<br>live!</span>
          <svg width="20" height="18" viewBox="0 0 20 18" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round">
            <path d="M2 3 C 8 2, 14 6, 17 14 M12 13 L 17 14 L 16 8"/>
          </svg>
        </div>
        <div class="float-badge badge-pos-1">
          <svg viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>
        <img src="{p1_today}" class="phone-img" alt="Today Screen">
      </div>
    </div>

    <!-- Card 2: Apps Portfolio -->
    <div class="card">
      <div class="card-title">Every app on<br><span>one single shelf.</span></div>
      <div class="phone-wrap">
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>
        <img src="{p2_apps}" class="phone-img" alt="Apps Screen">
      </div>
    </div>

    <!-- Card 3: Deep Analytics -->
    <div class="card">
      <div class="card-title">Deep analytics &<br><span>30-day trends.</span></div>
      
      <div class="phone-wrap">
        <div class="handwritten note-3">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round">
            <path d="M16 15 C 10 13, 5 9, 2 3 M2 8 L 2 3 L 7 4"/>
          </svg>
          <span>growth<br>curves</span>
        </div>
        <div class="float-badge badge-pos-3">
          <svg viewBox="0 0 24 24"><path d="M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z"/></svg>
        </div>
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>
        <img src="{p3_analytics}" class="phone-img" alt="Analytics Screen">
      </div>
    </div>

    <!-- Card 4: Hourly Reports -->
    <div class="card">
      <div class="card-title">Hourly reports &<br><span>eCPM curves.</span></div>
      <div class="phone-wrap">
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>
        <img src="{p4_reports}" class="phone-img" alt="Reports Screen">
      </div>
    </div>

    <!-- Card 5: Goals & Targets -->
    <div class="card">
      <div class="card-title">Set daily goals &<br><span>watch them fill.</span></div>
      
      <div class="phone-wrap">
        <div class="handwritten note-5">
          <span>weekly<br>streaks</span>
          <svg width="20" height="18" viewBox="0 0 20 18" fill="none" stroke="#1D4ED8" stroke-width="2" stroke-linecap="round">
            <path d="M2 3 C 8 3, 14 7, 17 14 M11 14 L 17 14 L 16 8"/>
          </svg>
        </div>
        <div class="float-badge badge-pos-5">
          <svg viewBox="0 0 24 24"><path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2z"/></svg>
        </div>
        <div class="tape tape-left"></div>
        <div class="tape tape-right"></div>
        <img src="{p5_goal}" class="phone-img" alt="Goal Screen">
      </div>
    </div>
  </div>
</div>

</body>
</html>
"""

html_path = os.path.join(PLAYSTORE_DIR, "feature_graphic.html")
with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Generated HTML template at: {html_path}")

output_png_2x = os.path.join(PLAYSTORE_DIR, "feature_graphic_2x.png")
output_png = os.path.join(PLAYSTORE_DIR, "feature_graphic.png")

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
print(f"Saved final Google Play Feature Graphic (1024x500): {output_png}")
