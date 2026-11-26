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

export const Scene01Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const iconSpring = spring({
    frame,
    fps,
    config: { damping: 12, stiffness: 110 },
  });
  const titleSpring = spring({
    frame: frame - 12,
    fps,
    config: { damping: 16, stiffness: 100 },
  });
  const pillSpring = spring({
    frame: frame - 26,
    fps,
    config: { damping: 14, stiffness: 120 },
  });

  const pulse = 0.6 + 0.4 * Math.sin(frame / 12);

  const chips = [
    "Auto-Refresh (30s–5m)",
    "Always-On Floating Bubble",
    "Fullscreen TV & Pi HUD",
  ];

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        paddingBottom: 60,
      }}
    >
      {/* Glowing App Icon */}
      <div
        style={{
          transform: `scale(${interpolate(iconSpring, [0, 1], [0.55, 1])})`,
          opacity: iconSpring,
          borderRadius: 44,
          overflow: "hidden",
          boxShadow: "0 24px 70px rgba(217, 37, 42, 0.45)",
          border: "2px solid rgba(255,255,255,0.22)",
          marginBottom: 28,
        }}
      >
        <Img
          src={staticFile("assets/app_icon_512.png")}
          style={{ width: 176, height: 176, display: "block" }}
        />
      </div>

      {/* Hero Title */}
      <div
        style={{
          opacity: titleSpring,
          transform: `translateY(${interpolate(titleSpring, [0, 1], [28, 0])}px)`,
          textAlign: "center",
        }}
      >
        <div
          style={{
            fontSize: 102,
            fontWeight: 900,
            letterSpacing: -2.5,
            lineHeight: 1.02,
            background: "linear-gradient(180deg, #FFFFFF 0%, #CBD5E1 100%)",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
          }}
        >
          AdDash
        </div>
        <div
          style={{
            fontSize: 34,
            fontWeight: 500,
            color: "#94A3B8",
            marginTop: 12,
          }}
        >
          Live AdMob Revenue — Without Refreshing a Browser Tab
        </div>
      </div>

      {/* Live Earnings Pill */}
      <div
        style={{
          marginTop: 36,
          opacity: pillSpring,
          transform: `scale(${interpolate(pillSpring, [0, 1], [0.85, 1])})`,
          display: "flex",
          alignItems: "center",
          gap: 16,
          background: "rgba(22, 35, 58, 0.85)",
          border: "1.5px solid rgba(129, 201, 149, 0.45)",
          borderRadius: 999,
          padding: "16px 34px",
          boxShadow: "0 18px 44px rgba(0,0,0,0.45)",
        }}
      >
        <div
          style={{
            width: 14,
            height: 14,
            borderRadius: "50%",
            background: "#34D399",
            opacity: pulse,
            boxShadow: "0 0 14px #34D399",
          }}
        />
        <span style={{ fontSize: 28, fontWeight: 800, color: "#FFFFFF" }}>
          LIVE &nbsp;$1,209.79
        </span>
        <span style={{ fontSize: 24, fontWeight: 700, color: "#81C995" }}>
          ▲ +12.3%
        </span>
      </div>

      {/* 3 Feature Chips */}
      <div
        style={{
          display: "flex",
          gap: 24,
          marginTop: 48,
        }}
      >
        {chips.map((label, i) => {
          const s = spring({
            frame: frame - (40 + i * 10),
            fps,
            config: { damping: 15, stiffness: 110 },
          });
          return (
            <div
              key={label}
              style={{
                opacity: s,
                transform: `translateY(${interpolate(s, [0, 1], [22, 0])}px)`,
                background: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.14)",
                borderRadius: 16,
                padding: "16px 28px",
                fontSize: 21,
                fontWeight: 700,
                color: "#E2E8F0",
              }}
            >
              {label}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};
