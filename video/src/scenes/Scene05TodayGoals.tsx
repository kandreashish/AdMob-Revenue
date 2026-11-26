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

export const Scene05TodayGoals: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const s = spring({ frame, fps, config: { damping: 16, stiffness: 95 } });
  const floatY = Math.sin(frame / 45) * 10;

  const midCards = [
    { title: "Today & Horizons", desc: "Today, Yesterday, 7d, Month & Custom" },
    { title: "Core Ad Metrics", desc: "eCPM, Impressions, Clicks & Match Rate" },
    { title: "Daily Goal Ring", desc: "Live progress % with 7-day streak" },
  ];

  return (
    <AbsoluteFill
      style={{
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "130px 110px 80px 110px",
      }}
    >
      <div
        style={{
          opacity: s,
          transform: `perspective(1400px) rotateY(10deg) translateY(${floatY}px)`,
          filter: "drop-shadow(0 30px 65px rgba(0,0,0,0.6))",
        }}
      >
        <Img
          src={staticFile("assets/phone_1_today.png")}
          style={{ height: 740, width: "auto", display: "block" }}
        />
      </div>

      <div style={{ width: 520, display: "flex", flexDirection: "column", gap: 24 }}>
        {midCards.map((c, i) => {
          const cs = spring({
            frame: frame - (12 + i * 12),
            fps,
            config: { damping: 16, stiffness: 105 },
          });
          return (
            <div
              key={c.title}
              style={{
                opacity: cs,
                transform: `scale(${interpolate(cs, [0, 1], [0.9, 1])})`,
                background: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.14)",
                borderRadius: 22,
                padding: "24px 28px",
                textAlign: "center",
              }}
            >
              <div style={{ fontSize: 27, fontWeight: 800, color: "#FFFFFF" }}>
                {c.title}
              </div>
              <div style={{ fontSize: 19, color: "#CBD5E1", marginTop: 6 }}>
                {c.desc}
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          opacity: s,
          transform: `perspective(1400px) rotateY(-10deg) translateY(${-floatY}px)`,
          filter: "drop-shadow(0 30px 65px rgba(0,0,0,0.6))",
        }}
      >
        <Img
          src={staticFile("assets/phone_5_goal.png")}
          style={{ height: 740, width: "auto", display: "block" }}
        />
      </div>
    </AbsoluteFill>
  );
};
