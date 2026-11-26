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

export const Scene02Refresh: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const leftIn = spring({ frame, fps, config: { damping: 16, stiffness: 100 } });
  const rightIn = spring({
    frame: frame - 18,
    fps,
    config: { damping: 16, stiffness: 95 },
  });

  const spinAngle = (frame * 5) % 360;
  const cmdPulse = Math.sin(frame / 18) > 0.6 ? 0.92 : 1;

  const intervals = ["30s", "1 min", "2 min", "5 min"];
  const activeIdx = Math.floor(frame / 150) % intervals.length;

  return (
    <AbsoluteFill
      style={{
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "130px 90px 80px 90px",
      }}
    >
      {/* Left: 3D tilted frustrating AdMob browser */}
      <div
        style={{
          width: 720,
          opacity: leftIn,
          transform: `perspective(1400px) rotateY(8deg) translateX(${interpolate(
            leftIn,
            [0, 1],
            [-60, 0]
          )}px)`,
        }}
      >
        <div
          style={{
            background: "#1A202C",
            borderRadius: 22,
            border: "1px solid rgba(255,255,255,0.14)",
            overflow: "hidden",
            boxShadow: "0 30px 70px rgba(0,0,0,0.55)",
          }}
        >
          <div
            style={{
              height: 48,
              background: "#262D3D",
              display: "flex",
              alignItems: "center",
              padding: "0 18px",
              gap: 12,
              borderBottom: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <div style={{ display: "flex", gap: 8 }}>
              <div style={{ width: 12, height: 12, borderRadius: "50%", background: "#EF4444" }} />
              <div style={{ width: 12, height: 12, borderRadius: "50%", background: "#F59E0B" }} />
              <div style={{ width: 12, height: 12, borderRadius: "50%", background: "#10B981" }} />
            </div>
            <div
              style={{
                flex: 1,
                height: 30,
                background: "#131722",
                borderRadius: 8,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "0 12px",
                fontSize: 14,
                color: "#94A3B8",
              }}
            >
              <span>https://apps.admob.com/v2/home</span>
              <span
                style={{
                  background: "rgba(239,68,68,0.2)",
                  color: "#FCA5A5",
                  padding: "2px 10px",
                  borderRadius: 999,
                  fontSize: 11,
                  fontWeight: 800,
                }}
              >
                STALE DATA
              </span>
            </div>
          </div>

          <div
            style={{
              padding: 36,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 22,
            }}
          >
            <div style={{ fontSize: 24, fontWeight: 700, color: "#F8FAFC" }}>
              The Old Way: Manual Browser Refreshes
            </div>
            <div
              style={{
                width: "100%",
                padding: "14px 20px",
                borderRadius: 12,
                background: "rgba(239,68,68,0.14)",
                border: "1px solid rgba(239,68,68,0.35)",
                color: "#FECACA",
                fontSize: 16,
                fontWeight: 600,
                textAlign: "center",
              }}
            >
              Session expired or stale — Reload page to fetch latest earnings
            </div>

            {/* Spinning loader */}
            <div
              style={{
                width: 68,
                height: 68,
                borderRadius: "50%",
                border: "6px solid rgba(255,255,255,0.08)",
                borderTopColor: "#EF4444",
                transform: `rotate(${spinAngle}deg)`,
                margin: "14px 0",
              }}
            />

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                transform: `scale(${cmdPulse})`,
              }}
            >
              <span
                style={{
                  background: "#0F172A",
                  border: "1px solid #475569",
                  borderBottomWidth: 3,
                  padding: "8px 16px",
                  borderRadius: 8,
                  fontWeight: 700,
                  fontSize: 16,
                }}
              >
                ⌘ Cmd
              </span>
              <span style={{ color: "#94A3B8", fontWeight: 700 }}>+</span>
              <span
                style={{
                  background: "#0F172A",
                  border: "1px solid #475569",
                  borderBottomWidth: 3,
                  padding: "8px 16px",
                  borderRadius: 8,
                  fontWeight: 700,
                  fontSize: 16,
                }}
              >
                R
              </span>
              <span style={{ color: "#FCA5A5", fontSize: 16, fontWeight: 600, marginLeft: 8 }}>
                Repeated 20× a day
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Center: Auto-Refresh selector card */}
      <div
        style={{
          opacity: rightIn,
          transform: `scale(${interpolate(rightIn, [0, 1], [0.85, 1])})`,
          background: "rgba(15, 23, 42, 0.9)",
          border: "2px solid rgba(52, 211, 153, 0.55)",
          borderRadius: 24,
          padding: 26,
          width: 310,
          boxShadow: "0 24px 60px rgba(16, 185, 129, 0.2)",
        }}
      >
        <div
          style={{
            fontSize: 14,
            fontWeight: 800,
            letterSpacing: 1.4,
            color: "#6EE7B7",
            textAlign: "center",
            marginBottom: 16,
          }}
        >
          AUTO-REFRESH INTERVAL
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          {intervals.map((iv, idx) => {
            const active = idx === activeIdx;
            return (
              <div
                key={iv}
                style={{
                  padding: "12px 18px",
                  borderRadius: 12,
                  background: active ? "#10B981" : "rgba(255,255,255,0.06)",
                  color: active ? "#042F2E" : "#E2E8F0",
                  fontWeight: 800,
                  fontSize: 19,
                  textAlign: "center",
                  transition: "all 0.2s",
                }}
              >
                Every {iv}
              </div>
            );
          })}
        </div>
        <div
          style={{
            marginTop: 16,
            fontSize: 14,
            color: "#94A3B8",
            textAlign: "center",
            fontWeight: 600,
          }}
        >
          Local Cache • Instant Launch
        </div>
      </div>

      {/* Right: 3D tilted AdDash Phone */}
      <div
        style={{
          opacity: rightIn,
          transform: `perspective(1400px) rotateY(-10deg) translateX(${interpolate(
            rightIn,
            [0, 1],
            [60, 0]
          )}px)`,
          filter: "drop-shadow(0 30px 60px rgba(0,0,0,0.6))",
        }}
      >
        <Img
          src={staticFile("assets/phone_1_today.png")}
          style={{ height: 730, width: "auto", display: "block" }}
        />
      </div>
    </AbsoluteFill>
  );
};
