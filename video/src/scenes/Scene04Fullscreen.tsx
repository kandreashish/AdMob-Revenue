import React from "react";
import {
  AbsoluteFill,
  Audio,
  Sequence,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";

const PARTICLES = Array.from({ length: 48 }).map((_, i) => ({
  angle: (i / 48) * Math.PI * 2,
  speed: 140 + ((i * 73) % 240),
  color: ["#81C995", "#FBBC04", "#8AB4F8", "#F28B82", "#FFFFFF"][i % 5],
  size: 8 + (i % 7),
}));

export const Scene04Fullscreen: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const zoom = spring({ frame, fps, config: { damping: 18, stiffness: 80 } });

  // Live revenue tick every 420 frames (7 seconds)
  const tickInterval = 420;
  const ticks = Math.floor(frame / tickInterval);
  const tickFrame = frame % tickInterval;

  const bounce =
    ticks > 0 && tickFrame < 24
      ? 1 + 0.06 * Math.sin((tickFrame / 24) * Math.PI)
      : 1;

  const revenue = 1209.79 + ticks * 4.56;
  const delta = 132.4 + ticks * 4.56;

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        paddingTop: 75,
      }}
    >
      {/* Play chime SFX on each revenue tick */}
      {[1, 2, 3, 4].map((n) => (
        <Sequence key={n} from={n * tickInterval} durationInFrames={45}>
          <Audio src={staticFile("audio/chime.wav")} volume={0.45} />
        </Sequence>
      ))}

      {/* TV / Monitor Bezel */}
      <div
        style={{
          width: 1420,
          height: 690,
          borderRadius: 28,
          background:
            "radial-gradient(circle at 50% 45%, #16233A 0%, #0B0F17 85%)",
          border: "8px solid #334155",
          boxShadow: "0 40px 100px rgba(0,0,0,0.75)",
          transform: `scale(${interpolate(zoom, [0, 1], [0.88, 1])})`,
          position: "relative",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            marginBottom: 16,
          }}
        >
          <div
            style={{
              width: 12,
              height: 12,
              borderRadius: "50%",
              background: "#81C995",
              boxShadow: "0 0 12px #81C995",
            }}
          />
          <span
            style={{
              fontSize: 20,
              fontWeight: 800,
              letterSpacing: 2,
              color: "#81C995",
            }}
          >
            LIVE · TODAY
          </span>
        </div>

        <div
          style={{
            fontSize: 136,
            fontWeight: 900,
            color: "#E0E0E0",
            letterSpacing: -3,
            transform: `scale(${bounce})`,
          }}
        >
          ${revenue.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </div>

        <div
          style={{
            fontSize: 30,
            fontWeight: 700,
            color: "#81C995",
            marginTop: 12,
          }}
        >
          ▲ ${delta.toFixed(2)} (12.3%) vs yesterday
        </div>

        <div style={{ display: "flex", gap: 110, marginTop: 54 }}>
          {[
            ["eCPM", "$14.82"],
            ["Impressions", "81.6K"],
            ["Daily Goal", "88%"],
          ].map(([k, v]) => (
            <div key={k} style={{ textAlign: "center" }}>
              <div style={{ fontSize: 18, color: "#9AA0A6", fontWeight: 600 }}>
                {k}
              </div>
              <div style={{ fontSize: 38, color: "#E8EAED", fontWeight: 800, marginTop: 4 }}>
                {v}
              </div>
            </div>
          ))}
        </div>

        {/* Confetti burst */}
        {ticks > 0 &&
          tickFrame < 90 &&
          PARTICLES.map((p, idx) => {
            const t = tickFrame / 60;
            const px = Math.cos(p.angle) * p.speed * t;
            const py = Math.sin(p.angle) * p.speed * t + 140 * t * t;
            const op = interpolate(tickFrame, [0, 90], [1, 0]);
            return (
              <div
                key={idx}
                style={{
                  position: "absolute",
                  left: "50%",
                  top: "45%",
                  width: p.size,
                  height: p.size * 0.6,
                  background: p.color,
                  opacity: op,
                  transform: `translate(${px}px, ${py}px) rotate(${tickFrame * 6}deg)`,
                  borderRadius: 2,
                }}
              />
            );
          })}
      </div>

      {/* Stand */}
      <div style={{ width: 90, height: 34, background: "#334155" }} />
      <div
        style={{
          width: 360,
          height: 14,
          borderRadius: 8,
          background: "#475569",
        }}
      />

      <div
        style={{
          marginTop: 22,
          fontSize: 21,
          fontWeight: 600,
          color: "#CBD5E1",
        }}
      >
        OLED Burn-In Safe &nbsp;•&nbsp; Always-On Landscape &nbsp;•&nbsp; Desktop Multi-Monitor &nbsp;•&nbsp; Raspberry Pi Kiosk
      </div>
    </AbsoluteFill>
  );
};
