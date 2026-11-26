import React from "react";
import {
  AbsoluteFill,
  Img,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";

export const Scene07Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const s = spring({ frame, fps, config: { damping: 14, stiffness: 100 } });

  const platforms = [
    { name: "Android", sub: "App + Floating Bubble" },
    { name: "iOS", sub: "iPhone & iPad" },
    { name: "Desktop", sub: "macOS · Win · Linux" },
    { name: "Raspberry Pi", sub: "ARM64 Kiosk .deb" },
  ];

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        paddingBottom: 60,
      }}
    >
      <div
        style={{
          opacity: s,
          transform: `scale(${interpolate(s, [0, 1], [0.75, 1])})`,
          borderRadius: 42,
          overflow: "hidden",
          boxShadow: "0 24px 70px rgba(217, 37, 42, 0.45)",
          border: "2px solid rgba(255,255,255,0.22)",
          marginBottom: 28,
        }}
      >
        <Img
          src={staticFile("assets/app_icon_512.png")}
          style={{ width: 170, height: 170, display: "block" }}
        />
      </div>

      <div
        style={{
          opacity: s,
          fontSize: 78,
          fontWeight: 900,
          letterSpacing: -2,
          color: "#FFFFFF",
          textAlign: "center",
        }}
      >
        Stop Refreshing. Watch It Live.
      </div>
      <div
        style={{
          opacity: s,
          fontSize: 30,
          color: "#CBD5E1",
          marginTop: 12,
        }}
      >
        AdDash — Live AdMob Revenue, Floating Bubble & Fullscreen TV HUD
      </div>

      <div style={{ display: "flex", gap: 22, marginTop: 44 }}>
        {platforms.map((p, i) => {
          const ps = spring({
            frame: frame - (14 + i * 8),
            fps,
            config: { damping: 15, stiffness: 110 },
          });
          return (
            <div
              key={p.name}
              style={{
                opacity: ps,
                transform: `translateY(${interpolate(ps, [0, 1], [20, 0])}px)`,
                background: "rgba(255,255,255,0.07)",
                border: "1px solid rgba(255,255,255,0.18)",
                borderRadius: 18,
                padding: "18px 32px",
                textAlign: "center",
                minWidth: 220,
              }}
            >
              <div style={{ fontSize: 24, fontWeight: 800, color: "#FFFFFF" }}>
                {p.name}
              </div>
              <div
                style={{
                  fontSize: 13,
                  fontWeight: 700,
                  color: "#94A3B8",
                  textTransform: "uppercase",
                  letterSpacing: 0.8,
                  marginTop: 4,
                }}
              >
                {p.sub}
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          marginTop: 40,
          fontSize: 24,
          fontWeight: 700,
          color: "#FCA5A5",
        }}
      >
        github.com/kandreashish/AdMob-Revenue
      </div>
    </AbsoluteFill>
  );
};
