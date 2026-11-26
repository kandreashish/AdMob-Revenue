import React from "react";
import {
  AbsoluteFill,
  Img,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";

export const Scene06AnalyticsWallet: React.FC<{ durationFrames: number }> = ({
  durationFrames,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const items = [
    { src: "assets/phone_2_apps.png", label: "All Apps Shelf", crop: false },
    { src: "assets/phone_3_analytics.png", label: "7/30/90d Analytics", crop: false },
    { src: "assets/phone_4_reports.png", label: "Hourly & Format Reports", crop: false },
    { src: "assets/07_wallet.png", label: "Wallet & Payouts", crop: true },
  ];

  const active = Math.min(3, Math.floor((frame / Math.max(1, durationFrames)) * 4));

  return (
    <AbsoluteFill
      style={{
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: 44,
        paddingTop: 95,
      }}
    >
      {items.map((item, idx) => {
        const isOn = idx === active;
        const s = spring({
          frame: frame - idx * 8,
          fps,
          config: { damping: 16, stiffness: 100 },
        });
        const scale = isOn ? 1.06 : 0.94;
        return (
          <div
            key={item.label}
            style={{
              opacity: s,
              transform: `scale(${scale}) translateY(${isOn ? -18 : 0}px)`,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              transition: "transform 0.35s ease",
              filter: isOn
                ? "drop-shadow(0 28px 60px rgba(217, 37, 42, 0.35))"
                : "drop-shadow(0 18px 40px rgba(0,0,0,0.5))",
            }}
          >
            {item.crop ? (
              <div
                style={{
                  width: 342,
                  height: 675,
                  borderRadius: 42,
                  overflow: "hidden",
                  border: "5px solid #0F172A",
                  background: "#0F172A",
                }}
              >
                <Img
                  src={staticFile(item.src)}
                  style={{
                    width: "100%",
                    height: "auto",
                    marginTop: -125,
                    display: "block",
                  }}
                />
              </div>
            ) : (
              <Img
                src={staticFile(item.src)}
                style={{ height: 675, width: "auto", display: "block" }}
              />
            )}
            <div
              style={{
                marginTop: 18,
                fontSize: 21,
                fontWeight: 800,
                color: isOn ? "#FFFFFF" : "#94A3B8",
              }}
            >
              {item.label}
            </div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
