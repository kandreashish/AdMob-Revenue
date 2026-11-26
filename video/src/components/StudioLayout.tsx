import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";

export interface CaptionCue {
  text: string;
  startSec: number;
  endSec: number;
  startFrame: number;
  endFrame: number;
}

interface StudioLayoutProps {
  badge?: string;
  title?: string;
  accentColor?: string;
  captions?: CaptionCue[];
  globalProgress: number;
  children: React.ReactNode;
}

export const StudioLayout: React.FC<StudioLayoutProps> = ({
  badge,
  title,
  accentColor = "#D9252A",
  captions = [],
  globalProgress,
  children,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerSpring = spring({
    frame,
    fps,
    config: { damping: 16, stiffness: 120 },
  });

  const orbX = Math.sin(frame / 140) * 90;
  const orbY = Math.cos(frame / 180) * 50;

  const activeCaption = captions.find(
    (c) => frame >= c.startFrame && frame <= c.endFrame
  );

  return (
    <AbsoluteFill
      style={{
        background:
          "radial-gradient(120% 100% at 50% 0%, #161E31 0%, #090D16 70%, #05070C 100%)",
        fontFamily:
          "'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif",
        color: "#F8FAFC",
        overflow: "hidden",
      }}
    >
      {/* Ambient animated glow */}
      <div
        style={{
          position: "absolute",
          width: 1100,
          height: 700,
          left: 410 + orbX,
          top: -260 + orbY,
          borderRadius: "50%",
          background: `radial-gradient(circle, ${accentColor}38 0%, transparent 70%)`,
          filter: "blur(90px)",
          pointerEvents: "none",
        }}
      />

      {/* Subtle studio grid */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          backgroundImage:
            "linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)",
          backgroundSize: "64px 64px",
          pointerEvents: "none",
        }}
      />

      {/* Header bar */}
      {badge && title && (
        <div
          style={{
            position: "absolute",
            top: 46,
            left: 84,
            zIndex: 20,
            opacity: interpolate(headerSpring, [0, 1], [0, 1]),
            transform: `translateY(${interpolate(headerSpring, [0, 1], [-18, 0])}px)`,
          }}
        >
          <div
            style={{
              display: "inline-block",
              padding: "6px 16px",
              borderRadius: 999,
              background: accentColor,
              fontSize: 15,
              fontWeight: 800,
              letterSpacing: 1.4,
              textTransform: "uppercase",
              boxShadow: `0 6px 24px ${accentColor}55`,
              marginBottom: 10,
            }}
          >
            {badge}
          </div>
          <div
            style={{
              fontSize: 44,
              fontWeight: 800,
              letterSpacing: -0.8,
              color: "#FFFFFF",
              textShadow: "0 4px 20px rgba(0,0,0,0.4)",
            }}
          >
            {title}
          </div>
        </div>
      )}

      {/* Scene content */}
      <AbsoluteFill>{children}</AbsoluteFill>

      {/* Kinetic bottom caption pill */}
      {activeCaption && (
        <div
          style={{
            position: "absolute",
            bottom: 34,
            left: 0,
            right: 0,
            display: "flex",
            justifyContent: "center",
            zIndex: 40,
            pointerEvents: "none",
          }}
        >
          <div
            style={{
              background: "rgba(9, 13, 22, 0.84)",
              backdropFilter: "blur(14px)",
              border: "1px solid rgba(255,255,255,0.14)",
              borderRadius: 16,
              padding: "12px 26px",
              fontSize: 23,
              fontWeight: 600,
              color: "#F8FAFC",
              maxWidth: 1280,
              textAlign: "center",
              boxShadow: "0 12px 32px rgba(0,0,0,0.45)",
            }}
          >
            {activeCaption.text}
          </div>
        </div>
      )}

      {/* Bottom progress bar */}
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          height: 5,
          background: "rgba(255,255,255,0.08)",
          zIndex: 50,
        }}
      >
        <div
          style={{
            width: `${Math.min(100, Math.max(0, globalProgress * 100))}%`,
            height: "100%",
            background: "linear-gradient(90deg, #D9252A 0%, #F59E0B 100%)",
          }}
        />
      </div>
    </AbsoluteFill>
  );
};
