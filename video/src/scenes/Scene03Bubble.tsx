import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";

export const Scene03Bubble: React.FC<{ durationFrames: number }> = ({
  durationFrames,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const progress = Math.min(1, frame / Math.max(1, durationFrames));

  // Animate bubble across 4 goal states (Red -> Amber -> Blue -> Green)
  let bx = 68;
  let by = 220;
  let gradient = "linear-gradient(135deg, #D93025, #EA4335)";
  let amount = "$312.40";
  let goalLabel = "23% Goal";

  if (progress < 0.25) {
    const p = progress / 0.25;
    bx = 68;
    by = 190 + p * 170;
  } else if (progress < 0.5) {
    const p = (progress - 0.25) / 0.25;
    bx = interpolate(p, [0, 1], [68, 352]);
    by = 360;
    gradient = "linear-gradient(135deg, #E37400, #FBBC04)";
    amount = "$684.10";
    goalLabel = "50% Goal";
  } else if (progress < 0.75) {
    const p = (progress - 0.5) / 0.25;
    bx = 352;
    by = interpolate(p, [0, 1], [360, 210]);
    gradient = "linear-gradient(135deg, #3367D6, #4285F4)";
    amount = "$1,209";
    goalLabel = "88% Goal";
  } else {
    bx = 352;
    by = 210;
    gradient = "linear-gradient(135deg, #1E8E3E, #34A853)";
    amount = "$1,412";
    goalLabel = "103% Goal";
  }

  const sizePx = 92 + 18 * Math.sin(frame / 45);
  const idleOpacity = 0.78 + 0.22 * Math.cos(frame / 55);

  const cards = [
    {
      title: "Always On Top",
      desc: "Floats over any app, snaps to screen edges, and remembers position.",
    },
    {
      title: "Goal-Aware Gradient Colors",
      desc: "Red (<33%) → Amber (33–65%) → Blue (66–99%) → Green (100% Goal Hit).",
    },
    {
      title: "Custom Size & Opacity",
      desc: "Adjust diameter (36dp–96dp) and idle opacity (20%–100%) live.",
    },
    {
      title: "Drag to ✕ & Tap to Open",
      desc: "Auto-refreshes while screen is on. Drag to bottom target to hide.",
    },
  ];

  return (
    <AbsoluteFill
      style={{
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "130px 110px 80px 140px",
      }}
    >
      {/* Left: 3D-angled Android Phone with Floating Bubble */}
      <div
        style={{
          width: 420,
          height: 760,
          borderRadius: 48,
          background: "#0F172A",
          border: "6px solid #475569",
          boxShadow: "0 35px 80px rgba(0,0,0,0.65)",
          position: "relative",
          overflow: "hidden",
          transform: "perspective(1400px) rotateY(9deg)",
        }}
      >
        {/* Background app mockup */}
        <div style={{ padding: "32px 24px" }}>
          <div
            style={{
              fontSize: 16,
              fontWeight: 700,
              color: "#64748B",
              marginBottom: 20,
            }}
          >
            Any App You're Using
          </div>
          {Array.from({ length: 7 }).map((_, i) => (
            <div
              key={i}
              style={{
                height: 62,
                borderRadius: 16,
                background: "rgba(51, 65, 85, 0.45)",
                marginBottom: 16,
              }}
            />
          ))}
        </div>

        {/* Floating Bubble */}
        <div
          style={{
            position: "absolute",
            left: bx - sizePx / 2,
            top: by - sizePx / 2,
            width: sizePx,
            height: sizePx,
            borderRadius: "50%",
            background: gradient,
            opacity: idleOpacity,
            border: "3px solid rgba(255,255,255,0.88)",
            boxShadow: "0 14px 34px rgba(0,0,0,0.55)",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <div style={{ fontSize: 19, fontWeight: 900, color: "#FFF" }}>
            {amount}
          </div>
          <div style={{ fontSize: 11, fontWeight: 700, color: "#F1F5F9" }}>
            {goalLabel}
          </div>
        </div>

        {/* Bottom X dismiss target */}
        <div
          style={{
            position: "absolute",
            bottom: 26,
            left: "50%",
            transform: "translateX(-50%)",
            width: 56,
            height: 56,
            borderRadius: "50%",
            background: "rgba(15,23,42,0.92)",
            border: "2px solid rgba(255,255,255,0.7)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 24,
            fontWeight: 700,
            color: "#FFF",
          }}
        >
          ×
        </div>
      </div>

      {/* Right: Feature Cards */}
      <div style={{ width: 920, display: "flex", flexDirection: "column", gap: 22 }}>
        {cards.map((c, idx) => {
          const s = spring({
            frame: frame - idx * 14,
            fps,
            config: { damping: 16, stiffness: 105 },
          });
          return (
            <div
              key={c.title}
              style={{
                opacity: s,
                transform: `translateX(${interpolate(s, [0, 1], [40, 0])}px)`,
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.12)",
                borderRadius: 22,
                padding: "24px 32px",
              }}
            >
              <div style={{ fontSize: 27, fontWeight: 800, color: "#FFFFFF" }}>
                {c.title}
              </div>
              <div style={{ fontSize: 20, color: "#CBD5E1", marginTop: 6 }}>
                {c.desc}
              </div>
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};
